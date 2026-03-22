package com.xbk.agent.framework.roleplay.infrastructure.agentframework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.roleplay.domain.memory.CamelDialogueTurn;
import com.xbk.agent.framework.roleplay.domain.role.CamelRoleContract;
import com.xbk.agent.framework.roleplay.domain.role.CamelRoleType;
import com.xbk.agent.framework.roleplay.infrastructure.agentframework.support.CamelTranscriptStateSupport;
import com.xbk.agent.framework.roleplay.support.CamelPromptTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 交易员 handoff 节点
 *
 * 职责：读取程序员 baton，生成交易员回复，并把控制权交回状态图
 *
 * @author xiexu
 */
public class CamelTraderHandoffNode implements AsyncNodeAction {

    private static final String REQUEST_PREFIX = "camel-flow-trader-";

    /**
     * 底层统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 交易员角色契约。
     */
    private final CamelRoleContract contract;

    /**
     * 创建交易员 handoff 节点。
     *
     * @param agentLlmGateway 统一 LLM 网关
     * @param contract 交易员契约
     */
    public CamelTraderHandoffNode(AgentLlmGateway agentLlmGateway, CamelRoleContract contract) {
        this.agentLlmGateway = agentLlmGateway;
        this.contract = contract;
    }

    /**
     * 执行交易员节点。
     *
     * @param state 全局状态
     * @return 写回状态
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String conversationId = state.value("conversation_id", "");
        String task = state.value("input", "");
        String programmerMessage = state.value("message_for_trader", "");
        List<Message> messages = new ArrayList<Message>();
        // FlowAgent 版不再重放完整字符串历史，而是只传当前交易员真正需要消费的 baton：
        // 任务基线 + 程序员最新产物。
        messages.add(buildMessage(conversationId, MessageRole.SYSTEM, contract.getSystemPrompt(), contract.getAgentName()));
        messages.add(buildMessage(conversationId, MessageRole.USER, CamelPromptTemplates.traderInitialPrompt(task), "task"));
        if (!programmerMessage.isBlank()) {
            messages.add(buildMessage(conversationId, MessageRole.USER, programmerMessage, CamelRoleType.PROGRAMMER.getDisplayName()));
        }
        String rawOutput = chat(conversationId, List.copyOf(messages));
        List<CamelDialogueTurn> transcript = extractTranscript(state);
        int traderTurnCount = countRoleTurns(transcript, CamelRoleType.TRADER) + 1;
        String output = CamelPromptTemplates.normalizeTraderOutput(rawOutput, traderTurnCount);
        transcript.add(new CamelDialogueTurn(transcript.size() + 1, CamelRoleType.TRADER, output));
        boolean done = CamelPromptTemplates.containsTaskDoneMarker(rawOutput)
                && CamelPromptTemplates.canTraderFinish(traderTurnCount);
        int nextTurnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0)) + 1;
        // 交易员节点负责把“下一棒要交给谁”和“程序员下一轮应该收到什么”显式写回状态。
        return CompletableFuture.completedFuture(Map.of(
                "last_trader_output", output,
                "message_for_programmer", output,
                "active_role", done ? CamelRoleType.TRADER.getStateValue() : CamelRoleType.PROGRAMMER.getStateValue(),
                "done", Boolean.valueOf(done),
                "stop_reason", done ? rawOutput : state.value("stop_reason", ""),
                "turn_count", Integer.valueOf(nextTurnCount),
                "transcript", CamelTranscriptStateSupport.toStateTranscript(transcript)));
    }

    /**
     * 提取 transcript。
     *
     * @param state 全局状态
     * @return transcript 副本
     */
    private List<CamelDialogueTurn> extractTranscript(OverAllState state) {
        return CamelTranscriptStateSupport.readTranscript(state.value("transcript").orElse(List.of()));
    }

    /**
     * 统计某个角色在 transcript 中的累计发言次数。
     *
     * @param transcript transcript 副本
     * @param roleType 角色类型
     * @return 发言次数
     */
    private int countRoleTurns(List<CamelDialogueTurn> transcript, CamelRoleType roleType) {
        int count = 0;
        for (CamelDialogueTurn turn : transcript) {
            if (turn.getRoleType() == roleType) {
                count++;
            }
        }
        return count;
    }

    /**
     * 调用统一网关并提取文本结果。
     *
     * @param conversationId 会话标识
     * @param messages 当前节点消息
     * @return 文本结果
     */
    private String chat(String conversationId, List<Message> messages) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(REQUEST_PREFIX + UUID.randomUUID())
                .conversationId(defaultText(conversationId))
                .messages(messages)
                .build());
        return extractText(response);
    }

    /**
     * 构造统一消息。
     *
     * @param conversationId 会话标识
     * @param role 消息角色
     * @param content 消息内容
     * @param name 角色名称
     * @return 统一消息
     */
    private Message buildMessage(String conversationId, MessageRole role, String content, String name) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(defaultText(conversationId))
                .role(role)
                .name(name)
                .content(defaultText(content))
                .build();
    }

    /**
     * 提取响应文本。
     *
     * @param response LLM 响应
     * @return 文本结果
     */
    private String extractText(LlmResponse response) {
        if (response.getOutputMessage() != null && response.getOutputMessage().getContent() != null) {
            return response.getOutputMessage().getContent();
        }
        return response.getRawText();
    }

    /**
     * 返回非空文本，避免状态空值污染请求。
     *
     * @param value 原始值
     * @return 非空文本
     */
    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}

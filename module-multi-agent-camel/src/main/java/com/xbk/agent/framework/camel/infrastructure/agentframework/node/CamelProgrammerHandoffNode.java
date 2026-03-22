package com.xbk.agent.framework.camel.infrastructure.agentframework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.camel.domain.memory.CamelDialogueTurn;
import com.xbk.agent.framework.camel.domain.role.CamelRoleContract;
import com.xbk.agent.framework.camel.domain.role.CamelRoleType;
import com.xbk.agent.framework.camel.infrastructure.agentframework.support.CamelTranscriptStateSupport;
import com.xbk.agent.framework.camel.support.CamelPromptTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 程序员 handoff 节点
 *
 * 职责：读取交易员 baton，生成程序员代码输出，并把控制权交回状态图
 *
 * @author xiexu
 */
public class CamelProgrammerHandoffNode implements AsyncNodeAction {

    private static final String REQUEST_PREFIX = "camel-flow-programmer-";

    /**
     * 底层统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 程序员角色契约。
     */
    private final CamelRoleContract contract;

    /**
     * 创建程序员 handoff 节点。
     *
     * @param agentLlmGateway 统一 LLM 网关
     * @param contract 程序员契约
     */
    public CamelProgrammerHandoffNode(AgentLlmGateway agentLlmGateway, CamelRoleContract contract) {
        this.agentLlmGateway = agentLlmGateway;
        this.contract = contract;
    }

    /**
     * 执行程序员节点。
     *
     * @param state 全局状态
     * @return 写回状态
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String conversationId = state.value("conversation_id", "");
        String task = state.value("input", "");
        String traderMessage = state.value("message_for_programmer", "");
        List<Message> messages = new ArrayList<Message>();
        // 程序员节点消费的 baton 更小，只需要任务基线和交易员最新需求，
        // 不必像手写版那样重新拼整段 transcript。
        messages.add(buildMessage(conversationId, MessageRole.SYSTEM, contract.getSystemPrompt(), contract.getAgentName()));
        messages.add(buildMessage(conversationId, MessageRole.USER, CamelPromptTemplates.programmerInitialPrompt(task), "task"));
        if (!traderMessage.isBlank()) {
            messages.add(buildMessage(conversationId, MessageRole.USER, traderMessage, CamelRoleType.TRADER.getDisplayName()));
        }
        String output = chat(conversationId, List.copyOf(messages));
        String cleanedScript = CamelPromptTemplates.stripTaskDoneMarker(output);
        List<CamelDialogueTurn> transcript = extractTranscript(state);
        // 程序员没有结束权，即使它违规输出了终止标记，也只把标记剥离后交回给交易员审查。
        transcript.add(new CamelDialogueTurn(transcript.size() + 1, CamelRoleType.PROGRAMMER, cleanedScript));
        int nextTurnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0)) + 1;
        // 程序员节点除了把原始回复回写给交易员，还会额外沉淀一份去掉结束标记的纯脚本，便于最终结果直接取用。
        return CompletableFuture.completedFuture(Map.of(
                "last_programmer_output", cleanedScript,
                "current_java_code", cleanedScript,
                "message_for_trader", cleanedScript,
                "active_role", CamelRoleType.TRADER.getStateValue(),
                "done", Boolean.FALSE,
                "stop_reason", state.value("stop_reason", ""),
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

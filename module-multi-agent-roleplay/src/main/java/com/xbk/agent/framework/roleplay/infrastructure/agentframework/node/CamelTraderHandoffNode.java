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
 * 交易员 handoff 节点（框架版 CAMEL）
 *
 * 职责：从 OverAllState 中读取程序员传来的"接力棒"（message_for_trader），
 * 扮演交易员角色生成回复，并将结果写回状态，把控制权交还给状态图路由到程序员节点。
 *
 * <p>"handoff"机制说明：
 * CAMEL 范式中两个角色的通信通过状态中的两个接力字段实现：
 * <pre>
 *   message_for_trader   → 程序员写给交易员的内容（程序员节点写入）
 *   message_for_programmer → 交易员写给程序员的内容（本节点写入）
 * </pre>
 * 每个节点执行完后只需把自己的输出写进对应字段，框架的条件边负责路由到下一个节点。
 * 这与手写版 HandwrittenCamelAgent 的 while 循环相比，
 * 把"谁调用谁"的逻辑从代码控制流变成了状态图的边声明。
 *
 * <p>与手写版 CamelTraderAgent 的对照：
 * <pre>
 *   手写版 CamelTraderAgent.reply(task, programmerMessage, conversationId)：
 *     → 接收程序员消息字符串参数，返回交易员回复字符串
 *     → 消息构建（含完整 transcript 历史）由 CamelConversationMemory 管理
 *
 *   框架版 CamelTraderHandoffNode.apply(OverAllState state)：
 *     → 从 OverAllState 读取 message_for_trader，只传当前 baton（任务基线+最新消息）
 *     → 不重放完整 transcript 给 LLM，减少 token 消耗
 *     → 自己负责追加 transcript 记录并序列化写回状态
 * </pre>
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
        // 读取当前对话标识，保证交易员本轮发言挂在同一条 conversation 链路上。
        String conversationId = state.value("conversation_id", "");
        // 读取用户原始任务，作为每轮交易员推理的基线上下文，始终传入 Prompt。
        String task = state.value("input", "");
        // 读取程序员传来的接力棒（上一轮程序员节点写入），可能为空（第一轮）。
        String programmerMessage = state.value("message_for_trader", "");

        List<Message> messages = new ArrayList<Message>();
        // FlowAgent 版不再重放完整字符串历史，只传交易员真正需要消费的最小 baton：
        // 系统提示（角色约束）+ 任务基线 + 程序员最新产物，减少无效 token 消耗。
        // 对比：手写版通过 CamelConversationMemory 按角色视角重放完整历史，
        //       框架版精简为"任务基线 + 最新一棒"，体现了两种上下文管理策略的权衡。
        messages.add(buildMessage(conversationId, MessageRole.SYSTEM, contract.getSystemPrompt(), contract.getAgentName()));
        messages.add(buildMessage(conversationId, MessageRole.USER, CamelPromptTemplates.traderInitialPrompt(task), "task"));
        // 第一轮 programmerMessage 为空，不加入；后续轮每次只传最新一棒，不累积。
        if (!programmerMessage.isBlank()) {
            messages.add(buildMessage(conversationId, MessageRole.USER, programmerMessage, CamelRoleType.PROGRAMMER.getDisplayName()));
        }
        // 调用 LLM，保留原始输出用于结束标记检测（normalizeTraderOutput 可能会修改输出内容）。
        String rawOutput = chat(conversationId, List.copyOf(messages));
        // 提取当前 transcript，用于统计交易员已发言次数（决定是否允许终止）。
        List<CamelDialogueTurn> transcript = extractTranscript(state);
        // 计算交易员本轮是第几次发言（已有次数+1），供 canTraderFinish 判断最少轮次约束。
        int traderTurnCount = countRoleTurns(transcript, CamelRoleType.TRADER) + 1;
        // 对原始输出做格式化清洗（如添加轮次前缀），保持对话记录的可读性。
        String output = CamelPromptTemplates.normalizeTraderOutput(rawOutput, traderTurnCount);
        // 把本轮发言追加进 transcript，按当前列表长度递增轮次号。
        transcript.add(new CamelDialogueTurn(transcript.size() + 1, CamelRoleType.TRADER, output));
        // 终止条件：原始输出包含结束标记 且 已完成最少轮次（防止首轮就被意外终止）。
        // 只有交易员有权终止，程序员节点强制将 done 置为 false。
        boolean done = CamelPromptTemplates.containsTaskDoneMarker(rawOutput)
                && CamelPromptTemplates.canTraderFinish(traderTurnCount);
        // 总轮次加一，用于外部轮次上限保护。
        int nextTurnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0)) + 1;
        // 把本轮状态增量写回，框架自动 merge 进 OverAllState。
        // message_for_programmer：交易员的输出即程序员下一轮的输入（接力棒传递）。
        // active_role：若终止则停留在交易员（已完成），否则切换到程序员（继续对话）。
        return CompletableFuture.completedFuture(Map.of(
                "last_trader_output", output,
                "message_for_programmer", output,
                "active_role", done ? CamelRoleType.TRADER.getStateValue() : CamelRoleType.PROGRAMMER.getStateValue(),
                "done", Boolean.valueOf(done),
                // 只有真正终止时才记录 stop_reason，避免提前污染结束状态。
                "stop_reason", done ? rawOutput : state.value("stop_reason", ""),
                "turn_count", Integer.valueOf(nextTurnCount),
                // transcript 序列化后存入状态，跨节点流动。
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

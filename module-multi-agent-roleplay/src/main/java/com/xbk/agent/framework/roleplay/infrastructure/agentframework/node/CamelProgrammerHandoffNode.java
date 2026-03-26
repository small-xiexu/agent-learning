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
import com.xbk.agent.framework.roleplay.infrastructure.agentframework.support.CamelStateKeys;
import com.xbk.agent.framework.roleplay.infrastructure.agentframework.support.CamelTranscriptStateSupport;
import com.xbk.agent.framework.roleplay.support.CamelPromptTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 程序员 handoff 节点（框架版 CAMEL）
 *
 * 职责：从 OverAllState 中读取交易员传来的"接力棒"（message_for_programmer），
 * 扮演程序员角色生成代码输出，并将结果写回状态，把控制权交还给状态图路由到交易员节点。
 *
 * <p>"handoff"机制说明（与 CamelTraderHandoffNode 对称）：
 * <pre>
 *   CamelTraderHandoffNode    写入 message_for_programmer → CamelProgrammerHandoffNode 读取
 *   CamelProgrammerHandoffNode 写入 message_for_trader   → CamelTraderHandoffNode 读取
 * </pre>
 * 两个节点通过这对字段实现"双向传棒"，框架条件边根据 done 字段决定继续还是终止。
 *
 * <p>程序员节点的关键约束：
 * 程序员在 CAMEL 协议中没有终止权——即使它违规输出了结束标记（TASK_DONE），
 * 本节点也会通过 {@code stripTaskDoneMarker} 将其剥离，并强制写入 {@code done=false}。
 * 只有交易员节点有权正式终止对话。
 *
 * <p>与手写版 CamelProgrammerAgent 的对照：
 * <pre>
 *   手写版 CamelProgrammerAgent.reply(task, traderMessage, conversationId)：
 *     → 接收交易员消息字符串参数，返回代码输出字符串
 *     → 手写版通过 CamelConversationMemory 按角色视角（USER/ASSISTANT 映射）构建历史
 *
 *   框架版 CamelProgrammerHandoffNode.apply(OverAllState state)：
 *     → 从 OverAllState 读取 message_for_programmer，只传最新一棒给 LLM
 *     → 去除了角色视角映射逻辑，直接把交易员消息作为 USER 消息传入
 *     → 额外沉淀 current_java_code 字段，便于最终结果直接提取最新脚本
 * </pre>
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
        // 读取当前对话标识，保证程序员本轮发言挂在同一条 conversation 链路上。
        String conversationId = state.value(CamelStateKeys.CONVERSATION_ID, "");
        // 读取用户原始任务，作为程序员每轮推理的基线上下文，始终传入 Prompt。
        String task = state.value(CamelStateKeys.INPUT, "");
        // 读取交易员传来的接力棒（上一轮交易员节点写入的 message_for_programmer）。
        String traderMessage = state.value(CamelStateKeys.MESSAGE_FOR_PROGRAMMER, "");

        List<Message> messages = new ArrayList<Message>();
        // 程序员节点消费的 baton 更小：系统提示（角色约束）+ 任务基线 + 交易员最新需求。
        // 不必像手写版那样通过 CamelConversationMemory 重建完整的角色视角消息列表，
        // 框架版用"最新一棒"替代"完整历史重放"，在对话上下文精确性和 token 效率间做了取舍。
        messages.add(buildMessage(conversationId, MessageRole.SYSTEM, contract.getSystemPrompt(), contract.getAgentName()));
        messages.add(buildMessage(conversationId, MessageRole.USER, CamelPromptTemplates.programmerInitialPrompt(task), "task"));
        // 第一轮 traderMessage 不为空（由交易员首轮输出写入），此后每轮都只传最新一棒。
        if (!traderMessage.isBlank()) {
            messages.add(buildMessage(conversationId, MessageRole.USER, traderMessage, CamelRoleType.TRADER.getDisplayName()));
        }
        // 调用 LLM，得到程序员本轮的原始代码输出。
        String output = chat(conversationId, List.copyOf(messages));
        // 程序员没有结束权——即使它违规输出了 TASK_DONE 标记，也强制剥离后才传给交易员。
        // 这是 CAMEL 协议中角色权限约束的核心体现：只有交易员才能宣告任务完成。
        String cleanedScript = CamelPromptTemplates.stripTaskDoneMarker(output);
        // 提取当前 transcript，准备追加本轮记录。
        List<CamelDialogueTurn> transcript = extractTranscript(state);
        // 追加程序员本轮发言（已剥离结束标记的纯脚本），保持 transcript 的可读性。
        transcript.add(new CamelDialogueTurn(transcript.size() + 1, CamelRoleType.PROGRAMMER, cleanedScript));
        // 总轮次加一，用于外部轮次上限保护。
        int nextTurnCount = state.value(CamelStateKeys.TURN_COUNT, Integer.class).orElse(Integer.valueOf(0)) + 1;
        // 把本轮状态增量写回，框架自动 merge 进 OverAllState。
        return CompletableFuture.completedFuture(Map.of(
                CamelStateKeys.LAST_PROGRAMMER_OUTPUT, cleanedScript,
                // 额外沉淀一份纯脚本，便于最终结果的 AlibabaCamelFlowAgent 直接提取最新代码。
                CamelStateKeys.CURRENT_JAVA_CODE, cleanedScript,
                // message_for_trader：程序员的输出即交易员下一轮的输入（接力棒传递）。
                CamelStateKeys.MESSAGE_FOR_TRADER, cleanedScript,
                // 程序员完成后，下一位固定是交易员（程序员无权终止）。
                CamelStateKeys.ACTIVE_ROLE, CamelRoleType.TRADER.getStateValue(),
                // 程序员节点强制写入 done=false，不允许程序员终止对话。
                CamelStateKeys.DONE, Boolean.FALSE,
                // 停止原因沿用旧值，避免在非终止节点里误覆盖结束状态。
                CamelStateKeys.STOP_REASON, state.value(CamelStateKeys.STOP_REASON, ""),
                CamelStateKeys.TURN_COUNT, Integer.valueOf(nextTurnCount),
                // transcript 序列化后存入状态，跨节点流动。
                CamelStateKeys.TRANSCRIPT, CamelTranscriptStateSupport.toStateTranscript(transcript)));
    }

    /**
     * 提取 transcript。
     *
     * @param state 全局状态
     * @return transcript 副本
     */
    private List<CamelDialogueTurn> extractTranscript(OverAllState state) {
        return CamelTranscriptStateSupport.readTranscript(state.value(CamelStateKeys.TRANSCRIPT).orElse(List.of()));
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

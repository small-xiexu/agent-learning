package com.xbk.agent.framework.conversation.infrastructure.agentframework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.conversation.domain.memory.ConversationTurn;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleContract;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.support.ConversationStateSupport;
import com.xbk.agent.framework.conversation.support.ConversationPromptTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * CodeReviewer 节点（框架版 AutoGen 群聊）
 *
 * 职责：读取 OverAllState 中的共享群聊历史，扮演审查员角色评审最新 Python 脚本，
 * 通过写入 {@code done=true/false} 驱动条件边决定群聊是终止还是回环到 ProductManagerNode。
 *
 * <p>在 AutoGen 群聊范式中的定位：
 * CodeReviewer 是群聊中唯一有权终止对话的节点——
 * 只有当它在输出中包含结束标记（TASK_DONE）时，{@code done} 字段才会被设为 {@code true}，
 * 触发 {@code AlibabaConversationFlowAgent} 中的条件边跳转到 {@code StateGraph.END}。
 * ProductManager 和 Engineer 均无终止权限，即使它们误输出了结束标记也会被剥离。
 *
 * <p>与手写版 CodeReviewerAgent 的对照：
 * <pre>
 *   手写版 CodeReviewerAgent.execute(history, task, conversationId)：
 *     → 返回审查意见字符串
 *     → 是否终止由 RoundRobinGroupChat 调用 shouldStop() 外部判断
 *
 *   框架版 CodeReviewerNode.apply(OverAllState state)：
 *     → 审查员节点自己检测 containsTaskDoneMarker(output) 并写入 done 字段
 *     → 条件边 AlibabaConversationFlowAgent.nextFromCodeReviewer() 读取 done 字段决定跳转
 *     → 终止判断从"调用方外部判断"变成了"节点内部写状态 + 图结构声明出口"
 * </pre>
 *
 * @author xiexu
 */
public class CodeReviewerNode implements AsyncNodeAction {

    private static final String REQUEST_PREFIX = "conversation-flow-code-reviewer-";

    /**
     * 统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 审查员契约。
     */
    private final ConversationRoleContract contract;

    /**
     * 创建审查员节点。
     *
     * @param agentLlmGateway 统一网关
     * @param contract 角色契约
     */
    public CodeReviewerNode(AgentLlmGateway agentLlmGateway, ConversationRoleContract contract) {
        this.agentLlmGateway = agentLlmGateway;
        this.contract = contract;
    }

    /**
     * 执行审查员节点。
     *
     * @param state 全局状态
     * @return 写回状态
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        // 读取当前群聊的会话标识，保证 Reviewer 的本轮审查也写回同一条 conversation。
        String conversationId = state.value("conversation_id", "");
        // 从共享状态里恢复完整群聊历史，让 Reviewer 能看到最新需求、最新脚本和之前的审查结果。
        List<Message> sharedMessages = ConversationStateSupport.readSharedMessages(
                state.value("shared_messages").orElse(List.of()));
        // 组装当前节点发给模型的完整请求消息。
        List<Message> requestMessages = new ArrayList<Message>();
        // 先放 Reviewer 的系统提示，强调这一轮只能做验收和阻塞问题判断。
        requestMessages.add(buildMessage(conversationId, MessageRole.SYSTEM, contract.getSystemPrompt(), contract.getAgentName()));
        // 再拼接整个共享群聊历史，让审查判断基于完整上下文而不是单条代码。
        requestMessages.addAll(sharedMessages);
        // 调模型生成本轮审查意见，里面可能包含结束标记，也可能只是修订要求。
        String output = chat(conversationId, List.copyOf(requestMessages));
        // 把 Reviewer 输出写回共享消息历史，供下一轮 ProductManager 继续消费。
        sharedMessages.add(buildMessage(conversationId, MessageRole.ASSISTANT, output, contract.getRoleType().getDisplayName()));
        // 从状态里恢复 transcript，保留这一轮的审查记录。
        List<ConversationTurn> transcript = ConversationStateSupport.readTranscript(state.value("transcript").orElse(List.of()));
        // 追加一条 Reviewer 发言，方便后续回放整场群聊轨迹。
        transcript.add(new ConversationTurn(transcript.size() + 1, ConversationRoleType.CODE_REVIEWER, output));
        // 检查本轮输出里是否出现结束标记，只有 Reviewer 才有权正式宣布任务完成。
        boolean done = ConversationPromptTemplates.containsTaskDoneMarker(output);
        // Reviewer 执行完成后，总轮次加一，防止流程无限回环。
        int nextTurnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0)) + 1;
        // 把最新审查结论和共享状态一次性写回给 Flow runtime。
        return CompletableFuture.completedFuture(Map.of(
                // 记录 Reviewer 最近一次输出，便于日志和测试断言。
                "last_reviewer_output", output,
                // 根据审查结果写入 review_status，供外部和测试快速判断当前处于“通过”还是“待修改”。
                "review_status", done ? "approved" : "revise",
                // 如果已完成，就把活动角色停留在 Reviewer；否则重新切回 ProductManager 进入下一轮协作。
                "active_role", done
                        ? ConversationRoleType.CODE_REVIEWER.getStateValue()
                        : ConversationRoleType.PRODUCT_MANAGER.getStateValue(),
                // 写回更新后的总轮次。
                "turn_count", Integer.valueOf(nextTurnCount),
                // 把本轮是否已完成显式写入状态，供条件边决定是继续还是结束。
                "done", Boolean.valueOf(done),
                // 只有真正完成时才写入停止原因；否则沿用旧值，避免提前污染结束状态。
                "stop_reason", done ? output : state.value("stop_reason", ""),
                // 把更新后的共享消息重新编码为可持久化到状态里的结构。
                "shared_messages", ConversationStateSupport.toStateMessages(sharedMessages),
                // 把更新后的 transcript 重新编码为可持久化到状态里的结构。
                "transcript", ConversationStateSupport.toStateTranscript(transcript)));
    }

    /**
     * 调用统一网关。
     *
     * @param conversationId 会话标识
     * @param messages 消息列表
     * @return 文本响应
     */
    private String chat(String conversationId, List<Message> messages) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(REQUEST_PREFIX + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(messages)
                .build());
        if (response.getOutputMessage() != null && response.getOutputMessage().getContent() != null) {
            return response.getOutputMessage().getContent();
        }
        return response.getRawText();
    }

    /**
     * 构造统一消息。
     *
     * @param conversationId 会话标识
     * @param role 消息角色
     * @param content 内容
     * @param name 名称
     * @return 统一消息
     */
    private Message buildMessage(String conversationId, MessageRole role, String content, String name) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .content(content == null ? "" : content)
                .name(name)
                .build();
    }
}

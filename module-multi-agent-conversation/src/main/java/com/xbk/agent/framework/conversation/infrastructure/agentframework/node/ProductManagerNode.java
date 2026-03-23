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
 * ProductManager 节点
 *
 * 职责：读取共享群聊历史，生成下一步产品需求，并写回同一份 shared_messages
 *
 * @author xiexu
 */
public class ProductManagerNode implements AsyncNodeAction {

    private static final String REQUEST_PREFIX = "conversation-flow-product-manager-";

    /**
     * 统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 产品经理契约。
     */
    private final ConversationRoleContract contract;

    /**
     * 创建产品经理节点。
     *
     * @param agentLlmGateway 统一网关
     * @param contract 角色契约
     */
    public ProductManagerNode(AgentLlmGateway agentLlmGateway, ConversationRoleContract contract) {
        this.agentLlmGateway = agentLlmGateway;
        this.contract = contract;
    }

    /**
     * 执行产品经理节点。
     *
     * @param state 全局状态
     * @return 写回状态
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        // 读取当前群聊的会话标识，保证本轮 ProductManager 发言仍然挂在同一条 conversation 上。
        String conversationId = state.value("conversation_id", "");
        // 从共享状态里恢复所有人都能看到的群聊历史，作为本轮继续发言的公共上下文。
        List<Message> sharedMessages = ConversationStateSupport.readSharedMessages(
                state.value("shared_messages").orElse(List.of()));
        // 组装当前节点要发给模型的完整消息列表。
        List<Message> requestMessages = new ArrayList<Message>();
        // 先放入 ProductManager 自己的系统提示，明确这一轮只能做需求拆解和范围收敛。
        requestMessages.add(buildMessage(conversationId, MessageRole.SYSTEM, contract.getSystemPrompt(), contract.getAgentName()));
        // 再把历史群聊消息整体拼上去，让模型基于共享上下文继续发言。
        requestMessages.addAll(sharedMessages);
        // 先保留模型返回的原始文本，便于后续单独做协议清洗或问题排查。
        String rawOutput = chat(conversationId, List.copyOf(requestMessages));
        // 去掉误输出的结束标记，避免 ProductManager 越权终止流程。
        String output = ConversationPromptTemplates.stripTaskDoneMarker(rawOutput);
        // 把 ProductManager 的本轮输出追加回共享消息历史，供后续 Engineer 和 Reviewer 继续消费。
        sharedMessages.add(buildMessage(conversationId, MessageRole.ASSISTANT, output, contract.getRoleType().getDisplayName()));
        // 从状态里恢复 transcript，用更轻量的轮次记录保留这次发言，方便日志、断言和回放。
        List<ConversationTurn> transcript = ConversationStateSupport.readTranscript(state.value("transcript").orElse(List.of()));
        // 追加一条新的 ProductManager 发言记录，轮次号始终按 transcript 当前长度递增。
        transcript.add(new ConversationTurn(transcript.size() + 1, ConversationRoleType.PRODUCT_MANAGER, output));
        // 当前节点执行完成后，总轮次加一，作为下一步是否需要停止的判断依据之一。
        int nextTurnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0)) + 1;
        // 把本轮更新后的状态一次性写回给 Flow runtime，驱动后续节点继续执行。
        return CompletableFuture.completedFuture(Map.of(
                // 记录 ProductManager 最近一次输出，便于调试和测试断言。
                "last_product_output", output,
                // ProductManager 说完之后，下一位固定轮到 Engineer。
                "active_role", ConversationRoleType.ENGINEER.getStateValue(),
                // 写回更新后的总轮次。
                "turn_count", Integer.valueOf(nextTurnCount),
                // ProductManager 节点本身无权结束流程，所以这里明确保持未完成状态。
                "done", Boolean.FALSE,
                // 停止原因沿用旧值，避免在非结束节点里误覆盖真正的终止信息。
                "stop_reason", state.value("stop_reason", ""),
                // 把更新后的共享消息重新编码为可放入状态的结构。
                "shared_messages", ConversationStateSupport.toStateMessages(sharedMessages),
                // 把更新后的 transcript 重新编码为可放入状态的结构。
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

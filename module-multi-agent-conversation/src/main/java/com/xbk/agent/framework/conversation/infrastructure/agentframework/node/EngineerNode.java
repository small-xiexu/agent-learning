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
 * Engineer 节点
 *
 * 职责：读取共享群聊历史，输出最新完整 Python 脚本，并写回同一份 shared_messages
 *
 * @author xiexu
 */
public class EngineerNode implements AsyncNodeAction {

    private static final String REQUEST_PREFIX = "conversation-flow-engineer-";

    /**
     * 统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 工程师契约。
     */
    private final ConversationRoleContract contract;

    /**
     * 创建工程师节点。
     *
     * @param agentLlmGateway 统一网关
     * @param contract 角色契约
     */
    public EngineerNode(AgentLlmGateway agentLlmGateway, ConversationRoleContract contract) {
        this.agentLlmGateway = agentLlmGateway;
        this.contract = contract;
    }

    /**
     * 执行工程师节点。
     *
     * @param state 全局状态
     * @return 写回状态
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String conversationId = state.value("conversation_id", "");
        List<Message> sharedMessages = ConversationStateSupport.readSharedMessages(
                state.value("shared_messages").orElse(List.of()));
        List<Message> requestMessages = new ArrayList<Message>();
        requestMessages.add(buildMessage(conversationId, MessageRole.SYSTEM, contract.getSystemPrompt(), contract.getAgentName()));
        requestMessages.addAll(sharedMessages);
        String output = chat(conversationId, List.copyOf(requestMessages));
        String normalizedScript = ConversationPromptTemplates.normalizePythonScript(output);
        sharedMessages.add(buildMessage(conversationId, MessageRole.ASSISTANT, normalizedScript, contract.getRoleType().getDisplayName()));
        List<ConversationTurn> transcript = ConversationStateSupport.readTranscript(state.value("transcript").orElse(List.of()));
        transcript.add(new ConversationTurn(transcript.size() + 1, ConversationRoleType.ENGINEER, normalizedScript));
        int nextTurnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0)) + 1;
        return CompletableFuture.completedFuture(Map.of(
                "last_engineer_output", normalizedScript,
                "current_python_script", normalizedScript,
                "active_role", ConversationRoleType.CODE_REVIEWER.getStateValue(),
                "turn_count", Integer.valueOf(nextTurnCount),
                "done", Boolean.FALSE,
                "stop_reason", state.value("stop_reason", ""),
                "shared_messages", ConversationStateSupport.toStateMessages(sharedMessages),
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

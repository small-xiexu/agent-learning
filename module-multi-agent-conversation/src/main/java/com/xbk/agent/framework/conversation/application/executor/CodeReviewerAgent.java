package com.xbk.agent.framework.conversation.application.executor;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.conversation.domain.memory.ConversationMemory;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleContract;

import java.util.List;
import java.util.UUID;

/**
 * CodeReviewer 执行器
 *
 * 职责：基于统一网关扮演代码审查员角色，给出阻塞性审查意见或批准结束
 *
 * @author xiexu
 */
public class CodeReviewerAgent {

    private static final String REQUEST_PREFIX = "conversation-code-reviewer-";

    /**
     * 底层统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 审查员角色契约。
     */
    private final ConversationRoleContract contract;

    /**
     * 创建审查员执行器。
     *
     * @param agentLlmGateway 统一网关
     */
    public CodeReviewerAgent(AgentLlmGateway agentLlmGateway) {
        this(agentLlmGateway, ConversationRoleContract.codeReviewerContract());
    }

    /**
     * 创建审查员执行器。
     *
     * @param agentLlmGateway 统一网关
     * @param contract 角色契约
     */
    public CodeReviewerAgent(AgentLlmGateway agentLlmGateway, ConversationRoleContract contract) {
        this.agentLlmGateway = agentLlmGateway;
        this.contract = contract;
    }

    /**
     * 基于共享群聊上下文生成回复。
     *
     * @param memory 群聊记忆
     * @param conversationId 会话标识
     * @return 审查员输出
     */
    public String reply(ConversationMemory memory, String conversationId) {
        return chat(conversationId, memory.toMessagesForRole(conversationId, contract));
    }

    /**
     * 返回角色契约。
     *
     * @return 角色契约
     */
    public ConversationRoleContract getContract() {
        return contract;
    }

    /**
     * 调用统一网关并返回文本。
     *
     * @param conversationId 会话标识
     * @param messages 请求消息
     * @return 文本响应
     */
    private String chat(String conversationId, List<Message> messages) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(REQUEST_PREFIX + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(messages)
                .build());
        return extractText(response);
    }

    /**
     * 提取响应文本。
     *
     * @param response LLM 响应
     * @return 响应文本
     */
    private String extractText(LlmResponse response) {
        if (response.getOutputMessage() != null && response.getOutputMessage().getContent() != null) {
            return response.getOutputMessage().getContent();
        }
        return response.getRawText();
    }
}

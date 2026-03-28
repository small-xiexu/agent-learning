package com.xbk.agent.framework.engineering.handwritten.agent;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.handwritten.hub.MessageHub;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 Agent 抽象基类。
 *
 * 职责：统一承载 Agent 名称、消息发送能力和通用模型调用逻辑。
 *
 * @author xiexu
 */
public abstract class AbstractHandwrittenAgent implements HandwrittenMessageAgent {

    private final String agentName;
    private final AgentLlmGateway agentLlmGateway;
    private final MessageHub messageHub;

    /**
     * 创建手写版 Agent。
     *
     * @param agentName Agent 名称
     * @param agentLlmGateway 统一网关
     * @param messageHub 消息中心
     */
    protected AbstractHandwrittenAgent(String agentName, AgentLlmGateway agentLlmGateway, MessageHub messageHub) {
        this.agentName = agentName;
        this.agentLlmGateway = agentLlmGateway;
        this.messageHub = messageHub;
    }

    /**
     * 返回 Agent 名称。
     *
     * @return Agent 名称
     */
    @Override
    public String getAgentName() {
        return agentName;
    }

    /**
     * 发送消息到 MessageHub。
     *
     * @param message 工程消息
     */
    @Override
    public void send(EngineeringMessage message) {
        messageHub.publish(message);
    }

    /**
     * 返回统一网关。
     *
     * @return 统一网关
     */
    protected AgentLlmGateway getAgentLlmGateway() {
        return agentLlmGateway;
    }

    /**
     * 返回消息中心。
     *
     * @return 消息中心
     */
    protected MessageHub getMessageHub() {
        return messageHub;
    }

    /**
     * 让当前 Agent 使用统一网关发起一次同步问答。
     *
     * @param conversationId 会话标识
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return 文本响应
     */
    protected String askModel(String conversationId, String systemPrompt, String userPrompt) {
        if (agentLlmGateway == null) {
            throw new IllegalStateException("AgentLlmGateway is required for " + agentName);
        }
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(agentName + "-request-" + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(List.of(
                        message(conversationId, MessageRole.SYSTEM, systemPrompt),
                        message(conversationId, MessageRole.USER, userPrompt)))
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
     * @param content 消息文本
     * @return 统一消息
     */
    private Message message(String conversationId, MessageRole role, String content) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .build();
    }
}

package com.xbk.agent.framework.engineering.handwritten.agent;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.message.MessageType;
import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.domain.ticket.SpecialistRequestPayload;
import com.xbk.agent.framework.engineering.domain.ticket.SpecialistResponsePayload;
import com.xbk.agent.framework.engineering.handwritten.hub.MessageHub;
import com.xbk.agent.framework.engineering.handwritten.support.HandwrittenAgentPromptTemplates;

import java.util.UUID;

/**
 * 手写版技术专家 Agent。
 *
 * 职责：消费技术主题请求，调用统一网关生成技术答复，并把结果按 replyTo 回包给接待员。
 *
 * @author xiexu
 */
public class HandwrittenTechSupportAgent extends AbstractHandwrittenAgent {

    /**
     * 创建技术专家 Agent。
     *
     * @param agentLlmGateway 统一网关
     * @param messageHub 消息中心
     */
    public HandwrittenTechSupportAgent(AgentLlmGateway agentLlmGateway, MessageHub messageHub) {
        super("tech_support_agent", agentLlmGateway, messageHub);
    }

    /**
     * 接收并处理专家请求。
     *
     * @param message 工程消息
     */
    @Override
    public void receive(EngineeringMessage message) {
        if (message.getMessageType() != MessageType.SPECIALIST_REQUEST) {
            return;
        }
        SpecialistRequestPayload payload = (SpecialistRequestPayload) message.getPayload();
        String answer = askModel(
                message.getConversationId(),
                HandwrittenAgentPromptTemplates.techSupportSystemPrompt(),
                payload.getOriginalRequest());
        send(EngineeringMessage.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(message.getConversationId())
                .correlationId(message.getCorrelationId())
                .fromAgent(getAgentName())
                .toAgent(message.getFromAgent())
                .topic(message.getReplyTo())
                .messageType(MessageType.SPECIALIST_RESPONSE)
                .payload(new SpecialistResponsePayload(SpecialistType.TECH_SUPPORT, answer))
                .build());
    }
}

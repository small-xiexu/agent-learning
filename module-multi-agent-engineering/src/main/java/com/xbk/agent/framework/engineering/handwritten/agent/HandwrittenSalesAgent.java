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
 * 手写版销售顾问 Agent。
 *
 * <p>专门处理销售咨询的顾问。它只做一件事：
 * 收到前台转来的销售问题 → 调用 LLM 以商务顾问角色作答 → 把答复发回给前台。
 *
 * <p>流程和技术专家完全一致，差别只是系统提示词不同——
 * 告诉 LLM 的角色是"销售顾问"而不是"技术工程师"。
 *
 * @author xiexu
 */
public class HandwrittenSalesAgent extends AbstractHandwrittenAgent {

    /**
     * 创建销售 Agent。
     *
     * @param agentLlmGateway 统一网关
     * @param messageHub 消息中心
     */
    public HandwrittenSalesAgent(AgentLlmGateway agentLlmGateway, MessageHub messageHub) {
        super("sales_agent", agentLlmGateway, messageHub);
    }

    /**
     * 接收并处理销售请求。
     *
     * @param message 工程消息
     */
    @Override
    public void receive(EngineeringMessage message) {
        // 顾问只处理发给自己的请求（SPECIALIST_REQUEST），其他类型直接忽略。
        if (message.getMessageType() != MessageType.SPECIALIST_REQUEST) {
            return;
        }
        SpecialistRequestPayload payload = (SpecialistRequestPayload) message.getPayload();

        // 告诉 LLM："你是一位销售顾问"，然后把用户的问题发给它，等待商务答复。
        String answer = askModel(
                message.getConversationId(),
                HandwrittenAgentPromptTemplates.salesSystemPrompt(),
                payload.getOriginalRequest());

        // 按消息里的"回寄地址"（replyTo）把答复发回前台，correlationId 原样带回。
        send(EngineeringMessage.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(message.getConversationId())
                .correlationId(message.getCorrelationId())
                .fromAgent(getAgentName())
                .toAgent(message.getFromAgent())
                .topic(message.getReplyTo())
                .messageType(MessageType.SPECIALIST_RESPONSE)
                .payload(new SpecialistResponsePayload(SpecialistType.SALES, answer))
                .build());
    }
}

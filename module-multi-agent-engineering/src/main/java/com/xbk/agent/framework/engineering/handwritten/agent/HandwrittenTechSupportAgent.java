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
 * <p>专门处理技术问题的专家。它只做一件事：
 * 收到前台转来的技术问题 → 调用 LLM 生成技术答复 → 把答复发回给前台。
 *
 * <p>它不主动找任何人，只等消息来。
 * 消息里附有"回寄地址"（{@code replyTo}），专家按地址把答复发回去，形成完整回路。
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
        // 专家只处理发给自己的请求（SPECIALIST_REQUEST），
        // 如果收到其他类型的消息（比如误发的广播），直接忽略，不做任何处理。
        if (message.getMessageType() != MessageType.SPECIALIST_REQUEST) {
            return;
        }
        SpecialistRequestPayload payload = (SpecialistRequestPayload) message.getPayload();

        // 告诉 LLM："你是一位技术工程师"，然后把用户的原始问题发给它，等待技术答复。
        String answer = askModel(
                message.getConversationId(),
                HandwrittenAgentPromptTemplates.techSupportSystemPrompt(),
                payload.getOriginalRequest());

        // 把答复发回去。发到哪里？看消息里的 replyTo 字段——
        // 前台发请求时写了"回寄地址 = RECEPTIONIST_REPLY 频道"，
        // 专家按这个地址回包，前台就能在那里收到。
        // correlationId 必须原样带回，前台凭它找到"信封"，把结果塞进去唤醒 Coordinator。
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

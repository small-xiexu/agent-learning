package com.xbk.agent.framework.engineering.handwritten.agent;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.application.routing.CustomerIntentClassifier;
import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.message.MessageType;
import com.xbk.agent.framework.engineering.domain.message.MessageTopic;
import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;
import com.xbk.agent.framework.engineering.domain.ticket.CustomerServiceRequest;
import com.xbk.agent.framework.engineering.domain.ticket.SpecialistRequestPayload;
import com.xbk.agent.framework.engineering.domain.ticket.SpecialistResponsePayload;
import com.xbk.agent.framework.engineering.domain.trace.DeliveryRecord;
import com.xbk.agent.framework.engineering.domain.trace.EngineeringTrace;
import com.xbk.agent.framework.engineering.handwritten.hub.MessageHub;
import com.xbk.agent.framework.engineering.handwritten.runtime.ConversationContextStore;
import com.xbk.agent.framework.engineering.handwritten.runtime.PendingResponseRegistry;
import com.xbk.agent.framework.engineering.handwritten.support.HandwrittenAgentPromptTemplates;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 手写版接待员 Agent。
 *
 * 职责：分析意图、包装并转发专家请求，以及在回包时组装最终运行结果。
 *
 * @author xiexu
 */
public class HandwrittenReceptionistAgent extends AbstractHandwrittenAgent {

    private final CustomerIntentClassifier customerIntentClassifier;
    private final PendingResponseRegistry pendingResponseRegistry;
    private final ConversationContextStore conversationContextStore;

    /**
     * 创建接待员 Agent。
     *
     * @param agentLlmGateway 统一网关
     * @param customerIntentClassifier 意图分类器
     * @param messageHub 消息中心
     * @param pendingResponseRegistry 待回包注册表
     * @param conversationContextStore 会话上下文存储
     */
    public HandwrittenReceptionistAgent(AgentLlmGateway agentLlmGateway,
                                        CustomerIntentClassifier customerIntentClassifier,
                                        MessageHub messageHub,
                                        PendingResponseRegistry pendingResponseRegistry,
                                        ConversationContextStore conversationContextStore) {
        super("receptionist_agent", agentLlmGateway, messageHub);
        this.customerIntentClassifier = customerIntentClassifier;
        this.pendingResponseRegistry = pendingResponseRegistry;
        this.conversationContextStore = conversationContextStore;
    }

    /**
     * 接收消息。
     *
     * @param message 工程消息
     */
    @Override
    public void receive(EngineeringMessage message) {
        if (message.getMessageType() == MessageType.CUSTOMER_REQUEST) {
            handleCustomerRequest(message);
            return;
        }
        if (message.getMessageType() == MessageType.SPECIALIST_RESPONSE) {
            handleSpecialistResponse(message);
        }
    }

    /**
     * 处理用户请求。
     *
     * @param message 用户请求消息
     */
    private void handleCustomerRequest(EngineeringMessage message) {
        String requestText = extractRequestText(message.getPayload());
        conversationContextStore.registerConversation(message.getConversationId(), requestText);
        conversationContextStore.recordRoute(message.getConversationId(), getAgentName());
        RoutingDecision routingDecision = customerIntentClassifier.classify(message.getConversationId(), requestText);
        conversationContextStore.recordRoutingDecision(message.getConversationId(), routingDecision);
        conversationContextStore.recordRoute(message.getConversationId(), routingDecision.getTargetAgentName());
        SpecialistRequestPayload payload = new SpecialistRequestPayload(
                requestText,
                routingDecision.getReason(),
                routingDecision.getIntentType());
        send(EngineeringMessage.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(message.getConversationId())
                .correlationId(message.getCorrelationId())
                .fromAgent(getAgentName())
                .toAgent(routingDecision.getTargetAgentName())
                .topic(routingDecision.getTargetTopic())
                .messageType(MessageType.SPECIALIST_REQUEST)
                .replyTo(MessageTopic.RECEPTIONIST_REPLY)
                .payload(payload)
                .build());
    }

    /**
     * 处理专家回包。
     *
     * @param message 专家回包
     */
    private void handleSpecialistResponse(EngineeringMessage message) {
        SpecialistResponsePayload payload = (SpecialistResponsePayload) message.getPayload();
        RoutingDecision routingDecision = conversationContextStore.getRoutingDecision(message.getConversationId());
        String specialistResponse = payload.getResolvedText();
        String finalResponse = HandwrittenAgentPromptTemplates.receptionistSummary(
                payload.getSpecialistType(),
                specialistResponse);
        List<DeliveryRecord> records = getMessageHub().snapshotAuditRecords().stream()
                .filter(record -> message.getConversationId().equals(findConversationId(record, message)))
                .collect(Collectors.toList());
        pendingResponseRegistry.complete(message.getCorrelationId(), EngineeringRunResult.builder()
                .conversationId(message.getConversationId())
                .requestText(conversationContextStore.getRequestText(message.getConversationId()))
                .intentType(routingDecision.getIntentType())
                .specialistType(payload.getSpecialistType())
                .routingDecision(routingDecision)
                .specialistResponse(specialistResponse)
                .finalResponse(finalResponse)
                .routeTrail(conversationContextStore.getRouteTrail(message.getConversationId()))
                .trace(new EngineeringTrace(records))
                .build());
    }

    /**
     * 提取原始请求文本。
     *
     * @param payload payload
     * @return 请求文本
     */
    private String extractRequestText(Object payload) {
        if (payload instanceof CustomerServiceRequest) {
            return ((CustomerServiceRequest) payload).getRequestText();
        }
        return String.valueOf(payload);
    }

    /**
     * 读取记录所属会话。
     *
     * @param record 投递记录
     * @param message 当前消息
     * @return 会话标识
     */
    private String findConversationId(DeliveryRecord record, EngineeringMessage message) {
        return record.getCorrelationId() != null && record.getCorrelationId().equals(message.getCorrelationId())
                ? message.getConversationId()
                : message.getConversationId();
    }
}

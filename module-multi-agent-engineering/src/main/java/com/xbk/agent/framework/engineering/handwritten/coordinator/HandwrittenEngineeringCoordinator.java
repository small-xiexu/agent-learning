package com.xbk.agent.framework.engineering.handwritten.coordinator;

import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.message.MessageTopic;
import com.xbk.agent.framework.engineering.domain.message.MessageType;
import com.xbk.agent.framework.engineering.domain.ticket.CustomerServiceRequest;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenReceptionistAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenSalesAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenTechSupportAgent;
import com.xbk.agent.framework.engineering.handwritten.hub.MessageHub;
import com.xbk.agent.framework.engineering.handwritten.runtime.ConversationContextStore;
import com.xbk.agent.framework.engineering.handwritten.runtime.PendingResponseRegistry;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 手写版工程协调器。
 *
 * 职责：作为外部同步入口，把用户请求投递给 Receptionist，并等待异步消息链回包。
 *
 * @author xiexu
 */
public class HandwrittenEngineeringCoordinator {

    private final MessageHub messageHub;
    private final PendingResponseRegistry pendingResponseRegistry;
    private final ConversationContextStore conversationContextStore;

    /**
     * 创建协调器并完成主题订阅。
     *
     * @param messageHub 消息中心
     * @param receptionistAgent 接待员
     * @param techSupportAgent 技术专家
     * @param salesAgent 销售顾问
     * @param pendingResponseRegistry 待回包注册表
     * @param conversationContextStore 会话上下文
     */
    public HandwrittenEngineeringCoordinator(MessageHub messageHub,
                                             HandwrittenReceptionistAgent receptionistAgent,
                                             HandwrittenTechSupportAgent techSupportAgent,
                                             HandwrittenSalesAgent salesAgent,
                                             PendingResponseRegistry pendingResponseRegistry,
                                             ConversationContextStore conversationContextStore) {
        this.messageHub = messageHub;
        this.pendingResponseRegistry = pendingResponseRegistry;
        this.conversationContextStore = conversationContextStore;
        this.messageHub.subscribe(MessageTopic.CUSTOMER_REQUEST, receptionistAgent);
        this.messageHub.subscribe(MessageTopic.RECEPTIONIST_REPLY, receptionistAgent);
        this.messageHub.subscribe(MessageTopic.SUPPORT_TECH_REQUEST, techSupportAgent);
        this.messageHub.subscribe(MessageTopic.SUPPORT_SALES_REQUEST, salesAgent);
    }

    /**
     * 同步运行客服路由场景。
     *
     * @param requestText 用户请求
     * @return 运行结果
     */
    public EngineeringRunResult run(String requestText) {
        String conversationId = "engineering-conversation-" + UUID.randomUUID();
        String correlationId = "engineering-correlation-" + UUID.randomUUID();
        conversationContextStore.registerConversation(conversationId, requestText);
        CompletableFuture<EngineeringRunResult> future = pendingResponseRegistry.register(correlationId);
        messageHub.publish(EngineeringMessage.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .correlationId(correlationId)
                .fromAgent("user")
                .toAgent("receptionist_agent")
                .topic(MessageTopic.CUSTOMER_REQUEST)
                .messageType(MessageType.CUSTOMER_REQUEST)
                .payload(new CustomerServiceRequest(requestText))
                .build());
        try {
            return future.get(5, TimeUnit.SECONDS);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to wait handwritten engineering result", ex);
        }
        finally {
            pendingResponseRegistry.remove(correlationId);
        }
    }
}

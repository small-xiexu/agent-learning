package com.xbk.agent.framework.engineering.handwritten.hub;

import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.trace.DeliveryRecord;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenMessageAgent;
import com.xbk.agent.framework.engineering.handwritten.runtime.DeliveryAuditLog;

import java.util.List;

/**
 * 内存版消息中心。
 *
 * 职责：模拟 AgentScope 风格的中心消息代理，通过主题订阅与异步投递彻底解耦 Agent。
 *
 * @author xiexu
 */
public class InMemoryMessageHub implements MessageHub {

    private final TopicSubscriptionRegistry subscriptionRegistry;
    private final AsyncMessageDispatcher dispatcher;
    private final DeliveryAuditLog deliveryAuditLog;

    /**
     * 创建默认内存消息中心。
     */
    public InMemoryMessageHub() {
        this.subscriptionRegistry = new TopicSubscriptionRegistry();
        this.dispatcher = new AsyncMessageDispatcher();
        this.deliveryAuditLog = new DeliveryAuditLog();
    }

    /**
     * 订阅主题。
     *
     * @param topic 主题
     * @param agent Agent
     */
    @Override
    public void subscribe(String topic, HandwrittenMessageAgent agent) {
        subscriptionRegistry.subscribe(topic, agent);
    }

    /**
     * 发布消息。
     *
     * @param message 工程消息
     */
    @Override
    public void publish(EngineeringMessage message) {
        deliveryAuditLog.recordPublished(message);
        for (HandwrittenMessageAgent agent : subscriptionRegistry.getSubscribers(message.getTopic())) {
            dispatcher.dispatch(() -> {
                deliveryAuditLog.recordDelivered(message, agent.getAgentName());
                agent.receive(message);
                deliveryAuditLog.recordConsumed(message, agent.getAgentName());
            });
        }
    }

    /**
     * 返回审计快照。
     *
     * @return 审计记录列表
     */
    @Override
    public List<DeliveryRecord> snapshotAuditRecords() {
        return deliveryAuditLog.snapshot();
    }

    /**
     * 关闭消息中心。
     */
    @Override
    public void close() {
        dispatcher.close();
    }
}

package com.xbk.agent.framework.engineering.handwritten.hub;

import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.trace.DeliveryRecord;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenMessageAgent;
import com.xbk.agent.framework.engineering.handwritten.mq.RocketMqMessageProducer;
import com.xbk.agent.framework.engineering.handwritten.runtime.DeliveryAuditLog;

import java.util.List;

/**
 * 基于 RocketMQ 的消息中心。
 *
 * 职责：与 InMemoryMessageHub 对外语义保持一致，区别在于消息通过 RocketMQ 传输，
 * 让 MessageHub 的消息驱动范式从内存模型平滑演进到真实消息中间件。
 *
 * <p>设计边界说明：
 * <ul>
 *   <li>subscribe 依然是本地注册，与 InMemoryMessageHub 相同；
 *   <li>publish 把消息发往 RocketMQ，不直接调用订阅者；
 *   <li>消息的真正送达来自 RocketMQ Broker 回调，由外部 @RocketMQMessageListener 触发 receiveFromMq；
 *   <li>receiveFromMq 是 MQ 侧唯一入口，它恢复了与 InMemoryMessageHub 相同的分发路径。
 * </ul>
 *
 * <p>这套设计体现了消息驱动范式的关键价值：
 * Agent 代码不感知传输介质，无论 InMemory 还是 MQ 实现，只有 publish/subscribe/close 三个动作。
 *
 * @author xiexu
 */
public class MqBackedMessageHub implements MessageHub {

    /**
     * 本地主题-订阅者注册表。
     * MQ 投递回来后，仍需通过这张表找到本地订阅者。
     */
    private final TopicSubscriptionRegistry subscriptionRegistry;

    /**
     * 异步分发器。
     * 与 InMemoryMessageHub 共用相同的线程池分发语义。
     */
    private final AsyncMessageDispatcher dispatcher;

    /**
     * 投递审计日志。
     * 记录 publish、deliver、consume 全生命周期，与内存版行为保持同构。
     */
    private final DeliveryAuditLog deliveryAuditLog;

    /**
     * RocketMQ 生产者。
     * publish 时把消息序列化并投到 Broker，真正实现"时序解耦"。
     */
    private final RocketMqMessageProducer producer;

    /**
     * 创建基于 RocketMQ 的消息中心。
     *
     * @param producer RocketMQ 消息生产者
     */
    public MqBackedMessageHub(RocketMqMessageProducer producer) {
        this.subscriptionRegistry = new TopicSubscriptionRegistry();
        this.dispatcher = new AsyncMessageDispatcher();
        this.deliveryAuditLog = new DeliveryAuditLog();
        this.producer = producer;
    }

    /**
     * 订阅主题。
     *
     * <p>注册关系仅在本地维护。当 MQ 消息回调触发 receiveFromMq 时，才通过这张表找到订阅者。
     *
     * @param topic 逻辑主题
     * @param agent 订阅方 Agent
     */
    @Override
    public void subscribe(String topic, HandwrittenMessageAgent agent) {
        subscriptionRegistry.subscribe(topic, agent);
    }

    /**
     * 发布消息。
     *
     * <p>先记录审计日志，再通过 RocketMQ 投递。消息不会在本方法里直达订阅者，
     * 真正的分发发生在 Broker 回调触发 receiveFromMq 之后。
     * 这正是消息驱动"时序解耦"的核心体现。
     *
     * @param message 工程消息
     */
    @Override
    public void publish(EngineeringMessage message) {
        deliveryAuditLog.recordPublished(message);
        producer.send(message);
    }

    /**
     * 从 RocketMQ 接收并分发消息。
     *
     * <p>这是 MQ 侧到本地订阅者的唯一入口，由外部 @RocketMQMessageListener 在消费到消息后调用。
     * 分发流程与 InMemoryMessageHub.publish 后半段完全一致，保证两套实现行为同构。
     *
     * @param message 从 MQ 反序列化得到的工程消息
     */
    public void receiveFromMq(EngineeringMessage message) {
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

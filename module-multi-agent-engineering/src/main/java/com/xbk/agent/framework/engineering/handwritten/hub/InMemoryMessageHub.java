package com.xbk.agent.framework.engineering.handwritten.hub;

import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.trace.DeliveryRecord;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenMessageAgent;
import com.xbk.agent.framework.engineering.handwritten.runtime.DeliveryAuditLog;

import java.util.List;

/**
 * 内存版消息中心。
 *
 * <p>这是整套手写版架构的"邮局"。
 * 所有 Agent 都不直接互相调用，而是通过邮局发消息、收消息——
 * 发件人不知道谁会收，收件人不知道谁发的，双方只认"频道名"（topic）。
 *
 * <p>内部三个组件各司其职：
 * <ul>
 *   <li>{@code subscriptionRegistry}：登记表，记录"哪个频道有哪些订阅者"；
 *   <li>{@code dispatcher}：投递员，把消息放进线程池异步送达，不阻塞发件人；
 *   <li>{@code deliveryAuditLog}：快递单，记录每条消息的发布/投递/消费三个时间点，供教学展示。
 * </ul>
 *
 * @author xiexu
 */
public class InMemoryMessageHub implements MessageHub {

    /** 订阅登记表：topic → 订阅该频道的 Agent 列表。*/
    private final TopicSubscriptionRegistry subscriptionRegistry;

    /** 异步投递员：拿到消息后丢进线程池，不阻塞当前线程。*/
    private final AsyncMessageDispatcher dispatcher;

    /** 快递单：记录每条消息的发布、投递、消费三个时间点。*/
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
     * <p>发件人调用这个方法把消息投进邮局，然后立刻返回，不等任何人处理完。
     * 邮局找出订阅了这个频道的所有 Agent，每人异步送一份，互不干扰。
     *
     * @param message 工程消息
     */
    @Override
    public void publish(EngineeringMessage message) {
        // 打上"已发布"时间戳，这是快递单的第一个时间点：消息进入邮局的时刻。
        deliveryAuditLog.recordPublished(message);

        // 找出订阅了这个频道的所有 Agent，每人各自异步收一份（广播语义）。
        // dispatcher 把每次 receive() 包进线程池任务，发件人不用等。
        List<HandwrittenMessageAgent> subscribers = subscriptionRegistry.getSubscribers(message.getTopic());
        for (HandwrittenMessageAgent agent : subscribers) {
            dispatcher.dispatch(() -> {
                // 快递单第二个时间点：开始送达（Agent 的 receive() 即将被调用）。
                deliveryAuditLog.recordDelivered(message, agent.getAgentName());
                agent.receive(message);
                // 快递单第三个时间点：Agent 处理完毕，消费结束。
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

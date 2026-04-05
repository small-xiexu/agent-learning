package com.xbk.agent.framework.engineering.handwritten.runtime;

import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.trace.DeliveryRecord;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 投递审计日志。
 *
 * <p>这是整条消息链的"快递记录单"。每条消息经历三个阶段，各打一次时间戳：
 * <ol>
 *   <li>publish：消息进入 MessageHub（邮局收件）；
 *   <li>deliver：消息开始送达某个 Agent（快递员敲门）；
 *   <li>consume：该 Agent 的 receive() 处理完毕（收件人签收）。
 * </ol>
 * 最终可以通过 {@code snapshot()} 拿到所有记录，还原出完整的消息流转时间轴。
 *
 * @author xiexu
 */
public class DeliveryAuditLog {

    // 多个 Agent 线程会并发往这里写记录，同时 snapshot() 随时可能被读取。
    // CopyOnWriteArrayList：每次写操作都复制一份新数组，读取时不加锁，
    // 适合"写少读多"的日志场景，保证快照读不会被并发写干扰。
    private final CopyOnWriteArrayList<DeliveryRecord> records = new CopyOnWriteArrayList<DeliveryRecord>();

    /**
     * 记录发布事件。
     *
     * @param message 消息
     */
    public void recordPublished(EngineeringMessage message) {
        records.add(toRecord("publish", message, null));
    }

    /**
     * 记录投递事件。
     *
     * @param message 消息
     * @param toAgent 目标 Agent
     */
    public void recordDelivered(EngineeringMessage message, String toAgent) {
        records.add(toRecord("deliver", message, toAgent));
    }

    /**
     * 记录消费事件。
     *
     * @param message 消息
     * @param toAgent 目标 Agent
     */
    public void recordConsumed(EngineeringMessage message, String toAgent) {
        records.add(toRecord("consume", message, toAgent));
    }

    /**
     * 返回所有投递记录的快照。
     *
     * @return 不可变的投递记录列表
     */
    public List<DeliveryRecord> snapshot() {
        // List.copyOf 返回不可变列表，调用方无法修改，日志数据不会被外部破坏。
        return List.copyOf(records);
    }

    /**
     * 构造一条投递记录。
     *
     * @param eventType 事件类型（publish / deliver / consume）
     * @param message 消息
     * @param toAgent 目标 Agent（publish 阶段为 null）
     * @return 投递记录
     */
    private DeliveryRecord toRecord(String eventType, EngineeringMessage message, String toAgent) {
        // 每条记录独立打一次当前时间，三条记录（publish/deliver/consume）的时间差
        // 可以看出消息在队列里等了多久、Agent 处理花了多少时间。
        return new DeliveryRecord(
                eventType,
                message.getMessageId(),
                message.getTopic(),
                message.getFromAgent(),
                toAgent,
                message.getCorrelationId(),
                Instant.now());
    }
}

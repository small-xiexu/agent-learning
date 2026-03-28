package com.xbk.agent.framework.engineering.handwritten.runtime;

import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.trace.DeliveryRecord;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 投递审计日志。
 *
 * 职责：把 publish、deliver、consume 等消息运行时事件收敛成统一投递记录。
 *
 * @author xiexu
 */
public class DeliveryAuditLog {

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
     * 返回快照。
     *
     * @return 投递记录快照
     */
    public List<DeliveryRecord> snapshot() {
        return List.copyOf(records);
    }

    /**
     * 构造投递记录。
     *
     * @param eventType 事件类型
     * @param message 消息
     * @param toAgent 目标 Agent
     * @return 投递记录
     */
    private DeliveryRecord toRecord(String eventType, EngineeringMessage message, String toAgent) {
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

package com.xbk.agent.framework.engineering.domain.trace;

import java.time.Instant;

/**
 * 消息投递记录。
 *
 * 职责：保留单条消息在运行时的 publish、deliver、consume 等审计事件。
 *
 * @author xiexu
 */
public final class DeliveryRecord {

    private final String eventType;
    private final String messageId;
    private final String topic;
    private final String fromAgent;
    private final String toAgent;
    private final String correlationId;
    private final Instant occurredAt;

    /**
     * 创建投递记录。
     *
     * @param eventType 事件类型
     * @param messageId 消息标识
     * @param topic 主题
     * @param fromAgent 发送方
     * @param toAgent 接收方
     * @param correlationId 关联标识
     * @param occurredAt 发生时间
     */
    public DeliveryRecord(String eventType,
                          String messageId,
                          String topic,
                          String fromAgent,
                          String toAgent,
                          String correlationId,
                          Instant occurredAt) {
        this.eventType = eventType;
        this.messageId = messageId;
        this.topic = topic;
        this.fromAgent = fromAgent;
        this.toAgent = toAgent;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTopic() {
        return topic;
    }

    public String getFromAgent() {
        return fromAgent;
    }

    public String getToAgent() {
        return toAgent;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}

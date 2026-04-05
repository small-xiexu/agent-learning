package com.xbk.agent.framework.engineering.domain.trace;

import java.time.Instant;

/**
 * 消息投递记录。
 * <p>
 * 职责：保留单条消息在运行时的 publish、deliver、consume 等审计事件。
 *
 * @author xiexu
 */
public final class DeliveryRecord {

    /**
     * 事件类型：publish（进入 Hub）/ deliver（开始投递给 Agent）/ consume（Agent 消费完成）。
     */
    private final String eventType;

    /**
     * 被追踪的消息标识，与 EngineeringMessage.messageId 对应，用于跨记录关联同一条消息的三段轨迹。
     */
    private final String messageId;

    /**
     * 消息主题，还原该消息在哪个频道上流转。
     */
    private final String topic;

    /**
     * 发送方 Agent 名称，publish 事件时即为原始发件方。
     */
    private final String fromAgent;

    /**
     * 接收方 Agent 名称，deliver / consume 事件时填写，publish 事件时为 null。
     */
    private final String toAgent;

    /**
     * 请求-回包关联标识，用于把同一次用户请求的所有投递记录归组过滤，写入 EngineeringTrace。
     */
    private final String correlationId;

    /**
     * 事件发生时间，三段记录的时间差可还原消息在 Hub 内排队和 Agent 处理各自耗时。
     */
    private final Instant occurredAt;

    /**
     * 创建投递记录。
     *
     * @param eventType     事件类型
     * @param messageId     消息标识
     * @param topic         主题
     * @param fromAgent     发送方
     * @param toAgent       接收方
     * @param correlationId 关联标识
     * @param occurredAt    发生时间
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

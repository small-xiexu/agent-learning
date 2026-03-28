package com.xbk.agent.framework.engineering.domain.message;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工程消息。
 *
 * 职责：作为手写版 MessageHub 和未来 MQ 实现的统一消息载体，承接主题、回包地址、关联标识和 payload。
 *
 * @author xiexu
 */
public final class EngineeringMessage {

    private final String messageId;
    private final String conversationId;
    private final String correlationId;
    private final String fromAgent;
    private final String toAgent;
    private final String topic;
    private final MessageType messageType;
    private final String replyTo;
    private final Instant timestamp;
    private final Object payload;
    private final Map<String, Object> headers;

    /**
     * 使用构建器创建消息。
     *
     * @param builder 构建器
     */
    private EngineeringMessage(Builder builder) {
        this.messageId = requireText(builder.messageId, "messageId");
        this.conversationId = requireText(builder.conversationId, "conversationId");
        this.correlationId = requireText(builder.correlationId, "correlationId");
        this.fromAgent = requireText(builder.fromAgent, "fromAgent");
        this.toAgent = builder.toAgent;
        this.topic = requireText(builder.topic, "topic");
        this.messageType = requireMessageType(builder.messageType);
        this.replyTo = builder.replyTo;
        this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
        this.payload = builder.payload;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.headers));
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getMessageId() {
        return messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getFromAgent() {
        return fromAgent;
    }

    public String getToAgent() {
        return toAgent;
    }

    public String getTopic() {
        return topic;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Object getPayload() {
        return payload;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    /**
     * 校验非空文本。
     *
     * @param value 字段值
     * @param field 字段名
     * @return 合法文本
     */
    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    /**
     * 校验消息类型。
     *
     * @param messageType 消息类型
     * @return 合法消息类型
     */
    private static MessageType requireMessageType(MessageType messageType) {
        if (messageType == null) {
            throw new IllegalArgumentException("messageType must not be null");
        }
        return messageType;
    }

    /**
     * 工程消息构建器。
     *
     * 职责：组装不可变消息对象。
     *
     * @author xiexu
     */
    public static final class Builder {

        private String messageId;
        private String conversationId;
        private String correlationId;
        private String fromAgent;
        private String toAgent;
        private String topic;
        private MessageType messageType;
        private String replyTo;
        private Instant timestamp;
        private Object payload;
        private Map<String, Object> headers = Collections.emptyMap();

        /**
         * 设置消息标识。
         *
         * @param messageId 消息标识
         * @return 构建器
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * 设置会话标识。
         *
         * @param conversationId 会话标识
         * @return 构建器
         */
        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        /**
         * 设置关联标识。
         *
         * @param correlationId 关联标识
         * @return 构建器
         */
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /**
         * 设置发送方 Agent。
         *
         * @param fromAgent 发送方
         * @return 构建器
         */
        public Builder fromAgent(String fromAgent) {
            this.fromAgent = fromAgent;
            return this;
        }

        /**
         * 设置目标 Agent。
         *
         * @param toAgent 目标 Agent
         * @return 构建器
         */
        public Builder toAgent(String toAgent) {
            this.toAgent = toAgent;
            return this;
        }

        /**
         * 设置主题。
         *
         * @param topic 主题
         * @return 构建器
         */
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        /**
         * 设置消息类型。
         *
         * @param messageType 消息类型
         * @return 构建器
         */
        public Builder messageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        /**
         * 设置回包主题。
         *
         * @param replyTo 回包主题
         * @return 构建器
         */
        public Builder replyTo(String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        /**
         * 设置时间戳。
         *
         * @param timestamp 时间戳
         * @return 构建器
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * 设置 payload。
         *
         * @param payload payload
         * @return 构建器
         */
        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        /**
         * 设置消息头。
         *
         * @param headers 消息头
         * @return 构建器
         */
        public Builder headers(Map<String, Object> headers) {
            this.headers = headers == null ? Collections.<String, Object>emptyMap() : headers;
            return this;
        }

        /**
         * 构建消息。
         *
         * @return 工程消息
         */
        public EngineeringMessage build() {
            return new EngineeringMessage(this);
        }
    }
}

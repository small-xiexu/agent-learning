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

    /** 消息唯一标识。每条消息独立生成，用于审计日志和 trace 中的消息级追踪。 */
    private final String messageId;

    /** 会话标识。同一次用户请求的所有消息共用同一个 conversationId，跨消息关联上下文。 */
    private final String conversationId;

    /**
     * 请求-回包关联标识。
     *
     * 由 Coordinator 生成并写入第一条消息，后续每一跳必须原样透传。
     * Receptionist 回包时用它在 PendingResponseRegistry 中找到对应的 Future 并唤醒 Coordinator 线程。
     * 是整条异步链和同步等待点之间的唯一桥梁。
     */
    private final String correlationId;

    /** 发送方 Agent 名称。用于 trace 还原"谁发了这条消息"，以及专家回包时填写 toAgent 字段。 */
    private final String fromAgent;

    /**
     * 目标 Agent 名称（可选）。
     *
     * 消息驱动模型下路由主要靠 topic，toAgent 是辅助语义，供接收方确认消息是否发给自己。
     * 广播消息可为 null。
     */
    private final String toAgent;

    /**
     * 消息主题。
     *
     * MessageHub 的路由键：订阅了该主题的 Agent 才会收到这条消息。
     * 取值见 MessageTopic 常量（CUSTOMER_REQUEST / SUPPORT_TECH_REQUEST / RECEPTIONIST_REPLY 等）。
     */
    private final String topic;

    /**
     * 消息类型。
     *
     * 标识消息在业务流程中的阶段：CUSTOMER_REQUEST / SPECIALIST_REQUEST / SPECIALIST_RESPONSE。
     * Receptionist 用它区分"处理用户请求"还是"处理专家回包"，决定进入哪条分支。
     */
    private final MessageType messageType;

    /**
     * 回包主题（可选）。
     *
     * 由发送方（Receptionist 转发时）填入，告知接收方（专家 Agent）处理完后把结果发到哪个主题。
     * 专家不需要硬编码回包地址，直接读取此字段即可，使路由规则对专家透明。
     */
    private final String replyTo;

    /**
     * 消息创建时间戳。
     *
     * 若构建时未显式设置则自动取当前时间（Instant.now()），用于 DeliveryAuditLog 的时间轴还原。
     */
    private final Instant timestamp;

    /**
     * 业务载荷。
     *
     * 运行时类型由 messageType 决定：
     * - CUSTOMER_REQUEST      → CustomerServiceRequest
     * - SPECIALIST_REQUEST    → SpecialistRequestPayload
     * - SPECIALIST_RESPONSE   → SpecialistResponsePayload
     * 接收方需根据 messageType 强转后使用。
     */
    private final Object payload;

    /**
     * 扩展消息头（不可变）。
     *
     * 用于传递不适合放在固定字段里的扩展元数据（如优先级、来源标签等）。
     * 默认为空 Map，不参与核心路由逻辑。
     */
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

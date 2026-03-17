package com.xbk.agent.framework.core.memory;

import com.xbk.agent.framework.core.common.enums.MessageRole;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 统一消息模型
 * <p>
 * 职责：封装对话、工具与系统消息的基础协议
 *
 * @author xiexu
 */
public final class Message {

    /**
     * 消息唯一标识，用于追踪、去重与引用。
     */
    private final String messageId;
    /**
     * 消息所属会话标识，用于隔离不同对话上下文。
     */
    private final String conversationId;
    /**
     * 消息角色，决定消息在上下文中的语义位置。
     */
    private final MessageRole role;
    /**
     * 消息主体内容，承载对话文本、思考结果或观察结果。
     */
    private final String content;
    /**
     * 消息可选名称，可用于角色名、参与者名或工具名。
     */
    private final String name;
    /**
     * 工具调用关联标识，用于把一次调用请求与工具结果对应起来。
     */
    private final String toolCallId;
    /**
     * 扩展元数据槽位，只承载附加信息，不替代核心协议字段。
     */
    private final Map<String, Object> metadata;
    /**
     * 消息创建时间，未显式指定时在构建阶段自动生成。
     */
    private final Instant createdAt;

    /**
     * 使用构建器创建消息对象
     *
     * @param builder 构建器
     */
    private Message(Builder builder) {
        this.messageId = requireText(builder.messageId, "messageId");
        this.conversationId = requireText(builder.conversationId, "conversationId");
        this.role = requireRole(builder.role);
        this.content = builder.content;
        this.name = builder.name;
        this.toolCallId = builder.toolCallId;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata));
        this.createdAt = builder.createdAt == null ? Instant.now() : builder.createdAt;
    }

    /**
     * 创建消息构建器
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

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getName() {
        return name;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 校验文本字段
     *
     * @param value 待校验文本
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
     * 校验消息角色
     *
     * @param role 消息角色
     * @return 合法角色
     */
    private static MessageRole requireRole(MessageRole role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return role;
    }

    /**
     * 消息构建器
     * <p>
     * 职责：组装不可变消息对象
     *
     * @author xiexu
     */
    public static final class Builder {

        private String messageId;
        private String conversationId;
        private MessageRole role;
        private String content;
        private String name;
        private String toolCallId;
        private Map<String, Object> metadata = Collections.emptyMap();
        private Instant createdAt;

        /**
         * 设置消息标识
         *
         * @param messageId 消息标识
         * @return 构建器
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * 设置会话标识
         *
         * @param conversationId 会话标识
         * @return 构建器
         */
        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        /**
         * 设置消息角色
         *
         * @param role 消息角色
         * @return 构建器
         */
        public Builder role(MessageRole role) {
            this.role = role;
            return this;
        }

        /**
         * 设置消息内容
         *
         * @param content 消息内容
         * @return 构建器
         */
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        /**
         * 设置消息名称
         *
         * @param name 消息名称
         * @return 构建器
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置工具调用标识
         *
         * @param toolCallId 工具调用标识
         * @return 构建器
         */
        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        /**
         * 设置元数据
         *
         * @param metadata 元数据
         * @return 构建器
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata == null ? Collections.<String, Object>emptyMap() : metadata;
            return this;
        }

        /**
         * 设置创建时间
         *
         * @param createdAt 创建时间
         * @return 构建器
         */
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * 构建消息对象
         *
         * @return 消息对象
         */
        public Message build() {
            return new Message(this);
        }
    }
}

package com.xbk.agent.framework.core.memory;

import com.xbk.agent.framework.core.common.enums.MessageRole;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 统一消息模型
 *
 * 职责：封装对话、工具与系统消息的基础协议
 *
 * @author xiexu
 */
public final class Message {

    private final String messageId;
    private final String conversationId;
    private final MessageRole role;
    private final String content;
    private final String name;
    private final String toolCallId;
    private final Map<String, Object> metadata;
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

    /**
     * 返回消息标识
     *
     * @return 消息标识
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 返回会话标识
     *
     * @return 会话标识
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * 返回消息角色
     *
     * @return 消息角色
     */
    public MessageRole getRole() {
        return role;
    }

    /**
     * 返回消息内容
     *
     * @return 消息内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 返回消息名称
     *
     * @return 消息名称
     */
    public String getName() {
        return name;
    }

    /**
     * 返回工具调用标识
     *
     * @return 工具调用标识
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * 返回只读元数据
     *
     * @return 元数据
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 返回创建时间
     *
     * @return 创建时间
     */
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
     *
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

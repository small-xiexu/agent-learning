package com.xbk.agent.framework.core.tool;

import com.xbk.agent.framework.core.memory.MemorySession;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 工具上下文
 *
 * 职责：向工具执行提供会话、智能体与共享属性
 *
 * @author xiexu
 */
public final class ToolContext {

    /**
     * 当前工具调用所属的会话标识。
     */
    private final String conversationId;
    /**
     * 发起本次工具调用的智能体标识。
     */
    private final String agentId;
    /**
     * 当前调用轮次标识，可用于区分同一会话中的不同步骤。
     */
    private final String turnId;
    /**
     * 当前会话的记忆视图，允许工具读取或补充上下文。
     */
    private final MemorySession memorySession;
    /**
     * 工具上下文共享属性，用于传递额外运行时数据。
     */
    private final Map<String, Object> attributes;

    /**
     * 使用构建器创建工具上下文
     *
     * @param builder 构建器
     */
    private ToolContext(Builder builder) {
        this.conversationId = requireText(builder.conversationId, "conversationId");
        this.agentId = requireText(builder.agentId, "agentId");
        this.turnId = requireText(builder.turnId, "turnId");
        this.memorySession = Objects.requireNonNull(builder.memorySession, "memorySession must not be null");
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.attributes));
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getTurnId() {
        return turnId;
    }

    public MemorySession getMemorySession() {
        return memorySession;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 校验文本字段
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
     * 工具上下文构建器
     *
     * 职责：组装不可变工具上下文
     *
     * @author xiexu
     */
    public static final class Builder {

        private String conversationId;
        private String agentId;
        private String turnId;
        private MemorySession memorySession;
        private Map<String, Object> attributes = Collections.emptyMap();

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
         * 设置智能体标识
         *
         * @param agentId 智能体标识
         * @return 构建器
         */
        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        /**
         * 设置轮次标识
         *
         * @param turnId 轮次标识
         * @return 构建器
         */
        public Builder turnId(String turnId) {
            this.turnId = turnId;
            return this;
        }

        /**
         * 设置会话内存
         *
         * @param memorySession 会话内存
         * @return 构建器
         */
        public Builder memorySession(MemorySession memorySession) {
            this.memorySession = memorySession;
            return this;
        }

        /**
         * 设置上下文属性
         *
         * @param attributes 上下文属性
         * @return 构建器
         */
        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes == null ? Collections.<String, Object>emptyMap() : attributes;
            return this;
        }

        /**
         * 构建工具上下文
         *
         * @return 工具上下文
         */
        public ToolContext build() {
            return new ToolContext(this);
        }
    }
}

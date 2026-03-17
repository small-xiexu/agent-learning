package com.xbk.agent.framework.core.llm.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具调用结果
 *
 * 职责：描述模型返回的工具调用指令
 *
 * @author xiexu
 */
public final class ToolCall {

    private final String toolCallId;
    private final String toolName;
    private final Map<String, Object> arguments;

    /**
     * 使用构建器创建工具调用
     *
     * @param builder 构建器
     */
    private ToolCall(Builder builder) {
        this.toolCallId = builder.toolCallId;
        this.toolName = builder.toolName;
        this.arguments = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.arguments));
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
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
     * 返回工具名称
     *
     * @return 工具名称
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * 返回调用参数
     *
     * @return 调用参数
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * 工具调用构建器
     *
     * 职责：组装不可变工具调用
     *
     * @author xiexu
     */
    public static final class Builder {

        private String toolCallId;
        private String toolName;
        private Map<String, Object> arguments = Collections.emptyMap();

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
         * 设置工具名称
         *
         * @param toolName 工具名称
         * @return 构建器
         */
        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        /**
         * 设置调用参数
         *
         * @param arguments 调用参数
         * @return 构建器
         */
        public Builder arguments(Map<String, Object> arguments) {
            this.arguments = arguments == null ? Collections.<String, Object>emptyMap() : arguments;
            return this;
        }

        /**
         * 构建工具调用
         *
         * @return 工具调用
         */
        public ToolCall build() {
            return new ToolCall(this);
        }
    }
}

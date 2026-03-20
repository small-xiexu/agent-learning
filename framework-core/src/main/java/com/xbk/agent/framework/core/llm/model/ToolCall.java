package com.xbk.agent.framework.core.llm.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具调用结果
 * 1、可以先把它理解成“模型发出来的一张工具任务单”
 * 2、它会告诉运行时：要调哪个工具、参数是什么、这次调用的 ID 是什么
 * 3、这里特别要注意：`ToolCall` 不是工具执行结果，而是模型发出的工具调用指令
 *
 * @author xiexu
 */
public final class ToolCall {

    /**
     * 单次工具调用标识，用于与工具结果或流式片段对齐。
     */
    private final String toolCallId;
    /**
     * 模型请求调用的工具名称。
     */
    private final String toolName;
    /**
     * 传递给工具的结构化参数。
     */
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

    public String getToolCallId() {
        return toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * 工具调用构建器
     * <p>
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

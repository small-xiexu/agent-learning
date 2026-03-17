package com.xbk.agent.framework.core.llm.option;

import com.xbk.agent.framework.core.common.enums.ToolChoiceMode;

/**
 * 工具调用选项
 *
 * 职责：定义模型调用工具时的行为配置
 *
 * @author xiexu
 */
public final class ToolCallingOptions {

    /**
     * 是否启用工具调用能力。
     */
    private final boolean enabled;
    /**
     * 工具选择模式，控制模型是自动选择还是被强制调用工具。
     */
    private final ToolChoiceMode toolChoiceMode;
    /**
     * 是否允许模型在一次轮次中并行发起多个工具调用。
     */
    private final boolean parallelToolCalls;
    /**
     * 模型与工具之间允许发生的最大往返次数。
     */
    private final int maxToolRoundTrips;
    /**
     * 工具结果是否自动写回下一轮模型上下文。
     */
    private final boolean includeToolResultsInContext;

    /**
     * 使用构建器创建工具调用选项
     *
     * @param builder 构建器
     */
    private ToolCallingOptions(Builder builder) {
        this.enabled = builder.enabled;
        this.toolChoiceMode = builder.toolChoiceMode;
        this.parallelToolCalls = builder.parallelToolCalls;
        this.maxToolRoundTrips = builder.maxToolRoundTrips;
        this.includeToolResultsInContext = builder.includeToolResultsInContext;
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ToolChoiceMode getToolChoiceMode() {
        return toolChoiceMode;
    }

    public boolean isParallelToolCalls() {
        return parallelToolCalls;
    }

    public int getMaxToolRoundTrips() {
        return maxToolRoundTrips;
    }

    public boolean isIncludeToolResultsInContext() {
        return includeToolResultsInContext;
    }

    /**
     * 工具调用选项构建器
     *
     * 职责：组装不可变工具调用选项
     *
     * @author xiexu
     */
    public static final class Builder {

        private boolean enabled;
        private ToolChoiceMode toolChoiceMode = ToolChoiceMode.NONE;
        private boolean parallelToolCalls;
        private int maxToolRoundTrips = 1;
        private boolean includeToolResultsInContext = true;

        /**
         * 设置是否启用工具调用
         *
         * @param enabled 是否启用
         * @return 构建器
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * 设置工具选择模式
         *
         * @param toolChoiceMode 工具选择模式
         * @return 构建器
         */
        public Builder toolChoiceMode(ToolChoiceMode toolChoiceMode) {
            this.toolChoiceMode = toolChoiceMode == null ? ToolChoiceMode.NONE : toolChoiceMode;
            return this;
        }

        /**
         * 设置是否允许并行工具调用
         *
         * @param parallelToolCalls 是否允许并行工具调用
         * @return 构建器
         */
        public Builder parallelToolCalls(boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        /**
         * 设置最大工具往返次数
         *
         * @param maxToolRoundTrips 最大工具往返次数
         * @return 构建器
         */
        public Builder maxToolRoundTrips(int maxToolRoundTrips) {
            this.maxToolRoundTrips = maxToolRoundTrips;
            return this;
        }

        /**
         * 设置是否将工具结果回写上下文
         *
         * @param includeToolResultsInContext 是否回写上下文
         * @return 构建器
         */
        public Builder includeToolResultsInContext(boolean includeToolResultsInContext) {
            this.includeToolResultsInContext = includeToolResultsInContext;
            return this;
        }

        /**
         * 构建工具调用选项
         *
         * @return 工具调用选项
         */
        public ToolCallingOptions build() {
            return new ToolCallingOptions(this);
        }
    }
}

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

    private final boolean enabled;
    private final ToolChoiceMode toolChoiceMode;
    private final boolean parallelToolCalls;
    private final int maxToolRoundTrips;
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

    /**
     * 返回是否启用工具调用
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 返回工具选择模式
     *
     * @return 工具选择模式
     */
    public ToolChoiceMode getToolChoiceMode() {
        return toolChoiceMode;
    }

    /**
     * 返回是否允许并行工具调用
     *
     * @return 是否允许并行工具调用
     */
    public boolean isParallelToolCalls() {
        return parallelToolCalls;
    }

    /**
     * 返回最大工具往返次数
     *
     * @return 最大工具往返次数
     */
    public int getMaxToolRoundTrips() {
        return maxToolRoundTrips;
    }

    /**
     * 返回是否将工具结果回写上下文
     *
     * @return 是否回写上下文
     */
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

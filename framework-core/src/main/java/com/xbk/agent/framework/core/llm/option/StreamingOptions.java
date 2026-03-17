package com.xbk.agent.framework.core.llm.option;

/**
 * 流式选项
 *
 * 职责：定义流式输出时的事件行为配置
 *
 * @author xiexu
 */
public final class StreamingOptions {

    /**
     * 是否启用流式输出能力。
     */
    private final boolean enabled;
    /**
     * 是否向上游发出文本增量事件。
     */
    private final boolean emitTextDelta;
    /**
     * 是否向上游发出工具调用增量事件。
     */
    private final boolean emitToolCallDelta;
    /**
     * 是否在流式完成时追加用量信息。
     */
    private final boolean emitUsageOnComplete;
    /**
     * 是否在流结束后聚合出最终完整消息。
     */
    private final boolean aggregateFinalMessage;

    /**
     * 使用构建器创建流式选项
     *
     * @param builder 构建器
     */
    private StreamingOptions(Builder builder) {
        this.enabled = builder.enabled;
        this.emitTextDelta = builder.emitTextDelta;
        this.emitToolCallDelta = builder.emitToolCallDelta;
        this.emitUsageOnComplete = builder.emitUsageOnComplete;
        this.aggregateFinalMessage = builder.aggregateFinalMessage;
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

    public boolean isEmitTextDelta() {
        return emitTextDelta;
    }

    public boolean isEmitToolCallDelta() {
        return emitToolCallDelta;
    }

    public boolean isEmitUsageOnComplete() {
        return emitUsageOnComplete;
    }

    public boolean isAggregateFinalMessage() {
        return aggregateFinalMessage;
    }

    /**
     * 流式选项构建器
     *
     * 职责：组装不可变流式选项
     *
     * @author xiexu
     */
    public static final class Builder {

        private boolean enabled;
        private boolean emitTextDelta = true;
        private boolean emitToolCallDelta = true;
        private boolean emitUsageOnComplete = true;
        private boolean aggregateFinalMessage = true;

        /**
         * 设置是否启用流式输出
         *
         * @param enabled 是否启用
         * @return 构建器
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * 设置是否输出文本增量
         *
         * @param emitTextDelta 是否输出文本增量
         * @return 构建器
         */
        public Builder emitTextDelta(boolean emitTextDelta) {
            this.emitTextDelta = emitTextDelta;
            return this;
        }

        /**
         * 设置是否输出工具调用增量
         *
         * @param emitToolCallDelta 是否输出工具调用增量
         * @return 构建器
         */
        public Builder emitToolCallDelta(boolean emitToolCallDelta) {
            this.emitToolCallDelta = emitToolCallDelta;
            return this;
        }

        /**
         * 设置是否在完成时输出用量
         *
         * @param emitUsageOnComplete 是否输出用量
         * @return 构建器
         */
        public Builder emitUsageOnComplete(boolean emitUsageOnComplete) {
            this.emitUsageOnComplete = emitUsageOnComplete;
            return this;
        }

        /**
         * 设置是否聚合最终消息
         *
         * @param aggregateFinalMessage 是否聚合最终消息
         * @return 构建器
         */
        public Builder aggregateFinalMessage(boolean aggregateFinalMessage) {
            this.aggregateFinalMessage = aggregateFinalMessage;
            return this;
        }

        /**
         * 构建流式选项
         *
         * @return 流式选项
         */
        public StreamingOptions build() {
            return new StreamingOptions(this);
        }
    }
}

package com.xbk.agent.framework.core.llm.model;

/**
 * LLM 用量信息
 *
 * 职责：记录一次调用的 token 统计
 *
 * @author xiexu
 */
public final class LlmUsage {

    /** 输入提示消耗的 token 数量。 */
    private final int inputTokens;
    /** 输出内容消耗的 token 数量。 */
    private final int outputTokens;
    /** 当前调用总 token 数，通常等于输入与输出之和。 */
    private final int totalTokens;

    /**
     * 使用构建器创建用量信息
     *
     * @param builder 构建器
     */
    private LlmUsage(Builder builder) {
        this.inputTokens = builder.inputTokens;
        this.outputTokens = builder.outputTokens;
        this.totalTokens = builder.totalTokens;
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    /**
     * 用量构建器
     *
     * 职责：组装不可变用量信息
     *
     * @author xiexu
     */
    public static final class Builder {

        private int inputTokens;
        private int outputTokens;
        private int totalTokens;

        /**
         * 设置输入 token 数
         *
         * @param inputTokens 输入 token 数
         * @return 构建器
         */
        public Builder inputTokens(int inputTokens) {
            this.inputTokens = inputTokens;
            return this;
        }

        /**
         * 设置输出 token 数
         *
         * @param outputTokens 输出 token 数
         * @return 构建器
         */
        public Builder outputTokens(int outputTokens) {
            this.outputTokens = outputTokens;
            return this;
        }

        /**
         * 设置总 token 数
         *
         * @param totalTokens 总 token 数
         * @return 构建器
         */
        public Builder totalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
            return this;
        }

        /**
         * 构建用量信息
         *
         * @return 用量信息
         */
        public LlmUsage build() {
            return new LlmUsage(this);
        }
    }
}

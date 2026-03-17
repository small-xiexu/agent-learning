package com.xbk.agent.framework.core.llm.model;

/**
 * 结构化 LLM 响应
 *
 * 职责：组合通用响应与结构化输出结果
 *
 * @param <T> 输出类型
 * @author xiexu
 */
public final class StructuredLlmResponse<T> {

    /**
     * 通用 LLM 响应部分，保留文本、工具和用量等公共信息。
     */
    private final LlmResponse response;
    /**
     * 反序列化后的结构化输出对象。
     */
    private final T structuredOutput;
    /**
     * 标记结构化输出是否通过了预期 schema 校验。
     */
    private final boolean schemaValidated;

    /**
     * 使用构建器创建结构化响应
     *
     * @param builder 构建器
     */
    private StructuredLlmResponse(Builder<T> builder) {
        this.response = builder.response;
        this.structuredOutput = builder.structuredOutput;
        this.schemaValidated = builder.schemaValidated;
    }

    /**
     * 创建构建器
     *
     * @param <T> 输出类型
     * @return 构建器
     */
    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    public LlmResponse getResponse() {
        return response;
    }

    public T getStructuredOutput() {
        return structuredOutput;
    }

    public boolean isSchemaValidated() {
        return schemaValidated;
    }

    /**
     * 结构化响应构建器
     *
     * 职责：组装不可变结构化响应
     *
     * @param <T> 输出类型
     * @author xiexu
     */
    public static final class Builder<T> {

        private LlmResponse response;
        private T structuredOutput;
        private boolean schemaValidated;

        /**
         * 设置通用响应
         *
         * @param response 通用响应
         * @return 构建器
         */
        public Builder<T> response(LlmResponse response) {
            this.response = response;
            return this;
        }

        /**
         * 设置结构化输出
         *
         * @param structuredOutput 结构化输出
         * @return 构建器
         */
        public Builder<T> structuredOutput(T structuredOutput) {
            this.structuredOutput = structuredOutput;
            return this;
        }

        /**
         * 设置是否通过 schema 校验
         *
         * @param schemaValidated 是否通过 schema 校验
         * @return 构建器
         */
        public Builder<T> schemaValidated(boolean schemaValidated) {
            this.schemaValidated = schemaValidated;
            return this;
        }

        /**
         * 构建结构化响应
         *
         * @return 结构化响应
         */
        public StructuredLlmResponse<T> build() {
            return new StructuredLlmResponse<T>(this);
        }
    }
}

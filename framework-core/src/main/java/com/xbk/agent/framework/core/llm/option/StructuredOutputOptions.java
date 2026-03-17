package com.xbk.agent.framework.core.llm.option;

/**
 * 结构化输出选项
 *
 * 职责：定义结构化输出时的 schema 行为
 *
 * @author xiexu
 */
public final class StructuredOutputOptions {

    private final boolean enabled;
    private final String schemaName;
    private final String schemaDescription;
    private final boolean strict;
    private final boolean includeRawTextFallback;

    /**
     * 使用构建器创建结构化输出选项
     *
     * @param builder 构建器
     */
    private StructuredOutputOptions(Builder builder) {
        this.enabled = builder.enabled;
        this.schemaName = builder.schemaName;
        this.schemaDescription = builder.schemaDescription;
        this.strict = builder.strict;
        this.includeRawTextFallback = builder.includeRawTextFallback;
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
     * 返回是否启用结构化输出
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 返回 schema 名称
     *
     * @return schema 名称
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * 返回 schema 描述
     *
     * @return schema 描述
     */
    public String getSchemaDescription() {
        return schemaDescription;
    }

    /**
     * 返回是否严格校验
     *
     * @return 是否严格校验
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * 返回是否包含文本回退
     *
     * @return 是否包含文本回退
     */
    public boolean isIncludeRawTextFallback() {
        return includeRawTextFallback;
    }

    /**
     * 结构化输出选项构建器
     *
     * 职责：组装不可变结构化输出选项
     *
     * @author xiexu
     */
    public static final class Builder {

        private boolean enabled;
        private String schemaName;
        private String schemaDescription;
        private boolean strict;
        private boolean includeRawTextFallback = true;

        /**
         * 设置是否启用结构化输出
         *
         * @param enabled 是否启用
         * @return 构建器
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * 设置 schema 名称
         *
         * @param schemaName schema 名称
         * @return 构建器
         */
        public Builder schemaName(String schemaName) {
            this.schemaName = schemaName;
            return this;
        }

        /**
         * 设置 schema 描述
         *
         * @param schemaDescription schema 描述
         * @return 构建器
         */
        public Builder schemaDescription(String schemaDescription) {
            this.schemaDescription = schemaDescription;
            return this;
        }

        /**
         * 设置是否严格校验
         *
         * @param strict 是否严格校验
         * @return 构建器
         */
        public Builder strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        /**
         * 设置是否包含文本回退
         *
         * @param includeRawTextFallback 是否包含文本回退
         * @return 构建器
         */
        public Builder includeRawTextFallback(boolean includeRawTextFallback) {
            this.includeRawTextFallback = includeRawTextFallback;
            return this;
        }

        /**
         * 构建结构化输出选项
         *
         * @return 结构化输出选项
         */
        public StructuredOutputOptions build() {
            return new StructuredOutputOptions(this);
        }
    }
}

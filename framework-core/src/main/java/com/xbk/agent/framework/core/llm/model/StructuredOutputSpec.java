package com.xbk.agent.framework.core.llm.model;

/**
 * 结构化输出定义
 *
 * 职责：描述目标类型与 schema 元数据
 *
 * @param <T> 输出类型
 * @author xiexu
 */
public final class StructuredOutputSpec<T> {

    private final Class<T> targetType;
    private final String schemaName;
    private final String schemaDescription;
    private final boolean strict;

    /**
     * 使用构建器创建结构化输出定义
     *
     * @param builder 构建器
     */
    private StructuredOutputSpec(Builder<T> builder) {
        if (builder.targetType == null) {
            throw new IllegalArgumentException("targetType must not be null");
        }
        this.targetType = builder.targetType;
        this.schemaName = builder.schemaName;
        this.schemaDescription = builder.schemaDescription;
        this.strict = builder.strict;
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

    /**
     * 返回目标类型
     *
     * @return 目标类型
     */
    public Class<T> getTargetType() {
        return targetType;
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
     * 结构化输出定义构建器
     *
     * 职责：组装不可变结构化输出定义
     *
     * @param <T> 输出类型
     * @author xiexu
     */
    public static final class Builder<T> {

        private Class<T> targetType;
        private String schemaName;
        private String schemaDescription;
        private boolean strict;

        /**
         * 设置目标类型
         *
         * @param targetType 目标类型
         * @return 构建器
         */
        public Builder<T> targetType(Class<T> targetType) {
            this.targetType = targetType;
            return this;
        }

        /**
         * 设置 schema 名称
         *
         * @param schemaName schema 名称
         * @return 构建器
         */
        public Builder<T> schemaName(String schemaName) {
            this.schemaName = schemaName;
            return this;
        }

        /**
         * 设置 schema 描述
         *
         * @param schemaDescription schema 描述
         * @return 构建器
         */
        public Builder<T> schemaDescription(String schemaDescription) {
            this.schemaDescription = schemaDescription;
            return this;
        }

        /**
         * 设置是否严格校验
         *
         * @param strict 是否严格校验
         * @return 构建器
         */
        public Builder<T> strict(boolean strict) {
            this.strict = strict;
            return this;
        }

        /**
         * 构建结构化输出定义
         *
         * @return 结构化输出定义
         */
        public StructuredOutputSpec<T> build() {
            return new StructuredOutputSpec<T>(this);
        }
    }
}

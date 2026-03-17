package com.xbk.agent.framework.core.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具定义
 *
 * 职责：描述工具名称、输入与输出语义
 *
 * @author xiexu
 */
public final class ToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;
    private final String outputDescription;
    private final List<String> tags;
    private final boolean idempotent;

    /**
     * 使用构建器创建工具定义
     *
     * @param builder 构建器
     */
    private ToolDefinition(Builder builder) {
        this.name = requireText(builder.name, "name");
        this.description = requireText(builder.description, "description");
        this.inputSchema = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.inputSchema));
        this.outputDescription = builder.outputDescription;
        this.tags = List.copyOf(builder.tags);
        this.idempotent = builder.idempotent;
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
     * 返回工具名称
     *
     * @return 工具名称
     */
    public String getName() {
        return name;
    }

    /**
     * 返回工具描述
     *
     * @return 工具描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 返回输入结构定义
     *
     * @return 输入结构定义
     */
    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    /**
     * 返回输出描述
     *
     * @return 输出描述
     */
    public String getOutputDescription() {
        return outputDescription;
    }

    /**
     * 返回标签列表
     *
     * @return 标签列表
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * 返回是否幂等
     *
     * @return 是否幂等
     */
    public boolean isIdempotent() {
        return idempotent;
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
     * 工具定义构建器
     *
     * 职责：组装不可变工具定义
     *
     * @author xiexu
     */
    public static final class Builder {

        private String name;
        private String description;
        private Map<String, Object> inputSchema = Collections.emptyMap();
        private String outputDescription;
        private List<String> tags = Collections.emptyList();
        private boolean idempotent;

        /**
         * 设置工具名称
         *
         * @param name 工具名称
         * @return 构建器
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置工具描述
         *
         * @param description 工具描述
         * @return 构建器
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * 设置输入结构
         *
         * @param inputSchema 输入结构
         * @return 构建器
         */
        public Builder inputSchema(Map<String, Object> inputSchema) {
            this.inputSchema = inputSchema == null ? Collections.<String, Object>emptyMap() : inputSchema;
            return this;
        }

        /**
         * 设置输出描述
         *
         * @param outputDescription 输出描述
         * @return 构建器
         */
        public Builder outputDescription(String outputDescription) {
            this.outputDescription = outputDescription;
            return this;
        }

        /**
         * 设置标签列表
         *
         * @param tags 标签列表
         * @return 构建器
         */
        public Builder tags(List<String> tags) {
            this.tags = tags == null ? Collections.<String>emptyList() : tags;
            return this;
        }

        /**
         * 设置是否幂等
         *
         * @param idempotent 是否幂等
         * @return 构建器
         */
        public Builder idempotent(boolean idempotent) {
            this.idempotent = idempotent;
            return this;
        }

        /**
         * 构建工具定义
         *
         * @return 工具定义
         */
        public ToolDefinition build() {
            return new ToolDefinition(this);
        }
    }
}

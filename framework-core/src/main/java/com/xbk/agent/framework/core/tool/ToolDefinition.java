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

    /** 工具唯一名称，是模型选择工具和注册中心查找工具的关键键。 */
    private final String name;
    /** 工具职责描述，供模型理解该工具的适用场景。 */
    private final String description;
    /** 工具输入结构定义，用于约束参数形态和字段语义。 */
    private final Map<String, Object> inputSchema;
    /** 工具输出语义说明，用于帮助模型理解调用结果。 */
    private final String outputDescription;
    /** 工具标签集合，可用于分类、过滤和路由。 */
    private final List<String> tags;
    /** 标记工具是否幂等，供调度和重试策略参考。 */
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    public String getOutputDescription() {
        return outputDescription;
    }

    public List<String> getTags() {
        return tags;
    }

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

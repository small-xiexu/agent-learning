package com.xbk.agent.framework.core.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具请求
 *
 * 职责：封装一次工具调用的输入参数
 *
 * @author xiexu
 */
public final class ToolRequest {

    private final String toolName;
    private final String invocationId;
    private final Map<String, Object> arguments;
    private final Map<String, Object> metadata;

    /**
     * 使用构建器创建工具请求
     *
     * @param builder 构建器
     */
    private ToolRequest(Builder builder) {
        this.toolName = requireText(builder.toolName, "toolName");
        this.invocationId = requireText(builder.invocationId, "invocationId");
        this.arguments = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.arguments));
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata));
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
    public String getToolName() {
        return toolName;
    }

    /**
     * 返回调用标识
     *
     * @return 调用标识
     */
    public String getInvocationId() {
        return invocationId;
    }

    /**
     * 返回参数映射
     *
     * @return 参数映射
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * 返回元数据
     *
     * @return 元数据
     */
    public Map<String, Object> getMetadata() {
        return metadata;
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
     * 工具请求构建器
     *
     * 职责：组装不可变工具请求
     *
     * @author xiexu
     */
    public static final class Builder {

        private String toolName;
        private String invocationId;
        private Map<String, Object> arguments = Collections.emptyMap();
        private Map<String, Object> metadata = Collections.emptyMap();

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
         * 设置调用标识
         *
         * @param invocationId 调用标识
         * @return 构建器
         */
        public Builder invocationId(String invocationId) {
            this.invocationId = invocationId;
            return this;
        }

        /**
         * 设置请求参数
         *
         * @param arguments 请求参数
         * @return 构建器
         */
        public Builder arguments(Map<String, Object> arguments) {
            this.arguments = arguments == null ? Collections.<String, Object>emptyMap() : arguments;
            return this;
        }

        /**
         * 设置元数据
         *
         * @param metadata 元数据
         * @return 构建器
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata == null ? Collections.<String, Object>emptyMap() : metadata;
            return this;
        }

        /**
         * 构建工具请求
         *
         * @return 工具请求
         */
        public ToolRequest build() {
            return new ToolRequest(this);
        }
    }
}

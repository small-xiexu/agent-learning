package com.xbk.agent.framework.core.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具结果
 *
 * 职责：承载工具执行后的文本与结构化结果
 *
 * @author xiexu
 */
public final class ToolResult {

    private final boolean success;
    private final String content;
    private final Map<String, Object> structuredData;
    private final String errorCode;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    /**
     * 使用构建器创建工具结果
     *
     * @param builder 构建器
     */
    private ToolResult(Builder builder) {
        this.success = builder.success;
        this.content = builder.content;
        this.structuredData = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.structuredData));
        this.errorCode = builder.errorCode;
        this.errorMessage = builder.errorMessage;
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
     * 返回是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 返回文本结果
     *
     * @return 文本结果
     */
    public String getContent() {
        return content;
    }

    /**
     * 返回结构化结果
     *
     * @return 结构化结果
     */
    public Map<String, Object> getStructuredData() {
        return structuredData;
    }

    /**
     * 返回错误码
     *
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 返回错误消息
     *
     * @return 错误消息
     */
    public String getErrorMessage() {
        return errorMessage;
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
     * 工具结果构建器
     *
     * 职责：组装不可变工具结果
     *
     * @author xiexu
     */
    public static final class Builder {

        private boolean success;
        private String content;
        private Map<String, Object> structuredData = Collections.emptyMap();
        private String errorCode;
        private String errorMessage;
        private Map<String, Object> metadata = Collections.emptyMap();

        /**
         * 设置成功标记
         *
         * @param success 成功标记
         * @return 构建器
         */
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        /**
         * 设置文本结果
         *
         * @param content 文本结果
         * @return 构建器
         */
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        /**
         * 设置结构化结果
         *
         * @param structuredData 结构化结果
         * @return 构建器
         */
        public Builder structuredData(Map<String, Object> structuredData) {
            this.structuredData = structuredData == null ? Collections.<String, Object>emptyMap() : structuredData;
            return this;
        }

        /**
         * 设置错误码
         *
         * @param errorCode 错误码
         * @return 构建器
         */
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * 设置错误消息
         *
         * @param errorMessage 错误消息
         * @return 构建器
         */
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
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
         * 构建工具结果
         *
         * @return 工具结果
         */
        public ToolResult build() {
            return new ToolResult(this);
        }
    }
}

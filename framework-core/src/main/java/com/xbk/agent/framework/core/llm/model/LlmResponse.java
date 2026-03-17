package com.xbk.agent.framework.core.llm.model;

import com.xbk.agent.framework.core.common.enums.LlmFinishReason;
import com.xbk.agent.framework.core.memory.Message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 响应
 *
 * 职责：承载同步对话结果与工具调用结果
 *
 * @author xiexu
 */
public final class LlmResponse {

    private final String requestId;
    private final String responseId;
    private final Message outputMessage;
    private final String rawText;
    private final List<ToolCall> toolCalls;
    private final LlmFinishReason finishReason;
    private final LlmUsage usage;
    private final Map<String, Object> metadata;

    /**
     * 使用构建器创建响应对象
     *
     * @param builder 构建器
     */
    private LlmResponse(Builder builder) {
        this.requestId = requireText(builder.requestId, "requestId");
        this.responseId = requireText(builder.responseId, "responseId");
        this.outputMessage = builder.outputMessage;
        this.rawText = builder.rawText;
        this.toolCalls = List.copyOf(builder.toolCalls);
        this.finishReason = builder.finishReason;
        this.usage = builder.usage;
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
     * 返回请求标识
     *
     * @return 请求标识
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 返回响应标识
     *
     * @return 响应标识
     */
    public String getResponseId() {
        return responseId;
    }

    /**
     * 返回输出消息
     *
     * @return 输出消息
     */
    public Message getOutputMessage() {
        return outputMessage;
    }

    /**
     * 返回原始文本
     *
     * @return 原始文本
     */
    public String getRawText() {
        return rawText;
    }

    /**
     * 返回工具调用列表
     *
     * @return 工具调用列表
     */
    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    /**
     * 返回结束原因
     *
     * @return 结束原因
     */
    public LlmFinishReason getFinishReason() {
        return finishReason;
    }

    /**
     * 返回用量信息
     *
     * @return 用量信息
     */
    public LlmUsage getUsage() {
        return usage;
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
     * LLM 响应构建器
     *
     * 职责：组装不可变响应对象
     *
     * @author xiexu
     */
    public static final class Builder {

        private String requestId;
        private String responseId;
        private Message outputMessage;
        private String rawText;
        private List<ToolCall> toolCalls = Collections.emptyList();
        private LlmFinishReason finishReason;
        private LlmUsage usage = LlmUsage.builder().build();
        private Map<String, Object> metadata = Collections.emptyMap();

        /**
         * 设置请求标识
         *
         * @param requestId 请求标识
         * @return 构建器
         */
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * 设置响应标识
         *
         * @param responseId 响应标识
         * @return 构建器
         */
        public Builder responseId(String responseId) {
            this.responseId = responseId;
            return this;
        }

        /**
         * 设置输出消息
         *
         * @param outputMessage 输出消息
         * @return 构建器
         */
        public Builder outputMessage(Message outputMessage) {
            this.outputMessage = outputMessage;
            return this;
        }

        /**
         * 设置原始文本
         *
         * @param rawText 原始文本
         * @return 构建器
         */
        public Builder rawText(String rawText) {
            this.rawText = rawText;
            return this;
        }

        /**
         * 设置工具调用列表
         *
         * @param toolCalls 工具调用列表
         * @return 构建器
         */
        public Builder toolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls == null ? Collections.<ToolCall>emptyList() : toolCalls;
            return this;
        }

        /**
         * 设置结束原因
         *
         * @param finishReason 结束原因
         * @return 构建器
         */
        public Builder finishReason(LlmFinishReason finishReason) {
            this.finishReason = finishReason;
            return this;
        }

        /**
         * 设置用量信息
         *
         * @param usage 用量信息
         * @return 构建器
         */
        public Builder usage(LlmUsage usage) {
            this.usage = usage == null ? LlmUsage.builder().build() : usage;
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
         * 构建响应对象
         *
         * @return 响应对象
         */
        public LlmResponse build() {
            return new LlmResponse(this);
        }
    }
}

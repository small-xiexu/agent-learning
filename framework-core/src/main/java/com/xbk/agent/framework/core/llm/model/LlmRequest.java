package com.xbk.agent.framework.core.llm.model;

import com.xbk.agent.framework.core.llm.option.ModelOptions;
import com.xbk.agent.framework.core.llm.option.StreamingOptions;
import com.xbk.agent.framework.core.llm.option.StructuredOutputOptions;
import com.xbk.agent.framework.core.llm.option.ToolCallingOptions;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.core.tool.ToolDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 请求
 *
 * 职责：封装统一门面调用所需的消息、工具与能力选项
 *
 * @author xiexu
 */
public final class LlmRequest {

    private final String requestId;
    private final String conversationId;
    private final List<Message> messages;
    private final List<ToolDefinition> availableTools;
    private final ModelOptions modelOptions;
    private final StreamingOptions streamingOptions;
    private final StructuredOutputOptions structuredOutputOptions;
    private final ToolCallingOptions toolCallingOptions;
    private final Map<String, Object> metadata;

    /**
     * 使用构建器创建请求对象
     *
     * @param builder 构建器
     */
    private LlmRequest(Builder builder) {
        this.requestId = requireText(builder.requestId, "requestId");
        this.conversationId = requireText(builder.conversationId, "conversationId");
        this.messages = List.copyOf(builder.messages);
        this.availableTools = List.copyOf(builder.availableTools);
        this.modelOptions = builder.modelOptions;
        this.streamingOptions = builder.streamingOptions;
        this.structuredOutputOptions = builder.structuredOutputOptions;
        this.toolCallingOptions = builder.toolCallingOptions;
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
     * 返回会话标识
     *
     * @return 会话标识
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * 返回消息列表
     *
     * @return 消息列表
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * 返回可用工具定义
     *
     * @return 工具定义列表
     */
    public List<ToolDefinition> getAvailableTools() {
        return availableTools;
    }

    /**
     * 返回模型选项
     *
     * @return 模型选项
     */
    public ModelOptions getModelOptions() {
        return modelOptions;
    }

    /**
     * 返回流式选项
     *
     * @return 流式选项
     */
    public StreamingOptions getStreamingOptions() {
        return streamingOptions;
    }

    /**
     * 返回结构化输出选项
     *
     * @return 结构化输出选项
     */
    public StructuredOutputOptions getStructuredOutputOptions() {
        return structuredOutputOptions;
    }

    /**
     * 返回工具调用选项
     *
     * @return 工具调用选项
     */
    public ToolCallingOptions getToolCallingOptions() {
        return toolCallingOptions;
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
     * LLM 请求构建器
     *
     * 职责：组装不可变请求对象
     *
     * @author xiexu
     */
    public static final class Builder {

        private String requestId;
        private String conversationId;
        private List<Message> messages = Collections.emptyList();
        private List<ToolDefinition> availableTools = Collections.emptyList();
        private ModelOptions modelOptions = ModelOptions.builder().build();
        private StreamingOptions streamingOptions = StreamingOptions.builder().build();
        private StructuredOutputOptions structuredOutputOptions = StructuredOutputOptions.builder().build();
        private ToolCallingOptions toolCallingOptions = ToolCallingOptions.builder().build();
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
         * 设置会话标识
         *
         * @param conversationId 会话标识
         * @return 构建器
         */
        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        /**
         * 设置消息列表
         *
         * @param messages 消息列表
         * @return 构建器
         */
        public Builder messages(List<Message> messages) {
            this.messages = messages == null ? Collections.<Message>emptyList() : messages;
            return this;
        }

        /**
         * 设置可用工具列表
         *
         * @param availableTools 可用工具列表
         * @return 构建器
         */
        public Builder availableTools(List<ToolDefinition> availableTools) {
            this.availableTools = availableTools == null ? Collections.<ToolDefinition>emptyList() : availableTools;
            return this;
        }

        /**
         * 设置模型选项
         *
         * @param modelOptions 模型选项
         * @return 构建器
         */
        public Builder modelOptions(ModelOptions modelOptions) {
            this.modelOptions = modelOptions == null ? ModelOptions.builder().build() : modelOptions;
            return this;
        }

        /**
         * 设置流式选项
         *
         * @param streamingOptions 流式选项
         * @return 构建器
         */
        public Builder streamingOptions(StreamingOptions streamingOptions) {
            this.streamingOptions = streamingOptions == null ? StreamingOptions.builder().build() : streamingOptions;
            return this;
        }

        /**
         * 设置结构化输出选项
         *
         * @param structuredOutputOptions 结构化输出选项
         * @return 构建器
         */
        public Builder structuredOutputOptions(StructuredOutputOptions structuredOutputOptions) {
            this.structuredOutputOptions = structuredOutputOptions == null
                    ? StructuredOutputOptions.builder().build()
                    : structuredOutputOptions;
            return this;
        }

        /**
         * 设置工具调用选项
         *
         * @param toolCallingOptions 工具调用选项
         * @return 构建器
         */
        public Builder toolCallingOptions(ToolCallingOptions toolCallingOptions) {
            this.toolCallingOptions = toolCallingOptions == null ? ToolCallingOptions.builder().build() : toolCallingOptions;
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
         * 构建请求对象
         *
         * @return 请求对象
         */
        public LlmRequest build() {
            return new LlmRequest(this);
        }
    }
}

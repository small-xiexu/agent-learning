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
 * 1、可以先把它理解成“这一轮发给模型的完整请求包”
 * 2、它里面装的不是只有 prompt，还包括消息列表、可用工具、模型参数、工具调用参数等
 * 3、也就是说，它代表的是“一次完整的模型调用输入”
 *
 * @author xiexu
 */
public final class LlmRequest {

    /**
     * 单次 LLM 调用请求标识，用于链路追踪与响应对账。
     */
    private final String requestId;
    /**
     * 当前调用归属的会话标识，用于多轮上下文隔离。
     */
    private final String conversationId;
    /**
     * 本轮请求发送给模型的完整消息列表。
     */
    private final List<Message> messages;
    /**
     * 当前允许模型感知和调用的工具定义集合。
     */
    private final List<ToolDefinition> availableTools;
    /**
     * 基础模型参数，例如模型名、温度、最大输出长度。
     */
    private final ModelOptions modelOptions;
    /**
     * 流式输出行为配置，仅在流式调用路径下生效。
     */
    private final StreamingOptions streamingOptions;
    /**
     * 结构化输出行为配置，用于约束 schema 相关能力。
     */
    private final StructuredOutputOptions structuredOutputOptions;
    /**
     * 工具调用行为配置，用于约束模型如何使用工具。
     */
    private final ToolCallingOptions toolCallingOptions;
    /**
     * 请求级扩展元数据，可承载追踪、租户和调试标签。
     */
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

    public String getRequestId() {
        return requestId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<ToolDefinition> getAvailableTools() {
        return availableTools;
    }

    public ModelOptions getModelOptions() {
        return modelOptions;
    }

    public StreamingOptions getStreamingOptions() {
        return streamingOptions;
    }

    public StructuredOutputOptions getStructuredOutputOptions() {
        return structuredOutputOptions;
    }

    public ToolCallingOptions getToolCallingOptions() {
        return toolCallingOptions;
    }

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
     * <p>
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

package com.xbk.agent.framework.core.llm.model;

import com.xbk.agent.framework.core.common.enums.LlmStreamEventType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流式事件
 *
 * 职责：承载模型流式输出中的增量事件
 *
 * @author xiexu
 */
public final class LlmStreamEvent {

    /**
     * 当前流式事件的唯一标识。
     */
    private final String eventId;
    /**
     * 事件类型，决定当前增量应按文本、工具或结束事件解释。
     */
    private final LlmStreamEventType type;
    /**
     * 文本增量片段，仅在文本流式输出时有值。
     */
    private final String textDelta;
    /**
     * 工具调用增量片段，仅在工具调用流式输出时有值。
     */
    private final ToolCallDelta toolCallDelta;
    /**
     * 截止当前事件的用量信息，通常在完成事件时最有参考价值。
     */
    private final LlmUsage usage;
    /**
     * 标记当前事件是否代表一次流式调用已经完成。
     */
    private final boolean completed;
    /**
     * 事件级扩展元数据，用于调试和底层兼容信息传递。
     */
    private final Map<String, Object> metadata;

    /**
     * 使用构建器创建流式事件
     *
     * @param builder 构建器
     */
    private LlmStreamEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.type = builder.type;
        this.textDelta = builder.textDelta;
        this.toolCallDelta = builder.toolCallDelta;
        this.usage = builder.usage;
        this.completed = builder.completed;
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

    public String getEventId() {
        return eventId;
    }

    public LlmStreamEventType getType() {
        return type;
    }

    public String getTextDelta() {
        return textDelta;
    }

    public ToolCallDelta getToolCallDelta() {
        return toolCallDelta;
    }

    public LlmUsage getUsage() {
        return usage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 流式事件构建器
     *
     * 职责：组装不可变流式事件
     *
     * @author xiexu
     */
    public static final class Builder {

        private String eventId;
        private LlmStreamEventType type;
        private String textDelta;
        private ToolCallDelta toolCallDelta;
        private LlmUsage usage;
        private boolean completed;
        private Map<String, Object> metadata = Collections.emptyMap();

        /**
         * 设置事件标识
         *
         * @param eventId 事件标识
         * @return 构建器
         */
        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        /**
         * 设置事件类型
         *
         * @param type 事件类型
         * @return 构建器
         */
        public Builder type(LlmStreamEventType type) {
            this.type = type;
            return this;
        }

        /**
         * 设置文本增量
         *
         * @param textDelta 文本增量
         * @return 构建器
         */
        public Builder textDelta(String textDelta) {
            this.textDelta = textDelta;
            return this;
        }

        /**
         * 设置工具调用增量
         *
         * @param toolCallDelta 工具调用增量
         * @return 构建器
         */
        public Builder toolCallDelta(ToolCallDelta toolCallDelta) {
            this.toolCallDelta = toolCallDelta;
            return this;
        }

        /**
         * 设置用量信息
         *
         * @param usage 用量信息
         * @return 构建器
         */
        public Builder usage(LlmUsage usage) {
            this.usage = usage;
            return this;
        }

        /**
         * 设置是否完成
         *
         * @param completed 是否完成
         * @return 构建器
         */
        public Builder completed(boolean completed) {
            this.completed = completed;
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
         * 构建流式事件
         *
         * @return 流式事件
         */
        public LlmStreamEvent build() {
            return new LlmStreamEvent(this);
        }
    }
}

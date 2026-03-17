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

    private final String eventId;
    private final LlmStreamEventType type;
    private final String textDelta;
    private final ToolCallDelta toolCallDelta;
    private final LlmUsage usage;
    private final boolean completed;
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

    /**
     * 返回事件标识
     *
     * @return 事件标识
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * 返回事件类型
     *
     * @return 事件类型
     */
    public LlmStreamEventType getType() {
        return type;
    }

    /**
     * 返回文本增量
     *
     * @return 文本增量
     */
    public String getTextDelta() {
        return textDelta;
    }

    /**
     * 返回工具调用增量
     *
     * @return 工具调用增量
     */
    public ToolCallDelta getToolCallDelta() {
        return toolCallDelta;
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
     * 返回是否完成
     *
     * @return 是否完成
     */
    public boolean isCompleted() {
        return completed;
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

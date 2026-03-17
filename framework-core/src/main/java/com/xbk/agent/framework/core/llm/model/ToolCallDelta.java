package com.xbk.agent.framework.core.llm.model;

/**
 * 工具调用增量
 *
 * 职责：描述流式输出中的工具调用片段
 *
 * @author xiexu
 */
public final class ToolCallDelta {

    private final String toolCallId;
    private final String toolName;
    private final String partialArgumentsText;

    /**
     * 使用构建器创建工具调用增量
     *
     * @param builder 构建器
     */
    private ToolCallDelta(Builder builder) {
        this.toolCallId = builder.toolCallId;
        this.toolName = builder.toolName;
        this.partialArgumentsText = builder.partialArgumentsText;
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
     * 返回工具调用标识
     *
     * @return 工具调用标识
     */
    public String getToolCallId() {
        return toolCallId;
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
     * 返回参数文本片段
     *
     * @return 参数文本片段
     */
    public String getPartialArgumentsText() {
        return partialArgumentsText;
    }

    /**
     * 工具调用增量构建器
     *
     * 职责：组装不可变工具调用增量
     *
     * @author xiexu
     */
    public static final class Builder {

        private String toolCallId;
        private String toolName;
        private String partialArgumentsText;

        /**
         * 设置工具调用标识
         *
         * @param toolCallId 工具调用标识
         * @return 构建器
         */
        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

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
         * 设置参数文本片段
         *
         * @param partialArgumentsText 参数文本片段
         * @return 构建器
         */
        public Builder partialArgumentsText(String partialArgumentsText) {
            this.partialArgumentsText = partialArgumentsText;
            return this;
        }

        /**
         * 构建工具调用增量
         *
         * @return 工具调用增量
         */
        public ToolCallDelta build() {
            return new ToolCallDelta(this);
        }
    }
}

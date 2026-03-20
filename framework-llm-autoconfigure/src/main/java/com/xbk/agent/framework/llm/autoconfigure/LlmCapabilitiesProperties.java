package com.xbk.agent.framework.llm.autoconfigure;

/**
 * LLM 能力配置
 *
 * 职责：表达当前 provider 声称支持的能力集合
 *
 * @author xiexu
 */
public class LlmCapabilitiesProperties {

    private boolean toolCalling;
    private boolean streaming;
    private boolean structuredOutput;

    public boolean isToolCalling() {
        return toolCalling;
    }

    public void setToolCalling(boolean toolCalling) {
        this.toolCalling = toolCalling;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    public boolean isStructuredOutput() {
        return structuredOutput;
    }

    public void setStructuredOutput(boolean structuredOutput) {
        this.structuredOutput = structuredOutput;
    }
}

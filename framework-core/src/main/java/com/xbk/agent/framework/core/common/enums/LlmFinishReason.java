package com.xbk.agent.framework.core.common.enums;

/**
 * LLM 结束原因枚举
 *
 * 职责：描述模型响应终止原因
 *
 * @author xiexu
 */
public enum LlmFinishReason {

    STOP,
    LENGTH,
    TOOL_CALL,
    CONTENT_FILTER,
    ERROR
}

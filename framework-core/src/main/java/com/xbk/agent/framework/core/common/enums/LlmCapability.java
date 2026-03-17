package com.xbk.agent.framework.core.common.enums;

/**
 * LLM 能力枚举
 *
 * 职责：声明底层模型适配器支持的能力集合
 *
 * @author xiexu
 */
public enum LlmCapability {

    SYNC_CHAT,
    STREAMING,
    STRUCTURED_OUTPUT,
    TOOL_CALLING
}

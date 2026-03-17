package com.xbk.agent.framework.core.common.enums;

/**
 * 流式事件类型枚举
 *
 * 职责：描述流式输出中的事件类别
 *
 * @author xiexu
 */
public enum LlmStreamEventType {

    TEXT_DELTA,
    TOOL_CALL_DELTA,
    USAGE,
    COMPLETE
}

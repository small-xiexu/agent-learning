package com.xbk.agent.framework.core.common.enums;

/**
 * LLM 结束原因枚举
 *
 * 职责：描述模型响应终止原因
 *
 * @author xiexu
 */
public enum LlmFinishReason {

    /**
     * 正常结束。
     *
     * 模型主动完成本轮输出，没有因为长度、过滤或异常被中断。
     */
    STOP,

    /**
     * 因输出长度限制结束。
     *
     * 常见于达到 maxTokens 或底层模型的响应上限。
     */
    LENGTH,

    /**
     * 因为发起工具调用而结束当前轮输出。
     *
     * 这通常不是失败，而是提示运行时先去执行工具，再继续后续轮次。
     */
    TOOL_CALL,

    /**
     * 因内容过滤策略结束。
     *
     * 常见于命中安全、合规或平台内容审查规则。
     */
    CONTENT_FILTER,

    /**
     * 因错误结束。
     *
     * 代表本轮调用出现异常，调用方通常需要结合错误上下文决定是否重试。
     */
    ERROR
}

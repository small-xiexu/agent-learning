package com.xbk.agent.framework.core.common.enums;

/**
 * LLM 能力枚举
 *
 * 职责：声明底层模型适配器支持的能力集合
 *
 * @author xiexu
 */
public enum LlmCapability {

    /**
     * 支持同步对话调用。
     *
     * 一次请求会直接返回完整响应，适合普通问答和非流式 Agent 编排。
     */
    SYNC_CHAT,

    /**
     * 支持流式输出。
     *
     * 模型会按片段持续返回内容，适合打字机效果、长文本生成和实时观察推理过程。
     */
    STREAMING,

    /**
     * 支持结构化输出。
     *
     * 模型结果可以按照约定的 Schema 或目标类型解析成对象，而不只是普通文本。
     */
    STRUCTURED_OUTPUT,

    /**
     * 支持工具调用。
     *
     * 模型可以在响应中发起工具请求，由运行时执行工具后再继续对话。
     */
    TOOL_CALLING
}

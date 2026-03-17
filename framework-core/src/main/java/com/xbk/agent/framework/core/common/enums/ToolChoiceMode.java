package com.xbk.agent.framework.core.common.enums;

/**
 * 工具选择模式枚举
 *
 * 职责：描述模型调用工具时的选择策略
 *
 * @author xiexu
 */
public enum ToolChoiceMode {

    /**
     * 不允许调用工具。
     *
     * 模型只能直接生成文本结果，适合纯问答或明确禁止外部副作用的场景。
     */
    NONE,

    /**
     * 由模型自行决定是否调用工具。
     *
     * 这是最常见的模式，适合 ReAct 这类需要边思考边决策的场景。
     */
    AUTO,

    /**
     * 要求模型必须调用工具。
     *
     * 如果当前轮没有工具调用，就视为不符合预期，常用于强约束工作流。
     */
    REQUIRED,

    /**
     * 强制模型只选择一个工具。
     *
     * 适合明确指定某一类工具路径，避免模型在同一轮发起多工具分叉。
     */
    FORCE_ONE
}

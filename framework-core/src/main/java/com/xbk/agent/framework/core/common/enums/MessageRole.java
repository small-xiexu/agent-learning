package com.xbk.agent.framework.core.common.enums;

/**
 * 消息角色枚举
 *
 * 职责：定义统一消息协议中的角色类型
 *
 * @author xiexu
 */
public enum MessageRole {

    /**
     * 系统消息。
     *
     * 用于注入角色设定、行为约束和全局指令，通常优先级最高。
     */
    SYSTEM,

    /**
     * 用户消息。
     *
     * 表示终端用户或外部调用方向 Agent 提出的输入问题与需求。
     */
    USER,

    /**
     * 助手消息。
     *
     * 表示模型或 Agent 给出的思考、回复、计划或最终答案。
     */
    ASSISTANT,

    /**
     * 工具消息。
     *
     * 表示工具执行后的观察结果，通常会回填给模型作为下一轮上下文。
     */
    TOOL
}

package com.xbk.agent.framework.engineering.domain.message;

/**
 * 工程消息类型。
 *
 * 职责：区分用户请求、专家请求与专家回包等运行时消息语义。
 *
 * @author xiexu
 */
public enum MessageType {

    /**
     * 用户请求。
     *
     * 由 Coordinator 发出，投递到 CUSTOMER_REQUEST 主题。
     * Receptionist 订阅该主题，收到后执行意图分类并转发给专家。
     */
    CUSTOMER_REQUEST,

    /**
     * 专家请求。
     *
     * 由 Receptionist 在意图分类后发出，投递到 SUPPORT_TECH_REQUEST 或 SUPPORT_SALES_REQUEST 主题。
     * 对应专家 Agent 订阅各自的主题，收到后调用模型生成答复。
     */
    SPECIALIST_REQUEST,

    /**
     * 专家回包。
     *
     * 由专家 Agent 处理完请求后发出，投递到消息中 replyTo 字段指定的主题（RECEPTIONIST_REPLY）。
     * Receptionist 收到后组装最终结果，调用 PendingResponseRegistry.complete() 唤醒 Coordinator。
     */
    SPECIALIST_RESPONSE
}

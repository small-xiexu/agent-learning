package com.xbk.agent.framework.engineering.domain.message;

/**
 * 工程消息类型。
 *
 * 职责：区分用户请求、专家请求与专家回包等运行时消息语义。
 *
 * @author xiexu
 */
public enum MessageType {

    CUSTOMER_REQUEST,
    SPECIALIST_REQUEST,
    SPECIALIST_RESPONSE
}

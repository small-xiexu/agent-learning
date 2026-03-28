package com.xbk.agent.framework.engineering.domain.message;

/**
 * 工程消息主题常量。
 *
 * 职责：统一手写版 MessageHub 使用的主题命名，避免各处散落裸字符串。
 *
 * @author xiexu
 */
public final class MessageTopic {

    public static final String CUSTOMER_REQUEST = "customer.request";
    public static final String SUPPORT_TECH_REQUEST = "support.tech.request";
    public static final String SUPPORT_SALES_REQUEST = "support.sales.request";
    public static final String RECEPTIONIST_REPLY = "support.reply.receptionist";
    public static final String DEAD_LETTER = "dead.letter";

    private MessageTopic() {
    }
}

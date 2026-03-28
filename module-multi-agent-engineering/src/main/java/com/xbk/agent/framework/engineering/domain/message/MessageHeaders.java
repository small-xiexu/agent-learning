package com.xbk.agent.framework.engineering.domain.message;

/**
 * 工程消息头键名。
 *
 * 职责：为扩展元数据保留统一字段名，避免不同实现对 metadata 键名理解不一致。
 *
 * @author xiexu
 */
public final class MessageHeaders {

    public static final String INTENT_TYPE = "intent_type";
    public static final String SPECIALIST_TYPE = "specialist_type";
    public static final String ORIGINAL_REQUEST = "original_request";

    private MessageHeaders() {
    }
}

package com.xbk.agent.framework.engineering.domain.message;

/**
 * 工程消息头键名。
 *
 * 职责：为扩展元数据保留统一字段名，避免不同实现对 metadata 键名理解不一致。
 *
 * @author xiexu
 */
public final class MessageHeaders {

    /** 意图类型头。可将 CustomerIntentType 枚举值序列化后写入，供下游无需解析 payload 即可读取意图。 */
    public static final String INTENT_TYPE = "intent_type";

    /** 专家类型头。可将 SpecialistType 枚举值写入，便于审计日志和 trace 快速定位处理方。 */
    public static final String SPECIALIST_TYPE = "specialist_type";

    /** 原始请求头。存放用户原始请求文本的冗余副本，供中间节点不解析 payload 也能读取请求内容。 */
    public static final String ORIGINAL_REQUEST = "original_request";

    private MessageHeaders() {
    }
}

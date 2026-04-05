package com.xbk.agent.framework.engineering.domain.message;

/**
 * 工程消息主题常量。
 * <p>
 * 职责：统一手写版 MessageHub 使用的主题命名，避免各处散落裸字符串。
 *
 * @author xiexu
 */
public final class MessageTopic {

    /**
     * 用户请求主题。Coordinator 发布用户请求，Receptionist 订阅并处理意图分类。
     */
    public static final String CUSTOMER_REQUEST = "customer.request";

    /**
     * 技术支持请求主题。Receptionist 在识别到技术诉求后，将专家请求发布到此主题，TechSupportAgent 订阅。
     */
    public static final String SUPPORT_TECH_REQUEST = "support.tech.request";

    /**
     * 销售咨询请求主题。Receptionist 在识别到商务诉求后，将专家请求发布到此主题，SalesAgent 订阅。
     */
    public static final String SUPPORT_SALES_REQUEST = "support.sales.request";

    /**
     * 接待员回包主题。专家处理完后按 replyTo 将结果发布到此主题，Receptionist 订阅并组装最终结果。
     */
    public static final String RECEPTIONIST_REPLY = "support.reply.receptionist";

    /**
     * 死信主题。消息处理失败或无法路由时的兜底投递目标，用于异常审计和人工排查。
     */
    public static final String DEAD_LETTER = "dead.letter";

    private MessageTopic() {
    }
}

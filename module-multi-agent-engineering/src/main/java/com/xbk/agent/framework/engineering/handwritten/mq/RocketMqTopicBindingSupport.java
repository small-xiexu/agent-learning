package com.xbk.agent.framework.engineering.handwritten.mq;

import com.xbk.agent.framework.engineering.domain.message.MessageTopic;

import java.util.HashMap;
import java.util.Map;

/**
 * RocketMQ 主题绑定支持。
 *
 * 职责：把手写版的逻辑主题（如 customer.request）映射成 RocketMQ 目的地（topic:tag）。
 * 这样 Agent 代码只看逻辑主题，MQ 传输细节收敛在这一个类里。
 *
 * <p>映射规则：逻辑主题 → "ENGINEERING_MSG:${TAG}"，TAG 由点替换为下划线并大写。
 *
 * <p>示例：
 * <pre>
 *   customer.request          → ENGINEERING_MSG:CUSTOMER_REQUEST
 *   support.tech.request      → ENGINEERING_MSG:SUPPORT_TECH_REQUEST
 *   support.sales.request     → ENGINEERING_MSG:SUPPORT_SALES_REQUEST
 *   support.reply.receptionist → ENGINEERING_MSG:SUPPORT_REPLY_RECEPTIONIST
 * </pre>
 *
 * @author xiexu
 */
public final class RocketMqTopicBindingSupport {

    /**
     * 统一 RocketMQ 主题名，所有工程消息共用一个 Topic，通过 Tag 区分子主题。
     * 这样做的好处是 Broker 端 Topic 数量可控，且一个 Consumer Group 可订阅全部消息。
     */
    public static final String ENGINEERING_TOPIC = "ENGINEERING_MSG";

    /**
     * 逻辑主题 → RocketMQ Tag 的正向映射表。
     */
    private static final Map<String, String> TOPIC_TO_TAG = new HashMap<String, String>();

    /**
     * RocketMQ Tag → 逻辑主题的反向映射表。
     */
    private static final Map<String, String> TAG_TO_TOPIC = new HashMap<String, String>();

    static {
        register(MessageTopic.CUSTOMER_REQUEST, "CUSTOMER_REQUEST");
        register(MessageTopic.SUPPORT_TECH_REQUEST, "SUPPORT_TECH_REQUEST");
        register(MessageTopic.SUPPORT_SALES_REQUEST, "SUPPORT_SALES_REQUEST");
        register(MessageTopic.RECEPTIONIST_REPLY, "SUPPORT_REPLY_RECEPTIONIST");
        register(MessageTopic.DEAD_LETTER, "DEAD_LETTER");
    }

    private RocketMqTopicBindingSupport() {
    }

    /**
     * 注册映射关系。
     *
     * @param logicalTopic 逻辑主题
     * @param tag RocketMQ Tag
     */
    private static void register(String logicalTopic, String tag) {
        TOPIC_TO_TAG.put(logicalTopic, tag);
        TAG_TO_TOPIC.put(tag, logicalTopic);
    }

    /**
     * 把逻辑主题转换为 RocketMQ 目的地（topic:tag）。
     *
     * @param logicalTopic 逻辑主题，如 "customer.request"
     * @return RocketMQ 目的地，如 "ENGINEERING_MSG:CUSTOMER_REQUEST"
     * @throws IllegalArgumentException 主题未注册
     */
    public static String toRocketMqDestination(String logicalTopic) {
        String tag = TOPIC_TO_TAG.get(logicalTopic);
        if (tag == null) {
            throw new IllegalArgumentException("Unregistered logical topic: " + logicalTopic);
        }
        return ENGINEERING_TOPIC + ":" + tag;
    }

    /**
     * 把 RocketMQ Tag 反解回逻辑主题。
     *
     * @param tag RocketMQ Tag，如 "CUSTOMER_REQUEST"
     * @return 逻辑主题，如 "customer.request"
     * @throws IllegalArgumentException Tag 未注册
     */
    public static String fromRocketMqTag(String tag) {
        String logicalTopic = TAG_TO_TOPIC.get(tag);
        if (logicalTopic == null) {
            throw new IllegalArgumentException("Unregistered RocketMQ tag: " + tag);
        }
        return logicalTopic;
    }

    /**
     * 返回统一的 RocketMQ 订阅表达式（订阅所有工程消息）。
     *
     * @return 订阅表达式，格式为 "TAG1 || TAG2 || ..."
     */
    public static String buildSubscribeExpression() {
        return String.join(" || ", TOPIC_TO_TAG.values());
    }
}

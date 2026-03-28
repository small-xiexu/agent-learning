package com.xbk.agent.framework.engineering.handwritten.hub;

import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenMessageAgent;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 主题订阅注册表。
 *
 * 职责：维护主题与订阅 Agent 列表的映射关系，供 MessageHub 发布消息时查找订阅方。
 *
 * @author xiexu
 */
public class TopicSubscriptionRegistry {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<HandwrittenMessageAgent>> subscriptions =
            new ConcurrentHashMap<String, CopyOnWriteArrayList<HandwrittenMessageAgent>>();

    /**
     * 注册主题订阅。
     *
     * @param topic 主题
     * @param agent Agent
     */
    public void subscribe(String topic, HandwrittenMessageAgent agent) {
        subscriptions.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<HandwrittenMessageAgent>())
                .add(agent);
    }

    /**
     * 返回主题订阅者列表。
     *
     * @param topic 主题
     * @return 订阅者列表
     */
    public List<HandwrittenMessageAgent> getSubscribers(String topic) {
        return subscriptions.getOrDefault(topic, new CopyOnWriteArrayList<HandwrittenMessageAgent>());
    }
}

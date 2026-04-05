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

    // 外层 ConcurrentHashMap：subscribe() 和 getSubscribers() 可能被不同线程并发调用，需要线程安全的 Map。
    // 内层 CopyOnWriteArrayList：同一主题的订阅者列表读多写少（订阅只在构造期，读在每次 publish），
    // CopyOnWriteArrayList 的写时复制保证迭代时不需要加锁，适合这种场景。
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<HandwrittenMessageAgent>> subscriptions =
            new ConcurrentHashMap<String, CopyOnWriteArrayList<HandwrittenMessageAgent>>();

    /**
     * 注册主题订阅。
     *
     * @param topic 主题
     * @param agent Agent
     */
    public void subscribe(String topic, HandwrittenMessageAgent agent) {
        // computeIfAbsent 保证同一个 topic 只创建一次列表，即使并发调用也不会覆盖已有订阅者。
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
        // 未注册的主题返回空列表而非 null，调用方可直接 for-each 而不必做 null 判断。
        return subscriptions.getOrDefault(topic, new CopyOnWriteArrayList<HandwrittenMessageAgent>());
    }
}

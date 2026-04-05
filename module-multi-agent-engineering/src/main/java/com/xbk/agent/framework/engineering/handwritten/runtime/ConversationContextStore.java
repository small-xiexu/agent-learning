package com.xbk.agent.framework.engineering.handwritten.runtime;

import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 会话上下文存储。
 *
 * <p>前台在处理一次请求时，要经历两个阶段：
 * <ol>
 *   <li>收到用户问题，判断意图，把问题转发给专家；
 *   <li>收到专家答复，组装最终结果。
 * </ol>
 * 这两个阶段之间有时间间隔，运行在不同的线程调用里。
 * 这个类就是用来跨越这个间隔保存中间状态的——
 * 原始请求是什么、路由给了哪位专家、走过哪些节点，都存在这里。
 *
 * @author xiexu
 */
public class ConversationContextStore {

    // 前台的两个阶段在不同线程执行，写入和读取会并发发生，必须用线程安全的 Map。
    private final ConcurrentHashMap<String, ConversationContext> contexts =
            new ConcurrentHashMap<String, ConversationContext>();

    /**
     * 注册会话，保存原始请求文本。
     *
     * @param conversationId 会话标识
     * @param requestText    原始请求
     */
    public void registerConversation(String conversationId, String requestText) {
        // putIfAbsent：确保同一个 conversationId 只写入一次，重复调用不会覆盖已有记录。
        contexts.putIfAbsent(conversationId, new ConversationContext(requestText));
    }

    /**
     * 记录路由决策。
     *
     * @param conversationId  会话标识
     * @param routingDecision 路由决策
     */
    public void recordRoutingDecision(String conversationId, RoutingDecision routingDecision) {
        ConversationContext context = requireContext(conversationId);
        context.setRoutingDecision(routingDecision);
    }

    /**
     * 记录路由轨迹。
     *
     * @param conversationId 会话标识
     * @param routeStep      路由节点
     */
    public void recordRoute(String conversationId, String routeStep) {
        ConversationContext context = requireContext(conversationId);
        context.getRouteTrail().add(routeStep);
    }

    /**
     * 返回请求文本。
     *
     * @param conversationId 会话标识
     * @return 请求文本
     */
    public String getRequestText(String conversationId) {
        return requireContext(conversationId).getRequestText();
    }

    /**
     * 返回路由决策。
     *
     * @param conversationId 会话标识
     * @return 路由决策
     */
    public RoutingDecision getRoutingDecision(String conversationId) {
        return requireContext(conversationId).getRoutingDecision();
    }

    /**
     * 返回路由轨迹。
     *
     * @param conversationId 会话标识
     * @return 路由轨迹的副本
     */
    public List<String> getRouteTrail(String conversationId) {
        // 返回副本，不暴露内部列表引用。
        // 调用方拿到后随便改，也不会影响这里保存的数据。
        return new ArrayList<String>(requireContext(conversationId).getRouteTrail());
    }

    /**
     * 返回会话上下文，找不到时快速失败。
     *
     * @param conversationId 会话标识
     * @return 会话上下文
     */
    private ConversationContext requireContext(String conversationId) {
        ConversationContext context = contexts.get(conversationId);
        // 如果找不到，说明在 registerConversation() 之前就来读了，属于代码逻辑错误，
        // 直接抛异常比悄悄返回 null 更容易发现问题。
        if (context == null) {
            throw new IllegalStateException("No conversation context for " + conversationId);
        }
        return context;
    }

    /**
     * 会话上下文。
     *
     * <p>一次请求对应一个 ConversationContext，里面装着这次请求从开始到结束要用到的所有中间数据。
     *
     * @author xiexu
     */
    private static final class ConversationContext {

        /**
         * 用户的原始问题，在整条链里传来传去时保持不变，最终写入结果对象。
         */
        private final String requestText;

        // 路由轨迹：记录消息经过了哪些 Agent（比如 receptionist_agent → tech_support_agent）。
        // 前台处理两个阶段时都在往这里追加，两个阶段可能在不同线程，
        // 用 CopyOnWriteArrayList 保证并发追加时不出问题。
        private final CopyOnWriteArrayList<String> routeTrail = new CopyOnWriteArrayList<String>();

        /**
         * LLM 判断出的路由决策：应该转给哪位专家、理由是什么。回包阶段需要读取它来组装结果。
         */
        private RoutingDecision routingDecision;

        /**
         * 创建会话上下文。
         *
         * @param requestText 原始请求
         */
        private ConversationContext(String requestText) {
            this.requestText = requestText;
        }

        /**
         * 返回请求文本。
         *
         * @return 请求文本
         */
        private String getRequestText() {
            return requestText;
        }

        /**
         * 返回路由轨迹。
         *
         * @return 路由轨迹
         */
        private CopyOnWriteArrayList<String> getRouteTrail() {
            return routeTrail;
        }

        /**
         * 返回路由决策。
         *
         * @return 路由决策
         */
        private RoutingDecision getRoutingDecision() {
            return routingDecision;
        }

        /**
         * 设置路由决策。
         *
         * @param routingDecision 路由决策
         */
        private void setRoutingDecision(RoutingDecision routingDecision) {
            this.routingDecision = routingDecision;
        }
    }
}

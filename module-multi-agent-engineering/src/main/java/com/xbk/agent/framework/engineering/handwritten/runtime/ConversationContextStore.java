package com.xbk.agent.framework.engineering.handwritten.runtime;

import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 会话上下文存储。
 *
 * 职责：保存单次客服链路中的原始请求、路由决策和路由轨迹，供回包组装结果时复用。
 *
 * @author xiexu
 */
public class ConversationContextStore {

    private final ConcurrentHashMap<String, ConversationContext> contexts =
            new ConcurrentHashMap<String, ConversationContext>();

    /**
     * 注册会话。
     *
     * @param conversationId 会话标识
     * @param requestText 原始请求
     */
    public void registerConversation(String conversationId, String requestText) {
        contexts.putIfAbsent(conversationId, new ConversationContext(requestText));
    }

    /**
     * 记录路由决策。
     *
     * @param conversationId 会话标识
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
     * @param routeStep 路由节点
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
     * @return 路由轨迹
     */
    public List<String> getRouteTrail(String conversationId) {
        return new ArrayList<String>(requireContext(conversationId).getRouteTrail());
    }

    /**
     * 返回会话上下文。
     *
     * @param conversationId 会话标识
     * @return 会话上下文
     */
    private ConversationContext requireContext(String conversationId) {
        ConversationContext context = contexts.get(conversationId);
        if (context == null) {
            throw new IllegalStateException("No conversation context for " + conversationId);
        }
        return context;
    }

    /**
     * 会话上下文。
     *
     * 职责：承载一次客服请求在路由期间需要复用的局部状态。
     *
     * @author xiexu
     */
    private static final class ConversationContext {

        private final String requestText;
        private final CopyOnWriteArrayList<String> routeTrail = new CopyOnWriteArrayList<String>();
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

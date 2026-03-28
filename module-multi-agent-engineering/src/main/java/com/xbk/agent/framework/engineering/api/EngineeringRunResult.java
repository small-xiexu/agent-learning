package com.xbk.agent.framework.engineering.api;

import com.xbk.agent.framework.engineering.domain.routing.CustomerIntentType;
import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;
import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.domain.trace.EngineeringTrace;

import java.util.List;

/**
 * 工程模块运行结果。
 *
 * 职责：统一承载用户请求、路由决策、专家答复、最终回复和投递轨迹，供手写版与框架版对照使用。
 *
 * @author xiexu
 */
public final class EngineeringRunResult {

    private final String conversationId;
    private final String requestText;
    private final CustomerIntentType intentType;
    private final SpecialistType specialistType;
    private final RoutingDecision routingDecision;
    private final String specialistResponse;
    private final String finalResponse;
    private final List<String> routeTrail;
    private final EngineeringTrace trace;

    /**
     * 使用构建器创建结果对象。
     *
     * @param builder 构建器
     */
    private EngineeringRunResult(Builder builder) {
        this.conversationId = builder.conversationId;
        this.requestText = builder.requestText;
        this.intentType = builder.intentType;
        this.specialistType = builder.specialistType;
        this.routingDecision = builder.routingDecision;
        this.specialistResponse = builder.specialistResponse;
        this.finalResponse = builder.finalResponse;
        this.routeTrail = builder.routeTrail == null ? List.<String>of() : List.copyOf(builder.routeTrail);
        this.trace = builder.trace;
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getRequestText() {
        return requestText;
    }

    public CustomerIntentType getIntentType() {
        return intentType;
    }

    public SpecialistType getSpecialistType() {
        return specialistType;
    }

    public RoutingDecision getRoutingDecision() {
        return routingDecision;
    }

    public String getSpecialistResponse() {
        return specialistResponse;
    }

    public String getFinalResponse() {
        return finalResponse;
    }

    public List<String> getRouteTrail() {
        return routeTrail;
    }

    public EngineeringTrace getTrace() {
        return trace;
    }

    /**
     * 运行结果构建器。
     *
     * 职责：组装不可变运行结果。
     *
     * @author xiexu
     */
    public static final class Builder {

        private String conversationId;
        private String requestText;
        private CustomerIntentType intentType;
        private SpecialistType specialistType;
        private RoutingDecision routingDecision;
        private String specialistResponse;
        private String finalResponse;
        private List<String> routeTrail;
        private EngineeringTrace trace;

        /**
         * 设置会话标识。
         *
         * @param conversationId 会话标识
         * @return 构建器
         */
        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        /**
         * 设置请求文本。
         *
         * @param requestText 请求文本
         * @return 构建器
         */
        public Builder requestText(String requestText) {
            this.requestText = requestText;
            return this;
        }

        /**
         * 设置意图类型。
         *
         * @param intentType 意图类型
         * @return 构建器
         */
        public Builder intentType(CustomerIntentType intentType) {
            this.intentType = intentType;
            return this;
        }

        /**
         * 设置专家类型。
         *
         * @param specialistType 专家类型
         * @return 构建器
         */
        public Builder specialistType(SpecialistType specialistType) {
            this.specialistType = specialistType;
            return this;
        }

        /**
         * 设置路由决策。
         *
         * @param routingDecision 路由决策
         * @return 构建器
         */
        public Builder routingDecision(RoutingDecision routingDecision) {
            this.routingDecision = routingDecision;
            return this;
        }

        /**
         * 设置专家响应。
         *
         * @param specialistResponse 专家响应
         * @return 构建器
         */
        public Builder specialistResponse(String specialistResponse) {
            this.specialistResponse = specialistResponse;
            return this;
        }

        /**
         * 设置最终回复。
         *
         * @param finalResponse 最终回复
         * @return 构建器
         */
        public Builder finalResponse(String finalResponse) {
            this.finalResponse = finalResponse;
            return this;
        }

        /**
         * 设置路由轨迹。
         *
         * @param routeTrail 路由轨迹
         * @return 构建器
         */
        public Builder routeTrail(List<String> routeTrail) {
            this.routeTrail = routeTrail;
            return this;
        }

        /**
         * 设置工程轨迹。
         *
         * @param trace 工程轨迹
         * @return 构建器
         */
        public Builder trace(EngineeringTrace trace) {
            this.trace = trace;
            return this;
        }

        /**
         * 构建运行结果。
         *
         * @return 运行结果
         */
        public EngineeringRunResult build() {
            return new EngineeringRunResult(this);
        }
    }
}

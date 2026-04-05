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

    /** 会话唯一标识。用于在多轮对话或日志中关联同一次用户会话的所有请求与响应。 */
    private final String conversationId;

    /** 用户原始请求文本。路由器、意图分类器的输入来源，最终也会写入 trace 供审计回溯。 */
    private final String requestText;

    /**
     * 意图分类结果。
     *
     * 由 IntentClassifier（意图分类节点）识别并写入，SpecialistRouter 读取后据此决定转交哪个专家 Agent。
     * 是路由链的第一个控制字段：分类错则后续所有路由都会走偏。
     */
    private final CustomerIntentType intentType;

    /**
     * 命中的专家类型。
     *
     * 由 SpecialistRouter 根据 intentType 映射得出，标识本次请求最终由哪个专家 Agent 处理。
     * 手写版与框架版均写入此字段，供调用方判断"谁回答了这个问题"。
     */
    private final SpecialistType specialistType;

    /**
     * 路由决策详情。
     *
     * 包含路由器的完整判断过程（置信度、候选路径等），比 specialistType 更细粒度。
     * 主要用于调试和 trace 展示；业务逻辑一般只需读 specialistType。
     */
    private final RoutingDecision routingDecision;

    /** 专家 Agent 的原始回复。未经 Synthesizer 加工，保留原始语义，用于 trace 对比和调试。 */
    private final String specialistResponse;

    /**
     * 最终对外输出的回复文本。
     *
     * 由 ResponseSynthesizer 在 specialistResponse 基础上润色生成，是整条流水线的最终产物，
     * 也是调用方（演示入口、API 层）实际展示给用户的内容。
     */
    private final String finalResponse;

    /**
     * 路由轨迹列表。
     *
     * 按顺序记录本次请求经过的每个节点名称（如 IntentClassifier → SpecialistRouter → …），
     * 用于可视化流水线执行路径，以及排查路由跳转是否符合预期。
     */
    private final List<String> routeTrail;

    /**
     * 工程执行轨迹。
     *
     * 聚合了手写版与框架版对照所需的全量调试信息（节点耗时、中间状态快照等）。
     * 仅用于教学演示和对照分析，生产环境可按需裁剪。
     */
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

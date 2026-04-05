package com.xbk.agent.framework.engineering.domain.routing;

/**
 * 路由决策。
 *
 * 职责：统一承载意图识别后的目标主题、目标 Agent 和决策理由，供手写版与框架版共用。
 *
 * @author xiexu
 */
public final class RoutingDecision {

    /** LLM 识别出的用户意图类别，决定后续路由方向。 */
    private final CustomerIntentType intentType;

    /** 与 intentType 对应的专家类型，最终写入 EngineeringRunResult 标识"谁回答了这个问题"。 */
    private final SpecialistType specialistType;

    /** 路由决策的自然语言理由，由 LLM 生成，用于 trace 展示和调试，不参与路由逻辑。 */
    private final String reason;

    /** 目标消息主题（如 SUPPORT_TECH_REQUEST），Receptionist 据此将专家请求发布到正确主题。 */
    private final String targetTopic;

    /** 目标 Agent 名称（如 "tech_support_agent"），写入消息的 toAgent 字段和路由轨迹。 */
    private final String targetAgentName;

    /**
     * 创建路由决策。
     *
     * @param intentType 意图类型
     * @param specialistType 专家类型
     * @param reason 决策理由
     * @param targetTopic 目标主题
     * @param targetAgentName 目标 Agent 名称
     */
    public RoutingDecision(CustomerIntentType intentType,
                           SpecialistType specialistType,
                           String reason,
                           String targetTopic,
                           String targetAgentName) {
        this.intentType = intentType;
        this.specialistType = specialistType;
        this.reason = reason;
        this.targetTopic = targetTopic;
        this.targetAgentName = targetAgentName;
    }

    public CustomerIntentType getIntentType() {
        return intentType;
    }

    public SpecialistType getSpecialistType() {
        return specialistType;
    }

    public String getReason() {
        return reason;
    }

    public String getTargetTopic() {
        return targetTopic;
    }

    public String getTargetAgentName() {
        return targetAgentName;
    }
}

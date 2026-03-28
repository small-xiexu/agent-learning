package com.xbk.agent.framework.engineering.domain.routing;

/**
 * 路由决策。
 *
 * 职责：统一承载意图识别后的目标主题、目标 Agent 和决策理由，供手写版与框架版共用。
 *
 * @author xiexu
 */
public final class RoutingDecision {

    private final CustomerIntentType intentType;
    private final SpecialistType specialistType;
    private final String reason;
    private final String targetTopic;
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

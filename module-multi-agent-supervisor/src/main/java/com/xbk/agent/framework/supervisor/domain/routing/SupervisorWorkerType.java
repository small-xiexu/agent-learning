package com.xbk.agent.framework.supervisor.domain.routing;

/**
 * Supervisor 可路由的 Worker 类型
 *
 * 职责：统一手写版 JSON 路由值与框架版子 Agent 名称映射，
 * 让两套实现虽然运行时不同，但能共享同一套路由枚举语义
 *
 * @author xiexu
 */
public enum SupervisorWorkerType {

    WRITER("WRITER", "writer_agent"),
    TRANSLATOR("TRANSLATOR", "translator_agent"),
    REVIEWER("REVIEWER", "reviewer_agent"),
    FINISH("FINISH", "FINISH");

    private final String decisionValue;
    private final String frameworkAgentName;

    SupervisorWorkerType(String decisionValue, String frameworkAgentName) {
        this.decisionValue = decisionValue;
        this.frameworkAgentName = frameworkAgentName;
    }

    /**
     * 返回手写版 JSON 决策值。
     *
     * @return 决策值
     */
    public String getDecisionValue() {
        return decisionValue;
    }

    /**
     * 返回框架版子 Agent 名称。
     *
     * @return 子 Agent 名称
     */
    public String getFrameworkAgentName() {
        return frameworkAgentName;
    }

    /**
     * 根据路由文本解析 Worker 类型。
     *
     * 手写版通常传入 `WRITER` 这类 JSON 枚举值，框架版通常传入
     * `writer_agent` 这类子 Agent 名称；这里统一兼容两种来源。
     *
     * @param rawValue 原始路由文本
     * @return Worker 类型
     */
    public static SupervisorWorkerType fromRouteValue(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Supervisor route value must not be blank");
        }
        String normalizedValue = rawValue.trim();
        for (SupervisorWorkerType workerType : values()) {
            if (workerType.decisionValue.equalsIgnoreCase(normalizedValue)
                    || workerType.frameworkAgentName.equalsIgnoreCase(normalizedValue)) {
                return workerType;
            }
        }
        throw new IllegalArgumentException("Unsupported supervisor worker type: " + rawValue);
    }
}

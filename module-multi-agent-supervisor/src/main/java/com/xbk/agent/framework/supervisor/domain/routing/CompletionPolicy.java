package com.xbk.agent.framework.supervisor.domain.routing;

/**
 * Supervisor 完成策略
 *
 * 职责：统一限制监督者最大调度轮次，避免动态路由无限回环。
 * 手写版用它控制 `while` 循环，框架版则通过同类思路映射到 recursion limit。
 *
 * @author xiexu
 */
public final class CompletionPolicy {

    private final int maxRounds;

    /**
     * 创建完成策略。
     *
     * @param maxRounds 最大轮次
     */
    public CompletionPolicy(int maxRounds) {
        if (maxRounds <= 0) {
            throw new IllegalArgumentException("maxRounds must be greater than zero");
        }
        this.maxRounds = maxRounds;
    }

    /**
     * 返回最大轮次。
     *
     * @return 最大轮次
     */
    public int getMaxRounds() {
        return maxRounds;
    }

    /**
     * 判断当前轮次是否仍允许继续。
     *
     * 这里的 completedDecisionRounds 指“监督者已经做出过多少次路由决策”，
     * 而不是 Worker 已经成功执行了多少步。
     *
     * @param completedDecisionRounds 已完成的监督者决策轮次
     * @return true 表示仍可继续
     */
    public boolean allowsNextRound(int completedDecisionRounds) {
        return completedDecisionRounds < maxRounds;
    }
}

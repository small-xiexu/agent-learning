package com.xbk.agent.framework.supervisor.domain.routing;

/**
 * Supervisor 完成策略
 *
 * 职责：统一限制监督者最大调度轮次，避免动态路由无限回环。
 * 手写版用它控制 while 循环，框架版则通过同类思路映射到 recursion limit。
 *
 * <p><b>核心概念澄清：调度轮次 vs 实际执行步数</b>
 * <pre>
 *   调度轮次（completedDecisionRounds）：Supervisor 做出路由决策的次数
 *     → 即"Supervisor 调用 LLM 并解析出下一个 Worker"的次数
 *
 *   实际执行步数：Worker 真正执行任务的次数
 *     → 通常与调度轮次相等，但若 Supervisor 多次路由到同一个 Worker，
 *       同一个 Worker 可能被执行多次
 * </pre>
 * maxRounds 约束的是"调度轮次"而非"执行步数"，
 * 因为 Supervisor 的职责是路由控制，它本身的决策次数才是真正需要被限制的资源。
 *
 * <p>手写版与框架版的映射：
 * <pre>
 *   手写版：HandwrittenSupervisorCoordinator 里的 while 循环判断
 *     → while (policy.allowsNextRound(completedRounds)) { ... }
 *
 *   框架版：AlibabaSupervisorFlowAgent 构造时的 recursionLimit
 *     → CompileConfig.builder().recursionLimit(maxRounds * 4)
 *     → 框架内部每轮包含 Supervisor 调度 + Worker 执行等多个步骤，因此需放大
 * </pre>
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

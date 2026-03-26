package com.xbk.agent.framework.planreplan.infrastructure.agentframework.support;

/**
 * Plan-and-Solve 框架版状态键
 *
 * 职责：统一声明顺序编排版 Plan-and-Solve Agent 中所有节点读写 OverAllState 时使用的 key 常量。
 * PlannerAgent 和 ExecutorAgent 通过 outputKey 写入、instruction 占位符读取这些 key，
 * 任何一处写错都会导致 Planner 产出无法被 Executor 正确消费。
 *
 * <p>各 key 的生产者与消费者：
 * <pre>
 *   key             生产者（写入）                      消费者（读取）
 *   ──────────────────────────────────────────────────────────────────
 *   "input"         AlibabaSequentialPlanAndSolveAgent.run() 初始化
 *                                                  → PlannerAgent instruction（{input}）
 *                                                  → ExecutorAgent instruction（{input}）
 *
 *   "plan_result"   PlannerAgent（outputKey）       → ExecutorAgent instruction（{plan_result}）
 *                                                  → run() 提取规划结果
 *
 *   "final_answer"  ExecutorAgent（outputKey）      → run() 提取最终答案
 * </pre>
 *
 * @author xiexu
 */
public final class PlanReplanStateKeys {

    /**
     * 原始问题，由 run() 初始化，PlannerAgent 和 ExecutorAgent 的 instruction 占位符消费。
     */
    public static final String INPUT = "input";

    /**
     * 规划结果，由 PlannerAgent 通过 outputKey 写入，ExecutorAgent 的 instruction 占位符消费。
     */
    public static final String PLAN_RESULT = "plan_result";

    /**
     * 最终答案，由 ExecutorAgent 通过 outputKey 写入，run() 提取为最终结果。
     */
    public static final String FINAL_ANSWER = "final_answer";

    private PlanReplanStateKeys() {
    }
}

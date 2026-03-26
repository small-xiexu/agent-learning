package com.xbk.agent.framework.reflection.infrastructure.agentframework.support;

/**
 * Reflection 框架版状态键
 *
 * 职责：统一声明框架版 Reflection Agent 中所有节点读写 OverAllState 时使用的 key 常量。
 * 这些 key 是 JavaCoderNode、JavaReviewerNode 与 ReactAgent instruction 占位符三方的隐式协议，
 * 任何一处写错都会导致上下文传递断链。
 *
 * <p>各 key 的生产者与消费者：
 * <pre>
 *   key                生产者（写入）                    消费者（读取）
 *   ────────────────────────────────────────────────────────────────────
 *   "input"            AlibabaReflectionFlowAgent.run() 初始化
 *                                                  → JavaCoderNode（{input} 占位符）
 *                                                  → JavaReviewerNode（{input} 占位符）
 *
 *   "current_code"     JavaCoderAgent（outputKey）  → JavaReviewerNode（{current_code} 占位符）
 *                                                  → continueOrStop 条件边（终止判断辅助）
 *                                                  → run() 提取最终代码
 *
 *   "review_feedback"  JavaReviewerAgent（outputKey）→ JavaCoderNode（{review_feedback} 占位符）
 *                                                  → continueOrStop 条件边（"无需改进"判断）
 *                                                  → run() 提取最终评审
 *
 *   "iteration_count"  JavaReviewerNode（每轮 +1）  → continueOrStop 条件边（轮次上限判断）
 *                                                  → run() 提取实际迭代轮次
 * </pre>
 *
 * @author xiexu
 */
public final class ReflectionStateKeys {

    /**
     * 原始任务，由 run() 初始化，JavaCoderNode 和 JavaReviewerNode 的 instruction 占位符消费。
     */
    public static final String INPUT = "input";

    /**
     * 当前代码，由 JavaCoderAgent 通过 outputKey 写入，JavaReviewerNode 和条件边消费。
     */
    public static final String CURRENT_CODE = "current_code";

    /**
     * 评审意见，由 JavaReviewerAgent 通过 outputKey 写入，JavaCoderNode 和条件边消费。
     */
    public static final String REVIEW_FEEDBACK = "review_feedback";

    /**
     * 迭代轮次，由 JavaReviewerNode 每轮递增，条件边用于判断是否达到最大轮次上限。
     */
    public static final String ITERATION_COUNT = "iteration_count";

    private ReflectionStateKeys() {
    }
}

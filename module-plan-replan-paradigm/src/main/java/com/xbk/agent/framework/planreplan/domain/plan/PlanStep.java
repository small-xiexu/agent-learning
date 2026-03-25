package com.xbk.agent.framework.planreplan.domain.plan;

/**
 * 计划步骤
 *
 * 职责：表示 Plan-and-Solve 范式中 Planner 拆解出的单个可执行步骤，
 * 是整个计划列表的最小粒度单元。
 *
 * <p>在 Plan-and-Solve 的流动路径中：
 * <pre>
 *   Planner → List&lt;PlanStep&gt; → HandwrittenPlanAndSolveAgent（逐步取出） → Executor
 *                                                                          → StepExecutionRecord（执行后归档）
 * </pre>
 * PlanStep 只负责"是什么要做"，StepExecutionRecord 负责"做完后结果是什么"，
 * 两者分离保证了计划定义与执行历史互不干扰。
 *
 * <p>设计为 final + 全字段 final：计划一旦由 Planner 生成就不可修改，
 * 避免 Executor 在执行中途篡改步骤描述，保证计划的一致性与可回放性。
 *
 * @author xiexu
 */
public final class PlanStep {

    /**
     * 步骤在计划列表中的编号（从 1 开始），供 Executor 在 Prompt 中引用，
     * 让模型知道当前执行的是第几步，便于生成连贯的分步回答。
     */
    private final int stepIndex;

    /**
     * Planner 生成的自然语言步骤描述，直接作为 Executor 的任务指令传入 Prompt。
     */
    private final String instruction;

    /**
     * 创建计划步骤。
     *
     * @param stepIndex   步骤编号（从 1 开始）
     * @param instruction Planner 生成的步骤描述
     */
    public PlanStep(int stepIndex, String instruction) {
        this.stepIndex = stepIndex;
        this.instruction = instruction;
    }

    /**
     * 返回步骤编号。
     *
     * @return 步骤编号
     */
    public int getStepIndex() {
        return stepIndex;
    }

    /**
     * 返回步骤描述。
     *
     * @return 步骤描述
     */
    public String getInstruction() {
        return instruction;
    }
}

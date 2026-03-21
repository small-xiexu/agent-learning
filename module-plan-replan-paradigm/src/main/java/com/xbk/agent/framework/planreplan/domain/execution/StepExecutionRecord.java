package com.xbk.agent.framework.planreplan.domain.execution;

import com.xbk.agent.framework.planreplan.domain.plan.PlanStep;

/**
 * 步骤执行记录
 *
 * 职责：保存某个计划步骤及其执行结果，供后续步骤继续引用
 *
 * @author xiexu
 */
public final class StepExecutionRecord {

    private final PlanStep planStep;
    private final String stepResult;

    /**
     * 创建步骤执行记录。
     *
     * @param planStep 计划步骤
     * @param stepResult 步骤结果
     */
    public StepExecutionRecord(PlanStep planStep, String stepResult) {
        this.planStep = planStep;
        this.stepResult = stepResult;
    }

    public PlanStep getPlanStep() {
        return planStep;
    }

    public String getStepResult() {
        return stepResult;
    }
}

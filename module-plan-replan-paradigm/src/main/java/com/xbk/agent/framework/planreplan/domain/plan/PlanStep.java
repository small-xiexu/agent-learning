package com.xbk.agent.framework.planreplan.domain.plan;

/**
 * 计划步骤
 *
 * 职责：表示 Plan-and-Solve 中的单个可执行步骤
 *
 * @author xiexu
 */
public final class PlanStep {

    private final int stepIndex;
    private final String instruction;

    /**
     * 创建计划步骤。
     *
     * @param stepIndex 步骤编号
     * @param instruction 步骤说明
     */
    public PlanStep(int stepIndex, String instruction) {
        this.stepIndex = stepIndex;
        this.instruction = instruction;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public String getInstruction() {
        return instruction;
    }
}

package com.xbk.agent.framework.planreplan.domain.execution;

import com.xbk.agent.framework.planreplan.domain.plan.PlanStep;

/**
 * 步骤执行记录
 *
 * 职责：将 PlanStep（计划定义）与 Executor 的执行结果绑定在一起，
 * 形成可追溯的"输入步骤 → 输出结果"快照，是 Plan-and-Solve 历史积累机制的基本单元。
 *
 * <p>在 Plan-and-Solve 的流动路径中：
 * <pre>
 *   Executor 执行 PlanStep → StepExecutionRecord → 追加进 executionHistory
 *                                                      ↓
 *                                          下一步 Executor 把完整 history 拼入 Prompt
 *                                          → 让模型基于"已做过什么"生成连贯的下一步回答
 * </pre>
 *
 * <p>核心设计意图："历史累加"而非"覆盖状态"。
 * 每一步执行完后不会更新全局状态，而是追加一条新记录。
 * 这样 Executor 在后续步骤中始终能看到前面所有步骤的完整轨迹，
 * 避免了单步执行时上下文丢失导致模型给出前后矛盾的回答。
 *
 * <p>设计为 final + 全字段 final：执行结果一旦产生就不可修改，
 * 保证历史轨迹的不可篡改性，便于测试断言和问题回放。
 *
 * @author xiexu
 */
public final class StepExecutionRecord {

    /**
     * 原始计划步骤，包含步骤编号和 Planner 的描述。
     * 执行记录持有它是为了在拼接历史 Prompt 时能同时展示"计划了什么"和"执行结果是什么"。
     */
    private final PlanStep planStep;

    /**
     * Executor 调用 LLM 后得到的步骤执行结果文本。
     * 此结果会在后续步骤的 Prompt 中作为"历史上下文"传入，帮助模型保持连贯性。
     */
    private final String stepResult;

    /**
     * 创建步骤执行记录。
     *
     * @param planStep   已执行的计划步骤
     * @param stepResult Executor 产出的执行结果文本
     */
    public StepExecutionRecord(PlanStep planStep, String stepResult) {
        this.planStep = planStep;
        this.stepResult = stepResult;
    }

    /**
     * 返回原始计划步骤。
     *
     * @return 原始计划步骤
     */
    public PlanStep getPlanStep() {
        return planStep;
    }

    /**
     * 返回步骤执行结果。
     *
     * @return 执行结果文本
     */
    public String getStepResult() {
        return stepResult;
    }
}

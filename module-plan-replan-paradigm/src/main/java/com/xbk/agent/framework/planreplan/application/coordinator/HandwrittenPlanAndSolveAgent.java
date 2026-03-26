package com.xbk.agent.framework.planreplan.application.coordinator;

import com.xbk.agent.framework.planreplan.application.executor.HandwrittenExecutor;
import com.xbk.agent.framework.planreplan.application.executor.HandwrittenPlanner;
import com.xbk.agent.framework.planreplan.domain.execution.StepExecutionRecord;
import com.xbk.agent.framework.planreplan.domain.plan.PlanStep;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 手写版 Plan-and-Solve 协调器
 *
 * 职责：先调用 Planner 生成计划，再用 Java 循环逐步执行并手动累加历史状态
 *
 * @author xiexu
 */
public class HandwrittenPlanAndSolveAgent {

    private final HandwrittenPlanner planner;
    private final HandwrittenExecutor executor;

    /**
     * 创建手写版协调器。
     *
     * @param planner 手写版规划器
     * @param executor 手写版执行器
     */
    public HandwrittenPlanAndSolveAgent(HandwrittenPlanner planner, HandwrittenExecutor executor) {
        this.planner = planner;
        this.executor = executor;
    }

    /**
     * 运行手写版 Plan-and-Solve。
     *
     * @param question 原始问题
     * @return 运行结果
     */
    public RunResult run(String question) {
        String conversationId = "handwritten-plan-solve-" + UUID.randomUUID();
        // Planner 和后续每一步 Executor 共用同一个 conversationId，
        // 这样整次 Plan-and-Solve 运行在日志和底层 provider 视角里就是同一条连续会话。
        List<PlanStep> plan = planner.plan(question, conversationId);
        List<StepExecutionRecord> history = new ArrayList<StepExecutionRecord>();

        // 手写版的关键不是“有一个 plan”，而是“每执行完一步都把阶段结果显式写回 history”。
        // 下一步执行时，Executor 会重新消费 question + plan + history，
        // 相当于由 Java 代码手动维护“当前已经完成到哪里”的运行时事实。
        for (PlanStep step : plan) {
            String stepResult = executor.execute(question, plan, history, step, conversationId);
            history.add(new StepExecutionRecord(step, stepResult));
        }

        // 这个示例把最后一步结果直接视为最终答案，
        // 目的是让学习者清楚看到“最终答案其实就是计划执行链路的最后一个阶段产物”。
        String finalAnswer = history.isEmpty() ? "" : history.getLast().getStepResult();
        return new RunResult(question, plan, history, finalAnswer);
    }

    /**
     * 手写版运行结果。
     *
     * 职责：统一返回问题、计划、历史执行记录与最终答案
     *
     * @author xiexu
     */
    public static final class RunResult {

        private final String question;
        private final List<PlanStep> plan;
        private final List<StepExecutionRecord> history;
        private final String finalAnswer;

        /**
         * 创建运行结果。
         *
         * @param question 原始问题
         * @param plan 完整计划
         * @param history 历史记录
         * @param finalAnswer 最终答案
         */
        public RunResult(String question, List<PlanStep> plan, List<StepExecutionRecord> history, String finalAnswer) {
            this.question = question;
            this.plan = List.copyOf(plan);
            this.history = List.copyOf(history);
            this.finalAnswer = finalAnswer;
        }

        public String getQuestion() {
            return question;
        }

        public List<PlanStep> getPlan() {
            return plan;
        }

        public List<StepExecutionRecord> getHistory() {
            return history;
        }

        public String getFinalAnswer() {
            return finalAnswer;
        }
    }
}

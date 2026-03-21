package com.xbk.agent.framework.planreplan.infrastructure.agentframework;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;
import java.util.Map;

/**
 * Spring AI Alibaba 顺序编排版 Plan-and-Solve Agent
 *
 * 职责：用 PlannerAgent -> ExecutorAgent 的顺序流，把计划结果通过 outputKey 写入状态并传递给后续节点
 *
 * @author xiexu
 */
public class AlibabaSequentialPlanAndSolveAgent {

    private static final String PLAN_RESULT_KEY = "plan_result";
    private static final String FINAL_ANSWER_KEY = "final_answer";

    private final ReactAgent plannerAgent;
    private final ReactAgent executorAgent;
    private final SequentialAgent sequentialAgent;

    /**
     * 创建顺序编排版 Plan-and-Solve Agent。
     *
     * @param chatModel Spring AI ChatModel
     */
    public AlibabaSequentialPlanAndSolveAgent(ChatModel chatModel) {
        this.plannerAgent = createPlannerAgent(chatModel);
        this.executorAgent = createExecutorAgent(chatModel);
        this.sequentialAgent = SequentialAgent.builder()
                .name("alibaba-plan-solve-sequential-agent")
                .description("按 Planner -> Executor 顺序执行买苹果问题")
                .subAgents(List.of(plannerAgent, executorAgent))
                .build();
    }

    /**
     * 运行顺序编排版 Plan-and-Solve。
     *
     * @param question 原始问题
     * @return 运行结果
     */
    public RunResult run(String question) {
        OverAllState state;
        try {
            // 这里不是直接把 question 当方法参数继续往下传，而是先把它放进图运行时的全局状态。
            // 键名之所以叫 input，是因为下游 PlannerAgent / ExecutorAgent 的 instruction
            // 都通过 {input} 占位符读取这份原始问题。
            //
            // sequentialAgent.invoke(...) 启动后，会按顺序执行：
            // 1. PlannerAgent 读取 input，生成计划，并把结果写到 plan_result
            // 2. ExecutorAgent 再读取 input 和 plan_result，继续求解，并把结果写到 final_answer
            //
            // invoke(...) 返回 Optional<OverAllState>，表示这次顺序链可能有状态返回，也可能没有。
            // 这里用 orElseThrow(...) 做 fail fast：
            // 如果整条链执行完却没有任何状态产出，说明这次调用不符合我们对 SequentialAgent 的预期，
            // 那就立刻抛异常，而不是带着一个空结果继续往下走。
            state = sequentialAgent.invoke(Map.of("input", question))
                    .orElseThrow(() -> new IllegalStateException("SequentialAgent did not return state"));
        } catch (GraphRunnerException exception) {
            throw new IllegalStateException("SequentialAgent execution failed", exception);
        }
        String planResult = extractStateText(state, PLAN_RESULT_KEY);
        String finalAnswer = extractStateText(state, FINAL_ANSWER_KEY);
        return new RunResult(question, planResult, finalAnswer, state);
    }

    /**
     * 返回规划子 Agent。
     *
     * @return 规划子 Agent
     */
    public ReactAgent getPlannerAgent() {
        return plannerAgent;
    }

    /**
     * 返回执行子 Agent。
     *
     * @return 执行子 Agent
     */
    public ReactAgent getExecutorAgent() {
        return executorAgent;
    }

    /**
     * 返回顺序流 Agent。
     *
     * @return 顺序流 Agent
     */
    public SequentialAgent getSequentialAgent() {
        return sequentialAgent;
    }

    /**
     * 创建 PlannerAgent。
     *
     * @param chatModel Spring AI ChatModel
     * @return PlannerAgent
     */
    private ReactAgent createPlannerAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("apple-problem-planner-agent")
                .description("负责把买苹果问题拆成可执行计划")
                .model(chatModel)
                .systemPrompt("""
                        你是一名 Plan-and-Solve 规划器。
                        你的任务是先规划、后行动。
                        请把输入问题拆成清晰的编号步骤，必须使用规范编号列表输出，不要附加多余解释。
                        """)
                .instruction("请基于这个问题生成执行计划：{input}")
                .outputKey(PLAN_RESULT_KEY)
                // 这里显式关闭内容继承，让 Planner 只看当前输入和自己的指令。
                .includeContents(false)
                // 这里关闭中间推理回传，强调企业场景下默认只传结果，不传隐藏思维。
                .returnReasoningContents(false)
                .build();
    }

    /**
     * 创建 ExecutorAgent。
     *
     * @param chatModel Spring AI ChatModel
     * @return ExecutorAgent
     */
    private ReactAgent createExecutorAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("apple-problem-executor-agent")
                .description("负责读取计划状态并给出最终求解结果")
                .model(chatModel)
                .systemPrompt("""
                        你是一名 Plan-and-Solve 执行器。
                        你不需要自己重新规划，而是严格读取上游 Planner 的计划结果来求解问题。
                        """)
                .instruction("""
                        原始问题：{input}

                        规划结果：
                        {plan_result}

                        请根据这份计划完成求解，并直接给出最终答案。
                        """)
                .outputKey(FINAL_ANSWER_KEY)
                // 这里同样关闭父流程内容拼接，避免 Executor 被无关历史消息污染。
                .includeContents(false)
                // 这里只把 final_answer 写入状态，不把中间 reasoning 暴露给父流程。
                .returnReasoningContents(false)
                .build();
    }

    /**
     * 从状态对象中提取可读文本。
     *
     * @param state 全局状态
     * @param key 状态键
     * @return 文本结果
     */
    private String extractStateText(OverAllState state, String key) {
        Object value = state.value(key).orElse(null);
        if (value instanceof AssistantMessage assistantMessage) {
            return assistantMessage.getText();
        }
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 顺序编排版运行结果。
     *
     * 职责：返回原始问题、Planner 输出、最终答案以及完整状态快照
     *
     * @author xiexu
     */
    public static final class RunResult {

        private final String question;
        private final String planResult;
        private final String finalAnswer;
        private final OverAllState state;

        /**
         * 创建运行结果。
         *
         * @param question 原始问题
         * @param planResult 规划结果
         * @param finalAnswer 最终答案
         * @param state 全局状态
         */
        public RunResult(String question, String planResult, String finalAnswer, OverAllState state) {
            this.question = question;
            this.planResult = planResult;
            this.finalAnswer = finalAnswer;
            this.state = state;
        }

        public String getQuestion() {
            return question;
        }

        public String getPlanResult() {
            return planResult;
        }

        public String getFinalAnswer() {
            return finalAnswer;
        }

        public OverAllState getState() {
            return state;
        }
    }
}

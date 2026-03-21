package com.xbk.agent.framework.reflection.infrastructure.agentframework;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.xbk.agent.framework.reflection.infrastructure.agentframework.node.JavaCoderNode;
import com.xbk.agent.framework.reflection.infrastructure.agentframework.node.JavaReviewerNode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring AI Alibaba 图编排版 Reflection Agent
 *
 * 职责：用 StateGraph 条件边驱动“代码生成 -> 代码评审 -> 是否继续优化”的受控回环
 *
 * @author xiexu
 */
public class AlibabaReflectionFlowAgent extends FlowAgent {

    private static final String FLOW_NAME = "alibaba-reflection-flow-agent";
    private static final String CODER_NODE = "java_coder_node";
    private static final String REVIEWER_NODE = "java_reviewer_node";
    private static final String CURRENT_CODE_KEY = "current_code";
    private static final String REVIEW_FEEDBACK_KEY = "review_feedback";
    private static final String ITERATION_COUNT_KEY = "iteration_count";

    private final ReactAgent javaCoderAgent;
    private final ReactAgent javaReviewerAgent;
    private final ChatModel chatModel;
    private final int maxReflectionRounds;

    /**
     * 创建图编排版 Reflection Agent。
     *
     * @param chatModel Spring AI ChatModel
     * @param maxReflectionRounds 最大反思轮次
     */
    public AlibabaReflectionFlowAgent(ChatModel chatModel, int maxReflectionRounds) {
        this(chatModel, createJavaCoderAgent(chatModel), createJavaReviewerAgent(chatModel), maxReflectionRounds);
    }

    /**
     * 创建图编排版 Reflection Agent。
     *
     * @param chatModel Spring AI ChatModel
     * @param javaCoderAgent 代码生成 Agent
     * @param javaReviewerAgent 评审 Agent
     * @param maxReflectionRounds 最大反思轮次
     */
    private AlibabaReflectionFlowAgent(ChatModel chatModel,
                                       ReactAgent javaCoderAgent,
                                       ReactAgent javaReviewerAgent,
                                       int maxReflectionRounds) {
        super(FLOW_NAME,
                "Reflection 素数题图编排版 Agent",
                CompileConfig.builder().recursionLimit(Math.max(4, maxReflectionRounds * 4)).build(),
                List.<Agent>of(javaCoderAgent, javaReviewerAgent));
        this.javaCoderAgent = javaCoderAgent;
        this.javaReviewerAgent = javaReviewerAgent;
        this.chatModel = chatModel;
        this.maxReflectionRounds = maxReflectionRounds;
    }

    /**
     * 运行图编排版 Reflection。
     *
     * @param task 原始任务
     * @return 运行结果
     */
    public RunResult run(String task) {
        Map<String, Object> input = Map.of(
                "input", task,
                CURRENT_CODE_KEY, "",
                REVIEW_FEEDBACK_KEY, "",
                ITERATION_COUNT_KEY, Integer.valueOf(0));
        try {
            Optional<OverAllState> optionalState = invoke(input);
            OverAllState state = optionalState.orElseThrow(
                    () -> new IllegalStateException("Reflection FlowAgent did not return state"));
            String finalCode = extractStateText(state, CURRENT_CODE_KEY);
            String finalReview = extractStateText(state, REVIEW_FEEDBACK_KEY);
            int iterationCount = state.value(ITERATION_COUNT_KEY, Integer.class).orElse(Integer.valueOf(0));
            return new RunResult(task, finalCode, finalReview, iterationCount, state);
        } catch (GraphRunnerException exception) {
            throw new IllegalStateException("Reflection FlowAgent execution failed", exception);
        }
    }

    /**
     * 返回代码生成 Agent。
     *
     * @return 代码生成 Agent
     */
    public ReactAgent getJavaCoderAgent() {
        return javaCoderAgent;
    }

    /**
     * 返回代码评审 Agent。
     *
     * @return 代码评审 Agent
     */
    public ReactAgent getJavaReviewerAgent() {
        return javaReviewerAgent;
    }

    /**
     * 构建 Reflection 专属图结构。
     *
     * @param config Flow 图配置
     * @return Reflection 状态图
     * @throws GraphStateException 图构建失败时抛出异常
     */
    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
        StateGraph stateGraph = new StateGraph();
        stateGraph.addNode(CODER_NODE, new JavaCoderNode(chatModel));
        stateGraph.addNode(REVIEWER_NODE, new JavaReviewerNode(chatModel));
        stateGraph.addEdge(StateGraph.START, CODER_NODE);
        stateGraph.addEdge(CODER_NODE, REVIEWER_NODE);
        stateGraph.addConditionalEdges(REVIEWER_NODE, continueOrStop(), Map.of(
                "coder", CODER_NODE,
                "end", StateGraph.END));
        return stateGraph;
    }

    /**
     * 返回用于条件跳转的边动作。
     *
     * @return 条件边动作
     */
    private AsyncEdgeAction continueOrStop() {
        return state -> {
            String reviewFeedback = extractStateText(state, REVIEW_FEEDBACK_KEY);
            int iterationCount = state.value(ITERATION_COUNT_KEY, Integer.class).orElse(Integer.valueOf(0));
            boolean shouldStop = reviewFeedback.contains("无需改进") || iterationCount >= maxReflectionRounds;
            return java.util.concurrent.CompletableFuture.completedFuture(shouldStop ? "end" : "coder");
        };
    }

    /**
     * 从状态中提取文本值。
     *
     * @param state 全局状态
     * @param key 状态键
     * @return 文本值
     */
    private String extractStateText(OverAllState state, String key) {
        Object value = state.value(key).orElse(null);
        if (value instanceof AssistantMessage assistantMessage) {
            return assistantMessage.getText();
        }
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 创建代码生成 Agent。
     *
     * @param chatModel Spring AI ChatModel
     * @return 代码生成 Agent
     */
    private static ReactAgent createJavaCoderAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("java-reflection-coder-agent")
                .description("负责生成素数题 Java 初稿并根据评审意见输出优化版代码")
                .model(chatModel)
                .systemPrompt("""
                        你是一名 Reflection 流程里的 Java 代码生成者。
                        如果当前没有旧代码，就先给出可运行初稿。
                        如果已经有旧代码和评审意见，就根据评审意见输出优化后的完整代码。
                        请只输出代码，不要输出解释。
                        """)
                .instruction("""
                        原始任务：
                        {input}

                        当前代码：
                        {current_code}

                        评审意见：
                        {review_feedback}
                        """)
                .outputKey(CURRENT_CODE_KEY)
                .includeContents(false)
                .returnReasoningContents(false)
                .build();
    }

    /**
     * 创建代码评审 Agent。
     *
     * @param chatModel Spring AI ChatModel
     * @return 代码评审 Agent
     */
    private static ReactAgent createJavaReviewerAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("java-reflection-reviewer-agent")
                .description("负责从时间复杂度角度评审素数题 Java 代码")
                .model(chatModel)
                .systemPrompt("""
                        你是一名 Reflection 流程里的 Java 评审者。
                        请重点评审代码的时间复杂度和主要性能瓶颈。
                        如果已经无需继续优化，请明确输出“无需改进”。
                        """)
                .instruction("""
                        原始任务：
                        {input}

                        当前代码：
                        {current_code}

                        请从时间复杂度角度给出评审意见。
                        如果已经无需继续优化，请明确输出“无需改进”。
                        """)
                .outputKey(REVIEW_FEEDBACK_KEY)
                .includeContents(false)
                .returnReasoningContents(false)
                .build();
    }

    /**
     * 图编排版运行结果。
     *
     * 职责：返回最终代码、最终评审、轮次计数与完整状态
     *
     * @author xiexu
     */
    public static final class RunResult {

        private final String task;
        private final String finalCode;
        private final String finalReview;
        private final int iterationCount;
        private final OverAllState state;

        /**
         * 创建运行结果。
         *
         * @param task 原始任务
         * @param finalCode 最终代码
         * @param finalReview 最终评审
         * @param iterationCount 迭代轮次
         * @param state 全局状态
         */
        public RunResult(String task, String finalCode, String finalReview, int iterationCount, OverAllState state) {
            this.task = task;
            this.finalCode = finalCode;
            this.finalReview = finalReview;
            this.iterationCount = iterationCount;
            this.state = state;
        }

        public String getTask() {
            return task;
        }

        public String getFinalCode() {
            return finalCode;
        }

        public String getFinalReview() {
            return finalReview;
        }

        public int getIterationCount() {
            return iterationCount;
        }

        public OverAllState getState() {
            return state;
        }
    }
}

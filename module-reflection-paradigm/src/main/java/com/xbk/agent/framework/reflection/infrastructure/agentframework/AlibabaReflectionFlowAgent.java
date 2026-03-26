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
import com.xbk.agent.framework.reflection.infrastructure.agentframework.support.ReflectionStateKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Spring AI Alibaba 图编排版 Reflection Agent
 *
 * 职责：用 StateGraph 条件边驱动“代码生成 -> 代码评审 -> 是否继续优化”的受控回环
 *
 * 学习重点：
 * 1. 用 StateGraph 显式描述节点、普通边与条件边，而不是把流程控制写死在单个方法里。
 * 2. 用 OverAllState 作为跨节点共享上下文，保存任务、代码、评审意见和轮次信息。
 * 3. 由 reviewer 节点后的条件边统一决定“继续迭代”还是“结束流程”。
 *
 * @author xiexu
 */
public class AlibabaReflectionFlowAgent extends FlowAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlibabaReflectionFlowAgent.class);
    private static final String FLOW_NAME = "alibaba-reflection-flow-agent";

    // 图中的两个业务节点名称，用于在 StateGraph 中注册节点和连接执行路径。
    private static final String CODER_NODE = "java_coder_node";
    private static final String REVIEWER_NODE = "java_reviewer_node";

    // coder 负责产出初稿并根据 reviewer 意见持续迭代代码。
    private final ReactAgent javaCoderAgent;
    // reviewer 负责审查当前代码，并给出“继续优化”或“可以结束”的依据。
    private final ReactAgent javaReviewerAgent;
    // 图节点和内部 Agent 统一复用同一个底层大模型实例。
    private final ChatModel chatModel;
    // 业务层最大反思轮次，只用于控制最多评审多少轮，不等于图框架递归深度。
    private final int maxReflectionRounds;

    /**
     * 创建图编排版 Reflection Agent。
     *
     * @param chatModel Spring AI ChatModel
     * @param maxReflectionRounds 最大反思轮次
     */
    public AlibabaReflectionFlowAgent(ChatModel chatModel, int maxReflectionRounds) {
        this(
                chatModel,
                createJavaCoderAgent(chatModel),
                createJavaReviewerAgent(chatModel),
                maxReflectionRounds);
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
        // maxReflectionRounds 控制业务上最多反思多少轮；
        // recursionLimit 控制图框架内部最多允许走多深，避免回环流程过早触发递归上限。
        // 一轮 Reflection 会经历 coder、reviewer 和条件跳转等多个内部步骤，因此这里按轮次放大执行深度。
        super(FLOW_NAME,
                "Reflection 素数题图编排版 Agent",
                CompileConfig.builder()
                        .recursionLimit(Math.max(4, maxReflectionRounds * 4))
                        .build(),
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
        // 初始化图执行的全局状态：任务作为输入，代码与评审结果先置空，迭代次数从 0 开始。
        Map<String, Object> input = Map.of(
                ReflectionStateKeys.INPUT, task,
                ReflectionStateKeys.CURRENT_CODE, "",
                ReflectionStateKeys.REVIEW_FEEDBACK, "",
                ReflectionStateKeys.ITERATION_COUNT, Integer.valueOf(0));
        try {
            /**
             * invoke 会驱动整张图执行，并把各节点写回的状态聚合成最终的 OverAllState。
             * 图不是在构造器里就跑起来的，而是在第一次 `invoke(...)` 时才真正开始构图、编译、执行。
             * 它做了两件事：
             * 1、先构造初始输入状态
             * 2、再调用 `invoke(input)` 启动图执行
             */
            Optional<OverAllState> optionalState = invoke(input);
            OverAllState state = optionalState.orElseThrow(
                    () -> new IllegalStateException("Reflection FlowAgent did not return state"));
            // 调用方最终只关心结果对象，因此这里把图内部共享状态还原成领域语义更明确的 RunResult。
            String finalCode = extractStateText(state, ReflectionStateKeys.CURRENT_CODE);
            String finalReview = extractStateText(state, ReflectionStateKeys.REVIEW_FEEDBACK);
            int iterationCount = state.value(ReflectionStateKeys.ITERATION_COUNT, Integer.class).orElse(Integer.valueOf(0));
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
        // 定义图里的一个执行步骤，比如coder或reviewer
        stateGraph.addNode(CODER_NODE, new JavaCoderNode(chatModel));
        stateGraph.addNode(REVIEWER_NODE, new JavaReviewerNode(chatModel));

        // 主干路径固定为：START -> coder -> reviewer。
        // coder 负责生成代码，reviewer 负责评审代码，这两步形成 Reflection 的最小闭环。
        stateGraph.addEdge(StateGraph.START, CODER_NODE);
        stateGraph.addEdge(CODER_NODE, REVIEWER_NODE);

        // 条件边只挂在 reviewer 后面，而不是挂在 coder 后面。
        // 原因是 coder 的职责只是基于 input 和 review_feedback 产出/修订 current_code，
        // 它本身并不负责判断流程是否结束。
        // 真正的分流时机是在 reviewer 完成之后：
        // reviewer 会给出最新 review_feedback，并推进 iteration_count，
        // 条件边再统一根据“最新评审结果 + 当前轮次”决定是回到 coder 继续修订，还是直接结束流程。
        // 这里的 Map.of(...) 是“分支标签 -> 目标节点”的映射表：
        // 1. continueOrStop() 返回 "coder" 时，跳转到 CODER_NODE，进入下一轮代码修订；
        // 2. continueOrStop() 返回 "end" 时，跳转到 StateGraph.END，整个流程结束。
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
            String reviewFeedback = extractStateText(state, ReflectionStateKeys.REVIEW_FEEDBACK);
            int iterationCount = state.value(ReflectionStateKeys.ITERATION_COUNT, Integer.class).orElse(Integer.valueOf(0));

            // 结束条件有两个：
            // 1. reviewer 明确判断“无需改进”，说明本轮代码已收敛；
            // 2. 迭代轮次达到上限，防止图无限回环。
            // 返回值必须与 addConditionalEdges 中声明的分支标签一致。
            boolean reviewSaysNoFurtherImprovement = reviewFeedback.contains("无需改进");
            boolean iterationLimitReached = iterationCount >= maxReflectionRounds;
            boolean shouldStop = reviewSaysNoFurtherImprovement || iterationLimitReached;
            String nextNodeLabel = shouldStop ? "end" : "coder";
            LOGGER.info(
                    "conditional edge evaluated: iterationCount={}, maxReflectionRounds={}, reviewSaysNoFurtherImprovement={}, iterationLimitReached={}, nextNodeLabel={}, reviewFeedbackPreview={}",
                    iterationCount,
                    maxReflectionRounds,
                    reviewSaysNoFurtherImprovement,
                    iterationLimitReached,
                    nextNodeLabel,
                    summarizeForLog(reviewFeedback));
            return CompletableFuture.completedFuture(nextNodeLabel);
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

        // 状态值可能是框架封装的 AssistantMessage，也可能已经是普通字符串；
        // 这里统一解包，避免上层流程代码感知底层消息类型差异。
        if (value instanceof AssistantMessage assistantMessage) {
            return assistantMessage.getText();
        }
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 生成适合日志输出的简短摘要。
     *
     * @param text 原始文本
     * @return 日志摘要
     */
    private String summarizeForLog(String text) {
        if (text == null || text.isBlank()) {
            return "(empty)";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120) + "...";
    }

    /**
     * 创建代码生成 Agent。
     *
     * @param chatModel Spring AI ChatModel
     * @return 代码生成 Agent
     */
    private static ReactAgent createJavaCoderAgent(ChatModel chatModel) {
        // coder 的职责是“产出完整代码”，因此它同时读取任务、旧代码和评审意见，
        // 并把最新代码持续写回 CURRENT_CODE_KEY，供 reviewer 或最终结果继续消费。
        return ReactAgent.builder()
                .name("java-reflection-coder-agent")
                .description("负责生成素数题 Java 初稿并根据评审意见输出优化版代码")
                .model(chatModel)
                .systemPrompt("""
                        你是一名 Reflection 流程里的 Java 代码生成者。
                        如果当前没有旧代码，请优先给出一版正确、可运行、便于后续优化的基础实现。
                        第一轮不要一开始就追求最优时间复杂度，先保留清晰但仍有优化空间的实现。
                        如果已经有旧代码和评审意见，请严格根据评审意见输出优化后的完整代码。
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
                .outputKey(ReflectionStateKeys.CURRENT_CODE)
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
        // reviewer 的职责是“只做评审不改代码”，它消费当前代码并输出评审意见。
        // 这份评审既是下一轮 coder 的输入，也是条件边判断是否停止的依据。
        return ReactAgent.builder()
                .name("java-reflection-reviewer-agent")
                .description("负责从时间复杂度角度评审素数题 Java 代码")
                .model(chatModel)
                .systemPrompt("""
                        你是一名 Reflection 流程里的 Java 评审者。
                        请重点评审代码的时间复杂度和主要性能瓶颈。
                        如果仍然存在明确、可落地的复杂度优化空间，请不要输出“无需改进”，而是明确指出瓶颈和下一轮如何修改。
                        只有当当前实现已经没有明显的时间复杂度优化空间时，才明确输出“无需改进”。
                        """)
                .instruction("""
                        原始任务：
                        {input}

                        当前代码：
                        {current_code}

                        请从时间复杂度角度给出评审意见。
                        如果仍然存在明确、可落地的复杂度优化空间，请不要输出“无需改进”，而是明确指出瓶颈和下一轮如何修改。
                        只有当当前实现已经没有明显的时间复杂度优化空间时，才明确输出“无需改进”。
                        """)
                .outputKey(ReflectionStateKeys.REVIEW_FEEDBACK)
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

        /**
         * 返回原始任务。
         *
         * @return 原始任务
         */
        public String getTask() {
            return task;
        }

        /**
         * 返回最终代码。
         *
         * @return 最终代码
         */
        public String getFinalCode() {
            return finalCode;
        }

        /**
         * 返回最终评审结果。
         *
         * @return 最终评审结果
         */
        public String getFinalReview() {
            return finalReview;
        }

        /**
         * 返回实际迭代轮次。
         *
         * @return 实际迭代轮次
         */
        public int getIterationCount() {
            return iterationCount;
        }

        /**
         * 返回完整图状态。
         *
         * @return 完整图状态
         */
        public OverAllState getState() {
            return state;
        }
    }
}

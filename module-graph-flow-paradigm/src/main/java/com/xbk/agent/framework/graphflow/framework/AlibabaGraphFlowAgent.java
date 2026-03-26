package com.xbk.agent.framework.graphflow.framework;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.graphflow.common.tool.MockSearchTool;
import com.xbk.agent.framework.graphflow.framework.edge.SearchResultEdgeRouter;
import com.xbk.agent.framework.graphflow.framework.node.AnswerNodeAction;
import com.xbk.agent.framework.graphflow.framework.node.FallbackNodeAction;
import com.xbk.agent.framework.graphflow.framework.node.SearchNodeAction;
import com.xbk.agent.framework.graphflow.framework.node.UnderstandNodeAction;
import lombok.extern.slf4j.Slf4j;

import com.xbk.agent.framework.graphflow.framework.support.GraphFlowStateKeys;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Spring AI Alibaba 框架对照注释版图流 Agent
 *
 * 职责：用 StateGraph 声明式构建与手写版等价的"三步问答助手"图，
 * 并通过逐行注释精确映射手写版 while + switch 的每一行代码。
 *
 * ══ 整体对照关系 ════════════════════════════════════════════════════
 * 手写版 HandwrittenGraphFlow.run(userQuery)：
 *   GraphState initialState = GraphState.init(userQuery)   // 初始化状态
 *   graphRunner.run(initialState)                          // 启动 while 循环
 *     while (stepStatus != END) { switch(stepStatus) {...} }
 *
 * 框架版 AlibabaGraphFlowAgent.run(userQuery)：
 *   invoke(Map.of("user_query", userQuery))                // 启动图执行循环
 *     → 框架内部的 while 循环被 invoke() 封装
 *     → switch 被 addEdge / addConditionalEdges 路由表替代
 * ════════════════════════════════════════════════════════════════════
 *
 * @author xiexu
 */
@Slf4j
public class AlibabaGraphFlowAgent extends FlowAgent {

    private static final String FLOW_NAME = "alibaba-graph-flow-agent";

    // 节点名称常量，用于 addNode / addEdge 时引用，避免魔法字符串散落各处
    private static final String UNDERSTAND_NODE = "understand";
    private static final String SEARCH_NODE     = "search";
    private static final String ANSWER_NODE     = "answer";
    private static final String FALLBACK_NODE   = "fallback";

    private final AgentLlmGateway gateway;
    private final MockSearchTool searchTool;

    /**
     * 创建框架版图流 Agent（正常搜索）。
     *
     * @param gateway 统一大模型调用网关
     */
    public AlibabaGraphFlowAgent(AgentLlmGateway gateway) {
        this(gateway, new MockSearchTool(false));
    }

    /**
     * 创建框架版图流 Agent（支持注入失败搜索工具）。
     *
     * @param gateway    统一大模型调用网关
     * @param searchTool 搜索工具（可注入失败桩触发 FallbackNode 分支）
     */
    public AlibabaGraphFlowAgent(AgentLlmGateway gateway, MockSearchTool searchTool) {
        // recursionLimit=8 足够覆盖 4 个节点的线性执行深度，并留有余量
        // 等价于手写版：while 循环最多执行 4 次（4 个节点各执行一次）
        super(FLOW_NAME,
                "三步问答助手图流 Agent（框架对照注释版）",
                CompileConfig.builder().recursionLimit(8).build(),
                Collections.<Agent>emptyList());
        this.gateway = gateway;
        this.searchTool = searchTool;
    }

    /**
     * 执行三步问答图流程，返回运行结果。
     *
     * @param userQuery 用户原始提问
     * @return 运行结果（包含 finalAnswer 及完整图状态）
     */
    public RunResult run(String userQuery) {
        // 初始化图输入状态 ——
        // 等价于手写版：GraphState initialState = GraphState.init(userQuery)
        // 框架版用 Map 替代强类型 POJO，这是框架版与手写版最直观的差异
        Map<String, Object> input = Map.of(
                GraphFlowStateKeys.USER_QUERY,    userQuery,
                GraphFlowStateKeys.SEARCH_QUERY,  "",
                GraphFlowStateKeys.SEARCH_RESULTS,"",
                GraphFlowStateKeys.FINAL_ANSWER,  "",
                GraphFlowStateKeys.SEARCH_FAILED, Boolean.FALSE,
                GraphFlowStateKeys.ERROR_MESSAGE, "");
        try {
            // invoke(input) 启动框架内部的图执行循环 ——
            // 等价于手写版：graphRunner.run(initialState)，即 while 循环开始
            Optional<OverAllState> optionalState = invoke(input);
            OverAllState finalState = optionalState.orElseThrow(
                    () -> new IllegalStateException("AlibabaGraphFlowAgent 未返回状态"));

            String finalAnswer   = finalState.value(GraphFlowStateKeys.FINAL_ANSWER, String.class).orElse("");
            String searchResults = finalState.value(GraphFlowStateKeys.SEARCH_RESULTS, String.class).orElse("");
            boolean usedFallback = finalState.value(GraphFlowStateKeys.SEARCH_FAILED, Boolean.class).orElse(Boolean.FALSE);

            return new RunResult(userQuery, finalAnswer, searchResults, usedFallback, finalState);
        } catch (GraphRunnerException e) {
            throw new IllegalStateException("AlibabaGraphFlowAgent 执行失败", e);
        }
    }

    /**
     * 构建三步问答图结构。
     *
     * ══ 核心教学：每行 addNode/addEdge 与手写版代码的一一映射 ════════════
     *
     * addNode("understand", ...)  → 等价于手写版定义 UnderstandNode 类
     * addNode("search",     ...)  → 等价于手写版定义 SearchNode 类
     * addNode("answer",     ...)  → 等价于手写版定义 AnswerNode 类
     * addNode("fallback",   ...)  → 等价于手写版定义 FallbackNode 类
     *
     * addEdge(START, "understand")          → case INIT: understandNode.process(state)
     * addEdge("understand", "search")       → case UNDERSTOOD: searchNode.process(state)
     *
     * addConditionalEdges("search", router, → case SEARCH_SUCCESS: answerNode.process(state)
     *     Map.of("answer",   ANSWER_NODE,      case SEARCH_FAILED:  fallbackNode.process(state)
     *            "fallback", FALLBACK_NODE))
     *
     * addEdge("answer",   END)              → state.setStepStatus(END) in AnswerNode
     * addEdge("fallback", END)              → state.setStepStatus(END) in FallbackNode
     * ════════════════════════════════════════════════════════════════════
     *
     * @param config Flow 图配置
     * @return 构建完成的 StateGraph
     * @throws GraphStateException 图构建失败时抛出
     */
    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
        StateGraph stateGraph = new StateGraph();

        // ── 注册节点 ────────────────────────────────────────────────────────
        // 等价于手写版：四个 Node 类的实例化
        stateGraph.addNode(UNDERSTAND_NODE, new UnderstandNodeAction(gateway));
        stateGraph.addNode(SEARCH_NODE,     new SearchNodeAction(searchTool));
        stateGraph.addNode(ANSWER_NODE,     new AnswerNodeAction(gateway));
        stateGraph.addNode(FALLBACK_NODE,   new FallbackNodeAction(gateway));

        // ── 声明固定边（无条件跳转）────────────────────────────────────────
        // 等价于手写版 GraphRunner.switch：
        //   case INIT      → understandNode（START 是框架内置的图入口节点名）
        //   case UNDERSTOOD → searchNode
        stateGraph.addEdge(StateGraph.START, UNDERSTAND_NODE);
        stateGraph.addEdge(UNDERSTAND_NODE, SEARCH_NODE);

        // ── 声明条件边（SearchNode 后的 Fork 分叉）─────────────────────────
        // 等价于手写版 GraphRunner.switch 里最关键的两个分支：
        //   case SEARCH_SUCCESS → answerNode.process(state)
        //   case SEARCH_FAILED  → fallbackNode.process(state)
        //
        // 这里先由 SearchResultEdgeRouter.apply(state) 做判断：
        // - 搜索成功时返回 "answer"
        // - 搜索失败时返回 "fallback"
        //
        // Map.of(...) 不是业务数据，而是一张“分支标签 -> 目标节点名”的路由表。
        // 框架拿到路由器返回的标签后，会在这张表里查出下一跳：
        // - "answer"   -> ANSWER_NODE
        // - "fallback" -> FALLBACK_NODE
        stateGraph.addConditionalEdges(
                SEARCH_NODE,
                new SearchResultEdgeRouter(),
                Map.of(
                        "answer",   ANSWER_NODE,
                        "fallback", FALLBACK_NODE));

        // ── 声明终止边（两条出口边指向 END）──────────────────────────────────
        // 等价于手写版 AnswerNode/FallbackNode 里的 state.setStepStatus(END)
        // 框架通过图结构声明终止路径，节点本身无需感知"我是最后一个节点"
        stateGraph.addEdge(ANSWER_NODE,   StateGraph.END);
        stateGraph.addEdge(FALLBACK_NODE, StateGraph.END);

        return stateGraph;
    }

    /**
     * 框架版运行结果。
     *
     * 职责：封装 finalAnswer、是否走了降级分支、完整图状态
     *
     * @author xiexu
     */
    public static final class RunResult {

        private final String userQuery;
        private final String finalAnswer;
        private final String searchResults;
        private final boolean usedFallback;
        private final OverAllState state;

        /**
         * 创建运行结果。
         *
         * @param userQuery     用户原始提问
         * @param finalAnswer   最终回答
         * @param searchResults 搜索结果（降级时为空）
         * @param usedFallback  是否触发了降级分支
         * @param state         完整图状态
         */
        public RunResult(String userQuery, String finalAnswer, String searchResults,
                         boolean usedFallback, OverAllState state) {
            this.userQuery = userQuery;
            this.finalAnswer = finalAnswer;
            this.searchResults = searchResults;
            this.usedFallback = usedFallback;
            this.state = state;
        }

        /**
         * 返回用户原始提问。
         *
         * @return 用户原始提问
         */
        public String getUserQuery() {
            return userQuery;
        }

        /**
         * 返回最终回答。
         *
         * @return 最终回答
         */
        public String getFinalAnswer() {
            return finalAnswer;
        }

        /**
         * 返回搜索结果。
         *
         * @return 搜索结果（降级时为空字符串）
         */
        public String getSearchResults() {
            return searchResults;
        }

        /**
         * 返回是否触发了 FallbackNode 降级分支。
         *
         * @return true 表示走了降级路径
         */
        public boolean isUsedFallback() {
            return usedFallback;
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

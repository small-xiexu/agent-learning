package com.xbk.agent.framework.graphflow.handwritten.engine;

import com.xbk.agent.framework.graphflow.common.state.GraphState;
import com.xbk.agent.framework.graphflow.common.state.StepStatus;
import com.xbk.agent.framework.graphflow.handwritten.node.AnswerNode;
import com.xbk.agent.framework.graphflow.handwritten.node.FallbackNode;
import com.xbk.agent.framework.graphflow.handwritten.node.SearchNode;
import com.xbk.agent.framework.graphflow.handwritten.node.UnderstandNode;
import lombok.extern.slf4j.Slf4j;

/**
 * 手写版图执行引擎
 *
 * 职责：用 while 循环驱动状态机流转，用 switch 语句实现条件边（Conditional Edges）路由。
 * 这正是 LangGraph / FlowAgent 框架在底层做的事：
 * - while 循环 = 框架的 CompiledGraph.invoke() 内部执行循环
 * - switch(stepStatus) = 框架的 addEdge / addConditionalEdges 路由表
 * - stepStatus == END = 框架的跳转到 StateGraph.END 节点
 *
 * @author xiexu
 */
@Slf4j
public class GraphRunner {

    private final UnderstandNode understandNode;
    private final SearchNode searchNode;
    private final AnswerNode answerNode;
    private final FallbackNode fallbackNode;

    /**
     * 创建图执行引擎，注入所有节点。
     *
     * @param understandNode 意图理解节点
     * @param searchNode     搜索节点
     * @param answerNode     总结回答节点
     * @param fallbackNode   降级回答节点
     */
    public GraphRunner(UnderstandNode understandNode,
                       SearchNode searchNode,
                       AnswerNode answerNode,
                       FallbackNode fallbackNode) {
        this.understandNode = understandNode;
        this.searchNode = searchNode;
        this.answerNode = answerNode;
        this.fallbackNode = fallbackNode;
    }

    /**
     * 驱动图从初始状态执行到 END 状态。
     *
     * 这个 while + switch 结构是整个手写版的核心教学点：
     * 它把"节点执行"和"条件路由"完全解耦——每个节点只负责更新状态，
     * 路由决策完全由引擎集中管理，这与 LangGraph 的设计哲学完全一致。
     *
     * @param state 初始图状态（stepStatus=INIT）
     * @return 执行完毕后的最终状态（stepStatus=END）
     */
    public GraphState run(GraphState state) {
        log.info("GraphRunner 开始执行，userQuery={}", state.getUserQuery());

        // ─── while 循环 ═══════════════════════════════════════════════════════
        // 框架等价：CompiledGraph.invoke(input) 内部的执行循环
        // 只要图还没到达 END，就继续执行下一个节点
        while (state.getStepStatus() != StepStatus.END) {

            // ─── switch（条件边路由表）══════════════════════════════════════════
            // 框架等价：addEdge / addConditionalEdges 声明的路由规则
            // INIT          → UnderstandNode  对应 addEdge(START, "understand")
            // UNDERSTOOD    → SearchNode      对应 addEdge("understand", "search")
            // SEARCH_SUCCESS→ AnswerNode      对应 addConditionalEdges("search", ...) → "answer"
            // SEARCH_FAILED → FallbackNode    对应 addConditionalEdges("search", ...) → "fallback"
            switch (state.getStepStatus()) {
                case INIT:
                    state = understandNode.process(state);
                    break;
                case UNDERSTOOD:
                    state = searchNode.process(state);
                    break;
                case SEARCH_SUCCESS:
                    state = answerNode.process(state);
                    break;
                case SEARCH_FAILED:
                    state = fallbackNode.process(state);
                    break;
                default:
                    throw new IllegalStateException("未知的 stepStatus: " + state.getStepStatus());
            }
            // ─── switch 结束 ════════════════════════════════════════════════════
        }
        // ─── while 结束：等价于框架执行到达 StateGraph.END 节点 ════════════════

        log.info("GraphRunner 执行完毕，stepStatus=END");
        return state;
    }
}

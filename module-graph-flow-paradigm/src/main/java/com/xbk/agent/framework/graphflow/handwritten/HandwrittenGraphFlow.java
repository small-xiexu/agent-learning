package com.xbk.agent.framework.graphflow.handwritten;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.graphflow.common.state.GraphState;
import com.xbk.agent.framework.graphflow.common.tool.MockSearchTool;
import com.xbk.agent.framework.graphflow.handwritten.engine.GraphRunner;
import com.xbk.agent.framework.graphflow.handwritten.node.AnswerNode;
import com.xbk.agent.framework.graphflow.handwritten.node.FallbackNode;
import com.xbk.agent.framework.graphflow.handwritten.node.SearchNode;
import com.xbk.agent.framework.graphflow.handwritten.node.UnderstandNode;

/**
 * 手写版图流门面类
 *
 * 职责：组装所有节点与图执行引擎，对外暴露单一入口 run(userQuery)。
 * 调用方无需感知节点细节，只需传入用户提问，得到最终回答与完整图状态。
 *
 * @author xiexu
 */
public class HandwrittenGraphFlow {

    private final GraphRunner graphRunner;

    /**
     * 创建手写版图流，使用正常搜索工具（不模拟失败）。
     *
     * @param gateway 统一大模型调用网关
     */
    public HandwrittenGraphFlow(AgentLlmGateway gateway) {
        this(gateway, new MockSearchTool(false));
    }

    /**
     * 创建手写版图流，支持注入可控失败的搜索工具（用于测试 FallbackNode 分支）。
     *
     * @param gateway    统一大模型调用网关
     * @param searchTool 搜索工具（可注入失败桩）
     */
    public HandwrittenGraphFlow(AgentLlmGateway gateway, MockSearchTool searchTool) {
        // 这里不是在“按顺序执行”四个节点，而是在启动前先把整张图需要的节点装配完整。
        // 其中 Understand / Answer / Fallback 依赖统一 gateway 调 LLM，
        // SearchNode 单独依赖搜索工具，负责把“关键词”真正落到外部工具调用上。
        UnderstandNode understandNode = new UnderstandNode(gateway);
        SearchNode searchNode = new SearchNode(searchTool);
        AnswerNode answerNode = new AnswerNode(gateway);
        FallbackNode fallbackNode = new FallbackNode(gateway);
        // 真正掌握执行顺序的不是这个门面类，而是 GraphRunner。
        // 它会根据 GraphState.stepStatus 决定下一跳跑哪个节点。
        this.graphRunner = new GraphRunner(understandNode, searchNode, answerNode, fallbackNode);
    }

    /**
     * 执行三步问答图流程，返回最终状态。
     *
     * @param userQuery 用户原始提问
     * @return 图执行完毕后的全局状态（包含 finalAnswer 及全链路字段）
     */
    public GraphState run(String userQuery) {
        // 门面类只负责把原始输入包装成初始状态，并把控制权交给 GraphRunner。
        // 后续节点切换、成功分支和降级分支都在图执行引擎内部完成。
        GraphState initialState = GraphState.init(userQuery);
        return graphRunner.run(initialState);
    }
}

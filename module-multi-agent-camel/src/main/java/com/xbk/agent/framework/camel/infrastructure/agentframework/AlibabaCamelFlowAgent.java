package com.xbk.agent.framework.camel.infrastructure.agentframework;

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
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.camel.api.CamelRunResult;
import com.xbk.agent.framework.camel.domain.memory.CamelDialogueTurn;
import com.xbk.agent.framework.camel.domain.role.CamelRoleContract;
import com.xbk.agent.framework.camel.domain.role.CamelRoleType;
import com.xbk.agent.framework.camel.infrastructure.agentframework.node.CamelProgrammerHandoffNode;
import com.xbk.agent.framework.camel.infrastructure.agentframework.node.CamelTraderHandoffNode;
import com.xbk.agent.framework.camel.infrastructure.agentframework.support.CamelTranscriptStateSupport;
import com.xbk.agent.framework.camel.support.CamelGatewayBackedChatModel;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Spring AI Alibaba 图编排版 CAMEL Agent
 *
 * 职责：用 FlowAgent、StateGraph 和条件边驱动交易员与程序员之间的受控 handoff 回环
 *
 * @author xiexu
 */
public class AlibabaCamelFlowAgent extends FlowAgent {

    private static final String FLOW_NAME = "alibaba-camel-flow-agent";
    private static final String TRADER_NODE = "trader_handoff_node";
    private static final String PROGRAMMER_NODE = "programmer_handoff_node";
    private static final String CURRENT_JAVA_CODE_KEY = "current_java_code";

    /**
     * 统一网关，所有节点最终都从这里发起模型调用。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 交易员角色契约。
     */
    private final CamelRoleContract traderContract;

    /**
     * 程序员角色契约。
     */
    private final CamelRoleContract programmerContract;

    /**
     * 交易员的框架元数据对象，主要用于表达角色和职责。
     */
    private final ReactAgent traderAgent;

    /**
     * 程序员的框架元数据对象，主要用于表达角色和职责。
     */
    private final ReactAgent programmerAgent;

    /**
     * 最大轮次上限，防止状态图无限回环。
     */
    private final int maxTurns;

    /**
     * 创建图编排版 CAMEL Agent。
     *
     * @param agentLlmGateway 统一网关
     * @param maxTurns 最大轮次
     */
    public AlibabaCamelFlowAgent(AgentLlmGateway agentLlmGateway, int maxTurns) {
        this(
                agentLlmGateway,
                new CamelGatewayBackedChatModel(agentLlmGateway),
                maxTurns,
                CamelRoleContract.traderContract(),
                CamelRoleContract.programmerContract());
    }

    /**
     * 创建图编排版 CAMEL Agent。
     *
     * @param agentLlmGateway 统一网关
     * @param chatModel 网关适配的 ChatModel
     * @param maxTurns 最大轮次
     * @param traderContract 交易员契约
     * @param programmerContract 程序员契约
     */
    private AlibabaCamelFlowAgent(AgentLlmGateway agentLlmGateway,
                                          ChatModel chatModel,
                                          int maxTurns,
                                          CamelRoleContract traderContract,
                                          CamelRoleContract programmerContract) {
        this(
                agentLlmGateway,
                traderContract,
                programmerContract,
                createReactAgent(chatModel, traderContract),
                createReactAgent(chatModel, programmerContract),
                maxTurns);
    }

    /**
     * 创建图编排版 CAMEL Agent。
     *
     * @param agentLlmGateway 统一网关
     * @param traderContract 交易员契约
     * @param programmerContract 程序员契约
     * @param traderAgent 交易员 Agent
     * @param programmerAgent 程序员 Agent
     * @param maxTurns 最大轮次
     */
    private AlibabaCamelFlowAgent(AgentLlmGateway agentLlmGateway,
                                          CamelRoleContract traderContract,
                                          CamelRoleContract programmerContract,
                                          ReactAgent traderAgent,
                                          ReactAgent programmerAgent,
                                          int maxTurns) {
        super(
                FLOW_NAME,
                "CAMEL 股票分析协作图编排 Agent",
                CompileConfig.builder()
                        .recursionLimit(Math.max(4, maxTurns * 2))
                        .build(),
                List.<Agent>of(traderAgent, programmerAgent));
        this.agentLlmGateway = agentLlmGateway;
        this.traderContract = traderContract;
        this.programmerContract = programmerContract;
        this.traderAgent = traderAgent;
        this.programmerAgent = programmerAgent;
        this.maxTurns = maxTurns;
    }

    /**
     * 运行图编排版 CAMEL。
     *
     * @param task 原始任务
     * @return 运行结果
     */
    public CamelRunResult run(String task) {
        try {
            Map<String, Object> input = new LinkedHashMap<String, Object>();
            // 原始协作任务，所有节点都会围绕它继续推进。
            input.put("input", task);
            // 本次图运行的会话唯一标识，用来把多轮 handoff 串在同一条会话链路里。
            input.put("conversation_id", FLOW_NAME + "-conversation-" + UUID.randomUUID());
            // 当前默认由交易员先接管控制权，负责发起第一棒需求。
            input.put("active_role", CamelRoleType.TRADER.getStateValue());
            // 当前已执行的总轮次数，后面会用它控制最大轮次上限。
            input.put("turn_count", Integer.valueOf(0));
            // 交给交易员节点消费的 baton，默认先为空，等待程序员回写。
            input.put("message_for_trader", "");
            // 交给程序员节点消费的 baton，默认先为空，等待交易员回写。
            input.put("message_for_programmer", "");
            // 最近一次交易员的原始输出，便于状态审计和后续条件判断。
            input.put("last_trader_output", "");
            // 最近一次程序员的原始输出，便于状态审计和后续条件判断。
            input.put("last_programmer_output", "");
            // 当前沉淀出的最新 Java 代码，作为最终结果的直接来源。
            input.put(CURRENT_JAVA_CODE_KEY, "");
            // 图运行是否已经满足结束条件，默认先不结束。
            input.put("done", Boolean.FALSE);
            // 结束原因，只有真正停止时才会被某个节点回写。
            input.put("stop_reason", "");
            // 完整 transcript 轨迹，供调试、审计和最终结果回放使用。
            input.put("transcript", List.of());
            Optional<OverAllState> optionalState = invoke(input);
            OverAllState state = optionalState.orElseThrow(
                    () -> new IllegalStateException("Camel FlowAgent did not return state"));
            String finalScript = state.value(CURRENT_JAVA_CODE_KEY, "");
            String stopReason = state.value("stop_reason", "");
            int turnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0));
            if (stopReason.isBlank() && turnCount >= maxTurns) {
                stopReason = "MAX_TURNS_REACHED";
            }
            CamelRoleType stopRole = determineStopRole(state);
            return new CamelRunResult(
                    task,
                    finalScript,
                    stopRole,
                    stopReason,
                    extractTranscript(state),
                    state);
        } catch (GraphRunnerException exception) {
            throw new IllegalStateException("Camel FlowAgent execution failed", exception);
        }
    }

    /**
     * 返回交易员 Agent 元数据。
     *
     * @return 交易员 Agent
     */
    public ReactAgent getTraderAgent() {
        return traderAgent;
    }

    /**
     * 返回程序员 Agent 元数据。
     *
     * @return 程序员 Agent
     */
    public ReactAgent getProgrammerAgent() {
        return programmerAgent;
    }

    /**
     * 构建具体状态图。
     *
     * @param config 图配置
     * @return 状态图
     * @throws GraphStateException 图构建异常
     */
    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
        StateGraph stateGraph = new StateGraph();
        // 整张图固定只有两个业务节点。
        // 交易员节点负责决定“继续交给程序员”还是“直接结束”，程序员节点则永远把控制权交还给交易员。
        stateGraph.addNode(TRADER_NODE, new CamelTraderHandoffNode(agentLlmGateway, traderContract));
        stateGraph.addNode(PROGRAMMER_NODE, new CamelProgrammerHandoffNode(agentLlmGateway, programmerContract));
        stateGraph.addEdge(StateGraph.START, TRADER_NODE);
        stateGraph.addConditionalEdges(TRADER_NODE, nextFromTrader(), Map.of(
                "programmer", PROGRAMMER_NODE,
                "end", StateGraph.END));
        stateGraph.addEdge(PROGRAMMER_NODE, TRADER_NODE);
        return stateGraph;
    }

    /**
     * 交易员节点后的条件边。
     *
     * @return 条件边动作
     */
    private AsyncEdgeAction nextFromTrader() {
        return state -> CompletableFuture.completedFuture(shouldStop(state) ? "end" : "programmer");
    }

    /**
     * 判断流程是否应该停止。
     *
     * @param state 全局状态
     * @return 是否停止
     */
    private boolean shouldStop(OverAllState state) {
        boolean done = state.value("done", Boolean.class).orElse(Boolean.FALSE);
        int turnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0));
        return done || turnCount >= maxTurns;
    }

    /**
     * 推断停止角色。
     *
     * @param state 全局状态
     * @return 停止角色
     */
    private CamelRoleType determineStopRole(OverAllState state) {
        String stopReason = state.value("stop_reason", "");
        if (stopReason != null && !stopReason.isBlank()) {
            return CamelRoleType.fromStateValue(state.value("active_role", CamelRoleType.TRADER.getStateValue()));
        }
        List<CamelDialogueTurn> transcript = extractTranscript(state);
        if (transcript.isEmpty()) {
            return CamelRoleType.TRADER;
        }
        return transcript.get(transcript.size() - 1).getRoleType();
    }

    /**
     * 提取 transcript。
     *
     * @param state 全局状态
     * @return transcript
     */
    private List<CamelDialogueTurn> extractTranscript(OverAllState state) {
        return List.copyOf(CamelTranscriptStateSupport.readTranscript(state.value("transcript").orElse(List.of())));
    }

    /**
     * 创建角色元数据 Agent。
     *
     * @param chatModel 网关适配的 ChatModel
     * @param contract 角色契约
     * @return ReactAgent
     */
    private static ReactAgent createReactAgent(ChatModel chatModel, CamelRoleContract contract) {
        return ReactAgent.builder()
                .name(contract.getAgentName())
                .description(contract.getDescription())
                .model(chatModel)
                .systemPrompt(contract.getSystemPrompt())
                .instruction("请根据状态继续推进 CAMEL 协作。")
                .outputKey(contract.getOutputKey())
                .includeContents(false)
                .returnReasoningContents(false)
                .build();
    }

    /*
     * 对照说明：
     * 这里不再把消息路由写死在 while 循环里，而是让 FlowAgent 把“当前轮到谁发言、下一步跳到哪里、什么时候结束”
     * 全部交给状态图和条件边管理。节点只关心读取最小必要 baton 并写回状态，因此工程上更容易扩展到多角色、
     * 多分支和审计回放场景。和手写版相比，它把流程控制从业务代码里抽离出来，可维护性和可治理性更强。
     */
}

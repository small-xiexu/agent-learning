package com.xbk.agent.framework.conversation.infrastructure.agentframework;

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
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.conversation.api.ConversationRunResult;
import com.xbk.agent.framework.conversation.domain.memory.ConversationTurn;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleContract;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.node.CodeReviewerNode;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.node.EngineerNode;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.node.ProductManagerNode;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.support.ConversationStateSupport;
import com.xbk.agent.framework.conversation.support.ConversationGatewayBackedChatModel;
import com.xbk.agent.framework.conversation.support.ConversationPromptTemplates;
import org.springframework.ai.chat.model.ChatModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Spring AI Alibaba 图编排版 Conversation Agent
 *
 * 职责：用 FlowAgent 和共享状态实现 AutoGen 风格的 RoundRobin 群聊回环
 *
 * @author xiexu
 */
public class AlibabaConversationFlowAgent extends FlowAgent {

    private static final String FLOW_NAME = "alibaba-conversation-flow-agent";
    private static final String PRODUCT_MANAGER_NODE = "product_manager_node";
    private static final String ENGINEER_NODE = "engineer_node";
    private static final String CODE_REVIEWER_NODE = "code_reviewer_node";
    private static final String CURRENT_PYTHON_SCRIPT_KEY = "current_python_script";

    /**
     * 统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 产品经理契约。
     */
    private final ConversationRoleContract productManagerContract;

    /**
     * 工程师契约。
     */
    private final ConversationRoleContract engineerContract;

    /**
     * 审查员契约。
     */
    private final ConversationRoleContract codeReviewerContract;

    /**
     * 产品经理元数据 Agent。
     */
    private final ReactAgent productManagerAgent;

    /**
     * 工程师元数据 Agent。
     */
    private final ReactAgent engineerAgent;

    /**
     * 审查员元数据 Agent。
     */
    private final ReactAgent codeReviewerAgent;

    /**
     * 最大轮次上限。
     */
    private final int maxTurns;

    /**
     * 创建图编排版 Conversation Agent。
     *
     * @param agentLlmGateway 统一网关
     * @param maxTurns 最大轮次
     */
    public AlibabaConversationFlowAgent(AgentLlmGateway agentLlmGateway, int maxTurns) {
        this(
                agentLlmGateway,
                new ConversationGatewayBackedChatModel(agentLlmGateway),
                maxTurns,
                ConversationRoleContract.productManagerContract(),
                ConversationRoleContract.engineerContract(),
                ConversationRoleContract.codeReviewerContract());
    }

    /**
     * 创建图编排版 Conversation Agent。
     *
     * @param agentLlmGateway 统一网关
     * @param chatModel ChatModel 适配器
     * @param maxTurns 最大轮次
     * @param productManagerContract 产品经理契约
     * @param engineerContract 工程师契约
     * @param codeReviewerContract 审查员契约
     */
    private AlibabaConversationFlowAgent(AgentLlmGateway agentLlmGateway,
                                         ChatModel chatModel,
                                         int maxTurns,
                                         ConversationRoleContract productManagerContract,
                                         ConversationRoleContract engineerContract,
                                         ConversationRoleContract codeReviewerContract) {
        this(
                agentLlmGateway,
                productManagerContract,
                engineerContract,
                codeReviewerContract,
                createReactAgent(chatModel, productManagerContract),
                createReactAgent(chatModel, engineerContract),
                createReactAgent(chatModel, codeReviewerContract),
                maxTurns);
    }

    /**
     * 创建图编排版 Conversation Agent。
     *
     * @param agentLlmGateway 统一网关
     * @param productManagerContract 产品经理契约
     * @param engineerContract 工程师契约
     * @param codeReviewerContract 审查员契约
     * @param productManagerAgent 产品经理元数据 Agent
     * @param engineerAgent 工程师元数据 Agent
     * @param codeReviewerAgent 审查员元数据 Agent
     * @param maxTurns 最大轮次
     */
    private AlibabaConversationFlowAgent(AgentLlmGateway agentLlmGateway,
                                         ConversationRoleContract productManagerContract,
                                         ConversationRoleContract engineerContract,
                                         ConversationRoleContract codeReviewerContract,
                                         ReactAgent productManagerAgent,
                                         ReactAgent engineerAgent,
                                         ReactAgent codeReviewerAgent,
                                         int maxTurns) {
        super(
                FLOW_NAME,
                "AutoGen RoundRobin Conversation Flow Agent",
                CompileConfig.builder()
                        .recursionLimit(Math.max(6, maxTurns * 3))
                        .build(),
                List.<Agent>of(productManagerAgent, engineerAgent, codeReviewerAgent));
        this.agentLlmGateway = agentLlmGateway;
        this.productManagerContract = productManagerContract;
        this.engineerContract = engineerContract;
        this.codeReviewerContract = codeReviewerContract;
        this.productManagerAgent = productManagerAgent;
        this.engineerAgent = engineerAgent;
        this.codeReviewerAgent = codeReviewerAgent;
        this.maxTurns = maxTurns;
    }

    /**
     * 运行图编排版 Conversation。
     *
     * @param task 原始任务
     * @return 运行结果
     */
    public ConversationRunResult run(String task) {
        try {
            String conversationId = FLOW_NAME + "-conversation-" + UUID.randomUUID();
            Map<String, Object> input = new LinkedHashMap<String, Object>();
            // 原始任务，所有节点都围绕同一目标群聊推进。
            input.put("input", task);
            // 统一会话标识，把整场群聊绑定到同一条上下文链路。
            input.put("conversation_id", conversationId);
            // 当前活动角色，初始固定为 ProductManager。
            input.put("active_role", ConversationRoleType.PRODUCT_MANAGER.getStateValue());
            // 已执行总轮次，用于防止群聊无限回环。
            input.put("turn_count", Integer.valueOf(0));
            // 各角色最近一次输出，便于调试和断言。
            input.put("last_product_output", "");
            input.put("last_engineer_output", "");
            input.put("last_reviewer_output", "");
            // 当前最新 Python 脚本，直接作为最终产物来源。
            input.put(CURRENT_PYTHON_SCRIPT_KEY, "");
            // 审查状态，默认 pending。
            input.put("review_status", "pending");
            // 是否已经结束。
            input.put("done", Boolean.FALSE);
            // 停止原因。
            input.put("stop_reason", "");
            // 群聊共享消息历史，这是 AutoGen 群聊最核心的公共上下文。
            input.put("shared_messages", ConversationStateSupport.toStateMessages(List.of(buildTaskMessage(conversationId, task))));
            // transcript 只用于回放和审计。
            input.put("transcript", List.of());
            Optional<OverAllState> optionalState = invoke(input);
            OverAllState state = optionalState.orElseThrow(
                    () -> new IllegalStateException("Conversation FlowAgent did not return state"));
            String finalScript = state.value(CURRENT_PYTHON_SCRIPT_KEY, "");
            String stopReason = state.value("stop_reason", "");
            int turnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0));
            if (stopReason.isBlank() && turnCount >= maxTurns) {
                stopReason = "MAX_TURNS_REACHED";
            }
            return new ConversationRunResult(
                    task,
                    finalScript,
                    determineStopRole(state),
                    stopReason,
                    extractTranscript(state),
                    extractSharedMessages(state),
                    state);
        } catch (GraphRunnerException exception) {
            throw new IllegalStateException("Conversation FlowAgent execution failed", exception);
        }
    }

    /**
     * 返回产品经理元数据 Agent。
     *
     * @return 产品经理元数据 Agent
     */
    public ReactAgent getProductManagerAgent() {
        return productManagerAgent;
    }

    /**
     * 返回工程师元数据 Agent。
     *
     * @return 工程师元数据 Agent
     */
    public ReactAgent getEngineerAgent() {
        return engineerAgent;
    }

    /**
     * 返回审查员元数据 Agent。
     *
     * @return 审查员元数据 Agent
     */
    public ReactAgent getCodeReviewerAgent() {
        return codeReviewerAgent;
    }

    /**
     * 构建具体状态图。
     *
     * @param config 图配置
     * @return 状态图
     * @throws GraphStateException 图异常
     */
    @Override
    protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
        // 先创建一张空状态图，后续把 RoundRobin 群聊的节点和边逐步装进去。
        StateGraph stateGraph = new StateGraph();
        // 注册 ProductManager 节点，让流程从“拆需求、给下一步目标”开始。
        stateGraph.addNode(PRODUCT_MANAGER_NODE, new ProductManagerNode(agentLlmGateway, productManagerContract));
        // 注册 Engineer 节点，让产品需求在下一步被转成最新完整脚本。
        stateGraph.addNode(ENGINEER_NODE, new EngineerNode(agentLlmGateway, engineerContract));
        // 注册 CodeReviewer 节点，让工程产物在这一环被审查并决定是否继续。
        stateGraph.addNode(CODE_REVIEWER_NODE, new CodeReviewerNode(agentLlmGateway, codeReviewerContract));
        // 从图起点进入 ProductManager，表示每轮群聊都先从需求分析开始。
        stateGraph.addEdge(StateGraph.START, PRODUCT_MANAGER_NODE);
        // ProductManager 完成后固定流向 Engineer，表示下一步一定是实现阶段。
        stateGraph.addEdge(PRODUCT_MANAGER_NODE, ENGINEER_NODE);
        // Engineer 完成后固定流向 Reviewer，表示代码产物必须经过审查。
        stateGraph.addEdge(ENGINEER_NODE, CODE_REVIEWER_NODE);
        // 在 Reviewer 后面挂条件边，由审查结论决定是继续回环，还是直接结束整场群聊。
        stateGraph.addConditionalEdges(CODE_REVIEWER_NODE, nextFromCodeReviewer(), Map.of(
                // 审查未通过时，回到 ProductManager，把阻塞意见继续翻译成下一轮任务目标。
                "product_manager", PRODUCT_MANAGER_NODE,
                // 审查通过或达到最大轮次时，直接进入图终点，停止后续协作。
                "end", StateGraph.END));
        // 返回构建完成的状态图，交给 FlowAgent runtime 在运行时编译并执行。
        return stateGraph;
    }

    /**
     * 审查员节点后的条件边。
     *
     * @return 条件边动作
     */
    private AsyncEdgeAction nextFromCodeReviewer() {
        return state -> CompletableFuture.completedFuture(shouldStop(state) ? "end" : "product_manager");
    }

    /**
     * 判断流程是否应停止。
     *
     * @param state 全局状态
     * @return 是否停止
     */
    private boolean shouldStop(OverAllState state) {
        // done 由 Reviewer 节点写入，表示当前任务是否已经被正式验收通过。
        boolean done = state.value("done", Boolean.class).orElse(Boolean.FALSE);
        // turn_count 记录整场群聊已经推进了多少轮；如果状态里还没有这个值，就从 0 开始算。
        int turnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0));
        // 只要“已经完成”或者“轮次达到上限”满足任意一个条件，就停止继续回环。
        return done || turnCount >= maxTurns;
    }

    /**
     * 推断停止角色。
     *
     * @param state 全局状态
     * @return 停止角色
     */
    private ConversationRoleType determineStopRole(OverAllState state) {
        List<ConversationTurn> transcript = extractTranscript(state);
        if (transcript.isEmpty()) {
            return ConversationRoleType.PRODUCT_MANAGER;
        }
        return transcript.get(transcript.size() - 1).getRoleType();
    }

    /**
     * 提取 transcript。
     *
     * @param state 全局状态
     * @return transcript
     */
    private List<ConversationTurn> extractTranscript(OverAllState state) {
        return List.copyOf(ConversationStateSupport.readTranscript(state.value("transcript").orElse(List.of())));
    }

    /**
     * 提取共享消息。
     *
     * @param state 全局状态
     * @return 共享消息
     */
    private List<Message> extractSharedMessages(OverAllState state) {
        return List.copyOf(ConversationStateSupport.readSharedMessages(state.value("shared_messages").orElse(List.of())));
    }

    /**
     * 构造任务启动消息。
     *
     * @param conversationId 会话标识
     * @param task 原始任务
     * @return 任务消息
     */
    private Message buildTaskMessage(String conversationId, String task) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .name("Task")
                .content(ConversationPromptTemplates.groupKickoffPrompt(task))
                .build();
    }

    /**
     * 创建角色元数据 Agent。
     *
     * @param chatModel ChatModel 适配器
     * @param contract 角色契约
     * @return ReactAgent
     */
    private static ReactAgent createReactAgent(ChatModel chatModel, ConversationRoleContract contract) {
        return ReactAgent.builder()
                .name(contract.getAgentName())
                .description(contract.getDescription())
                .model(chatModel)
                .systemPrompt(contract.getSystemPrompt())
                .instruction("请根据共享群聊上下文继续推进 RoundRobin 协作。")
                .outputKey(contract.getOutputKey())
                .includeContents(false)
                .returnReasoningContents(false)
                .build();
    }
}

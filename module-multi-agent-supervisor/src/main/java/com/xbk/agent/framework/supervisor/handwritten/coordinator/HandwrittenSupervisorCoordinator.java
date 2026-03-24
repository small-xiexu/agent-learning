package com.xbk.agent.framework.supervisor.handwritten.coordinator;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.supervisor.api.SupervisorRunResult;
import com.xbk.agent.framework.supervisor.domain.memory.SupervisorStepRecord;
import com.xbk.agent.framework.supervisor.domain.routing.CompletionPolicy;
import com.xbk.agent.framework.supervisor.domain.routing.RoutingDecision;
import com.xbk.agent.framework.supervisor.domain.routing.SupervisorWorkerType;
import com.xbk.agent.framework.supervisor.domain.state.SupervisorWorkflowState;
import com.xbk.agent.framework.supervisor.handwritten.executor.ReviewerAgent;
import com.xbk.agent.framework.supervisor.handwritten.executor.TranslatorAgent;
import com.xbk.agent.framework.supervisor.handwritten.executor.WriterAgent;
import com.xbk.agent.framework.supervisor.handwritten.memory.SupervisorScratchpad;
import com.xbk.agent.framework.supervisor.handwritten.parser.SupervisorDecisionJsonParser;
import com.xbk.agent.framework.supervisor.handwritten.prompt.HandwrittenSupervisorPromptTemplates;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 Supervisor 协调器
 *
 * 职责：显式维护 Scratchpad、JSON 路由解析和 Supervisor 受控回环，
 * 把“中心化调度”这件事完整摊开成可读的 Java 控制流
 *
 * @author xiexu
 */
public final class HandwrittenSupervisorCoordinator {

    private static final String REQUEST_PREFIX = "handwritten-supervisor-";

    private final AgentLlmGateway agentLlmGateway;
    private final WriterAgent writerAgent;
    private final TranslatorAgent translatorAgent;
    private final ReviewerAgent reviewerAgent;
    private final SupervisorScratchpad scratchpad;
    private final SupervisorDecisionJsonParser decisionJsonParser;
    private final CompletionPolicy completionPolicy;

    /**
     * 创建手写版 Supervisor 协调器。
     *
     * @param agentLlmGateway 统一网关
     * @param maxRounds 最大路由轮次
     */
    public HandwrittenSupervisorCoordinator(AgentLlmGateway agentLlmGateway, int maxRounds) {
        this(
                agentLlmGateway,
                new WriterAgent(agentLlmGateway),
                new TranslatorAgent(agentLlmGateway),
                new ReviewerAgent(agentLlmGateway),
                new SupervisorScratchpad(),
                new SupervisorDecisionJsonParser(),
                new CompletionPolicy(maxRounds));
    }

    /**
     * 创建手写版 Supervisor 协调器。
     *
     * @param agentLlmGateway 统一网关
     * @param writerAgent WriterAgent
     * @param translatorAgent TranslatorAgent
     * @param reviewerAgent ReviewerAgent
     * @param scratchpad Scratchpad
     * @param decisionJsonParser JSON 解析器
     * @param completionPolicy 完成策略
     */
    public HandwrittenSupervisorCoordinator(AgentLlmGateway agentLlmGateway,
                                            WriterAgent writerAgent,
                                            TranslatorAgent translatorAgent,
                                            ReviewerAgent reviewerAgent,
                                            SupervisorScratchpad scratchpad,
                                            SupervisorDecisionJsonParser decisionJsonParser,
                                            CompletionPolicy completionPolicy) {
        this.agentLlmGateway = agentLlmGateway;
        this.writerAgent = writerAgent;
        this.translatorAgent = translatorAgent;
        this.reviewerAgent = reviewerAgent;
        this.scratchpad = scratchpad;
        this.decisionJsonParser = decisionJsonParser;
        this.completionPolicy = completionPolicy;
    }

    /**
     * 运行手写版 Supervisor。
     *
     * @param task 原始任务
     * @return 统一运行结果
     */
    public SupervisorRunResult run(String task) {
        String conversationId = REQUEST_PREFIX + UUID.randomUUID();
        SupervisorWorkflowState workflowState = new SupervisorWorkflowState(task, conversationId);
        scratchpad.clear();
        // 手写版没有框架状态容器，因此原始任务也要主动写入 Scratchpad 供后续回放。
        scratchpad.seedTask(conversationId, task);

        int completedDecisionRounds = 0;
        while (completionPolicy.allowsNextRound(completedDecisionRounds)) {
            completedDecisionRounds++;
            RoutingDecision decision = askSupervisor(task, workflowState, conversationId);
            // routeTrail 记录的是 Supervisor 的真实决策序列，因此 FINISH 也要入轨迹。
            workflowState.recordRoute(decision.getNextWorker());
            scratchpad.appendSupervisorDecision(conversationId, decision);
            if (decision.getNextWorker() == SupervisorWorkerType.FINISH) {
                return buildResult(workflowState, "FINISH");
            }
            String workerOutput = dispatchWorker(decision, workflowState, conversationId);
            // Worker 输出一式两份：一份写当前事实，一份写审计历史。
            workflowState.applyWorkerOutput(decision.getNextWorker(), workerOutput);
            scratchpad.appendWorkerOutput(conversationId, decision.getNextWorker(), workerOutput);
            scratchpad.appendStepRecord(new SupervisorStepRecord(
                    workflowState.getCompletedWorkerSteps(),
                    decision.getNextWorker(),
                    decision.getTaskInstruction(),
                    workerOutput));
        }
        return buildResult(workflowState, "MAX_ROUNDS_REACHED");
    }

    /**
     * 请求监督者做出下一轮决策。
     *
     * @param task 原始任务
     * @param workflowState 当前工作流状态
     * @param conversationId 会话标识
     * @return 路由决策
     */
    private RoutingDecision askSupervisor(String task,
                                          SupervisorWorkflowState workflowState,
                                          String conversationId) {
        // 手写版必须显式把“当前事实 + 历史轨迹”拼回 Prompt，才能让主管做出下一轮路由决策。
        String userPrompt = HandwrittenSupervisorPromptTemplates.buildSupervisorUserPrompt(
                task,
                workflowState,
                scratchpad.snapshotMessages());
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(REQUEST_PREFIX + "router-" + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(List.of(
                        buildMessage(conversationId, MessageRole.SYSTEM,
                                HandwrittenSupervisorPromptTemplates.supervisorSystemPrompt()),
                        buildMessage(conversationId, MessageRole.USER, userPrompt)))
                .build());
        String responseText = response.getOutputMessage() != null && response.getOutputMessage().getContent() != null
                ? response.getOutputMessage().getContent()
                : response.getRawText();
        return decisionJsonParser.parse(responseText);
    }

    /**
     * 分发到具体 Worker。
     *
     * @param decision 路由决策
     * @param workflowState 当前工作流状态
     * @param conversationId 会话标识
     * @return Worker 输出
     */
    private String dispatchWorker(RoutingDecision decision,
                                  SupervisorWorkflowState workflowState,
                                  String conversationId) {
        // 由协调器显式控制 Worker 分发，确保控制权永远不会从 Supervisor 手里溜走。
        if (decision.getNextWorker() == SupervisorWorkerType.WRITER) {
            return writerAgent.write(workflowState.getTask(), decision.getTaskInstruction(), conversationId);
        }
        if (decision.getNextWorker() == SupervisorWorkerType.TRANSLATOR) {
            return translatorAgent.translate(
                    workflowState.getChineseDraft(),
                    decision.getTaskInstruction(),
                    conversationId);
        }
        if (decision.getNextWorker() == SupervisorWorkerType.REVIEWER) {
            return reviewerAgent.review(
                    workflowState.getEnglishTranslation(),
                    decision.getTaskInstruction(),
                    conversationId);
        }
        throw new IllegalStateException("Unsupported worker for dispatch: " + decision.getNextWorker());
    }

    /**
     * 构造统一运行结果。
     *
     * @param workflowState 工作流状态
     * @param stopReason 停止原因
     * @return 统一运行结果
     */
    private SupervisorRunResult buildResult(SupervisorWorkflowState workflowState, String stopReason) {
        // 手写版虽然没有框架状态，但仍然返回和框架版同构的结果对象，方便对照测试和上层消费。
        return new SupervisorRunResult(
                workflowState.getTask(),
                workflowState.getChineseDraft(),
                workflowState.getEnglishTranslation(),
                workflowState.getReviewedEnglish(),
                workflowState.snapshotRouteTrail().isEmpty()
                        ? SupervisorWorkerType.FINISH
                        : workflowState.snapshotRouteTrail().get(workflowState.snapshotRouteTrail().size() - 1),
                stopReason,
                workflowState.snapshotRouteTrail(),
                scratchpad.snapshotStepRecords(),
                scratchpad.snapshotMessages(),
                null);
    }

    /**
     * 构造统一消息。
     *
     * @param conversationId 会话标识
     * @param role 消息角色
     * @param content 消息内容
     * @return 统一消息
     */
    private Message buildMessage(String conversationId, MessageRole role, String content) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .build();
    }
}

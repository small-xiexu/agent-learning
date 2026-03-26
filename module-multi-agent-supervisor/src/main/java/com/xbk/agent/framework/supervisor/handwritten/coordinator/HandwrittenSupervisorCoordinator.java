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
        // 这个重载构造器提供“开箱即用”的教学入口：
        // 调用方只传统一网关和最大轮次，就能得到一套完整的 Writer / Translator / Reviewer 编排。
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
        // 这个重载则把所有依赖显式摊开，方便测试替身注入和教学场景下逐个替换组件观察行为。
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
     * 整个过程可以按“初始化 -> Supervisor 决策 -> Worker 执行 -> 状态沉淀 -> 终止判断”理解。
     * 和框架版不同，手写版不会把这些步骤藏在图引擎内部，而是显式展开成一段 while 循环，
     * 方便学习者看清 Supervisor 模式到底是如何一轮轮推进到 FINISH 的。
     *
     * @param task 原始任务
     * @return 统一运行结果
     */
    public SupervisorRunResult run(String task) {
        // 每次运行都创建新的会话标识，并重新初始化“当前事实快照”和“完整历史回放”。
        String conversationId = REQUEST_PREFIX + UUID.randomUUID();
        SupervisorWorkflowState workflowState = new SupervisorWorkflowState(task, conversationId);
        scratchpad.clear();
        // 手写版没有框架状态容器，因此原始任务也要主动写入 Scratchpad 供后续回放。
        scratchpad.seedTask(conversationId, task);

        // 这里统计的是“Supervisor 已经做了多少轮决策”，不是 Worker 已经执行了多少步。
        // 两者拆开记录，初学者更容易理解“调度轮次限制”和“实际执行步数”并不是同一件事。
        int completedDecisionRounds = 0;
        while (completionPolicy.allowsNextRound(completedDecisionRounds)) {
            completedDecisionRounds++;
            // 第一阶段：让 Supervisor 读取当前事实和历史轨迹，决定下一跳派谁执行。
            RoutingDecision decision = askSupervisor(task, workflowState, conversationId);
            // routeTrail 记录的是 Supervisor 的真实决策序列，因此 FINISH 也要入轨迹。
            workflowState.recordRoute(decision.getNextWorker());
            scratchpad.appendSupervisorDecision(conversationId, decision);
            if (decision.getNextWorker() == SupervisorWorkerType.FINISH) {
                // 主管显式宣布任务收敛后，直接返回最终结果，不再派发新的 Worker。
                return buildResult(workflowState, "FINISH");
            }

            // 第二阶段：由协调器把子任务分发给具体 Worker，拿回这一轮的业务产出。
            String workerOutput = dispatchWorker(decision, workflowState, conversationId);
            // Worker 输出一式两份：一份写当前事实，一份写审计历史。
            workflowState.applyWorkerOutput(decision.getNextWorker(), workerOutput);
            scratchpad.appendWorkerOutput(conversationId, decision.getNextWorker(), workerOutput);
            // stepRecord 只记录真正发生过的 Worker 执行，因此不会为 FINISH 额外生成一条步骤。
            scratchpad.appendStepRecord(new SupervisorStepRecord(
                    workflowState.getCompletedWorkerSteps(),
                    decision.getNextWorker(),
                    decision.getTaskInstruction(),
                    workerOutput));
        }
        // 达到最大调度轮次时也返回当前最成熟结果，方便学习者观察“未完全收敛”时系统停在了哪里。
        return buildResult(workflowState, "MAX_ROUNDS_REACHED");
    }

    /**
     * 请求监督者做出下一轮决策。
     *
     * 这一步体现了 Supervisor 模式最关键的能力：主管本身不直接产出业务内容，
     * 而是根据“当前事实快照 + 历史对话轨迹”决定下一位最合适的 Worker。
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
        // 优先读取规范化消息内容，拿不到时再回退到原始文本，避免解析器因响应封装差异而失效。
        String responseText = response.getOutputMessage() != null && response.getOutputMessage().getContent() != null
                ? response.getOutputMessage().getContent()
                : response.getRawText();
        return decisionJsonParser.parse(responseText);
    }

    /**
     * 分发到具体 Worker。
     *
     * 这里不是 Worker 自己决定下一跳，而是协调器根据 Supervisor 的决策显式派发。
     * 这种“控制权集中在主管手里”的写法，正是 Supervisor 范式和普通链式调用的重要区别。
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
            // Translator 不再回看原始任务主体，而是消费 Writer 已经交付的中文初稿。
            return translatorAgent.translate(
                    workflowState.getChineseDraft(),
                    decision.getTaskInstruction(),
                    conversationId);
        }
        if (decision.getNextWorker() == SupervisorWorkerType.REVIEWER) {
            // Reviewer 的输入是当前最新的英文译稿，职责是把结果推向最终完成态。
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
     * 手写版虽然没有图框架里的全局状态对象，但最终仍然要把“最终产物、路由轨迹、步骤审计”
     * 抽成和框架版同构的结果，方便学习者直接做手写版 / 框架版对照。
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
        // 即使这里只是组装 Prompt，也统一走项目自己的 Message 模型，
        // 这样 Supervisor 手写版和其他范式在网关层看到的输入协议保持一致。
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .build();
    }
}

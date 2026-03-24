package com.xbk.agent.framework.supervisor.framework.agent;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.supervisor.api.SupervisorRunResult;
import com.xbk.agent.framework.supervisor.domain.routing.SupervisorWorkerType;
import com.xbk.agent.framework.supervisor.framework.prompt.FrameworkSupervisorPromptTemplates;
import com.xbk.agent.framework.supervisor.framework.support.SupervisorGatewayBackedChatModel;
import com.xbk.agent.framework.supervisor.framework.support.SupervisorStateExtractor;
import com.xbk.agent.framework.supervisor.framework.support.SupervisorStateKeys;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
import java.util.Optional;

/**
 * Spring AI Alibaba 原生 Supervisor 封装器
 *
 * 职责：通过 `SupervisorAgent + ReactAgent` 原生能力实现多步骤动态路由与受控回环，
 * 并把框架原生状态重新封装成项目统一 `SupervisorRunResult`
 *
 * @author xiexu
 */
public final class AlibabaSupervisorFlowAgent {

    private final AgentLlmGateway agentLlmGateway;
    private final ReactAgent writerAgent;
    private final ReactAgent translatorAgent;
    private final ReactAgent reviewerAgent;
    private final ReactAgent mainRoutingAgent;
    private final SupervisorAgent supervisorAgent;

    /**
     * 创建框架版 Supervisor 封装器。
     *
     * @param agentLlmGateway 统一网关
     * @param maxRounds 最大调度轮次
     */
    public AlibabaSupervisorFlowAgent(AgentLlmGateway agentLlmGateway, int maxRounds) {
        this(agentLlmGateway, new SupervisorGatewayBackedChatModel(agentLlmGateway), maxRounds);
    }

    /**
     * 创建框架版 Supervisor 封装器。
     *
     * @param agentLlmGateway 统一网关
     * @param chatModel ChatModel 适配器
     * @param maxRounds 最大调度轮次
     */
    public AlibabaSupervisorFlowAgent(AgentLlmGateway agentLlmGateway, ChatModel chatModel, int maxRounds) {
        this.agentLlmGateway = agentLlmGateway;
        this.writerAgent = buildWriterAgent(chatModel);
        this.translatorAgent = buildTranslatorAgent(chatModel);
        this.reviewerAgent = buildReviewerAgent(chatModel);
        // mainRoutingAgent 只负责判断下一跳，本身不产出业务内容。
        this.mainRoutingAgent = buildMainRoutingAgent(chatModel);
        this.supervisorAgent = SupervisorAgent.builder()
                .name("blog_supervisor_agent")
                .description("中心化 Supervisor Agent，负责动态选择 writer、translator、reviewer 并在完成后 FINISH")
                .compileConfig(CompileConfig.builder()
                        .recursionLimit(Math.max(8, maxRounds * 4))
                        .build())
                .subAgents(List.of(writerAgent, translatorAgent, reviewerAgent))
                .mainAgent(mainRoutingAgent)
                .build();
    }

    /**
     * 运行框架版 Supervisor。
     *
     * @param task 原始任务
     * @return 统一运行结果
     */
    public SupervisorRunResult run(String task) {
        try {
            Optional<OverAllState> optionalState = supervisorAgent.invoke(task);
            OverAllState state = optionalState.orElseThrow(
                    () -> new IllegalStateException("SupervisorAgent did not return state"));
            // 先把原生状态还原为统一轨迹和产物，再暴露给测试与上层调用方。
            List<SupervisorWorkerType> routeTrail = SupervisorStateExtractor.extractRouteTrail(state);
            return new SupervisorRunResult(
                    task,
                    extractOutput(state, SupervisorStateKeys.WRITER_OUTPUT),
                    extractOutput(state, SupervisorStateKeys.TRANSLATOR_OUTPUT),
                    extractOutput(state, SupervisorStateKeys.REVIEWER_OUTPUT),
                    routeTrail.isEmpty() ? SupervisorWorkerType.FINISH : routeTrail.get(routeTrail.size() - 1),
                    routeTrail.isEmpty() ? "EMPTY_ROUTE" : routeTrail.get(routeTrail.size() - 1).name(),
                    routeTrail,
                    SupervisorStateExtractor.extractStepRecords(routeTrail, state),
                    SupervisorStateExtractor.extractMessages(state),
                    state);
        }
        catch (GraphRunnerException exception) {
            throw new IllegalStateException("SupervisorAgent execution failed", exception);
        }
    }

    /**
     * 提取指定输出键的文本。
     *
     * @param state 框架状态
     * @param key 输出键
     * @return 文本输出
     */
    public String extractOutput(OverAllState state, String key) {
        return SupervisorStateExtractor.extractOutput(state, key);
    }

    /**
     * 返回 WriterAgent。
     *
     * @return WriterAgent
     */
    public ReactAgent getWriterAgent() {
        return writerAgent;
    }

    /**
     * 返回 TranslatorAgent。
     *
     * @return TranslatorAgent
     */
    public ReactAgent getTranslatorAgent() {
        return translatorAgent;
    }

    /**
     * 返回 ReviewerAgent。
     *
     * @return ReviewerAgent
     */
    public ReactAgent getReviewerAgent() {
        return reviewerAgent;
    }

    /**
     * 返回主监督者路由 Agent。
     *
     * @return 主监督者路由 Agent
     */
    public ReactAgent getMainRoutingAgent() {
        return mainRoutingAgent;
    }

    /**
     * 返回 SupervisorAgent。
     *
     * @return SupervisorAgent
     */
    public SupervisorAgent getSupervisorAgent() {
        return supervisorAgent;
    }

    /**
     * 构造 WriterAgent。
     *
     * @param chatModel ChatModel
     * @return WriterAgent
     */
    private ReactAgent buildWriterAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("writer_agent")
                .description("撰写中文简短博客")
                .model(chatModel)
                .systemPrompt(FrameworkSupervisorPromptTemplates.writerSystemPrompt())
                .instruction(FrameworkSupervisorPromptTemplates.writerInstruction())
                // outputKey 是主路由 Agent 判断下一跳时依赖的核心状态协议。
                .outputKey(SupervisorStateKeys.WRITER_OUTPUT)
                .returnReasoningContents(false)
                .build();
    }

    /**
     * 构造 TranslatorAgent。
     *
     * @param chatModel ChatModel
     * @return TranslatorAgent
     */
    private ReactAgent buildTranslatorAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("translator_agent")
                .description("把中文博客翻译成英文")
                .model(chatModel)
                .systemPrompt(FrameworkSupervisorPromptTemplates.translatorSystemPrompt())
                .instruction(FrameworkSupervisorPromptTemplates.translatorInstruction())
                // translator_output 会成为 reviewer 的输入，同时也是 FINISH 判断依据之一。
                .outputKey(SupervisorStateKeys.TRANSLATOR_OUTPUT)
                .returnReasoningContents(false)
                .build();
    }

    /**
     * 构造 ReviewerAgent。
     *
     * @param chatModel ChatModel
     * @return ReviewerAgent
     */
    private ReactAgent buildReviewerAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("reviewer_agent")
                .description("对英文译文做语法和拼写审校")
                .model(chatModel)
                .systemPrompt(FrameworkSupervisorPromptTemplates.reviewerSystemPrompt())
                .instruction(FrameworkSupervisorPromptTemplates.reviewerInstruction())
                // reviewer_output 一旦非空，主路由 Agent 就会返回 FINISH。
                .outputKey(SupervisorStateKeys.REVIEWER_OUTPUT)
                .returnReasoningContents(false)
                .build();
    }

    /**
     * 构造主监督者路由 Agent。
     *
     * @param chatModel ChatModel
     * @return 主监督者路由 Agent
     */
    private ReactAgent buildMainRoutingAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("supervisor_router_agent")
                .description("根据当前状态决定下一位子 Agent 或 FINISH")
                .model(chatModel)
                .systemPrompt(FrameworkSupervisorPromptTemplates.supervisorRouterSystemPrompt())
                .instruction(FrameworkSupervisorPromptTemplates.supervisorRouterInstruction())
                .returnReasoningContents(false)
                .build();
    }
}

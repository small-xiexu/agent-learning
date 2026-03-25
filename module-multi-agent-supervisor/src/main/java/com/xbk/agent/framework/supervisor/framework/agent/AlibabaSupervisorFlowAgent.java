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
        // 保存统一的 LLM 网关，后续运行过程中需要通过它接入项目自己的大模型能力。
        this.agentLlmGateway = agentLlmGateway;
        // 创建写作子 Agent，负责先产出中文博客草稿。
        this.writerAgent = buildWriterAgent(chatModel);
        // 创建翻译子 Agent，负责把写作阶段的中文内容翻译成英文。
        this.translatorAgent = buildTranslatorAgent(chatModel);
        // 创建审校子 Agent，负责检查英文译文的语法、拼写和表达是否自然。
        this.reviewerAgent = buildReviewerAgent(chatModel);
        // 创建主路由 Agent，它只负责判断下一跳该调用哪个子 Agent。
        // 它本身不直接生成博客、翻译或审校结果，只做流程调度。
        this.mainRoutingAgent = buildMainRoutingAgent(chatModel);
        // 组装最终对外使用的 SupervisorAgent，把调度器、子 Agent 和编译配置整合起来。
        this.supervisorAgent = SupervisorAgent.builder()
                // 给这个 Supervisor 一个内部名称，便于日志、调试和框架内部识别。
                .name("blog_supervisor_agent")
                // 描述这个 Supervisor 的职责，让框架和模型明确它是一个中心化调度者。
                .description("中心化 Supervisor Agent，负责动态选择 writer、translator、reviewer 并在完成后 FINISH")
                // 配置图执行时的编译参数，例如允许递归回环的最大深度。
                .compileConfig(CompileConfig.builder()
                        // recursionLimit 用来限制最多能在多个 Agent 之间来回调度多少步，防止死循环。
                        // 这里至少给 8 步；如果 maxRounds 更大，就按每轮大约 4 步来放宽上限。
                        .recursionLimit(Math.max(8, maxRounds * 4))
                        // 完成 CompileConfig 构建。
                        .build())
                // 注册所有真正干活的子 Agent，Supervisor 后续会从这里选择下一跳。
                .subAgents(List.of(writerAgent, translatorAgent, reviewerAgent))
                // 指定主 Agent 为路由 Agent，也就是整个流程的大脑。
                .mainAgent(mainRoutingAgent)
                // 完成 SupervisorAgent 构建，得到最终可执行的监督者实例。
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

package com.xbk.agent.framework.planreplan.application.executor;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.planreplan.domain.execution.StepExecutionRecord;
import com.xbk.agent.framework.planreplan.domain.plan.PlanStep;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 手写版执行器
 *
 * 职责：把当前步骤和累加历史拼成上下文，逐步执行计划
 *
 * @author xiexu
 */
public class HandwrittenExecutor {

    private static final String EXECUTOR_SYSTEM_PROMPT = """
            你是一名 Plan-and-Solve 执行器。
            你会收到完整问题、完整计划、历史步骤结果以及当前步骤。
            请只执行当前步骤，输出这一小步的结果，不要跳过步骤直接给最终答案。
            """;

    private static final String EXECUTOR_PROMPT_TEMPLATE = """
            原始问题：
            {question}

            完整计划：
            {plan}

            历史步骤与结果：
            {history}

            当前步骤：{current_step}
            """;

    private final AgentLlmGateway agentLlmGateway;

    /**
     * 创建手写版执行器。
     *
     * @param agentLlmGateway 统一 LLM 网关
     */
    public HandwrittenExecutor(AgentLlmGateway agentLlmGateway) {
        this.agentLlmGateway = agentLlmGateway;
    }

    /**
     * 执行当前步骤。
     *
     * @param question 原始问题
     * @param plan 完整计划
     * @param history 历史步骤结果
     * @param currentStep 当前步骤
     * @param conversationId 会话标识
     * @return 当前步骤结果
     */
    public String execute(String question,
                          List<PlanStep> plan,
                          List<StepExecutionRecord> history,
                          PlanStep currentStep,
                          String conversationId) {
        String prompt = EXECUTOR_PROMPT_TEMPLATE
                .replace("{question}", question)
                .replace("{plan}", formatPlan(plan))
                .replace("{history}", formatHistory(history))
                .replace("{current_step}", formatCurrentStep(currentStep));

        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId("executor-" + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(List.of(
                        buildMessage(conversationId, MessageRole.SYSTEM, EXECUTOR_SYSTEM_PROMPT),
                        buildMessage(conversationId, MessageRole.USER, prompt)))
                .build());
        return extractText(response);
    }

    /**
     * 格式化完整计划。
     *
     * @param plan 完整计划
     * @return 计划文本
     */
    private String formatPlan(List<PlanStep> plan) {
        return plan.stream()
                .map(step -> step.getStepIndex() + ". " + step.getInstruction())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 格式化历史记录。
     *
     * @param history 历史步骤结果
     * @return 历史文本
     */
    private String formatHistory(List<StepExecutionRecord> history) {
        if (history == null || history.isEmpty()) {
            return "暂无历史执行结果";
        }
        return history.stream()
                .map(record -> formatCurrentStep(record.getPlanStep()) + " -> " + record.getStepResult())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 格式化当前步骤。
     *
     * @param currentStep 当前步骤
     * @return 当前步骤文本
     */
    private String formatCurrentStep(PlanStep currentStep) {
        return "步骤" + currentStep.getStepIndex() + "：" + currentStep.getInstruction();
    }

    /**
     * 提取响应文本。
     *
     * @param response LLM 响应
     * @return 响应文本
     */
    private String extractText(LlmResponse response) {
        if (response.getOutputMessage() != null && response.getOutputMessage().getContent() != null) {
            return response.getOutputMessage().getContent();
        }
        return response.getRawText();
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

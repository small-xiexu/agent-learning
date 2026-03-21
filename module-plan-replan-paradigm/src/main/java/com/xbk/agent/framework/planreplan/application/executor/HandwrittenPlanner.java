package com.xbk.agent.framework.planreplan.application.executor;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.planreplan.domain.plan.PlanStep;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 手写版规划器
 *
 * 职责：调用统一 LLM 网关，把原始问题拆成可执行的步骤列表
 *
 * @author xiexu
 */
public class HandwrittenPlanner {

    private static final Pattern PLAN_LINE_PATTERN = Pattern.compile("^(\\d+)\\.\\s*(.+)$");
    private static final String PLANNER_SYSTEM_PROMPT = """
            你是一名 Plan-and-Solve 规划器。
            请先理解用户问题，再拆成清晰、可按顺序执行的步骤。
            必须严格使用规范的编号列表输出，不要输出多余解释。
            输出格式必须是：
            1. 第一步
            2. 第二步
            3. 第三步
            """;

    private final AgentLlmGateway agentLlmGateway;

    /**
     * 创建手写版规划器。
     *
     * @param agentLlmGateway 统一 LLM 网关
     */
    public HandwrittenPlanner(AgentLlmGateway agentLlmGateway) {
        this.agentLlmGateway = agentLlmGateway;
    }

    /**
     * 生成计划步骤。
     *
     * @param question 原始问题
     * @param conversationId 会话标识
     * @return 计划步骤列表
     */
    public List<PlanStep> plan(String question, String conversationId) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId("planner-" + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(List.of(
                        buildMessage(conversationId, MessageRole.SYSTEM, PLANNER_SYSTEM_PROMPT),
                        buildMessage(conversationId, MessageRole.USER, question)))
                .build());
        String planText = extractText(response);
        return parsePlanSteps(planText);
    }

    /**
     * 解析模型返回的编号计划。
     *
     * @param planText 计划文本
     * @return 计划步骤列表
     */
    private List<PlanStep> parsePlanSteps(String planText) {
        List<PlanStep> steps = new ArrayList<PlanStep>();
        for (String line : planText.split("\\R")) {
            String trimmedLine = line.trim();
            Matcher matcher = PLAN_LINE_PATTERN.matcher(trimmedLine);
            if (!matcher.matches()) {
                continue;
            }
            int stepIndex = Integer.parseInt(matcher.group(1));
            String instruction = matcher.group(2).trim();
            steps.add(new PlanStep(stepIndex, instruction));
        }
        if (steps.isEmpty()) {
            throw new IllegalStateException("Planner did not return any valid plan steps: " + planText);
        }
        return steps;
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

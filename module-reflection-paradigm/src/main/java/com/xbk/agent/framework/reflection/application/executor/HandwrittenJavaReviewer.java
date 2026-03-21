package com.xbk.agent.framework.reflection.application.executor;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 Java 评审者
 *
 * 职责：从复杂度与可维护性角度审视当前代码，并给出是否继续优化的建议
 *
 * @author xiexu
 */
public class HandwrittenJavaReviewer {

    private static final String REVIEWER_SYSTEM_PROMPT = """
            你是一名严格的 Java 代码评审者。
            请重点评审当前代码的时间复杂度、主要性能瓶颈和可优化点。
            如果已经无需继续优化，请明确输出“无需改进”。
            """;

    private static final String REFLECT_PROMPT_TEMPLATE = """
            原始任务：
            {task}

            当前代码：
            {current_code}

            请从时间复杂度角度给出评审意见。
            如果代码已经足够好，请明确输出“无需改进”。
            """;

    private final AgentLlmGateway agentLlmGateway;

    /**
     * 创建手写版 Java 评审者。
     *
     * @param agentLlmGateway 统一 LLM 网关
     */
    public HandwrittenJavaReviewer(AgentLlmGateway agentLlmGateway) {
        this.agentLlmGateway = agentLlmGateway;
    }

    /**
     * 评审当前代码。
     *
     * @param task 原始任务
     * @param currentCode 当前代码
     * @param conversationId 会话标识
     * @return 评审意见
     */
    public String review(String task, String currentCode, String conversationId) {
        String prompt = REFLECT_PROMPT_TEMPLATE
                .replace("{task}", defaultText(task))
                .replace("{current_code}", defaultText(currentCode));
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId("reviewer-" + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(List.of(
                        buildMessage(conversationId, MessageRole.SYSTEM, REVIEWER_SYSTEM_PROMPT),
                        buildMessage(conversationId, MessageRole.USER, prompt)))
                .build());
        return extractText(response);
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

    /**
     * 返回非空文本。
     *
     * @param value 原始值
     * @return 非空文本
     */
    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}

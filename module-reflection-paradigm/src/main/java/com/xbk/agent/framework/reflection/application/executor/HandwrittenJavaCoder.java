package com.xbk.agent.framework.reflection.application.executor;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 Java 代码生成者
 *
 * 职责：基于任务生成初稿，并根据评审意见生成优化版本
 *
 * @author xiexu
 */
public class HandwrittenJavaCoder {

    private static final String CODER_SYSTEM_PROMPT = """
            你是一名 Java 代码生成者。
            你会先给出可运行初稿，再根据评审意见优化代码。
            请只输出代码，不要补充解释。
            """;

    private static final String INITIAL_PROMPT_TEMPLATE = """
            任务描述：
            {task}

            请先写出一版可以工作的 Java 初稿。
            """;

    private static final String REFINE_PROMPT_TEMPLATE = """
            任务描述：
            {task}

            当前代码：
            {current_code}

            评审意见：
            {review_feedback}

            请基于评审意见输出优化后的完整 Java 代码。
            """;

    private final AgentLlmGateway agentLlmGateway;

    /**
     * 创建手写版 Java 代码生成者。
     *
     * @param agentLlmGateway 统一 LLM 网关
     */
    public HandwrittenJavaCoder(AgentLlmGateway agentLlmGateway) {
        this.agentLlmGateway = agentLlmGateway;
    }

    /**
     * 生成初稿代码。
     *
     * @param task 任务描述
     * @param conversationId 会话标识
     * @return 初稿代码
     */
    public String generateInitialCode(String task, String conversationId) {
        String prompt = INITIAL_PROMPT_TEMPLATE
                .replace("{task}", defaultText(task));
        return chat(prompt, conversationId, "coder-initial-");
    }

    /**
     * 基于评审意见生成优化代码。
     *
     * @param task 任务描述
     * @param currentCode 当前代码
     * @param reviewFeedback 评审意见
     * @param conversationId 会话标识
     * @return 优化后的代码
     */
    public String refineCode(String task,
                             String currentCode,
                             String reviewFeedback,
                             String conversationId) {
        String prompt = REFINE_PROMPT_TEMPLATE
                .replace("{task}", defaultText(task))
                .replace("{current_code}", defaultText(currentCode))
                .replace("{review_feedback}", defaultText(reviewFeedback));
        return chat(prompt, conversationId, "coder-refine-");
    }

    /**
     * 调用统一网关并提取文本结果。
     *
     * @param prompt 用户提示词
     * @param conversationId 会话标识
     * @param requestPrefix 请求前缀
     * @return 文本结果
     */
    private String chat(String prompt, String conversationId, String requestPrefix) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(requestPrefix + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(List.of(
                        buildMessage(conversationId, MessageRole.SYSTEM, CODER_SYSTEM_PROMPT),
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
     * 返回非空文本，避免空占位符导致提示词难以阅读。
     *
     * @param value 原始值
     * @return 非空文本
     */
    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}

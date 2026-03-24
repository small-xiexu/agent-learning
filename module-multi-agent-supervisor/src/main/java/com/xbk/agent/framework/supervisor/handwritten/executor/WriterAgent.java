package com.xbk.agent.framework.supervisor.handwritten.executor;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.supervisor.handwritten.prompt.HandwrittenSupervisorPromptTemplates;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 WriterAgent
 *
 * 职责：基于统一 AgentLlmGateway 撰写中文博客初稿。
 * 在 Supervisor 体系里，它只负责专业执行，不负责决定下一跳。
 *
 * @author xiexu
 */
public final class WriterAgent {

    private static final String REQUEST_PREFIX = "supervisor-writer-";

    private final AgentLlmGateway agentLlmGateway;

    /**
     * 创建 WriterAgent。
     *
     * @param agentLlmGateway 统一网关
     */
    public WriterAgent(AgentLlmGateway agentLlmGateway) {
        this.agentLlmGateway = agentLlmGateway;
    }

    /**
     * 生成中文博客初稿。
     *
     * @param task 原始任务
     * @param taskInstruction 监督者任务指令
     * @param conversationId 会话标识
     * @return 中文博客初稿
     */
    public String write(String task, String taskInstruction, String conversationId) {
        return chat(
                conversationId,
                HandwrittenSupervisorPromptTemplates.writerSystemPrompt(),
                HandwrittenSupervisorPromptTemplates.buildWriterUserPrompt(task, taskInstruction));
    }

    /**
     * 调用统一网关并返回文本。
     *
     * @param conversationId 会话标识
     * @param systemPrompt 系统提示
     * @param userPrompt 用户提示
     * @return 文本响应
     */
    private String chat(String conversationId, String systemPrompt, String userPrompt) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(REQUEST_PREFIX + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(List.of(
                        buildMessage(conversationId, MessageRole.SYSTEM, systemPrompt),
                        buildMessage(conversationId, MessageRole.USER, userPrompt)))
                .build());
        // 统一网关在不同实现下可能返回结构化 outputMessage 或 rawText，这里统一兜底。
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

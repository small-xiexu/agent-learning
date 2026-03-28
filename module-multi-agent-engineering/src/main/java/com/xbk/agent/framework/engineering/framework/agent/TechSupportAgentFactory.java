package com.xbk.agent.framework.engineering.framework.agent;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.engineering.support.EngineeringPromptTemplates;

import java.util.List;
import java.util.UUID;

/**
 * 技术专家 Agent 工厂。
 *
 * 职责：封装技术专家的提示词和调用逻辑，对外提供简单的 handle(request) 方法。
 * Provider 侧的 A2A 控制器通过这个工厂处理传入的用户请求，并把结果返回给 Consumer。
 *
 * <p>为什么需要一个工厂类而不是直接用 ReactAgent？
 * 因为本模块要保持统一网关约束：所有模型调用都必须通过 AgentLlmGateway，
 * 不直接使用 ChatModel 或 ChatClient。工厂类是这一约束的落实点。
 *
 * @author xiexu
 */
public class TechSupportAgentFactory {

    private final AgentLlmGateway agentLlmGateway;

    /**
     * 创建技术专家工厂。
     *
     * @param agentLlmGateway 统一网关
     */
    public TechSupportAgentFactory(AgentLlmGateway agentLlmGateway) {
        this.agentLlmGateway = agentLlmGateway;
    }

    /**
     * 处理技术问题请求并返回专家答复。
     *
     * @param contextId 会话/任务上下文标识（来自 A2A 请求）
     * @param userRequest 用户问题文本
     * @return 技术专家的答复文本
     */
    public String handle(String contextId, String userRequest) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId("tech-provider-" + UUID.randomUUID())
                .conversationId(contextId)
                .messages(List.of(
                        systemMessage(contextId, EngineeringPromptTemplates.techSupportSystemPrompt()),
                        userMessage(contextId, userRequest)))
                .build());
        return extractText(response);
    }

    /**
     * 构造系统消息。
     *
     * @param conversationId 会话标识
     * @param content 文本内容
     * @return 框架消息
     */
    private Message systemMessage(String conversationId, String content) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.SYSTEM)
                .content(content)
                .build();
    }

    /**
     * 构造用户消息。
     *
     * @param conversationId 会话标识
     * @param content 文本内容
     * @return 框架消息
     */
    private Message userMessage(String conversationId, String content) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(content)
                .build();
    }

    /**
     * 提取响应文本。
     *
     * @param response LLM 响应
     * @return 文本内容
     */
    private String extractText(LlmResponse response) {
        if (response.getOutputMessage() != null && response.getOutputMessage().getContent() != null) {
            return response.getOutputMessage().getContent();
        }
        return response.getRawText() != null ? response.getRawText() : "";
    }
}

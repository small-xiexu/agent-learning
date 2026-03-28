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
 * 销售顾问 Agent 工厂。
 *
 * 职责：封装销售顾问的提示词和调用逻辑，与 TechSupportAgentFactory 保持结构对称。
 * 职责边界：只负责调用网关生成销售类答复，不处理路由或其他业务逻辑。
 *
 * @author xiexu
 */
public class SalesAgentFactory {

    private final AgentLlmGateway agentLlmGateway;

    /**
     * 创建销售顾问工厂。
     *
     * @param agentLlmGateway 统一网关
     */
    public SalesAgentFactory(AgentLlmGateway agentLlmGateway) {
        this.agentLlmGateway = agentLlmGateway;
    }

    /**
     * 处理销售咨询请求并返回顾问答复。
     *
     * @param contextId 会话/任务上下文标识
     * @param userRequest 用户问题文本
     * @return 销售顾问的答复文本
     */
    public String handle(String contextId, String userRequest) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId("sales-provider-" + UUID.randomUUID())
                .conversationId(contextId)
                .messages(List.of(
                        systemMessage(contextId, EngineeringPromptTemplates.salesConsultingSystemPrompt()),
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

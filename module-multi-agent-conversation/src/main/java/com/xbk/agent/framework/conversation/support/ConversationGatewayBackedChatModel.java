package com.xbk.agent.framework.conversation.support;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于统一网关的 ChatModel 适配器
 *
 * 职责：把 Spring AI Prompt 转成统一 LLM 请求，再经 AgentLlmGateway 返回 ChatResponse
 *
 * @author xiexu
 */
public class ConversationGatewayBackedChatModel implements ChatModel {

    /**
     * 统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 创建 ChatModel 适配器。
     *
     * @param agentLlmGateway 统一网关
     */
    public ConversationGatewayBackedChatModel(AgentLlmGateway agentLlmGateway) {
        this.agentLlmGateway = agentLlmGateway;
    }

    /**
     * 执行同步对话。
     *
     * @param prompt Spring AI Prompt
     * @return 聊天响应
     */
    @Override
    public ChatResponse call(Prompt prompt) {
        String conversationId = "conversation-chat-model-" + UUID.randomUUID();
        List<Message> messages = new ArrayList<Message>();
        for (org.springframework.ai.chat.messages.Message instruction : prompt.getInstructions()) {
            messages.add(toFrameworkMessage(conversationId, instruction));
        }
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId("conversation-chat-model-" + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(messages)
                .build());
        String text = response.getOutputMessage() != null && response.getOutputMessage().getContent() != null
                ? response.getOutputMessage().getContent()
                : response.getRawText();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text == null ? "" : text))));
    }

    /**
     * 转换为框架统一消息。
     *
     * @param conversationId 会话标识
     * @param message Spring AI 消息
     * @return 框架统一消息
     */
    private Message toFrameworkMessage(String conversationId, org.springframework.ai.chat.messages.Message message) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(resolveRole(message))
                .content(resolveContent(message))
                .build();
    }

    /**
     * 解析消息角色。
     *
     * @param message Spring AI 消息
     * @return 统一消息角色
     */
    private MessageRole resolveRole(org.springframework.ai.chat.messages.Message message) {
        if (message instanceof SystemMessage) {
            return MessageRole.SYSTEM;
        }
        if (message instanceof UserMessage) {
            return MessageRole.USER;
        }
        if (message instanceof AssistantMessage) {
            return MessageRole.ASSISTANT;
        }
        if (message instanceof ToolResponseMessage) {
            return MessageRole.TOOL;
        }
        return MessageRole.USER;
    }

    /**
     * 解析消息内容。
     *
     * @param message Spring AI 消息
     * @return 文本内容
     */
    private String resolveContent(org.springframework.ai.chat.messages.Message message) {
        if (message instanceof ToolResponseMessage) {
            return String.valueOf(((ToolResponseMessage) message).getResponses());
        }
        if (message instanceof AbstractMessage) {
            return ((AbstractMessage) message).getText();
        }
        return String.valueOf(message);
    }
}

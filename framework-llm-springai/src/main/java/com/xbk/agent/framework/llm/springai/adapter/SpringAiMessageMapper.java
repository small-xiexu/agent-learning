package com.xbk.agent.framework.llm.springai.adapter;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.memory.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spring AI 消息映射器
 *
 * 职责：完成框架消息与 Spring AI 消息之间的转换
 *
 * @author xiexu
 */
public class SpringAiMessageMapper {

    /**
     * 转换为 Spring AI 消息
     *
     * @param message 框架消息
     * @return Spring AI 消息
     */
    public org.springframework.ai.chat.messages.Message toSpringAiMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        if (message.getRole() == MessageRole.USER) {
            return new UserMessage(message.getContent());
        }
        if (message.getRole() == MessageRole.SYSTEM) {
            return new SystemMessage(message.getContent());
        }
        if (message.getRole() == MessageRole.ASSISTANT) {
            return new AssistantMessage(message.getContent());
        }
        if (message.getRole() == MessageRole.TOOL) {
            ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse(
                    message.getToolCallId(),
                    message.getName(),
                    message.getContent());
            return ToolResponseMessage.builder()
                    .responses(Collections.singletonList(response))
                    .build();
        }
        throw new IllegalArgumentException("unsupported message role: " + message.getRole());
    }

    /**
     * 批量转换为 Spring AI 消息
     *
     * @param messages 框架消息列表
     * @return Spring AI 消息列表
     */
    public List<org.springframework.ai.chat.messages.Message> toSpringAiMessages(List<Message> messages) {
        List<org.springframework.ai.chat.messages.Message> mappedMessages = new ArrayList<org.springframework.ai.chat.messages.Message>();
        for (Message message : messages) {
            mappedMessages.add(toSpringAiMessage(message));
        }
        return List.copyOf(mappedMessages);
    }
}

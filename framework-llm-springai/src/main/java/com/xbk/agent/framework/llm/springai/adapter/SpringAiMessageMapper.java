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
import java.util.Map;

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
            return toAssistantMessage(message);
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
     * 将框架助手消息转换为 Spring AI 助手消息，并尽量保留 tool_calls 元数据。
     *
     * @param message 框架消息
     * @return Spring AI 助手消息
     */
    @SuppressWarnings("unchecked")
    private AssistantMessage toAssistantMessage(Message message) {
        Object rawToolCalls = message.getMetadata().get(SpringAiResponseMapper.ASSISTANT_TOOL_CALLS_METADATA_KEY);
        if (!(rawToolCalls instanceof List<?> toolCallsMetadata) || toolCallsMetadata.isEmpty()) {
            return new AssistantMessage(message.getContent());
        }
        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<AssistantMessage.ToolCall>();
        for (Object item : toolCallsMetadata) {
            if (!(item instanceof Map<?, ?> toolCallMetadata)) {
                continue;
            }
            toolCalls.add(new AssistantMessage.ToolCall(
                    valueAsString(toolCallMetadata.get("id")),
                    valueAsString(toolCallMetadata.get("type")),
                    valueAsString(toolCallMetadata.get("name")),
                    valueAsString(toolCallMetadata.get("arguments"))));
        }
        if (toolCalls.isEmpty()) {
            return new AssistantMessage(message.getContent());
        }
        return AssistantMessage.builder()
                .content(message.getContent())
                .toolCalls(List.copyOf(toolCalls))
                .build();
    }

    /**
     * 将任意对象安全转换为字符串。
     *
     * @param value 待转换值
     * @return 字符串结果
     */
    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
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

package com.xbk.agent.framework.llm.springai.adapter;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.memory.Message;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SpringAiMessageMapper 测试
 *
 * 职责：验证框架消息与 Spring AI 消息之间的边界映射
 *
 * @author xiexu
 */
class SpringAiMessageMapperTest {

    /**
     * 验证用户消息会映射为 Spring AI 用户消息
     */
    @Test
    void shouldMapUserMessageToSpringAiUserMessage() {
        SpringAiMessageMapper mapper = new SpringAiMessageMapper();
        Message message = message(MessageRole.USER, "hello");

        org.springframework.ai.chat.messages.Message mapped = mapper.toSpringAiMessage(message);

        UserMessage userMessage = assertInstanceOf(UserMessage.class, mapped);
        assertEquals("hello", userMessage.getText());
    }

    /**
     * 验证系统消息会映射为 Spring AI 系统消息
     */
    @Test
    void shouldMapSystemMessageToSpringAiSystemMessage() {
        SpringAiMessageMapper mapper = new SpringAiMessageMapper();
        Message message = message(MessageRole.SYSTEM, "system");

        org.springframework.ai.chat.messages.Message mapped = mapper.toSpringAiMessage(message);

        SystemMessage systemMessage = assertInstanceOf(SystemMessage.class, mapped);
        assertEquals("system", systemMessage.getText());
    }

    /**
     * 验证助手消息会映射为 Spring AI 助手消息
     */
    @Test
    void shouldMapAssistantMessageToSpringAiAssistantMessage() {
        SpringAiMessageMapper mapper = new SpringAiMessageMapper();
        Message message = message(MessageRole.ASSISTANT, "done");

        org.springframework.ai.chat.messages.Message mapped = mapper.toSpringAiMessage(message);

        AssistantMessage assistantMessage = assertInstanceOf(AssistantMessage.class, mapped);
        assertEquals("done", assistantMessage.getText());
    }

    /**
     * 验证暂不支持的角色会快速失败
     */
    @Test
    void shouldMapToolMessageToSpringAiToolResponseMessage() {
        SpringAiMessageMapper mapper = new SpringAiMessageMapper();
        Message message = Message.builder()
                .messageId("tool-msg-1")
                .conversationId("conv-1")
                .role(MessageRole.TOOL)
                .content("晴朗，微风，气温25度")
                .name("WeatherTool")
                .toolCallId("call-1")
                .build();

        org.springframework.ai.chat.messages.Message mapped = mapper.toSpringAiMessage(message);

        ToolResponseMessage toolResponseMessage = assertInstanceOf(ToolResponseMessage.class, mapped);
        assertEquals(1, toolResponseMessage.getResponses().size());
        assertEquals("call-1", toolResponseMessage.getResponses().getFirst().id());
        assertEquals("WeatherTool", toolResponseMessage.getResponses().getFirst().name());
        assertEquals("晴朗，微风，气温25度", toolResponseMessage.getResponses().getFirst().responseData());
    }

    /**
     * 创建测试消息
     *
     * @param role 消息角色
     * @param content 消息内容
     * @return 消息对象
     */
    private Message message(MessageRole role, String content) {
        return Message.builder()
                .messageId("msg-" + role.name())
                .conversationId("conv-1")
                .role(role)
                .content(content)
                .build();
    }
}

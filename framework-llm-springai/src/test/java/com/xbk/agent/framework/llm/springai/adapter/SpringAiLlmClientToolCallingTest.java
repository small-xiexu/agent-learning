package com.xbk.agent.framework.llm.springai.adapter;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.option.ModelOptions;
import com.xbk.agent.framework.core.llm.option.ToolCallingOptions;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * SpringAiLlmClient 工具调用测试
 *
 * 职责：验证客户端会把工具定义带到 Spring AI Prompt 中，并把工具调用结果映射回框架响应
 *
 * @author xiexu
 */
class SpringAiLlmClientToolCallingTest {

    /**
     * 验证同步客户端会透传工具定义并映射模型返回的工具调用。
     */
    @Test
    void shouldSendToolsAndMapToolCallsWhenChatModelRequestsTool() {
        RecordingChatModel chatModel = new RecordingChatModel();
        SpringAiLlmClient client = new SpringAiLlmClient(chatModel);

        LlmResponse response = client.chat(request());

        ToolCallingChatOptions chatOptions =
                assertInstanceOf(ToolCallingChatOptions.class, chatModel.lastPrompt.get().getOptions());
        assertEquals(1, chatOptions.getToolCallbacks().size());
        assertEquals("WeatherTool", chatOptions.getToolCallbacks().getFirst().getToolDefinition().name());
        assertEquals("WeatherTool", response.getToolCalls().getFirst().getToolName());
        assertEquals(EnumSet.of(LlmCapability.SYNC_CHAT, LlmCapability.TOOL_CALLING), client.capabilities());
    }

    /**
     * 构造测试请求。
     *
     * @return 测试请求
     */
    private LlmRequest request() {
        Message message = Message.builder()
                .messageId("msg-1")
                .conversationId("conv-1")
                .role(MessageRole.USER)
                .content("今天北京天气如何？")
                .build();
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("WeatherTool")
                .description("查询天气")
                .inputSchema(Map.of("type", "object", "properties", Map.of("city", Map.of("type", "string"))))
                .build();
        return LlmRequest.builder()
                .requestId("req-1")
                .conversationId("conv-1")
                .messages(List.of(message))
                .availableTools(List.of(toolDefinition))
                .modelOptions(ModelOptions.builder().modelName("gpt-4o").build())
                .toolCallingOptions(ToolCallingOptions.builder().enabled(true).build())
                .build();
    }

    /**
     * 记录 Prompt 的假 ChatModel。
     *
     * 职责：验证客户端传给 Spring AI 的 Prompt 里包含工具调用配置
     *
     * @author xiexu
     */
    private static final class RecordingChatModel implements ChatModel {

        private final AtomicReference<Prompt> lastPrompt = new AtomicReference<Prompt>();

        /**
         * 执行同步对话。
         *
         * @param prompt 提示词
         * @return 模型响应
         */
        @Override
        public ChatResponse call(Prompt prompt) {
            lastPrompt.set(prompt);
            AssistantMessage assistantMessage = AssistantMessage.builder()
                    .content("Thought: 我需要查询天气")
                    .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "WeatherTool",
                            "{\"city\":\"北京\"}")))
                    .build();
            return ChatResponse.builder()
                    .generations(List.of(new Generation(assistantMessage)))
                    .build();
        }
    }
}

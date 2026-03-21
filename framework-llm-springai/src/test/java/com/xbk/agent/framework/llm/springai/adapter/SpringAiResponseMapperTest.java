package com.xbk.agent.framework.llm.springai.adapter;

import com.xbk.agent.framework.core.common.enums.LlmFinishReason;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.option.ModelOptions;
import com.xbk.agent.framework.core.llm.option.ToolCallingOptions;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SpringAiResponseMapper 测试
 *
 * 职责：验证 Spring AI 响应会正确映射为框架响应对象
 *
 * @author xiexu
 */
class SpringAiResponseMapperTest {

    /**
     * 验证工具调用响应会映射为框架 ToolCall。
     */
    @Test
    void shouldMapAssistantToolCallsBackToFrameworkResponse() {
        SpringAiResponseMapper mapper = new SpringAiResponseMapper();
        LlmRequest request = request();
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("Thought: 我需要先查天气")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "WeatherTool",
                        "{\"city\":\"北京\"}")))
                .build();
        ChatResponse response = ChatResponse.builder()
                .generations(List.of(new Generation(assistantMessage)))
                .metadata(ChatResponseMetadata.builder()
                        .usage(new DefaultUsage(5, 7, 12))
                        .build())
                .build();

        LlmResponse mapped = mapper.toLlmResponse(request, response);

        assertEquals(LlmFinishReason.TOOL_CALL, mapped.getFinishReason());
        assertEquals("Thought: 我需要先查天气", mapped.getRawText());
        assertEquals("WeatherTool", mapped.getToolCalls().getFirst().getToolName());
        assertEquals("北京", mapped.getToolCalls().getFirst().getArguments().get("city"));
        assertEquals(5, mapped.getUsage().getInputTokens());
        assertEquals(7, mapped.getUsage().getOutputTokens());
        assertEquals(12, mapped.getUsage().getTotalTokens());
    }

    /**
     * 验证工具调用响应在 assistant 文本为空时，会把输出消息内容规范为空字符串。
     */
    @Test
    void shouldNormalizeNullAssistantTextWhenToolCallResponseContainsNoText() {
        SpringAiResponseMapper mapper = new SpringAiResponseMapper();
        LlmRequest request = request();
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content(null)
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "WeatherTool",
                        "{\"city\":\"北京\"}")))
                .build();
        ChatResponse response = ChatResponse.builder()
                .generations(List.of(new Generation(assistantMessage)))
                .build();

        LlmResponse mapped = mapper.toLlmResponse(request, response);

        assertEquals(LlmFinishReason.TOOL_CALL, mapped.getFinishReason());
        assertEquals("", mapped.getOutputMessage().getContent());
        assertEquals("WeatherTool", mapped.getToolCalls().getFirst().getToolName());
        assertTrue(mapped.getOutputMessage().getMetadata()
                .containsKey(SpringAiResponseMapper.ASSISTANT_TOOL_CALLS_METADATA_KEY));
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
                .inputSchema(Map.of("city", "string"))
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
}

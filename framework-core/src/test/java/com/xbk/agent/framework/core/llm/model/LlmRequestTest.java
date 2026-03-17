package com.xbk.agent.framework.core.llm.model;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.common.enums.ToolChoiceMode;
import com.xbk.agent.framework.core.llm.option.ModelOptions;
import com.xbk.agent.framework.core.llm.option.ToolCallingOptions;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * LlmRequest 测试
 *
 * 职责：验证统一 LLM 请求 DTO 的只读契约
 *
 * @author xiexu
 */
class LlmRequestTest {

    /**
     * 验证请求对象会保留消息、工具与能力选项
     */
    @Test
    void shouldBuildReadOnlyLlmRequest() {
        Message message = Message.builder()
                .messageId("msg-1")
                .conversationId("conv-1")
                .role(MessageRole.USER)
                .content("hello")
                .build();
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("echo")
                .description("Echo tool")
                .build();
        LlmRequest request = LlmRequest.builder()
                .requestId("req-1")
                .conversationId("conv-1")
                .messages(List.of(message))
                .availableTools(List.of(toolDefinition))
                .modelOptions(ModelOptions.builder().modelName("qwen-max").temperature(0.2D).build())
                .toolCallingOptions(ToolCallingOptions.builder().enabled(true).toolChoiceMode(ToolChoiceMode.AUTO).build())
                .metadata(Map.of("traceId", "trace-1"))
                .build();

        assertEquals("req-1", request.getRequestId());
        assertEquals(1, request.getMessages().size());
        assertEquals(1, request.getAvailableTools().size());
        assertEquals("qwen-max", request.getModelOptions().getModelName());
        assertEquals(ToolChoiceMode.AUTO, request.getToolCallingOptions().getToolChoiceMode());
        assertEquals("trace-1", request.getMetadata().get("traceId"));
        assertThrows(UnsupportedOperationException.class, () -> request.getMessages().add(message));
    }
}

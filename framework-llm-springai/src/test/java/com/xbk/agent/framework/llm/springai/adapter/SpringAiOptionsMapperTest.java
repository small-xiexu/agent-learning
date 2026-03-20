package com.xbk.agent.framework.llm.springai.adapter;

import com.xbk.agent.framework.core.common.enums.ToolChoiceMode;
import com.xbk.agent.framework.core.llm.option.ModelOptions;
import com.xbk.agent.framework.core.llm.option.ToolCallingOptions;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * SpringAiOptionsMapper 测试
 *
 * 职责：验证普通模型选项和工具调用选项能正确映射到 Spring AI ChatOptions
 *
 * @author xiexu
 */
class SpringAiOptionsMapperTest {

    /**
     * 验证带工具定义的请求会构造 ToolCallingChatOptions。
     */
    @Test
    void shouldBuildToolCallingOptionsWhenRequestContainsTools() {
        SpringAiOptionsMapper mapper = new SpringAiOptionsMapper(new SpringAiToolMapper());
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("WeatherTool")
                .description("查询指定城市天气")
                .inputSchema(Map.of("city", "string"))
                .build();
        ModelOptions modelOptions = ModelOptions.builder()
                .modelName("gpt-4o")
                .temperature(0.2d)
                .build();
        ToolCallingOptions toolCallingOptions = ToolCallingOptions.builder()
                .enabled(true)
                .toolChoiceMode(ToolChoiceMode.AUTO)
                .includeToolResultsInContext(true)
                .build();

        ChatOptions chatOptions = mapper.toChatOptions(modelOptions, List.of(toolDefinition), toolCallingOptions);

        ToolCallingChatOptions toolOptions = assertInstanceOf(ToolCallingChatOptions.class, chatOptions);
        assertEquals("gpt-4o", toolOptions.getModel());
        assertFalse(toolOptions.getInternalToolExecutionEnabled());
        assertEquals(1, toolOptions.getToolCallbacks().size());
        assertEquals("WeatherTool", toolOptions.getToolCallbacks().getFirst().getToolDefinition().name());
    }
}

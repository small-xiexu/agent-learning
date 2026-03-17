package com.xbk.agent.framework.react;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.HelloAgentsLLM;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.core.tool.Tool;
import com.xbk.agent.framework.core.tool.ToolContext;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import com.xbk.agent.framework.core.tool.ToolRegistry;
import com.xbk.agent.framework.core.tool.ToolRequest;
import com.xbk.agent.framework.core.tool.ToolResult;
import com.xbk.agent.framework.core.tool.support.DefaultToolRegistry;
import com.xbk.agent.framework.react.application.executor.ReActAgent;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ReActAgent 工具调用配置测试
 *
 * 职责：验证手写版 ReActAgent 构建真实模型请求时会显式开启工具调用能力
 *
 * @author xiexu
 */
class ReActAgentToolCallingTest {

    /**
     * 验证请求里会显式带上工具调用开关和工具定义。
     */
    @Test
    void shouldEnableToolCallingWhenBuildingLlmRequest() {
        RecordingHelloAgentsLLM helloAgentsLLM = new RecordingHelloAgentsLLM();
        ReActAgent reactAgent = new ReActAgent(helloAgentsLLM, createToolRegistry(), 1);

        reactAgent.run("今天北京天气如何？");

        LlmRequest captured = helloAgentsLLM.lastRequest.get();
        assertNotNull(captured);
        assertTrue(captured.getToolCallingOptions().isEnabled());
        assertFalse(captured.getAvailableTools().isEmpty());
    }

    /**
     * 创建测试工具注册中心。
     *
     * @return 工具注册中心
     */
    private ToolRegistry createToolRegistry() {
        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new WeatherTool());
        return toolRegistry;
    }

    /**
     * 记录请求的假 LLM。
     *
     * 职责：只返回最终答案，用于检查 ReActAgent 构建出的首轮请求
     *
     * @author xiexu
     */
    private static final class RecordingHelloAgentsLLM implements HelloAgentsLLM {

        private final AtomicReference<LlmRequest> lastRequest = new AtomicReference<LlmRequest>();

        /**
         * 执行同步对话。
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            lastRequest.set(request);
            Message outputMessage = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .conversationId(request.getConversationId())
                    .role(MessageRole.ASSISTANT)
                    .content("Final Answer: 北京今天天气不错。")
                    .build();
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId(UUID.randomUUID().toString())
                    .outputMessage(outputMessage)
                    .rawText(outputMessage.getContent())
                    .build();
        }

        /**
         * 执行流式对话。
         *
         * @param request LLM 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("recording llm does not support streaming");
        }

        /**
         * 执行结构化输出对话。
         *
         * @param request LLM 请求
         * @param spec 结构化输出定义
         * @param <T> 输出类型
         * @return 结构化输出响应
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("recording llm does not support structured output");
        }

        /**
         * 返回支持能力集合。
         *
         * @return 能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return EnumSet.of(LlmCapability.SYNC_CHAT, LlmCapability.TOOL_CALLING);
        }
    }

    /**
     * 天气查询工具。
     *
     * 职责：提供一个最小工具定义，验证请求会把工具带给模型
     *
     * @author xiexu
     */
    private static final class WeatherTool implements Tool {

        /**
         * 返回工具定义。
         *
         * @return 工具定义
         */
        @Override
        public ToolDefinition definition() {
            return ToolDefinition.builder()
                    .name("WeatherTool")
                    .description("查询指定城市天气")
                    .build();
        }

        /**
         * 执行工具请求。
         *
         * @param request 工具请求
         * @param context 工具上下文
         * @return 工具执行结果
         */
        @Override
        public ToolResult execute(ToolRequest request, ToolContext context) {
            return ToolResult.builder()
                    .success(true)
                    .content("晴朗，微风，气温25度")
                    .build();
        }
    }
}

package com.xbk.agent.framework.core.tool;

import com.xbk.agent.framework.core.memory.MemorySession;
import com.xbk.agent.framework.core.memory.support.InMemoryMemory;
import com.xbk.agent.framework.core.tool.support.DefaultToolRegistry;
import com.xbk.agent.framework.core.common.exception.ToolNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DefaultToolRegistry 测试
 *
 * 职责：验证工具注册中心的最小协议行为
 *
 * @author xiexu
 */
class DefaultToolRegistryTest {

    /**
     * 验证可按名称注册并查找工具
     */
    @Test
    void shouldRegisterAndGetToolByName() {
        DefaultToolRegistry registry = new DefaultToolRegistry();
        Tool tool = new EchoTool();

        registry.register(tool);

        assertSame(tool, registry.get("echo"));
        assertEquals(1, registry.definitions().size());
        assertEquals("echo", registry.definitions().get(0).getName());
    }

    /**
     * 验证重复注册工具会被拒绝
     */
    @Test
    void shouldRejectDuplicateToolRegistration() {
        DefaultToolRegistry registry = new DefaultToolRegistry();
        registry.register(new EchoTool());

        assertThrows(IllegalArgumentException.class, () -> registry.register(new EchoTool()));
    }

    /**
     * 验证注册中心会委派执行工具
     */
    @Test
    void shouldDelegateToolExecution() {
        DefaultToolRegistry registry = new DefaultToolRegistry();
        registry.register(new EchoTool());

        MemorySession session = new InMemoryMemory().openSession("conv-1");
        ToolContext context = ToolContext.builder()
                .conversationId("conv-1")
                .agentId("agent-1")
                .turnId("turn-1")
                .memorySession(session)
                .build();
        ToolRequest request = ToolRequest.builder()
                .toolName("echo")
                .invocationId("invoke-1")
                .arguments(Map.of("text", "hello"))
                .build();

        ToolResult result = registry.execute(request, context);

        assertTrue(result.isSuccess());
        assertEquals("hello", result.getContent());
        assertEquals("hello", result.getStructuredData().get("echo"));
    }

    /**
     * 验证执行未知工具会抛出异常
     */
    @Test
    void shouldThrowWhenToolIsMissing() {
        DefaultToolRegistry registry = new DefaultToolRegistry();
        ToolRequest request = ToolRequest.builder()
                .toolName("missing")
                .invocationId("invoke-2")
                .build();
        ToolContext context = ToolContext.builder()
                .conversationId("conv-1")
                .agentId("agent-1")
                .turnId("turn-2")
                .memorySession(new InMemoryMemory().openSession("conv-1"))
                .build();

        assertThrows(ToolNotFoundException.class, () -> registry.execute(request, context));
    }

    /**
     * Echo 工具
     *
     * 职责：返回输入文本，便于验证工具委派行为
     *
     * @author xiexu
     */
    private static final class EchoTool implements Tool {

        /**
         * 返回工具定义
         *
         * @return 工具定义
         */
        @Override
        public ToolDefinition definition() {
            return ToolDefinition.builder()
                    .name("echo")
                    .description("Echo tool")
                    .build();
        }

        /**
         * 执行工具请求
         *
         * @param request 工具请求
         * @param context 工具上下文
         * @return 工具结果
         */
        @Override
        public ToolResult execute(ToolRequest request, ToolContext context) {
            String text = String.valueOf(request.getArguments().get("text"));
            return ToolResult.builder()
                    .success(true)
                    .content(text)
                    .structuredData(Map.of("echo", text))
                    .build();
        }
    }
}

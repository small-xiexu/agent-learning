package com.xbk.agent.framework.graphflow;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.graphflow.common.state.GraphState;
import com.xbk.agent.framework.graphflow.common.state.StepStatus;
import com.xbk.agent.framework.graphflow.common.tool.MockSearchTool;
import com.xbk.agent.framework.graphflow.framework.AlibabaGraphFlowAgent;
import com.xbk.agent.framework.graphflow.handwritten.HandwrittenGraphFlow;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 三步问答助手图流对照测试
 *
 * 职责：用桩对象验证手写版状态机与框架版图流的双路径行为：
 * - 正常路径：UnderstandNode → SearchNode(成功) → AnswerNode
 * - 降级路径：UnderstandNode → SearchNode(失败) → FallbackNode
 *
 * @author xiexu
 */
class GraphFlowDemoTest {

    private static final String USER_QUERY = "Spring AI 和 LangGraph 在多智能体编排上有什么区别？";

    // ══════════════════════════════════════════════════════════════════════════
    //  手写版测试
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 验证手写版正常路径：搜索成功时走 AnswerNode，stepStatus=END，finalAnswer 包含搜索摘要。
     */
    @Test
    void handwritten_shouldAnswerWithSearchResults_whenSearchSucceeds() {
        HandwrittenGatewayStub gateway = new HandwrittenGatewayStub(false);
        HandwrittenGraphFlow flow = new HandwrittenGraphFlow(gateway, new MockSearchTool(false));

        GraphState result = flow.run(USER_QUERY);

        assertEquals(StepStatus.END, result.getStepStatus());
        assertNotNull(result.getFinalAnswer());
        assertTrue(result.getFinalAnswer().contains("搜索结果摘要"),
                "正常路径下 finalAnswer 应包含搜索结果摘要，实际：" + result.getFinalAnswer());
        assertNotNull(result.getSearchQuery(), "searchQuery 不应为空");
        assertNotNull(result.getSearchResults(), "searchResults 不应为空");
    }

    /**
     * 验证手写版降级路径：搜索失败时走 FallbackNode，stepStatus=END，finalAnswer 包含降级标记。
     */
    @Test
    void handwritten_shouldAnswerWithFallback_whenSearchFails() {
        HandwrittenGatewayStub gateway = new HandwrittenGatewayStub(true);
        HandwrittenGraphFlow flow = new HandwrittenGraphFlow(gateway, new MockSearchTool(true));

        GraphState result = flow.run(USER_QUERY);

        assertEquals(StepStatus.END, result.getStepStatus());
        assertNotNull(result.getFinalAnswer());
        assertTrue(result.getFinalAnswer().contains("降级回答"),
                "降级路径下 finalAnswer 应包含\"降级回答\"标记，实际：" + result.getFinalAnswer());
        assertNotNull(result.getErrorMessage(), "搜索失败时 errorMessage 不应为空");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  框架版测试
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 验证框架版正常路径：搜索成功时走 AnswerNodeAction，usedFallback=false，finalAnswer 包含搜索摘要。
     */
    @Test
    void framework_shouldAnswerWithSearchResults_whenSearchSucceeds() {
        FrameworkChatModelStub chatModel = new FrameworkChatModelStub(false);
        AlibabaGraphFlowAgent agent = new AlibabaGraphFlowAgent(
                buildGatewayFromChatModel(chatModel), new MockSearchTool(false));

        AlibabaGraphFlowAgent.RunResult result = agent.run(USER_QUERY);

        assertFalse(result.isUsedFallback(), "搜索成功时不应触发降级分支");
        assertNotNull(result.getFinalAnswer());
        assertTrue(result.getFinalAnswer().contains("搜索结果摘要"),
                "正常路径下 finalAnswer 应包含搜索结果摘要，实际：" + result.getFinalAnswer());
        assertNotNull(result.getState().value("search_query").orElse(null), "search_query 不应为空");
        assertTrue(result.getState().value("final_answer").isPresent(), "final_answer 应存在于图状态");
    }

    /**
     * 验证框架版降级路径：搜索失败时走 FallbackNodeAction，usedFallback=true，finalAnswer 包含降级标记。
     */
    @Test
    void framework_shouldAnswerWithFallback_whenSearchFails() {
        FrameworkChatModelStub chatModel = new FrameworkChatModelStub(true);
        AlibabaGraphFlowAgent agent = new AlibabaGraphFlowAgent(
                buildGatewayFromChatModel(chatModel), new MockSearchTool(true));

        AlibabaGraphFlowAgent.RunResult result = agent.run(USER_QUERY);

        assertTrue(result.isUsedFallback(), "搜索失败时应触发降级分支");
        assertNotNull(result.getFinalAnswer());
        assertTrue(result.getFinalAnswer().contains("降级回答"),
                "降级路径下 finalAnswer 应包含\"降级回答\"标记，实际：" + result.getFinalAnswer());
        assertTrue(result.getState().value("search_failed").isPresent(), "search_failed 应存在于图状态");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  桩类
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 手写版网关桩。
     *
     * 职责：模拟两种路径：
     *   正常路径（isFallback=false）：第1次调用返回关键词，第2次调用返回搜索结果摘要
     *   降级路径（isFallback=true）：第1次调用返回关键词，第2次调用返回降级回答
     *
     * @author xiexu
     */
    private static final class HandwrittenGatewayStub implements AgentLlmGateway {

        private final AtomicInteger callCount = new AtomicInteger();
        private final boolean isFallback;

        /**
         * 创建手写版网关桩。
         *
         * @param isFallback true 表示模拟降级路径（搜索失败）
         */
        HandwrittenGatewayStub(boolean isFallback) {
            this.isFallback = isFallback;
        }

        /**
         * 按调用顺序返回固定响应。
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            int call = callCount.incrementAndGet();
            // 第 1 次：UnderstandNode 调用，提取关键词
            if (call == 1) {
                return buildResponse(request, "Spring AI LangGraph 多智能体编排");
            }
            // 第 2 次：AnswerNode 或 FallbackNode 调用
            if (isFallback) {
                return buildResponse(request, "（注：本次回答未使用实时搜索，信息可能不是最新的）\n降级回答：Spring AI 和 LangGraph 都支持多智能体编排。");
            }
            return buildResponse(request, "搜索结果摘要：Spring AI 提供 Java 原生集成，LangGraph 提供图式状态机编排能力。");
        }

        /**
         * 当前测试不需要流式调用。
         *
         * @param request LLM 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("graph flow test stub does not support stream");
        }

        /**
         * 当前测试不需要结构化输出。
         *
         * @param request LLM 请求
         * @param spec    结构化定义
         * @param <T>     输出类型
         * @return 永不返回
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("graph flow test stub does not support structured chat");
        }

        /**
         * 返回空能力集合。
         *
         * @return 空能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return Collections.emptySet();
        }

        /**
         * 构造统一响应对象。
         *
         * @param request 原始请求
         * @param text    输出文本
         * @return 统一响应
         */
        private LlmResponse buildResponse(LlmRequest request, String text) {
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId("response-" + UUID.randomUUID())
                    .outputMessage(Message.builder()
                            .messageId("message-" + UUID.randomUUID())
                            .conversationId(request.getConversationId())
                            .role(MessageRole.ASSISTANT)
                            .content(text)
                            .build())
                    .rawText(text)
                    .build();
        }
    }

    /**
     * 框架版 ChatModel 桩。
     *
     * 职责：模拟 UnderstandNodeAction / AnswerNodeAction / FallbackNodeAction 的模型调用。
     * 注意：框架版 SearchNodeAction 不调用 LLM，因此两条路径均只有 2 次 LLM 调用。
     *
     * @author xiexu
     */
    private static final class FrameworkChatModelStub implements ChatModel {

        private final AtomicInteger callCount = new AtomicInteger();
        private final boolean isFallback;

        /**
         * 创建框架版 ChatModel 桩。
         *
         * @param isFallback true 表示模拟降级路径
         */
        FrameworkChatModelStub(boolean isFallback) {
            this.isFallback = isFallback;
        }

        /**
         * 按调用顺序返回固定响应。
         *
         * @param prompt 当前提示词
         * @return 聊天响应
         */
        @Override
        public ChatResponse call(Prompt prompt) {
            int call = callCount.incrementAndGet();
            // 第 1 次：UnderstandNodeAction 提取关键词
            if (call == 1) {
                return buildChatResponse("Spring AI LangGraph 多智能体编排");
            }
            // 第 2 次：AnswerNodeAction 或 FallbackNodeAction 生成最终回答
            if (isFallback) {
                return buildChatResponse("（注：本次回答未使用实时搜索，信息可能不是最新的）\n降级回答：Spring AI 和 LangGraph 都支持多智能体编排。");
            }
            return buildChatResponse("搜索结果摘要：Spring AI 提供 Java 原生集成，LangGraph 提供图式状态机编排能力。");
        }

        /**
         * 构造 Spring AI 聊天响应。
         *
         * @param text 输出文本
         * @return 聊天响应
         */
        private ChatResponse buildChatResponse(String text) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }
    }

    /**
     * 将 ChatModel 桩包装成 AgentLlmGateway 供框架版测试使用。
     *
     * @param chatModel Spring AI ChatModel 桩
     * @return AgentLlmGateway 实现
     */
    private AgentLlmGateway buildGatewayFromChatModel(ChatModel chatModel) {
        return new AgentLlmGateway() {
            @Override
            public LlmResponse chat(LlmRequest request) {
                String promptText = request.getMessages().isEmpty()
                        ? "" : request.getMessages().getLast().getContent();
                org.springframework.ai.chat.prompt.Prompt prompt =
                        new org.springframework.ai.chat.prompt.Prompt(promptText);
                ChatResponse chatResponse = chatModel.call(prompt);
                String text = chatResponse.getResult().getOutput().getText();
                return LlmResponse.builder()
                        .requestId(request.getRequestId())
                        .responseId("response-" + UUID.randomUUID())
                        .outputMessage(Message.builder()
                                .messageId("message-" + UUID.randomUUID())
                                .conversationId(request.getConversationId())
                                .role(MessageRole.ASSISTANT)
                                .content(text)
                                .build())
                        .rawText(text)
                        .build();
            }

            @Override
            public void stream(LlmRequest request, LlmStreamHandler handler) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<LlmCapability> capabilities() {
                return Collections.emptySet();
            }
        };
    }
}

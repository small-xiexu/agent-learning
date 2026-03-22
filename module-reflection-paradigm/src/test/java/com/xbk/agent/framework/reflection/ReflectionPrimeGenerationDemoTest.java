package com.xbk.agent.framework.reflection;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.reflection.application.coordinator.HandwrittenReflectionAgent;
import com.xbk.agent.framework.reflection.application.executor.HandwrittenJavaCoder;
import com.xbk.agent.framework.reflection.application.executor.HandwrittenJavaReviewer;
import com.xbk.agent.framework.reflection.domain.memory.ReflectionMemory;
import com.xbk.agent.framework.reflection.domain.memory.ReflectionTurnRecord;
import com.xbk.agent.framework.reflection.infrastructure.agentframework.AlibabaReflectionFlowAgent;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reflection 素数生成题对照测试
 *
 * 职责：先用测试钉住手写版反思循环，再补图流版状态回环
 *
 * @author xiexu
 */
class ReflectionPrimeGenerationDemoTest {

    private static final String PRIME_TASK = "编写一个 Java 方法，找出 1 到 n 之间所有的素数 (prime numbers)，并返回一个 List<Integer>。";

    /**
     * 验证手写版 Reflection Agent 会在 while 循环里完成“初稿 -> 反思 -> 优化 -> 再反思”。
     */
    @Test
    void shouldImprovePrimeGeneratorWithHandwrittenReflectionAgent() {
        HandwrittenGatewayStub gateway = new HandwrittenGatewayStub();
        HandwrittenJavaCoder javaCoder = new HandwrittenJavaCoder(gateway);
        HandwrittenJavaReviewer javaReviewer = new HandwrittenJavaReviewer(gateway);
        ReflectionMemory memory = new ReflectionMemory();
        HandwrittenReflectionAgent agent = new HandwrittenReflectionAgent(javaCoder, javaReviewer, memory, 3);

        HandwrittenReflectionAgent.RunResult result = agent.run(PRIME_TASK);

        assertTrue(result.getFinalCode().contains("boolean[] composite"));
        assertTrue(result.getFinalReflection().contains("无需改进"));
        assertEquals(2, result.getMemory().size());
        assertTurnRecord(result.getMemory().get(0), "for (int divisor = 2; divisor < candidate; divisor++)", "埃拉托斯特尼筛法");
        assertTurnRecord(result.getMemory().get(1), "multiple = candidate * candidate", "无需改进");
    }

    /**
     * 验证 FlowAgent 版 Reflection 会通过条件边在状态图中形成受控回环。
     */
    @Test
    void shouldImprovePrimeGeneratorWithAlibabaReflectionFlowAgent() {
        ReflectionChatModelStub chatModel = new ReflectionChatModelStub();
        AlibabaReflectionFlowAgent agent = new AlibabaReflectionFlowAgent(chatModel, 3);

        AlibabaReflectionFlowAgent.RunResult result = agent.run(PRIME_TASK);

        assertTrue(result.getFinalCode().contains("boolean[] composite"));
        assertTrue(result.getFinalReview().contains("无需改进"));
        assertEquals(2, result.getIterationCount());
        assertTrue(result.getState().value("current_code").isPresent());
        assertTrue(result.getState().value("review_feedback").isPresent());
        assertTrue(result.getState().value("iteration_count").isPresent());
        assertEquals("java-reflection-coder-agent", agent.getJavaCoderAgent().name());
        assertEquals("java-reflection-reviewer-agent", agent.getJavaReviewerAgent().name());
        assertFalse(agent.getJavaCoderAgent().isReturnReasoningContents());
        assertFalse(agent.getJavaReviewerAgent().isReturnReasoningContents());
    }

    /**
     * 断言反思轮次记录包含代码和反馈。
     *
     * @param record 轮次记录
     * @param expectedCodeSnippet 预期代码片段
     * @param expectedReflectionSnippet 预期反馈片段
     */
    private void assertTurnRecord(ReflectionTurnRecord record,
                                  String expectedCodeSnippet,
                                  String expectedReflectionSnippet) {
        assertTrue(record.getExecution().contains(expectedCodeSnippet));
        assertTrue(record.getReflection().contains(expectedReflectionSnippet));
    }

    /**
     * 手写版网关桩。
     *
     * 职责：模拟“生成初稿 -> 反思 -> 优化 -> 再反思”的四次统一网关调用
     *
     * @author xiexu
     */
    private static final class HandwrittenGatewayStub implements AgentLlmGateway {

        private final AtomicInteger callCount = new AtomicInteger();

        /**
         * 返回固定响应并校验手写版提示词上下文。
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            int currentCall = callCount.incrementAndGet();
            String prompt = request.getMessages().isEmpty() ? "" : request.getMessages().getLast().getContent();
            if (currentCall == 1) {
                assertPromptContains(prompt, PRIME_TASK);
                return buildResponse(request, naivePrimeCode());
            }
            if (currentCall == 2) {
                assertPromptContains(prompt, naivePrimeCode(), "时间复杂度");
                return buildResponse(request, """
                        当前代码的主循环对每个 candidate 都再次遍历 divisor，时间复杂度接近 O(n^2)。
                        性能瓶颈在于重复做了大量除法判断。
                        建议改用埃拉托斯特尼筛法，把复杂度优化到接近 O(n log log n)。
                        """);
            }
            if (currentCall == 3) {
                assertPromptContains(prompt, naivePrimeCode(), "埃拉托斯特尼筛法");
                return buildResponse(request, optimizedPrimeCode());
            }
            assertPromptContains(prompt, optimizedPrimeCode(), "无需改进");
            return buildResponse(request, """
                    无需改进。
                    当前代码已经使用埃拉托斯特尼筛法，算法层面接近这个问题的常见最优解。
                    """);
        }

        /**
         * 当前测试不需要流式调用。
         *
         * @param request LLM 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("reflection test stub does not support stream");
        }

        /**
         * 当前测试不需要结构化输出。
         *
         * @param request LLM 请求
         * @param spec 结构化定义
         * @param <T> 输出类型
         * @return 永不返回
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("reflection test stub does not support structured chat");
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
         * @param text 输出文本
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

        /**
         * 断言提示词包含关键上下文。
         *
         * @param prompt 当前提示词
         * @param expectedParts 预期片段
         */
        private void assertPromptContains(String prompt, String... expectedParts) {
            for (String expectedPart : expectedParts) {
                assertTrue(prompt.contains(expectedPart), "提示词缺少预期片段: " + expectedPart + "\n实际提示词:\n" + prompt);
            }
        }
    }

    /**
     * FlowAgent 测试模型桩。
     *
     * 职责：模拟 JavaCoderNode 和 JavaReviewerNode 在图中的四次模型调用，并校验状态占位符已渲染
     *
     * @author xiexu
     */
    private static final class ReflectionChatModelStub implements ChatModel {

        private final AtomicInteger callCount = new AtomicInteger();

        /**
         * 返回图流版 Reflection 所需的固定响应。
         *
         * @param prompt 当前提示词
         * @return 聊天响应
         */
        @Override
        public ChatResponse call(Prompt prompt) {
            int currentCall = callCount.incrementAndGet();
            String promptText = prompt.getContents();
            if (currentCall == 1) {
                assertTrue(promptText.contains(PRIME_TASK), "JavaCoderNode 初稿提示词缺少任务描述");
                assertTrue(promptText.contains("优先给出一版正确、可运行、便于后续优化的基础实现"),
                        "JavaCoderNode 初稿提示词缺少“首轮先给基础版”约束");
                assertTrue(promptText.contains("第一轮不要一开始就追求最优时间复杂度"),
                        "JavaCoderNode 初稿提示词缺少“首轮不要直接追求最优复杂度”约束");
                return buildChatResponse(naivePrimeCode());
            }
            if (currentCall == 2) {
                assertTrue(promptText.contains(naivePrimeCode()), "JavaReviewerNode 提示词缺少当前代码");
                assertTrue(promptText.contains("如果仍然存在明确、可落地的复杂度优化空间，请不要输出“无需改进”"),
                        "JavaReviewerNode 提示词缺少“有优化空间时不要停止”约束");
                assertTrue(promptText.contains("只有当当前实现已经没有明显的时间复杂度优化空间时，才明确输出“无需改进”"),
                        "JavaReviewerNode 提示词缺少“仅在确实收敛时才停止”约束");
                return buildChatResponse("""
                        当前代码时间复杂度接近 O(n^2)，主要瓶颈是对每个 candidate 都重复试除。
                        建议改成埃拉托斯特尼筛法，并从 candidate * candidate 开始标记合数。
                        """);
            }
            if (currentCall == 3) {
                assertTrue(promptText.contains("埃拉托斯特尼筛法"), "JavaCoderNode 优化提示词缺少评审意见");
                assertTrue(promptText.contains(naivePrimeCode()), "JavaCoderNode 优化提示词缺少旧代码");
                return buildChatResponse(optimizedPrimeCode());
            }
            assertTrue(promptText.contains(optimizedPrimeCode()), "JavaReviewerNode 终审提示词缺少优化后代码");
            return buildChatResponse("""
                    无需改进。
                    当前代码已经使用埃拉托斯特尼筛法，算法层面已经达到这个问题的常见最优解。
                    """);
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
     * 返回低效初稿代码。
     *
     * @return 初稿代码
     */
    private static String naivePrimeCode() {
        return """
                public List<Integer> findPrimes(int n) {
                    List<Integer> result = new ArrayList<Integer>();
                    for (int candidate = 2; candidate <= n; candidate++) {
                        boolean prime = true;
                        for (int divisor = 2; divisor < candidate; divisor++) {
                            if (candidate % divisor == 0) {
                                prime = false;
                                break;
                            }
                        }
                        if (prime) {
                            result.add(candidate);
                        }
                    }
                    return result;
                }
                """;
    }

    /**
     * 返回筛法优化后的代码。
     *
     * @return 优化后代码
     */
    private static String optimizedPrimeCode() {
        return """
                public List<Integer> findPrimes(int n) {
                    List<Integer> result = new ArrayList<Integer>();
                    if (n < 2) {
                        return result;
                    }
                    boolean[] composite = new boolean[n + 1];
                    for (int candidate = 2; candidate * candidate <= n; candidate++) {
                        if (composite[candidate]) {
                            continue;
                        }
                        for (int multiple = candidate * candidate; multiple <= n; multiple += candidate) {
                            composite[multiple] = true;
                        }
                    }
                    for (int candidate = 2; candidate <= n; candidate++) {
                        if (!composite[candidate]) {
                            result.add(candidate);
                        }
                    }
                    return result;
                }
                """;
    }
}

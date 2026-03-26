package com.xbk.agent.framework.planreplan;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.planreplan.application.coordinator.HandwrittenPlanAndSolveAgent;
import com.xbk.agent.framework.planreplan.application.executor.HandwrittenExecutor;
import com.xbk.agent.framework.planreplan.application.executor.HandwrittenPlanner;
import com.xbk.agent.framework.planreplan.domain.execution.StepExecutionRecord;
import com.xbk.agent.framework.planreplan.domain.plan.PlanStep;
import com.xbk.agent.framework.planreplan.infrastructure.agentframework.AlibabaSequentialPlanAndSolveAgent;
import com.xbk.agent.framework.planreplan.infrastructure.agentframework.support.PlanReplanStateKeys;
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
 * Plan-and-Solve 苹果题对照测试
 *
 * 职责：先用测试钉住手写版执行链路，再继续补框架版实现
 *
 * @author xiexu
 */
class PlanAndSolveAppleProblemDemoTest {

    private static final String APPLE_QUESTION = "一个水果店周一卖出了15个苹果。周二卖出的苹果数量是周一的两倍。周三卖出的数量比周二少了5个。请问这三天总共卖出了多少个苹果？";

    /**
     * 验证手写版 Plan-and-Solve 可以按计划逐步求解买苹果问题。
     */
    @Test
    void shouldSolveAppleProblemWithHandwrittenPlanAndSolveAgent() {
        HandwrittenGatewayStub gateway = new HandwrittenGatewayStub();
        HandwrittenPlanner planner = new HandwrittenPlanner(gateway);
        HandwrittenExecutor executor = new HandwrittenExecutor(gateway);
        HandwrittenPlanAndSolveAgent agent = new HandwrittenPlanAndSolveAgent(planner, executor);

        HandwrittenPlanAndSolveAgent.RunResult result = agent.run(APPLE_QUESTION);

        assertEquals(4, result.getPlan().size());
        assertEquals(4, result.getHistory().size());
        assertTrue(result.getFinalAnswer().contains("70"));
        assertPlanStep(result.getPlan().get(0), 1, "读取周一卖出苹果数量。");
        assertHistoryResult(result.getHistory().get(0), "周一卖出了15个苹果。");
        assertHistoryResult(result.getHistory().get(3), "这三天总共卖出了70个苹果。");
    }

    /**
     * 验证顺序 Agent 版本可以通过 outputKey 状态流转完成同一道苹果题。
     */
    @Test
    void shouldSolveAppleProblemWithAlibabaSequentialAgent() {
        SequentialChatModelStub chatModel = new SequentialChatModelStub();
        AlibabaSequentialPlanAndSolveAgent agent = new AlibabaSequentialPlanAndSolveAgent(chatModel);

        AlibabaSequentialPlanAndSolveAgent.RunResult result = agent.run(APPLE_QUESTION);

        assertTrue(result.getPlanResult().contains("1. 读取周一卖出苹果数量。"));
        assertTrue(result.getFinalAnswer().contains("70"));
        assertEquals(PlanReplanStateKeys.PLAN_RESULT, agent.getPlannerAgent().getOutputKey());
        assertEquals(PlanReplanStateKeys.FINAL_ANSWER, agent.getExecutorAgent().getOutputKey());
        assertFalse(agent.getPlannerAgent().isIncludeContents());
        assertFalse(agent.getExecutorAgent().isIncludeContents());
        assertFalse(agent.getPlannerAgent().isReturnReasoningContents());
        assertFalse(agent.getExecutorAgent().isReturnReasoningContents());
        assertTrue(result.getState().value(PlanReplanStateKeys.PLAN_RESULT).isPresent());
        assertTrue(result.getState().value(PlanReplanStateKeys.FINAL_ANSWER).isPresent());
        assertTrue(result.getState().value(PlanReplanStateKeys.PLAN_RESULT).orElseThrow() instanceof AssistantMessage);
        assertTrue(result.getState().value(PlanReplanStateKeys.FINAL_ANSWER).orElseThrow() instanceof AssistantMessage);
    }

    /**
     * 断言计划步骤内容。
     *
     * @param step 计划步骤
     * @param expectedIndex 预期编号
     * @param expectedInstruction 预期步骤描述
     */
    private void assertPlanStep(PlanStep step, int expectedIndex, String expectedInstruction) {
        assertEquals(expectedIndex, step.getStepIndex());
        assertEquals(expectedInstruction, step.getInstruction());
    }

    /**
     * 断言执行记录结果。
     *
     * @param record 执行记录
     * @param expectedResult 预期结果
     */
    private void assertHistoryResult(StepExecutionRecord record, String expectedResult) {
        assertEquals(expectedResult, record.getStepResult());
    }

    /**
     * 手写版网关桩。
     *
     * 职责：用固定响应模拟 Planner 与 Executor 的多轮 LLM 调用，并校验执行上下文
     *
     * @author xiexu
     */
    private static final class HandwrittenGatewayStub implements AgentLlmGateway {

        private static final String PLAN_TEXT = """
                1. 读取周一卖出苹果数量。
                2. 计算周二卖出苹果数量。
                3. 计算周三卖出苹果数量。
                4. 计算三天总销量。
                """;

        private final AtomicInteger callCount = new AtomicInteger();

        /**
         * 返回固定响应并校验手写版上下文。
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            int currentCall = callCount.incrementAndGet();
            String prompt = request.getMessages().isEmpty() ? "" : request.getMessages().getLast().getContent();
            if (currentCall == 1) {
                assertPromptContains(prompt, APPLE_QUESTION);
                return buildResponse(request, PLAN_TEXT);
            }
            if (currentCall == 2) {
                assertPromptContains(prompt, APPLE_QUESTION, PLAN_TEXT, "当前步骤：步骤1：读取周一卖出苹果数量。");
                return buildResponse(request, "周一卖出了15个苹果。");
            }
            if (currentCall == 3) {
                assertPromptContains(prompt, "周一卖出了15个苹果。", "当前步骤：步骤2：计算周二卖出苹果数量。");
                return buildResponse(request, "周二卖出了30个苹果。");
            }
            if (currentCall == 4) {
                assertPromptContains(prompt, "周二卖出了30个苹果。", "当前步骤：步骤3：计算周三卖出苹果数量。");
                return buildResponse(request, "周三卖出了25个苹果。");
            }
            assertPromptContains(prompt, "周三卖出了25个苹果。", "当前步骤：步骤4：计算三天总销量。");
            return buildResponse(request, "这三天总共卖出了70个苹果。");
        }

        /**
         * 当前测试不需要流式调用。
         *
         * @param request LLM 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("handwritten test stub does not support stream");
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
            throw new UnsupportedOperationException("handwritten test stub does not support structured chat");
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
         * @return LLM 响应
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
     * 顺序 Agent 测试模型桩。
     *
     * 职责：模拟 PlannerAgent 和 ExecutorAgent 的两次模型调用，并校验状态占位符已经被替换
     *
     * @author xiexu
     */
    private static final class SequentialChatModelStub implements ChatModel {

        private static final String PLAN_TEXT = """
                1. 读取周一卖出苹果数量。
                2. 计算周二卖出苹果数量。
                3. 计算周三卖出苹果数量。
                4. 计算三天总销量。
                """;

        private final AtomicInteger callCount = new AtomicInteger();

        /**
         * 返回顺序 Agent 所需的固定响应。
         *
         * @param prompt 当前提示词
         * @return 聊天响应
         */
        @Override
        public ChatResponse call(Prompt prompt) {
            int currentCall = callCount.incrementAndGet();
            String promptText = prompt.getContents();
            if (currentCall == 1) {
                assertTrue(promptText.contains(APPLE_QUESTION), "PlannerAgent 提示词缺少原始问题");
                return buildChatResponse(PLAN_TEXT);
            }
            assertTrue(promptText.contains(APPLE_QUESTION), "ExecutorAgent 提示词缺少原始问题");
            assertTrue(promptText.contains(PLAN_TEXT), "ExecutorAgent 提示词缺少 Planner 输出计划");
            return buildChatResponse("周一卖出15个，周二卖出30个，周三卖出25个，所以三天总共卖出了70个苹果。");
        }

        /**
         * 构造最小聊天响应。
         *
         * @param text 输出文本
         * @return 聊天响应
         */
        private ChatResponse buildChatResponse(String text) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }
    }
}

package com.xbk.agent.framework.supervisor;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.LlmFinishReason;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.supervisor.api.SupervisorRunResult;
import com.xbk.agent.framework.supervisor.domain.routing.SupervisorWorkerType;
import com.xbk.agent.framework.supervisor.framework.agent.AlibabaSupervisorFlowAgent;
import com.xbk.agent.framework.supervisor.framework.support.SupervisorStateKeys;
import com.xbk.agent.framework.supervisor.handwritten.coordinator.HandwrittenSupervisorCoordinator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supervisor 双实现对照测试
 *
 * 职责：钉住“监督者动态路由 + 子 Agent 返回监督者继续决策”的 handwritten 与框架版统一行为。
 * 这组测试不关心真实模型质量，只关心 Supervisor 运行时是否遵守约定的路由闭环。
 *
 * @author xiexu
 */
class SupervisorPatternComparisonTest {

    private static final String BLOG_TASK = """
            请以“Spring AI Alibaba的多智能体优势”为主题写一篇简短的博客，
            然后将其翻译成英文，最后对英文翻译进行语法和拼写审查。
            """;

    /**
     * 验证手写版 Supervisor 可以完成写作、翻译、审校和 FINISH 闭环。
     */
    @Test
    void shouldCompleteBlogPipelineWithHandwrittenSupervisorCoordinator() {
        ScriptedSupervisorGateway gateway = new ScriptedSupervisorGateway();
        HandwrittenSupervisorCoordinator coordinator = new HandwrittenSupervisorCoordinator(gateway, 6);

        SupervisorRunResult result = coordinator.run(BLOG_TASK);

        assertEquals(SupervisorWorkerType.FINISH, result.getStopWorker());
        assertEquals(List.of(
                SupervisorWorkerType.WRITER,
                SupervisorWorkerType.TRANSLATOR,
                SupervisorWorkerType.REVIEWER,
                SupervisorWorkerType.FINISH), result.getRouteTrail());
        assertEquals(3, result.getStepRecords().size());
        assertTrue(result.getChineseDraft().contains("Spring AI Alibaba"));
        assertTrue(result.getEnglishTranslation().contains("central supervisor"));
        assertTrue(result.getReviewedEnglish().contains("dynamic delegation"));
        assertTrue(result.getFinalOutput().contains("grammar and spelling"));
        assertTrue(result.getScratchpadMessages().size() >= 8);
        assertTrue(result.getFlowState().isEmpty());
    }

    /**
     * 验证框架版 SupervisorAgent 可以完成同样的内容生产流水线。
     */
    @Test
    void shouldCompleteBlogPipelineWithAlibabaSupervisorAgent() {
        ScriptedSupervisorGateway gateway = new ScriptedSupervisorGateway();
        AlibabaSupervisorFlowAgent agent = new AlibabaSupervisorFlowAgent(gateway, 6);

        SupervisorRunResult result = agent.run(BLOG_TASK);
        OverAllState state = result.getFlowState().orElseThrow();

        assertEquals(SupervisorWorkerType.FINISH, result.getStopWorker());
        assertEquals(List.of(
                SupervisorWorkerType.WRITER,
                SupervisorWorkerType.TRANSLATOR,
                SupervisorWorkerType.REVIEWER,
                SupervisorWorkerType.FINISH), result.getRouteTrail());
        assertTrue(agent.extractOutput(state, SupervisorStateKeys.WRITER_OUTPUT).contains("Spring AI Alibaba"));
        assertTrue(agent.extractOutput(state, SupervisorStateKeys.TRANSLATOR_OUTPUT).contains("central supervisor"));
        assertTrue(agent.extractOutput(state, SupervisorStateKeys.REVIEWER_OUTPUT).contains("dynamic delegation"));
        assertEquals("blog_supervisor_agent", agent.getSupervisorAgent().name());
        assertEquals("writer_agent", agent.getWriterAgent().name());
        assertEquals("translator_agent", agent.getTranslatorAgent().name());
        assertEquals("reviewer_agent", agent.getReviewerAgent().name());
        assertFalse(result.getScratchpadMessages().isEmpty());
    }

    /**
     * 脚本化 Supervisor 网关。
     *
     * 职责：按固定顺序模拟 Supervisor -> Writer -> Supervisor -> Translator -> Supervisor -> Reviewer -> Supervisor 的七次调用
     *
     * @author xiexu
     */
    private static final class ScriptedSupervisorGateway implements AgentLlmGateway {

        private final AtomicInteger callCount = new AtomicInteger();

        /**
         * 返回脚本化响应。
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            int currentCall = callCount.incrementAndGet();
            String promptText = request.getMessages().stream()
                    .map(Message::getContent)
                    .reduce("", (left, right) -> left + "\n" + right);
            if (currentCall == 1) {
                // 第一轮一定先由主管做路由决策，目标是把任务交给 Writer。
                if (promptText.contains("next_worker")) {
                    return buildResponse(request, """
                            {
                              "next_worker": "WRITER",
                              "task_instruction": "请围绕主题先写一篇简短中文博客。"
                            }
                            """);
                }
                if (promptText.contains("[\"writer_agent\"]")) {
                    return buildResponse(request, """
                            ["writer_agent"]
                            """);
                }
                throw new IllegalStateException("Unexpected first prompt: " + promptText);
            }
            if (currentCall == 2) {
                // 第二次调用对应 Writer 真正执行写作。
                assertTrue(promptText.contains("中文博客") || promptText.contains("writer_agent"));
                return buildResponse(request, """
                        Spring AI Alibaba 在多智能体场景中的优势，首先体现在它提供了统一的 Agent 组合能力。
                        它让团队可以围绕中心监督者进行分工协作，把写作、翻译和审校拆成清晰的专业节点。
                        对企业来说，这种方式既提升了可治理性，也降低了流程失控的风险。
                        """);
            }
            if (currentCall == 3) {
                // 第三次调用回到主管，由主管判断下一步切到 Translator。
                assertTrue(promptText.contains("Spring AI Alibaba"));
                if (promptText.contains("next_worker")) {
                    return buildResponse(request, """
                            {
                              "next_worker": "TRANSLATOR",
                              "task_instruction": "请把当前中文博客准确翻译成英文。"
                            }
                            """);
                }
                if (promptText.contains("[\"translator_agent\"]")) {
                    return buildResponse(request, """
                            ["translator_agent"]
                            """);
                }
                throw new IllegalStateException("Unexpected third prompt: " + promptText);
            }
            if (currentCall == 4) {
                // 第四次调用对应 Translator 执行翻译。
                assertTrue(promptText.contains("翻译") || promptText.contains("translator_agent"));
                return buildResponse(request, """
                        One of Spring AI Alibaba's key strengths in multi-agent systems is its unified composition model.
                        It allows a central supervisor to coordinate specialized workers for writing, translation, and review.
                        For enterprise teams, this improves control, clarity, and delivery consistency.
                        """);
            }
            if (currentCall == 5) {
                // 第五次调用再回到主管，由主管判断下一步切到 Reviewer。
                assertTrue(promptText.contains("unified composition model"));
                if (promptText.contains("next_worker")) {
                    return buildResponse(request, """
                            {
                              "next_worker": "REVIEWER",
                              "task_instruction": "请对当前英文译文进行语法和拼写审校，并直接返回修订稿。"
                            }
                            """);
                }
                if (promptText.contains("[\"reviewer_agent\"]")) {
                    return buildResponse(request, """
                            ["reviewer_agent"]
                            """);
                }
                throw new IllegalStateException("Unexpected fifth prompt: " + promptText);
            }
            if (currentCall == 6) {
                // 第六次调用对应 Reviewer 完成最终审校。
                assertTrue(promptText.contains("grammar") || promptText.contains("reviewer_agent")
                        || promptText.contains("英文译文"));
                return buildResponse(request, """
                        One of Spring AI Alibaba's key strengths in multi-agent systems is its unified composition model.
                        It allows a central supervisor to coordinate specialized workers through dynamic delegation for writing, translation, and review.
                        For enterprise teams, this improves control, clarity, and grammar and spelling consistency.
                        """);
            }
            if (currentCall == 7) {
                // 第七次调用最后回到主管，由主管给出 FINISH。
                assertTrue(promptText.contains("dynamic delegation"));
                if (promptText.contains("next_worker")) {
                    return buildResponse(request, """
                            {
                              "next_worker": "FINISH",
                              "task_instruction": "任务已经完成。"
                            }
                            """);
                }
                if (promptText.contains("[\"FINISH\"]")) {
                    return buildResponse(request, """
                            ["FINISH"]
                            """);
                }
                throw new IllegalStateException("Unexpected seventh prompt: " + promptText);
            }
            throw new IllegalStateException("Unexpected call count: " + currentCall);
        }

        /**
         * 当前测试不覆盖流式能力。
         *
         * @param request LLM 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("stream is not used in this test");
        }

        /**
         * 当前测试不覆盖结构化输出能力。
         *
         * @param request LLM 请求
         * @param spec 输出规范
         * @param <T> 输出类型
         * @return 结构化响应
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("structuredChat is not used in this test");
        }

        /**
         * 返回支持能力。
         *
         * @return 能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return Set.of(LlmCapability.SYNC_CHAT);
        }

        /**
         * 构造统一响应对象。
         *
         * @param request 原始请求
         * @param content 响应文本
         * @return LLM 响应
         */
        private LlmResponse buildResponse(LlmRequest request, String content) {
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId("response-" + UUID.randomUUID())
                    .finishReason(LlmFinishReason.STOP)
                    .rawText(content)
                    .outputMessage(Message.builder()
                            .messageId("message-" + UUID.randomUUID())
                            .conversationId(request.getConversationId())
                            .role(MessageRole.ASSISTANT)
                            .content(content)
                            .build())
                    .build();
        }
    }
}

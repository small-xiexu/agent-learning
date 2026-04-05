package com.xbk.agent.framework.engineering.handwritten;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.application.routing.CustomerIntentClassifier;
import com.xbk.agent.framework.engineering.domain.routing.CustomerIntentType;
import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenReceptionistAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenSalesAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenTechSupportAgent;
import com.xbk.agent.framework.engineering.handwritten.coordinator.HandwrittenEngineeringCoordinator;
import com.xbk.agent.framework.engineering.handwritten.hub.InMemoryMessageHub;
import com.xbk.agent.framework.engineering.handwritten.runtime.ConversationContextStore;
import com.xbk.agent.framework.engineering.handwritten.runtime.PendingResponseRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手写版智能客服路由测试。
 *
 * 职责：验证 Receptionist 通过 MessageHub 把用户问题路由给专业客服，并通过异步回包完成完整闭环。
 *
 * @author xiexu
 */
class HandwrittenEngineeringRoutingTest {

    /**
     * 验证技术类问题会被路由给技术专家，并返回技术结果。
     */
    @Test
    void shouldRouteTechSupportQuestionToTechSpecialist() {
        HandwrittenEngineeringCoordinator coordinator = createCoordinator();

        EngineeringRunResult result = coordinator.run("我的服务启动时报 NullPointerException，帮我排查原因。");

        assertEquals(CustomerIntentType.TECH_SUPPORT, result.getIntentType());
        assertEquals(SpecialistType.TECH_SUPPORT, result.getSpecialistType());
        assertEquals("support.tech.request", result.getRoutingDecision().getTargetTopic());
        assertTrue(result.getSpecialistResponse().contains("NullPointerException"));
        assertTrue(result.getFinalResponse().contains("技术专家"));
        assertTrue(result.getRouteTrail().contains("receptionist_agent"));
        assertTrue(result.getRouteTrail().contains("tech_support_agent"));
        assertTrue(result.getTrace().getDeliveryRecords().size() >= 4);
    }

    /**
     * 验证销售类问题会被路由给销售顾问，并返回报价答复。
     */
    @Test
    void shouldRouteSalesQuestionToSalesSpecialist() {
        HandwrittenEngineeringCoordinator coordinator = createCoordinator();

        EngineeringRunResult result = coordinator.run("我想了解企业版购买方案和报价，顺便介绍一下部署方式。");

        assertEquals(CustomerIntentType.SALES_CONSULTING, result.getIntentType());
        assertEquals(SpecialistType.SALES, result.getSpecialistType());
        assertEquals("support.sales.request", result.getRoutingDecision().getTargetTopic());
        assertTrue(result.getSpecialistResponse().contains("企业版"));
        assertTrue(result.getFinalResponse().contains("销售顾问"));
        assertTrue(result.getRouteTrail().contains("sales_agent"));
    }

    /**
     * 验证协调器等待结果时使用无超时阻塞等待。
     */
    @Test
    void shouldWaitForResultWithoutTimedGet() {
        HandwrittenEngineeringCoordinator coordinator = createCoordinator(new UntimedOnlyPendingResponseRegistry());

        EngineeringRunResult result = coordinator.run("我的服务启动时报 NullPointerException，帮我排查原因。");

        assertEquals(CustomerIntentType.TECH_SUPPORT, result.getIntentType());
        assertEquals(SpecialistType.TECH_SUPPORT, result.getSpecialistType());
    }

    /**
     * 创建手写版协调器。
     *
     * @return 协调器
     */
    private HandwrittenEngineeringCoordinator createCoordinator() {
        return createCoordinator(new PendingResponseRegistry());
    }

    /**
     * 创建手写版协调器。
     *
     * @param pendingResponseRegistry 待回包注册表
     * @return 协调器
     */
    private HandwrittenEngineeringCoordinator createCoordinator(PendingResponseRegistry pendingResponseRegistry) {
        ScriptedEngineeringGateway gateway = new ScriptedEngineeringGateway();
        InMemoryMessageHub messageHub = new InMemoryMessageHub();
        ConversationContextStore contextStore = new ConversationContextStore();
        CustomerIntentClassifier classifier = new CustomerIntentClassifier(gateway);
        HandwrittenReceptionistAgent receptionistAgent = new HandwrittenReceptionistAgent(
                gateway, classifier, messageHub, pendingResponseRegistry, contextStore);
        HandwrittenTechSupportAgent techSupportAgent = new HandwrittenTechSupportAgent(gateway, messageHub);
        HandwrittenSalesAgent salesAgent = new HandwrittenSalesAgent(gateway, messageHub);
        return new HandwrittenEngineeringCoordinator(
                messageHub,
                receptionistAgent,
                techSupportAgent,
                salesAgent,
                pendingResponseRegistry,
                contextStore);
    }

    /**
     * 只允许无超时等待的注册表。
     *
     * 职责：如果协调器继续调用 get(timeout, unit)，测试会立刻失败，
     * 从而钉住"学习场景下应使用无限等待"这一行为。
     *
     * @author xiexu
     */
    private static final class UntimedOnlyPendingResponseRegistry extends PendingResponseRegistry {

        private final ConcurrentHashMap<String, CompletableFuture<EngineeringRunResult>> pendingResponses =
                new ConcurrentHashMap<String, CompletableFuture<EngineeringRunResult>>();

        /**
         * 注册只允许无超时等待的 Future。
         *
         * @param correlationId 关联标识
         * @return future
         */
        @Override
        public CompletableFuture<EngineeringRunResult> register(String correlationId) {
            CompletableFuture<EngineeringRunResult> future = new UntimedOnlyFuture();
            pendingResponses.put(correlationId, future);
            return future;
        }

        /**
         * 完成回包。
         *
         * @param correlationId 关联标识
         * @param result 运行结果
         */
        @Override
        public void complete(String correlationId, EngineeringRunResult result) {
            CompletableFuture<EngineeringRunResult> future = pendingResponses.get(correlationId);
            if (future != null) {
                future.complete(result);
            }
        }

        /**
         * 获取 future。
         *
         * @param correlationId 关联标识
         * @return future
         */
        @Override
        public CompletableFuture<EngineeringRunResult> get(String correlationId) {
            return pendingResponses.get(correlationId);
        }

        /**
         * 删除 future。
         *
         * @param correlationId 关联标识
         */
        @Override
        public void remove(String correlationId) {
            pendingResponses.remove(correlationId);
        }
    }

    /**
     * 只允许调用无超时 get() 的 Future。
     *
     * @author xiexu
     */
    private static final class UntimedOnlyFuture extends CompletableFuture<EngineeringRunResult> {

        /**
         * 禁止超时版本的等待。
         *
         * @param timeout 超时时间
         * @param unit 时间单位
         * @return 永不返回
         * @throws InterruptedException 忽略
         * @throws ExecutionException 忽略
         * @throws TimeoutException 忽略
         */
        @Override
        public EngineeringRunResult get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            throw new AssertionError("Coordinator should use future.get() without timeout");
        }
    }

    /**
     * 脚本化客服网关。
     *
     * 职责：根据提示词阶段返回接待员路由结果、技术答复或销售答复，保证测试不依赖真实模型。
     *
     * @author xiexu
     */
    private static final class ScriptedEngineeringGateway implements AgentLlmGateway {

        private final AtomicInteger callCount = new AtomicInteger();

        /**
         * 返回脚本化响应。
         *
         * @param request LLM 请求
         * @return 脚本化响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            callCount.incrementAndGet();
            String promptText = request.getMessages().stream()
                    .map(Message::getContent)
                    .reduce("", (left, right) -> left + "\n" + right);
            String responseText;
            if (promptText.contains("TECH_SUPPORT / SALES_CONSULTING / UNKNOWN")) {
                responseText = promptText.contains("报价") || promptText.contains("购买")
                        ? "SALES_CONSULTING"
                        : "TECH_SUPPORT";
            }
            else if (promptText.contains("技术支持专家")) {
                responseText = "技术专家诊断：这是典型的 NullPointerException，需要先检查 Bean 是否正确注入以及配置是否为空。";
            }
            else {
                responseText = "销售顾问建议：企业版支持私有化部署与标准支持服务，报价可按节点规模分层提供。";
            }
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId("response-" + UUID.randomUUID())
                    .outputMessage(Message.builder()
                            .messageId("message-" + UUID.randomUUID())
                            .conversationId(request.getConversationId())
                            .role(MessageRole.ASSISTANT)
                            .content(responseText)
                            .build())
                    .rawText(responseText)
                    .build();
        }

        /**
         * 当前测试不覆盖流式能力。
         *
         * @param request 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("stream is not used in this test");
        }

        /**
         * 当前测试不覆盖结构化输出。
         *
         * @param request 请求
         * @param spec 输出规范
         * @param <T> 输出类型
         * @return 永不返回
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("structuredChat is not used in this test");
        }

        /**
         * 返回能力集合。
         *
         * @return 能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return Set.of(LlmCapability.SYNC_CHAT);
        }
    }
}

package com.xbk.agent.framework.engineering;

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
import com.xbk.agent.framework.engineering.framework.agent.FrameworkReceptionistService;
import com.xbk.agent.framework.engineering.framework.client.SalesRemoteAgentFacade;
import com.xbk.agent.framework.engineering.framework.client.TechSupportRemoteAgentFacade;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenReceptionistAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenSalesAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenTechSupportAgent;
import com.xbk.agent.framework.engineering.handwritten.coordinator.HandwrittenEngineeringCoordinator;
import com.xbk.agent.framework.engineering.handwritten.hub.InMemoryMessageHub;
import com.xbk.agent.framework.engineering.handwritten.runtime.ConversationContextStore;
import com.xbk.agent.framework.engineering.handwritten.runtime.PendingResponseRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 手写版与框架版路由对照测试。
 *
 * 职责：证明两套实现在业务语义上完全一致：
 * 相同的输入产生相同的意图分类、相同的专家类型、结构一致的结果。
 *
 * <p>关键设计说明：
 * 本测试刻意用同一个 ScriptedGateway 驱动两套实现，对照点只有通信机制：
 * <ul>
 *   <li>手写版：消息发布到 InMemoryMessageHub → 专家订阅 → 异步回包；
 *   <li>框架版：意图分类后同步调用 Mock Facade → 直接返回专家结果。
 * </ul>
 * 两者的意图分类路径完全相同（都经过 CustomerIntentClassifier）。
 * 框架版使用 Mock Facade 是因为无需启动 Nacos 或真实 A2A Provider。
 *
 * @author xiexu
 */
class EngineeringComparisonTest {

    /**
     * 对照验证：技术类问题下两套实现返回相同的 intentType 和 specialistType。
     */
    @Test
    void bothImplementationsShouldRouteTechQuestionToTechSupport() {
        String techQuestion = "我的服务启动时报 NullPointerException，帮我排查原因。";

        // 手写版
        HandwrittenEngineeringCoordinator coordinator = createHandwrittenCoordinator();
        EngineeringRunResult handwrittenResult = coordinator.run(techQuestion);

        // 框架版（Mock Facade）
        FrameworkReceptionistService frameworkService = createFrameworkService("TECH_SUPPORT",
                "技术专家诊断：这是典型的 NullPointerException，需要先检查 Bean 是否正确注入以及配置是否为空。");
        EngineeringRunResult frameworkResult = frameworkService.handle(techQuestion);

        // 核心语义必须一致
        assertEquals(handwrittenResult.getIntentType(), frameworkResult.getIntentType(),
                "两套实现对技术问题的意图分类必须一致");
        assertEquals(CustomerIntentType.TECH_SUPPORT, frameworkResult.getIntentType());
        assertEquals(handwrittenResult.getSpecialistType(), frameworkResult.getSpecialistType(),
                "两套实现对技术问题的专家路由必须一致");
        assertEquals(SpecialistType.TECH_SUPPORT, frameworkResult.getSpecialistType());
    }

    /**
     * 对照验证：销售类问题下两套实现返回相同的 intentType 和 specialistType。
     */
    @Test
    void bothImplementationsShouldRouteSalesQuestionToSalesConsulting() {
        String salesQuestion = "我想了解企业版购买方案和报价，顺便介绍一下部署方式。";

        // 手写版
        HandwrittenEngineeringCoordinator coordinator = createHandwrittenCoordinator();
        EngineeringRunResult handwrittenResult = coordinator.run(salesQuestion);

        // 框架版（Mock Facade）
        FrameworkReceptionistService frameworkService = createFrameworkService("SALES_CONSULTING",
                "销售顾问建议：企业版支持私有化部署与标准支持服务，报价可按节点规模分层提供。");
        EngineeringRunResult frameworkResult = frameworkService.handle(salesQuestion);

        assertEquals(handwrittenResult.getIntentType(), frameworkResult.getIntentType(),
                "两套实现对销售问题的意图分类必须一致");
        assertEquals(CustomerIntentType.SALES_CONSULTING, frameworkResult.getIntentType());
        assertEquals(handwrittenResult.getSpecialistType(), frameworkResult.getSpecialistType(),
                "两套实现对销售问题的专家路由必须一致");
        assertEquals(SpecialistType.SALES, frameworkResult.getSpecialistType());
    }

    /**
     * 对照验证：两套实现返回的 EngineeringRunResult 结构字段均不为空。
     *
     * <p>这确保了两套实现在结果契约上保持一致，消费方可以用同一个结果模型处理。
     */
    @Test
    void bothImplementationsShouldReturnCompleteResultStructure() {
        String techQuestion = "帮我排查服务异常";

        HandwrittenEngineeringCoordinator coordinator = createHandwrittenCoordinator();
        EngineeringRunResult handwrittenResult = coordinator.run(techQuestion);

        FrameworkReceptionistService frameworkService = createFrameworkService("TECH_SUPPORT", "技术专家回答");
        EngineeringRunResult frameworkResult = frameworkService.handle(techQuestion);

        // 手写版结果完整性
        assertResultComplete(handwrittenResult, "handwritten");

        // 框架版结果完整性
        assertResultComplete(frameworkResult, "framework");
    }

    /**
     * 验证结果字段完整性。
     *
     * @param result 运行结果
     * @param label 用于断言错误信息的标签
     */
    private void assertResultComplete(EngineeringRunResult result, String label) {
        assertNotNull(result.getConversationId(), label + ": conversationId 不应为空");
        assertNotNull(result.getIntentType(), label + ": intentType 不应为空");
        assertNotNull(result.getSpecialistType(), label + ": specialistType 不应为空");
        assertNotNull(result.getRoutingDecision(), label + ": routingDecision 不应为空");
        assertNotNull(result.getSpecialistResponse(), label + ": specialistResponse 不应为空");
        assertNotNull(result.getFinalResponse(), label + ": finalResponse 不应为空");
        assertNotNull(result.getRouteTrail(), label + ": routeTrail 不应为空");
        assertNotEmpty(result.getRouteTrail(), label + ": routeTrail 不应为空列表");
    }

    private void assertNotEmpty(java.util.List<?> list, String message) {
        if (list == null || list.isEmpty()) {
            throw new AssertionError(message);
        }
    }

    // ─── 构建辅助方法 ─────────────────────────────────────────────────────────────

    /**
     * 创建手写版协调器（使用脚本化 Gateway）。
     */
    private HandwrittenEngineeringCoordinator createHandwrittenCoordinator() {
        ScriptedEngineeringGateway gateway = new ScriptedEngineeringGateway();
        InMemoryMessageHub messageHub = new InMemoryMessageHub();
        PendingResponseRegistry pendingResponseRegistry = new PendingResponseRegistry();
        ConversationContextStore contextStore = new ConversationContextStore();
        CustomerIntentClassifier classifier = new CustomerIntentClassifier(gateway);
        HandwrittenReceptionistAgent receptionistAgent = new HandwrittenReceptionistAgent(
                gateway, classifier, messageHub, pendingResponseRegistry, contextStore);
        HandwrittenTechSupportAgent techSupportAgent = new HandwrittenTechSupportAgent(gateway, messageHub);
        HandwrittenSalesAgent salesAgent = new HandwrittenSalesAgent(gateway, messageHub);
        return new HandwrittenEngineeringCoordinator(
                messageHub, receptionistAgent, techSupportAgent, salesAgent,
                pendingResponseRegistry, contextStore);
    }

    /**
     * 创建框架版接待员服务（使用 Mock Facade，不依赖 Nacos 或真实 Provider）。
     *
     * @param intentResponse 意图分类返回值（"TECH_SUPPORT" 或 "SALES_CONSULTING"）
     * @param specialistAnswer 专家回答文本
     */
    private FrameworkReceptionistService createFrameworkService(String intentResponse,
                                                                 String specialistAnswer) {
        AgentLlmGateway gateway = new FixedIntentGateway(intentResponse);
        CustomerIntentClassifier classifier = new CustomerIntentClassifier(gateway);
        TechSupportRemoteAgentFacade techFacade = mock(TechSupportRemoteAgentFacade.class);
        SalesRemoteAgentFacade salesFacade = mock(SalesRemoteAgentFacade.class);
        if ("TECH_SUPPORT".equals(intentResponse)) {
            when(techFacade.call(anyString(), anyString())).thenReturn(specialistAnswer);
        }
        else {
            when(salesFacade.call(anyString(), anyString())).thenReturn(specialistAnswer);
        }
        return new FrameworkReceptionistService(classifier, techFacade, salesFacade);
    }

    // ─── 脚本化 LLM Gateway ──────────────────────────────────────────────────────

    /**
     * 多阶段脚本化网关，适用于手写版（需要三阶段响应：分类 + 技术回答 + 销售回答）。
     */
    private static final class ScriptedEngineeringGateway implements AgentLlmGateway {

        private final AtomicInteger callCount = new AtomicInteger();

        @Override
        public LlmResponse chat(LlmRequest request) {
            callCount.incrementAndGet();
            String promptText = request.getMessages().stream()
                    .map(Message::getContent)
                    .reduce("", (l, r) -> l + "\n" + r);
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
            return buildResponse(request, responseText);
        }

        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public Set<LlmCapability> capabilities() {
            return Set.of(LlmCapability.SYNC_CHAT);
        }
    }

    /**
     * 固定意图返回网关，适用于框架版（只需要意图分类阶段）。
     */
    private static final class FixedIntentGateway implements AgentLlmGateway {

        private final String intentResponse;

        private FixedIntentGateway(String intentResponse) {
            this.intentResponse = intentResponse;
        }

        @Override
        public LlmResponse chat(LlmRequest request) {
            return buildResponse(request, intentResponse);
        }

        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public Set<LlmCapability> capabilities() {
            return Set.of(LlmCapability.SYNC_CHAT);
        }
    }

    /**
     * 构建测试用 LlmResponse。
     */
    private static LlmResponse buildResponse(LlmRequest request, String content) {
        return LlmResponse.builder()
                .requestId(request.getRequestId())
                .responseId("response-" + UUID.randomUUID())
                .outputMessage(Message.builder()
                        .messageId("message-" + UUID.randomUUID())
                        .conversationId(request.getConversationId())
                        .role(MessageRole.ASSISTANT)
                        .content(content)
                        .build())
                .rawText(content)
                .build();
    }
}

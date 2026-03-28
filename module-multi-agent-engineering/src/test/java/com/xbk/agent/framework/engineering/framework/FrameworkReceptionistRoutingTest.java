package com.xbk.agent.framework.engineering.framework;

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
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 框架版接待员路由测试。
 *
 * 职责：验证框架版与手写版在路由语义上完全一致，仅通信机制不同。
 * 本测试不依赖真实 Nacos 或真实 A2A Provider，使用 Mock Facade 代替。
 *
 * <p>核心断言目标：
 * <ol>
 *   <li>技术问题 → TechSupportRemoteAgentFacade 被调用一次；
 *   <li>销售问题 → SalesRemoteAgentFacade 被调用一次；
 *   <li>结果的 intentType/specialistType 与手写版保持一致。
 * </ol>
 *
 * @author xiexu
 */
class FrameworkReceptionistRoutingTest {

    /**
     * 验证技术类问题被路由到技术专家 Facade（而非销售顾问 Facade）。
     */
    @Test
    void shouldRouteTechQuestionToTechSupportFacade() {
        TechSupportRemoteAgentFacade techFacade = mock(TechSupportRemoteAgentFacade.class);
        SalesRemoteAgentFacade salesFacade = mock(SalesRemoteAgentFacade.class);
        when(techFacade.call(anyString(), anyString()))
                .thenReturn("技术专家诊断：请检查 Bean 注入配置，NullPointerException 通常源于依赖未正确注入。");

        FrameworkReceptionistService service = createService(techFacade, salesFacade, "TECH_SUPPORT");
        EngineeringRunResult result = service.handle("我的服务启动时报 NullPointerException，帮我排查。");

        assertEquals(CustomerIntentType.TECH_SUPPORT, result.getIntentType());
        assertEquals(SpecialistType.TECH_SUPPORT, result.getSpecialistType());
        assertTrue(result.getSpecialistResponse().contains("技术专家"));
        // 只有技术 Facade 被调用，销售 Facade 未调用
        verify(techFacade, times(1)).call(anyString(), anyString());
        verify(salesFacade, times(0)).call(anyString(), anyString());
    }

    /**
     * 验证销售类问题被路由到销售顾问 Facade（而非技术专家 Facade）。
     */
    @Test
    void shouldRouteSalesQuestionToSalesFacade() {
        TechSupportRemoteAgentFacade techFacade = mock(TechSupportRemoteAgentFacade.class);
        SalesRemoteAgentFacade salesFacade = mock(SalesRemoteAgentFacade.class);
        when(salesFacade.call(anyString(), anyString()))
                .thenReturn("销售顾问建议：企业版支持私有化部署，报价可按节点规模分层提供。");

        FrameworkReceptionistService service = createService(techFacade, salesFacade, "SALES_CONSULTING");
        EngineeringRunResult result = service.handle("我想了解企业版购买方案和报价。");

        assertEquals(CustomerIntentType.SALES_CONSULTING, result.getIntentType());
        assertEquals(SpecialistType.SALES, result.getSpecialistType());
        assertTrue(result.getSpecialistResponse().contains("销售顾问"));
        verify(salesFacade, times(1)).call(anyString(), anyString());
        verify(techFacade, times(0)).call(anyString(), anyString());
    }

    /**
     * 验证结果结构包含完整字段（conversationId、routeTrail、finalResponse 等）。
     */
    @Test
    void resultShouldContainCompleteStructure() {
        TechSupportRemoteAgentFacade techFacade = mock(TechSupportRemoteAgentFacade.class);
        SalesRemoteAgentFacade salesFacade = mock(SalesRemoteAgentFacade.class);
        when(techFacade.call(anyString(), anyString())).thenReturn("技术专家回答");

        FrameworkReceptionistService service = createService(techFacade, salesFacade, "TECH_SUPPORT");
        EngineeringRunResult result = service.handle("帮我排查服务异常");

        assertNotNull(result.getConversationId());
        assertNotNull(result.getRoutingDecision());
        assertNotNull(result.getRouteTrail());
        assertFalse(result.getRouteTrail().isEmpty());
        assertNotNull(result.getFinalResponse());
    }

    /**
     * 创建使用脚本化网关和 mock Facade 的接待员服务。
     *
     * @param techFacade mock 技术 Facade
     * @param salesFacade mock 销售 Facade
     * @param intentResponse 意图分类返回值
     * @return 接待员服务
     */
    private FrameworkReceptionistService createService(TechSupportRemoteAgentFacade techFacade,
                                                        SalesRemoteAgentFacade salesFacade,
                                                        String intentResponse) {
        AgentLlmGateway gateway = new ScriptedGateway(intentResponse);
        CustomerIntentClassifier classifier = new CustomerIntentClassifier(gateway);
        return new FrameworkReceptionistService(classifier, techFacade, salesFacade);
    }

    /**
     * 验证 result.getRouteTrail() 不为空。
     */
    private static void assertFalse(boolean condition) {
        assertTrue(!condition);
    }

    /**
     * 脚本化 LLM 网关，固定返回指定意图分类结果。
     */
    private static final class ScriptedGateway implements AgentLlmGateway {

        private final String intentResponse;

        private ScriptedGateway(String intentResponse) {
            this.intentResponse = intentResponse;
        }

        @Override
        public LlmResponse chat(LlmRequest request) {
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId("response-" + UUID.randomUUID())
                    .outputMessage(Message.builder()
                            .messageId("message-" + UUID.randomUUID())
                            .conversationId(request.getConversationId())
                            .role(MessageRole.ASSISTANT)
                            .content(intentResponse)
                            .build())
                    .rawText(intentResponse)
                    .build();
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
}

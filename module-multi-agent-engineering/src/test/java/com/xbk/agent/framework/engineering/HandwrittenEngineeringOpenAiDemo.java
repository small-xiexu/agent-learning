package com.xbk.agent.framework.engineering;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.application.routing.CustomerIntentClassifier;
import com.xbk.agent.framework.engineering.config.OpenAiEngineeringDemoPropertySupport;
import com.xbk.agent.framework.engineering.config.OpenAiEngineeringDemoTestConfig;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenReceptionistAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenSalesAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenTechSupportAgent;
import com.xbk.agent.framework.engineering.handwritten.coordinator.HandwrittenEngineeringCoordinator;
import com.xbk.agent.framework.engineering.handwritten.hub.InMemoryMessageHub;
import com.xbk.agent.framework.engineering.handwritten.runtime.ConversationContextStore;
import com.xbk.agent.framework.engineering.handwritten.runtime.PendingResponseRegistry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 手写版工程模块真实 OpenAI Demo。
 *
 * 职责：让统一 AgentLlmGateway 驱动接待员、技术专家和销售顾问之间的消息协作闭环。
 *
 * @author xiexu
 */
class HandwrittenEngineeringOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(HandwrittenEngineeringOpenAiDemo.class.getName());
    private static final String CUSTOMER_REQUEST = "我们的 Spring Boot 服务启动时报 NullPointerException，请帮我先判断根因并给出排查建议。";

    /**
     * 验证手写版工程模块可以通过真实 OpenAI 模型完成消息驱动客服路由。
     */
    @Test
    void shouldRunHandwrittenEngineeringAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiEngineeringDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.engineering.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiEngineeringDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            HandwrittenEngineeringCoordinator coordinator = createCoordinator(context.getBean(AgentLlmGateway.class));

            EngineeringRunResult result = coordinator.run(CUSTOMER_REQUEST);

            EngineeringDemoLogSupport.logHandwrittenResult(LOGGER,
                    "Handwritten Engineering + OpenAI(gpt-4o)",
                    result);

            assertFalse(result.getFinalResponse().isBlank());
            assertFalse(result.getRouteTrail().isEmpty());
            assertFalse(result.getTrace().getDeliveryRecords().isEmpty());
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
     *
     * @return Spring 上下文
     */
    private ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(OpenAiEngineeringDemoTestConfig.class)
                .profiles("openai-engineering-demo")
                .web(WebApplicationType.NONE)
                .run();
    }

    /**
     * 创建手写版工程协调器。
     *
     * @param agentLlmGateway 统一网关
     * @return 协调器
     */
    private HandwrittenEngineeringCoordinator createCoordinator(AgentLlmGateway agentLlmGateway) {
        InMemoryMessageHub messageHub = new InMemoryMessageHub();
        PendingResponseRegistry pendingResponseRegistry = new PendingResponseRegistry();
        ConversationContextStore contextStore = new ConversationContextStore();
        CustomerIntentClassifier classifier = new CustomerIntentClassifier(agentLlmGateway);
        HandwrittenReceptionistAgent receptionistAgent = new HandwrittenReceptionistAgent(
                agentLlmGateway,
                classifier,
                messageHub,
                pendingResponseRegistry,
                contextStore);
        HandwrittenTechSupportAgent techSupportAgent = new HandwrittenTechSupportAgent(agentLlmGateway, messageHub);
        HandwrittenSalesAgent salesAgent = new HandwrittenSalesAgent(agentLlmGateway, messageHub);
        return new HandwrittenEngineeringCoordinator(
                messageHub,
                receptionistAgent,
                techSupportAgent,
                salesAgent,
                pendingResponseRegistry,
                contextStore);
    }
}

package com.xbk.agent.framework.engineering;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.application.routing.CustomerIntentClassifier;
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
import org.springframework.core.env.Environment;

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
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            Environment environment = context.getEnvironment();
            String apiKey = environment.getProperty("llm.api-key");
            Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank()
                            && !"your-openai-api-key".equals(apiKey.trim()),
                    "需要在 application-llm-local.yml 中配置真实 llm.api-key");
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
                .properties("spring.config.import=optional:classpath:application-llm-local.yml")
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
        // ── 第一层：消息总线 ──────────────────────────────────────────────────────
        // 所有 Agent 通过 messageHub 发布和订阅消息，彼此之间没有任何直接依赖。
        // 这是整套手写版架构的核心：消息驱动解耦。
        InMemoryMessageHub messageHub = new InMemoryMessageHub();

        // ── 第二层：运行时状态 ────────────────────────────────────────────────────
        // pendingResponseRegistry：Coordinator 在这里放 Future 等结果，
        //   Receptionist 处理完后调用 complete() 把结果写回来唤醒 Coordinator。
        PendingResponseRegistry pendingResponseRegistry = new PendingResponseRegistry();
        // contextStore：跨消息边界保存会话状态（原始请求、路由决策、路由轨迹），
        //   因为 Receptionist 要先处理用户请求、再处理专家回包，两次调用之间状态不能丢。
        ConversationContextStore contextStore = new ConversationContextStore();

        // ── 第三层：意图分类器 ────────────────────────────────────────────────────
        // 调用真实 LLM 判断用户诉求是技术支持还是销售咨询。
        // 手写版与框架版共用同一个 classifier，路由判断逻辑完全一致。
        CustomerIntentClassifier classifier = new CustomerIntentClassifier(agentLlmGateway);

        // ── 第四层：各 Agent ─────────────────────────────────────────────────────
        // Receptionist：订阅 CUSTOMER_REQUEST 和 RECEPTIONIST_REPLY 两个主题。
        //   前者处理用户请求并转发给专家，后者收到专家回包后组装最终结果。
        HandwrittenReceptionistAgent receptionistAgent = new HandwrittenReceptionistAgent(
                agentLlmGateway,
                classifier,
                messageHub,
                pendingResponseRegistry,
                contextStore);
        // TechSupportAgent：只订阅 SUPPORT_TECH_REQUEST，调用 LLM 生成技术答复后按 replyTo 回包。
        HandwrittenTechSupportAgent techSupportAgent = new HandwrittenTechSupportAgent(agentLlmGateway, messageHub);
        // SalesAgent：只订阅 SUPPORT_SALES_REQUEST，调用 LLM 生成商务答复后按 replyTo 回包。
        HandwrittenSalesAgent salesAgent = new HandwrittenSalesAgent(agentLlmGateway, messageHub);

        // ── 第五层：协调器 ───────────────────────────────────────────────────────
        // Coordinator 在构造时完成所有主题订阅，之后只需调用 run() 即可驱动整条链。
        // 注意：messageHub、pendingResponseRegistry、contextStore 被多个对象共享，
        //   这是整套消息驱动系统各层之间协作的关键共享状态。
        return new HandwrittenEngineeringCoordinator(
                messageHub,
                receptionistAgent,
                techSupportAgent,
                salesAgent,
                pendingResponseRegistry,
                contextStore);
    }
}

package com.xbk.agent.framework.supervisor;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.supervisor.api.SupervisorRunResult;
import com.xbk.agent.framework.supervisor.config.OpenAiSupervisorDemoPropertySupport;
import com.xbk.agent.framework.supervisor.config.OpenAiSupervisorDemoTestConfig;
import com.xbk.agent.framework.supervisor.handwritten.coordinator.HandwrittenSupervisorCoordinator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手写版 Supervisor 真实 OpenAI 对照 Demo
 *
 * 职责：让统一 AgentLlmGateway 驱动 Supervisor 的 JSON 路由闭环，
 * 验证手写版在真实模型下也能跑通完整监督回环
 *
 * @author xiexu
 */
class HandwrittenSupervisorOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(HandwrittenSupervisorOpenAiDemo.class.getName());
    private static final String BLOG_TASK = """
            请以“Spring AI Alibaba的多智能体优势”为主题写一篇简短的博客，
            然后将其翻译成英文，最后对英文翻译进行语法和拼写审查。
            """;

    /**
     * 验证手写版 Supervisor 可以通过真实 OpenAI 模型完成内容生产流水线。
     */
    @Test
    void shouldRunHandwrittenSupervisorAgainstRealOpenAiModel() {
        // 真实 Demo 默认跳过，避免日常测试在未配 Key 的环境下误打外网。
        Assumptions.assumeTrue(OpenAiSupervisorDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.supervisor.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiSupervisorDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            HandwrittenSupervisorCoordinator coordinator = new HandwrittenSupervisorCoordinator(agentLlmGateway, 6);

            SupervisorRunResult result = coordinator.run(BLOG_TASK);

            SupervisorDemoLogSupport.logRunResult(LOGGER, "Handwritten Supervisor + OpenAI(gpt-4o)", result, null);

            assertFalse(result.getChineseDraft().isBlank());
            assertFalse(result.getEnglishTranslation().isBlank());
            assertFalse(result.getReviewedEnglish().isBlank());
            assertTrue(!result.getRouteTrail().isEmpty());
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
     *
     * @return Spring 上下文
     */
    private ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(OpenAiSupervisorDemoTestConfig.class)
                .profiles("openai-supervisor-demo")
                .web(WebApplicationType.NONE)
                .run();
    }
}

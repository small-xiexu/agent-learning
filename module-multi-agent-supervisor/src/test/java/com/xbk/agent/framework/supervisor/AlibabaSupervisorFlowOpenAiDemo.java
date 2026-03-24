package com.xbk.agent.framework.supervisor;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.supervisor.api.SupervisorRunResult;
import com.xbk.agent.framework.supervisor.config.OpenAiSupervisorDemoPropertySupport;
import com.xbk.agent.framework.supervisor.config.OpenAiSupervisorDemoTestConfig;
import com.xbk.agent.framework.supervisor.framework.agent.AlibabaSupervisorFlowAgent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI Alibaba 原生 Supervisor 真实 OpenAI 对照 Demo
 *
 * 职责：让 SupervisorAgent 在真实 OpenAI 模型下演示中心化动态路由与受控回环，
 * 验证框架版不仅能通过脚本化测试，也能在真实模型下跑通
 *
 * @author xiexu
 */
class AlibabaSupervisorFlowOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(AlibabaSupervisorFlowOpenAiDemo.class.getName());
    private static final String BLOG_TASK = """
            请以“Spring AI Alibaba的多智能体优势”为主题写一篇简短的博客，
            然后将其翻译成英文，最后对英文翻译进行语法和拼写审查。
            """;

    /**
     * 验证框架版 Supervisor 可以通过真实 OpenAI 模型完成内容生产流水线。
     */
    @Test
    void shouldRunAlibabaSupervisorAgainstRealOpenAiModel() {
        // 真实 Demo 默认跳过，避免日常测试在未配 Key 的环境下误打外网。
        Assumptions.assumeTrue(OpenAiSupervisorDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.supervisor.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiSupervisorDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            AlibabaSupervisorFlowAgent agent = new AlibabaSupervisorFlowAgent(agentLlmGateway, 6);

            SupervisorRunResult result = agent.run(BLOG_TASK);
            OverAllState state = result.getFlowState().orElseThrow();

            SupervisorDemoLogSupport.logRunResult(LOGGER, "SupervisorAgent + OpenAI(gpt-4o)", result, state);

            assertFalse(result.getChineseDraft().isBlank());
            assertFalse(result.getEnglishTranslation().isBlank());
            assertFalse(result.getReviewedEnglish().isBlank());
            assertTrue(result.getFlowState().isPresent());
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

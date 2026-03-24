package com.xbk.agent.framework.supervisor.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAI Supervisor Demo 配置判断测试
 *
 * 职责：验证真实 Demo 的 Key 检查和开关判断可以从 supervisor 模块自己的配置文件读取。
 * 这组测试关注的是 Demo 安全开关，不是 Supervisor 路由行为本身。
 *
 * @author xiexu
 */
class OpenAiSupervisorDemoPropertySupportTest {

    /**
     * 验证非空 API Key 会被识别为已配置。
     */
    @Test
    void shouldTreatNonBlankLlmApiKeyPropertyAsConfigured() {
        MockEnvironment environment = new MockEnvironment().withProperty("llm.api-key", "test-key");
        assertTrue(OpenAiSupervisorDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证空白 API Key 不应视为已配置。
     */
    @Test
    void shouldTreatBlankLlmApiKeyPropertyAsMissing() {
        MockEnvironment environment = new MockEnvironment().withProperty("llm.api-key", "   ");
        assertFalse(OpenAiSupervisorDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证占位 API Key 不应视为真实 Key。
     */
    @Test
    void shouldTreatExamplePlaceholderApiKeyAsMissing() {
        MockEnvironment environment = new MockEnvironment().withProperty("llm.api-key", "your-openai-api-key");
        assertFalse(OpenAiSupervisorDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证支持类可以从 Demo 配置文件中解析 llm.api-key。
     *
     * @throws Exception 读取配置失败时抛出异常
     */
    @Test
    void shouldLoadApiKeyFromDemoConfigFiles() throws Exception {
        String mainConfig = "openai-supervisor-demo-fixture/application-openai-supervisor-demo.yml";
        String localConfig = "openai-supervisor-demo-fixture/application-openai-supervisor-demo-local.yml";

        assertEquals("fixture-supervisor-key",
                OpenAiSupervisorDemoPropertySupport.loadEnvironment(mainConfig, localConfig).getProperty("llm.api-key"));
        assertTrue(OpenAiSupervisorDemoPropertySupport.hasConfiguredApiKey(mainConfig, localConfig));
        assertTrue(OpenAiSupervisorDemoPropertySupport.isDemoEnabled(mainConfig, localConfig));
    }

    /**
     * 验证 true 开关会启用真实 Demo。
     */
    @Test
    void shouldTreatTrueDemoEnabledPropertyAsEnabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.supervisor.openai.enabled", "true");
        assertTrue(OpenAiSupervisorDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证 false 开关不会启用真实 Demo。
     */
    @Test
    void shouldTreatFalseDemoEnabledPropertyAsDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.supervisor.openai.enabled", "false");
        assertFalse(OpenAiSupervisorDemoPropertySupport.isDemoEnabled(environment));
    }
}

package com.xbk.agent.framework.planreplan.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAI Plan-and-Solve Demo 配置判断测试
 *
 * 职责：验证真实 Demo 的 Key 检查和开关判断可以从模块自己的配置文件读取
 *
 * @author xiexu
 */
class OpenAiPlanSolveDemoPropertySupportTest {

    /**
     * 验证只要 Spring 环境里存在非空 llm.api-key，就视为已配置。
     */
    @Test
    void shouldTreatNonBlankLlmApiKeyPropertyAsConfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("llm.api-key", "test-key");

        assertTrue(OpenAiPlanSolveDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证空白 llm.api-key 不应被视为已配置。
     */
    @Test
    void shouldTreatBlankLlmApiKeyPropertyAsMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("llm.api-key", "   ");

        assertFalse(OpenAiPlanSolveDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证示例占位值不应被视为真实可用 Key。
     */
    @Test
    void shouldTreatExamplePlaceholderApiKeyAsMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("llm.api-key", "your-openai-api-key");

        assertFalse(OpenAiPlanSolveDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证支持类可以从 Demo 配置文件中解析 llm.api-key。
     *
     * @throws Exception 加载资源失败时抛出异常
     */
    @Test
    void shouldLoadApiKeyFromDemoConfigFiles() throws Exception {
        String mainConfig = "openai-plan-solve-demo-fixture/application-openai-plan-solve-demo.yml";
        String localConfig = "openai-plan-solve-demo-fixture/application-openai-plan-solve-demo-local.yml";

        assertEquals("fixture-plan-solve-key",
                OpenAiPlanSolveDemoPropertySupport.loadEnvironment(mainConfig, localConfig).getProperty("llm.api-key"));
        assertTrue(OpenAiPlanSolveDemoPropertySupport.hasConfiguredApiKey(mainConfig, localConfig));
        assertTrue(OpenAiPlanSolveDemoPropertySupport.isDemoEnabled(mainConfig, localConfig));
    }

    /**
     * 验证只有开关为 true 时才视为允许执行真实 Demo。
     */
    @Test
    void shouldTreatTrueDemoEnabledPropertyAsEnabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.plan-solve.openai.enabled", "true");

        assertTrue(OpenAiPlanSolveDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证默认或 false 开关不应启用真实 Demo。
     */
    @Test
    void shouldTreatFalseDemoEnabledPropertyAsDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.plan-solve.openai.enabled", "false");

        assertFalse(OpenAiPlanSolveDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证 Demo 配置加载不应回退到系统属性。
     *
     * @throws Exception 加载资源失败时抛出异常
     */
    @Test
    void shouldIgnoreSystemPropertyFallbacks() throws Exception {
        String mainConfig = "openai-plan-solve-demo-fixture/application-openai-plan-solve-demo.yml";
        String originalApiKey = System.getProperty("llm.api-key");
        String originalEnabled = System.getProperty("demo.plan-solve.openai.enabled");
        System.setProperty("llm.api-key", "system-property-key");
        System.setProperty("demo.plan-solve.openai.enabled", "true");
        try {
            assertFalse(OpenAiPlanSolveDemoPropertySupport.hasConfiguredApiKey(mainConfig, null));
            assertFalse(OpenAiPlanSolveDemoPropertySupport.isDemoEnabled(mainConfig, null));
        } finally {
            restoreSystemProperty("llm.api-key", originalApiKey);
            restoreSystemProperty("demo.plan-solve.openai.enabled", originalEnabled);
        }
    }

    /**
     * 恢复指定系统属性的原始值。
     *
     * @param key 属性名
     * @param value 原始值
     */
    private void restoreSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }
}

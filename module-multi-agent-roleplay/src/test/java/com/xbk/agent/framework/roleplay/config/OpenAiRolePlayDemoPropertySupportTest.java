package com.xbk.agent.framework.roleplay.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAI RolePlay Demo 配置判断测试
 *
 * 职责：验证真实 Demo 的 Key 检查和开关判断可以从 CAMEL 模块自己的配置文件读取
 *
 * @author xiexu
 */
class OpenAiRolePlayDemoPropertySupportTest {

    /**
     * 验证只要 Spring 环境里存在非空 llm.api-key，就视为已配置。
     */
    @Test
    void shouldTreatNonBlankLlmApiKeyPropertyAsConfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("llm.api-key", "test-key");

        assertTrue(OpenAiRolePlayDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证空白 llm.api-key 不应被视为已配置。
     */
    @Test
    void shouldTreatBlankLlmApiKeyPropertyAsMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("llm.api-key", "   ");

        assertFalse(OpenAiRolePlayDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证示例占位值不应被视为真实可用 Key。
     */
    @Test
    void shouldTreatExamplePlaceholderApiKeyAsMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("llm.api-key", "your-openai-api-key");

        assertFalse(OpenAiRolePlayDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证支持类可以从 Demo 配置文件中解析 llm.api-key。
     *
     * @throws Exception 加载资源失败时抛出异常
     */
    @Test
    void shouldLoadApiKeyFromDemoConfigFiles() throws Exception {
        String mainConfig = "openai-roleplay-demo-fixture/application-openai-roleplay-demo.yml";

        assertEquals("fixture-roleplay-key",
                OpenAiRolePlayDemoPropertySupport.loadEnvironment(mainConfig, null).getProperty("llm.api-key"));
        assertTrue(OpenAiRolePlayDemoPropertySupport.hasConfiguredApiKey(mainConfig, null));
        assertTrue(OpenAiRolePlayDemoPropertySupport.isDemoEnabled(mainConfig, null));
    }

    /**
     * 验证只有开关为 true 时才视为允许执行真实 Demo。
     */
    @Test
    void shouldTreatTrueDemoEnabledPropertyAsEnabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.roleplay.openai.enabled", "true");

        assertTrue(OpenAiRolePlayDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证默认或 false 开关不应启用真实 Demo。
     */
    @Test
    void shouldTreatFalseDemoEnabledPropertyAsDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.roleplay.openai.enabled", "false");

        assertFalse(OpenAiRolePlayDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证主配置保持模板态，只显式导入共享 LLM 本地配置和 Demo 本地开关配置。
     *
     * @throws Exception 加载资源失败时抛出异常
     */
    @Test
    void shouldKeepMainConfigAsTemplateAndImportSharedLocalFiles() throws Exception {
        Resource resource = new ClassPathResource("application-openai-roleplay-demo.yml");
        assertNotNull(resource);

        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(resource.getFilename(), resource);
        MockEnvironment environment = new MockEnvironment();
        for (int index = propertySources.size() - 1; index >= 0; index--) {
            environment.getPropertySources().addLast(propertySources.get(index));
        }

        assertEquals("optional:application-llm-local.yml", environment.getProperty("spring.config.import[0]"));
        assertEquals("optional:application-openai-roleplay-demo-local.yml",
                environment.getProperty("spring.config.import[1]"));
        assertEquals("https://api.openai.com", environment.getProperty("llm.base-url"));
        assertFalse(OpenAiRolePlayDemoPropertySupport.hasConfiguredApiKey(environment));
        assertFalse(OpenAiRolePlayDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证 Demo 配置加载不应回退到系统属性。
     *
     * @throws Exception 加载资源失败时抛出异常
     */
    @Test
    void shouldIgnoreSystemPropertyFallbacks() throws Exception {
        String mainConfig = "openai-roleplay-demo-fixture/missing.yml";
        String originalApiKey = System.getProperty("llm.api-key");
        String originalEnabled = System.getProperty("demo.roleplay.openai.enabled");
        System.setProperty("llm.api-key", "system-property-key");
        System.setProperty("demo.roleplay.openai.enabled", "true");
        try {
            assertFalse(OpenAiRolePlayDemoPropertySupport.hasConfiguredApiKey(mainConfig, null));
            assertFalse(OpenAiRolePlayDemoPropertySupport.isDemoEnabled(mainConfig, null));
        } finally {
            restoreSystemProperty("llm.api-key", originalApiKey);
            restoreSystemProperty("demo.roleplay.openai.enabled", originalEnabled);
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

package com.xbk.agent.framework.react.config;

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
 * OpenAI ReAct Demo 配置判断测试
 *
 * 职责：验证真实 Demo 的 Key 检查可以从 Spring 配置读取，而不只依赖环境变量
 *
 * @author xiexu
 */
class OpenAiReactDemoPropertySupportTest {

    /**
     * 验证只要 Spring 环境里存在非空 llm.api-key，就视为已配置。
     */
    @Test
    void shouldTreatNonBlankLlmApiKeyPropertyAsConfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("llm.api-key", "test-key");

        assertTrue(OpenAiReactDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证空白 llm.api-key 不应被视为已配置。
     */
    @Test
    void shouldTreatBlankLlmApiKeyPropertyAsMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("llm.api-key", "   ");

        assertFalse(OpenAiReactDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证示例占位值不应被视为真实可用 Key。
     */
    @Test
    void shouldTreatExamplePlaceholderApiKeyAsMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("llm.api-key", "your-openai-api-key");

        assertFalse(OpenAiReactDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证支持类可以从 Demo 配置文件中解析 llm.api-key。
     *
     * @throws Exception 加载资源失败时抛出异常
     */
    @Test
    void shouldLoadApiKeyFromDemoConfigFiles() throws Exception {
        String mainConfig = "openai-react-demo-fixture/application-openai-react-demo.yml";

        assertEquals("fixture-react-key",
                OpenAiReactDemoPropertySupport.loadEnvironment(mainConfig, null).getProperty("llm.api-key"));
        assertTrue(OpenAiReactDemoPropertySupport.hasConfiguredApiKey(mainConfig, null));
        assertTrue(OpenAiReactDemoPropertySupport.isDemoEnabled(mainConfig, null));
    }

    /**
     * 验证只有开关为 true 时才视为允许执行真实 Demo。
     */
    @Test
    void shouldTreatTrueDemoEnabledPropertyAsEnabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.react.openai.enabled", "true");

        assertTrue(OpenAiReactDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证默认或 false 开关不应启用真实 Demo。
     */
    @Test
    void shouldTreatFalseDemoEnabledPropertyAsDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.react.openai.enabled", "false");

        assertFalse(OpenAiReactDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证主配置保持模板态，只显式导入共享 LLM 本地配置和 Demo 本地开关配置。
     *
     * @throws Exception 加载资源失败时抛出异常
     */
    @Test
    void shouldKeepMainConfigAsTemplateAndImportSharedLocalFiles() throws Exception {
        Resource resource = new ClassPathResource("application-openai-react-demo.yml");
        assertNotNull(resource);

        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(resource.getFilename(), resource);
        MockEnvironment environment = new MockEnvironment();
        for (int index = propertySources.size() - 1; index >= 0; index--) {
            environment.getPropertySources().addLast(propertySources.get(index));
        }

        assertEquals("optional:application-llm-local.yml", environment.getProperty("spring.config.import[0]"));
        assertEquals("optional:application-openai-react-demo-local.yml",
                environment.getProperty("spring.config.import[1]"));
        assertEquals("https://api.openai.com", environment.getProperty("llm.base-url"));
        assertFalse(OpenAiReactDemoPropertySupport.hasConfiguredApiKey(environment));
        assertFalse(OpenAiReactDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证 Demo 配置加载不应回退到系统属性。
     *
     * @throws Exception 加载资源失败时抛出异常
     */
    @Test
    void shouldIgnoreSystemPropertyFallbacks() throws Exception {
        String mainConfig = "openai-react-demo-fixture/missing.yml";
        String originalApiKey = System.getProperty("llm.api-key");
        String originalEnabled = System.getProperty("demo.react.openai.enabled");
        System.setProperty("llm.api-key", "system-property-key");
        System.setProperty("demo.react.openai.enabled", "true");
        try {
            assertFalse(OpenAiReactDemoPropertySupport.hasConfiguredApiKey(mainConfig, null));
            assertFalse(OpenAiReactDemoPropertySupport.isDemoEnabled(mainConfig, null));
        } finally {
            restoreSystemProperty("llm.api-key", originalApiKey);
            restoreSystemProperty("demo.react.openai.enabled", originalEnabled);
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

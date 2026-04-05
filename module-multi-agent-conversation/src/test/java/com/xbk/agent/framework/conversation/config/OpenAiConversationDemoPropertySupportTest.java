package com.xbk.agent.framework.conversation.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAI Conversation Demo 配置判断测试
 *
 * 职责：验证真实 Demo 的 Key 检查和开关判断可以从 conversation 模块自己的配置文件读取
 *
 * @author xiexu
 */
class OpenAiConversationDemoPropertySupportTest {

    /**
     * 验证非空 API Key 会被识别为已配置。
     */
    @Test
    void shouldTreatNonBlankLlmApiKeyPropertyAsConfigured() {
        MockEnvironment environment = new MockEnvironment().withProperty("llm.api-key", "test-key");
        assertTrue(OpenAiConversationDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证空白 API Key 不应视为已配置。
     */
    @Test
    void shouldTreatBlankLlmApiKeyPropertyAsMissing() {
        MockEnvironment environment = new MockEnvironment().withProperty("llm.api-key", "   ");
        assertFalse(OpenAiConversationDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证占位 API Key 不应视为真实 Key。
     */
    @Test
    void shouldTreatExamplePlaceholderApiKeyAsMissing() {
        MockEnvironment environment = new MockEnvironment().withProperty("llm.api-key", "your-openai-api-key");
        assertFalse(OpenAiConversationDemoPropertySupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证支持类可以从 Demo 配置文件中解析 llm.api-key。
     *
     * @throws Exception 读取配置失败时抛出异常
     */
    @Test
    void shouldLoadApiKeyFromDemoConfigFiles() throws Exception {
        String mainConfig = "openai-conversation-demo-fixture/application-openai-conversation-demo.yml";

        assertEquals("fixture-conversation-key",
                OpenAiConversationDemoPropertySupport.loadEnvironment(mainConfig, null).getProperty("llm.api-key"));
        assertTrue(OpenAiConversationDemoPropertySupport.hasConfiguredApiKey(mainConfig, null));
        assertTrue(OpenAiConversationDemoPropertySupport.isDemoEnabled(mainConfig, null));
    }

    /**
     * 验证 fixture 的共享 LLM 本地配置与真实模板格式保持一致。
     *
     * @throws Exception 读取配置失败时抛出异常
     */
    @Test
    void shouldKeepFixtureLocalConfigAlignedWithRealLocalConfig() throws Exception {
        Resource resource = new ClassPathResource("openai-conversation-demo-fixture/application-llm-local.yml");
        assertNotNull(resource);

        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(resource.getFilename(), resource);
        MockEnvironment environment = new MockEnvironment();
        for (int index = propertySources.size() - 1; index >= 0; index--) {
            environment.getPropertySources().addLast(propertySources.get(index));
        }

        assertEquals("/v1/chat/completions", environment.getProperty("llm.chat-completions-path"));
    }

    /**
     * 验证主配置保持模板态，真实值应放到 local 配置中覆盖。
     *
     * @throws Exception 读取配置失败时抛出异常
     */
    @Test
    void shouldKeepMainConfigAsTemplateOnly() throws Exception {
        Resource resource = new ClassPathResource("application-openai-conversation-demo.yml");
        assertNotNull(resource);

        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(resource.getFilename(), resource);
        MockEnvironment environment = new MockEnvironment();
        for (int index = propertySources.size() - 1; index >= 0; index--) {
            environment.getPropertySources().addLast(propertySources.get(index));
        }

        assertEquals("optional:application-llm-local.yml", environment.getProperty("spring.config.import[0]"));
        assertEquals("optional:application-openai-conversation-demo-local.yml",
                environment.getProperty("spring.config.import[1]"));
        assertEquals("https://api.openai.com", environment.getProperty("llm.base-url"));
        assertFalse(OpenAiConversationDemoPropertySupport.hasConfiguredApiKey(environment));
        assertFalse(OpenAiConversationDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证 true 开关会启用真实 Demo。
     */
    @Test
    void shouldTreatTrueDemoEnabledPropertyAsEnabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.conversation.openai.enabled", "true");
        assertTrue(OpenAiConversationDemoPropertySupport.isDemoEnabled(environment));
    }

    /**
     * 验证 false 开关不会启用真实 Demo。
     */
    @Test
    void shouldTreatFalseDemoEnabledPropertyAsDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("demo.conversation.openai.enabled", "false");
        assertFalse(OpenAiConversationDemoPropertySupport.isDemoEnabled(environment));
    }
}

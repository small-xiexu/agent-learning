package com.xbk.agent.framework.conversation.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        String localConfig = "openai-conversation-demo-fixture/application-openai-conversation-demo-local.yml";

        assertEquals("fixture-conversation-key",
                OpenAiConversationDemoPropertySupport.loadEnvironment(mainConfig, localConfig).getProperty("llm.api-key"));
        assertTrue(OpenAiConversationDemoPropertySupport.hasConfiguredApiKey(mainConfig, localConfig));
        assertTrue(OpenAiConversationDemoPropertySupport.isDemoEnabled(mainConfig, localConfig));
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

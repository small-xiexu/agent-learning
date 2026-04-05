package com.xbk.agent.framework.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAiDemoConfigSupport 测试
 *
 * 职责：验证共享 Demo 配置支持类的递归导入、本地覆盖与公共属性判断行为
 *
 * @author xiexu
 */
class OpenAiDemoConfigSupportTest {

    /**
     * 验证递归 import 与相对路径解析后，仍可读取本地 LLM 配置与 Demo 开关。
     *
     * @throws Exception 加载配置失败时抛出异常
     */
    @Test
    void shouldLoadApiKeyAndDemoEnabledFromRecursiveImports() throws Exception {
        String mainConfig = "openai-demo-config-support-fixture/recursive/application-openai-demo.yml";

        assertEquals("fixture-shared-key",
                OpenAiDemoConfigSupport.loadEnvironment(mainConfig, null).getProperty("llm.api-key"));
        assertEquals("/v1/chat/completions",
                OpenAiDemoConfigSupport.loadEnvironment(mainConfig, null).getProperty("llm.chat-completions-path"));
        assertTrue(OpenAiDemoConfigSupport.hasConfiguredApiKey(mainConfig, null));
        assertTrue(OpenAiDemoConfigSupport.isDemoEnabled(mainConfig, null, "demo.sample.openai.enabled"));
    }

    /**
     * 验证显式传入的 local 配置文件仍保持高于主配置的优先级。
     *
     * @throws Exception 加载配置失败时抛出异常
     */
    @Test
    void shouldAllowExplicitLocalConfigToOverrideMainTemplate() throws Exception {
        String mainConfig = "openai-demo-config-support-fixture/explicit/application-openai-demo.yml";
        String localConfig = "openai-demo-config-support-fixture/explicit/application-openai-demo-local.yml";

        assertEquals("explicit-local-key",
                OpenAiDemoConfigSupport.loadEnvironment(mainConfig, localConfig).getProperty("llm.api-key"));
        assertTrue(OpenAiDemoConfigSupport.hasConfiguredApiKey(mainConfig, localConfig));
        assertTrue(OpenAiDemoConfigSupport.isDemoEnabled(mainConfig, localConfig, "demo.sample.openai.enabled"));
    }

    /**
     * 验证示例占位值不应视为真实 API Key。
     */
    @Test
    void shouldTreatExampleApiKeyAsMissing() {
        MockEnvironment environment = new MockEnvironment().withProperty("llm.api-key", "your-openai-api-key");

        assertFalse(OpenAiDemoConfigSupport.hasConfiguredApiKey(environment));
    }

    /**
     * 验证空白开关值不会启用真实 Demo。
     */
    @Test
    void shouldTreatMissingDemoEnabledPropertyAsDisabled() {
        MockEnvironment environment = new MockEnvironment();

        assertFalse(OpenAiDemoConfigSupport.isDemoEnabled(environment, "demo.sample.openai.enabled"));
    }
}

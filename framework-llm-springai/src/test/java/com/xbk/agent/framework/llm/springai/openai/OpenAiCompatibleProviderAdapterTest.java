package com.xbk.agent.framework.llm.springai.openai;

import com.xbk.agent.framework.llm.autoconfigure.LlmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * OpenAiCompatibleProviderAdapter 测试
 *
 * 职责：验证统一 llm 配置会被正确映射为 Spring AI OpenAI compatible 模型对象
 *
 * @author xiexu
 */
class OpenAiCompatibleProviderAdapterTest {

    /**
     * 验证工厂会把统一配置映射到 OpenAI API 和默认模型选项。
     */
    @Test
    void shouldMapLlmPropertiesToOpenAiApiAndDefaultOptions() {
        LlmProperties properties = new LlmProperties();
        properties.setBaseUrl("https://apis.itedus.cn");
        properties.setApiKey("test-key");
        properties.setModel("gpt-4o");
        properties.setChatCompletionsPath("/v1/chat/completions");

        OpenAiChatModel chatModel = new OpenAiCompatibleChatModelFactory().createChatModel(properties);
        OpenAiApi openAiApi = (OpenAiApi) ReflectionTestUtils.getField(chatModel, "openAiApi");
        OpenAiChatOptions options = (OpenAiChatOptions) chatModel.getDefaultOptions();

        assertEquals("https://apis.itedus.cn", ReflectionTestUtils.invokeMethod(openAiApi, "getBaseUrl"));
        assertEquals("/v1/chat/completions", ReflectionTestUtils.invokeMethod(openAiApi, "getCompletionsPath"));
        assertEquals("test-key", ((ApiKey) ReflectionTestUtils.invokeMethod(openAiApi, "getApiKey")).getValue());
        assertEquals("gpt-4o", options.getModel());
    }

    /**
     * 验证未显式配置聊天路径时回退默认值。
     */
    @Test
    void shouldUseDefaultCompletionsPathWhenPathIsBlank() {
        LlmProperties properties = new LlmProperties();
        properties.setBaseUrl("https://apis.itedus.cn");
        properties.setApiKey("test-key");
        properties.setModel("gpt-4o");

        OpenAiChatModel chatModel = new OpenAiCompatibleChatModelFactory().createChatModel(properties);
        OpenAiApi openAiApi = (OpenAiApi) ReflectionTestUtils.getField(chatModel, "openAiApi");

        assertEquals("/v1/chat/completions", ReflectionTestUtils.invokeMethod(openAiApi, "getCompletionsPath"));
    }
}

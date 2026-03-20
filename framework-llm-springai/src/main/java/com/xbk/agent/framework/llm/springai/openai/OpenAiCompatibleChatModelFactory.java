package com.xbk.agent.framework.llm.springai.openai;

import com.xbk.agent.framework.llm.autoconfigure.LlmProperties;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * OpenAI compatible ChatModel 工厂
 *
 * 职责：把统一 llm 配置转换为 Spring AI OpenAI compatible ChatModel
 *
 * @author xiexu
 */
public class OpenAiCompatibleChatModelFactory {

    static final String DEFAULT_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    /**
     * 创建 ChatModel。
     *
     * @param properties 统一配置
     * @return OpenAI compatible ChatModel
     */
    public OpenAiChatModel createChatModel(LlmProperties properties) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .completionsPath(resolveChatCompletionsPath(properties))
                .build();
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .build();
    }

    private String resolveChatCompletionsPath(LlmProperties properties) {
        String path = properties.getChatCompletionsPath();
        if (path == null || path.isBlank()) {
            return DEFAULT_CHAT_COMPLETIONS_PATH;
        }
        return path;
    }
}

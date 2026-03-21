package com.xbk.agent.framework.llm.springai.openai;

import com.xbk.agent.framework.llm.autoconfigure.LlmProperties;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/**
 * OpenAI compatible ChatModel 工厂
 *
 * 职责：在启动阶段把统一 {@link LlmProperties} 中的 `baseUrl`、`apiKey`、`model`
 * 和 `chatCompletionsPath` 转换为 Spring AI 的 {@link OpenAiChatModel}；
 * 它只负责创建底层模型对象，不负责创建统一网关
 *
 * @author xiexu
 */
public class OpenAiCompatibleChatModelFactory {

    static final String DEFAULT_CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    /**
     * 创建工厂
     */
    public OpenAiCompatibleChatModelFactory() {
    }

    /**
     * 基于统一配置创建 ChatModel
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

    /**
     * 解析聊天接口路径
     *
     * @param properties 统一配置
     * @return 聊天接口路径
     */
    private String resolveChatCompletionsPath(LlmProperties properties) {
        String path = properties.getChatCompletionsPath();
        if (path == null || path.isBlank()) {
            return DEFAULT_CHAT_COMPLETIONS_PATH;
        }
        return path;
    }
}

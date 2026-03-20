package com.xbk.agent.framework.llm.springai.autoconfigure;

import com.xbk.agent.framework.llm.autoconfigure.LlmProperties;
import com.xbk.agent.framework.llm.springai.openai.OpenAiCompatibleChatModelFactory;
import com.xbk.agent.framework.llm.springai.openai.OpenAiCompatibleProviderAdapter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring AI provider 自动装配
 *
 * 职责：按统一 provider 配置注册默认 OpenAI compatible adapter 和 ChatModel
 *
 * @author xiexu
 */
@AutoConfiguration
@ConditionalOnClass(OpenAiChatModel.class)
@EnableConfigurationProperties(LlmProperties.class)
public class SpringAiProviderAutoConfiguration {

    /**
     * 注册 OpenAI compatible ChatModel 工厂。
     *
     * @return 工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAiCompatibleChatModelFactory openAiCompatibleChatModelFactory() {
        return new OpenAiCompatibleChatModelFactory();
    }

    /**
     * 注册默认 OpenAI compatible adapter。
     *
     * @param chatModelFactory ChatModel 工厂
     * @return 默认 adapter
     */
    @Bean
    @ConditionalOnMissingBean(OpenAiCompatibleProviderAdapter.class)
    public OpenAiCompatibleProviderAdapter openAiCompatibleProviderAdapter(
            OpenAiCompatibleChatModelFactory chatModelFactory) {
        return new OpenAiCompatibleProviderAdapter(chatModelFactory);
    }

    /**
     * 在 OpenAI compatible 场景下暴露默认 ChatModel。
     *
     * @param properties 统一配置
     * @param chatModelFactory ChatModel 工厂
     * @return ChatModel
     */
    @Bean
    @ConditionalOnProperty(prefix = "llm", name = "provider", havingValue = "openai-compatible")
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel chatModel(LlmProperties properties, OpenAiCompatibleChatModelFactory chatModelFactory) {
        return chatModelFactory.createChatModel(properties);
    }
}

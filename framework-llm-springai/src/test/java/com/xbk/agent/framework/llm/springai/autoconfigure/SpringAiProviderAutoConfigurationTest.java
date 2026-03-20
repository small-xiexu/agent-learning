package com.xbk.agent.framework.llm.springai.autoconfigure;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.llm.autoconfigure.AgentLlmGatewayAutoConfiguration;
import com.xbk.agent.framework.llm.autoconfigure.ProviderAdapter;
import com.xbk.agent.framework.llm.springai.openai.OpenAiCompatibleChatModelFactory;
import com.xbk.agent.framework.llm.springai.openai.OpenAiCompatibleProviderAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * SpringAiProviderAutoConfiguration 测试
 *
 * 职责：验证 Spring AI provider 自动装配能注册 OpenAI compatible adapter 与 ChatModel
 *
 * @author xiexu
 */
class SpringAiProviderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AgentLlmGatewayAutoConfiguration.class,
                    SpringAiProviderAutoConfiguration.class))
            .withPropertyValues(
                    "llm.provider=openai-compatible",
                    "llm.base-url=https://apis.itedus.cn",
                    "llm.api-key=test-key",
                    "llm.model=gpt-4o");

    /**
     * 验证默认情况下会注册 adapter、ChatModel 和统一网关。
     */
    @Test
    void shouldRegisterProviderAdapterChatModelAndGateway() {
        contextRunner.run(context -> {
            assertNotNull(context.getBean(ProviderAdapter.class));
            assertNotNull(context.getBean(ChatModel.class));
            assertNotNull(context.getBean(AgentLlmGateway.class));
        });
    }

    /**
     * 验证已有自定义 ChatModel 时不会被默认实现覆盖。
     */
    @Test
    void shouldKeepUserProvidedChatModel() {
        contextRunner.withUserConfiguration(CustomChatModelConfiguration.class)
                .run(context -> assertSame(
                        context.getBean("customChatModel"),
                        context.getBean(ChatModel.class)));
    }

    /**
     * 验证已有自定义 OpenAI compatible adapter 时不会重复注册默认 adapter。
     */
    @Test
    void shouldNotRegisterDuplicateOpenAiCompatibleAdapter() {
        contextRunner.withUserConfiguration(CustomAdapterConfiguration.class)
                .run(context -> assertSame(
                        context.getBean("customOpenAiCompatibleProviderAdapter"),
                        context.getBean(OpenAiCompatibleProviderAdapter.class)));
    }

    /**
     * 自定义 ChatModel 配置。
     *
     * 职责：验证默认 ChatModel Bean 会被用户实现覆盖
     *
     * @author xiexu
     */
    @Configuration(proxyBeanMethods = false)
    static class CustomChatModelConfiguration {

        @Bean
        ChatModel customChatModel() {
            return new ChatModel() {
                @Override
                public ChatResponse call(Prompt prompt) {
                    throw new UnsupportedOperationException("test chat model does not call");
                }
            };
        }
    }

    /**
     * 自定义 adapter 配置。
     *
     * 职责：验证默认 OpenAI compatible adapter 可被用户实现替换
     *
     * @author xiexu
     */
    @Configuration(proxyBeanMethods = false)
    static class CustomAdapterConfiguration {

        @Bean
        OpenAiCompatibleProviderAdapter customOpenAiCompatibleProviderAdapter() {
            return new OpenAiCompatibleProviderAdapter(new OpenAiCompatibleChatModelFactory());
        }
    }
}

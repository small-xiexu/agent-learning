package com.xbk.agent.framework.react.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.DefaultResponseErrorHandler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * OpenAI ReAct Demo 配置装载测试
 *
 * 职责：验证引入 OpenAI starter 后，最小自动配置可以提供 ChatModel Bean
 *
 * @author xiexu
 */
class OpenAiReactDemoTestConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ToolCallingAutoConfiguration.class,
                    OpenAiChatAutoConfiguration.class))
            .withBean(DefaultResponseErrorHandler.class, DefaultResponseErrorHandler::new)
            .withBean(RetryTemplate.class, RetryTemplate::defaultInstance)
            .withPropertyValues(
                    "spring.ai.openai.api-key=test-key",
                    "spring.ai.openai.chat.options.model=gpt-4o");

    /**
     * 验证自动配置可以提供 ChatModel。
     */
    @Test
    void shouldLoadOpenAiChatModelBean() {
        contextRunner.run(context -> assertNotNull(context.getBean(ChatModel.class)));
    }
}

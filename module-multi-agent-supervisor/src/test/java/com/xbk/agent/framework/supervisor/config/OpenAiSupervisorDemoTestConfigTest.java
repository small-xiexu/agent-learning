package com.xbk.agent.framework.supervisor.config;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.llm.autoconfigure.AgentLlmGatewayAutoConfiguration;
import com.xbk.agent.framework.llm.springai.autoconfigure.SpringAiProviderAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * OpenAI Supervisor Demo 配置装载测试
 *
 * 职责：验证统一 llm 配置下，supervisor 模块最小自动配置可以提供 AgentLlmGateway 与 ChatModel Bean。
 * 它验证的是接模能力是否打通，而不是多 Agent 行为。
 *
 * @author xiexu
 */
class OpenAiSupervisorDemoTestConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AgentLlmGatewayAutoConfiguration.class,
                    SpringAiProviderAutoConfiguration.class))
            .withPropertyValues(
                    "llm.provider=openai-compatible",
                    "llm.base-url=https://api.openai.com",
                    "llm.api-key=test-key",
                    "llm.model=gpt-4o");

    /**
     * 验证自动配置可以提供 AgentLlmGateway 与 ChatModel。
     */
    @Test
    void shouldLoadAgentLlmGatewayAndChatModelBeans() {
        contextRunner.run(context -> {
            assertNotNull(context.getBean(AgentLlmGateway.class));
            assertNotNull(context.getBean(ChatModel.class));
        });
    }
}

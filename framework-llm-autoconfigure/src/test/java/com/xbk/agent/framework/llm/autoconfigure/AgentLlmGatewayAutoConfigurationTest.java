package com.xbk.agent.framework.llm.autoconfigure;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AgentLlmGatewayAutoConfiguration 测试
 *
 * 职责：验证统一网关会按 provider 配置自动装配，并在缺少适配器时失败
 *
 * @author xiexu
 */
class AgentLlmGatewayAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgentLlmGatewayAutoConfiguration.class));

    /**
     * 验证存在匹配 adapter 时会自动装配统一网关。
     */
    @Test
    void shouldAutoConfigureAgentLlmGatewayWhenMatchingAdapterExists() {
        contextRunner.withUserConfiguration(MatchingAdapterConfiguration.class)
                .withPropertyValues("llm.provider=openai-compatible")
                .run(context -> assertNotNull(context.getBean(AgentLlmGateway.class)));
    }

    /**
     * 验证缺少匹配 adapter 时启动失败。
     */
    @Test
    void shouldFailWhenNoMatchingAdapterExists() {
        contextRunner.withPropertyValues("llm.provider=openai-compatible")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    /**
     * 匹配 adapter 配置。
     *
     * 职责：为自动装配测试提供最小 adapter 实现
     *
     * @author xiexu
     */
    @Configuration(proxyBeanMethods = false)
    static class MatchingAdapterConfiguration {

        @Bean
        ProviderAdapter providerAdapter() {
            return new ProviderAdapter() {
                @Override
                public boolean supports(String providerId) {
                    return "openai-compatible".equals(providerId);
                }

                @Override
                public AgentLlmGateway createGateway(LlmProperties properties) {
                    return new TestAgentLlmGateway();
                }
            };
        }
    }

    /**
     * 最小网关实现。
     *
     * 职责：仅用于自动装配测试，不参与真实模型调用
     *
     * @author xiexu
     */
    private static final class TestAgentLlmGateway implements AgentLlmGateway {

        @Override
        public LlmResponse chat(LlmRequest request) {
            throw new UnsupportedOperationException("test gateway does not chat");
        }

        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("test gateway does not stream");
        }

        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("test gateway does not support structured chat");
        }

        @Override
        public Set<LlmCapability> capabilities() {
            return Set.of();
        }
    }
}

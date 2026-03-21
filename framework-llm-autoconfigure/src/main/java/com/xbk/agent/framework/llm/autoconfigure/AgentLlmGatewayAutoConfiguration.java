package com.xbk.agent.framework.llm.autoconfigure;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * AgentLlmGateway 自动装配
 *
 * 职责：在 Spring Boot 启动阶段读取统一 `llm.*` 配置，选择匹配的 {@link ProviderAdapter}，
 * 并自动装配上层统一使用的 {@link AgentLlmGateway}
 *
 * @author xiexu
 */
@AutoConfiguration
@EnableConfigurationProperties(LlmProperties.class)
public class AgentLlmGatewayAutoConfiguration {

    /**
     * 注册 provider adapter 解析器
     *
     * @param adapters 已注册 adapter
     * @return adapter 解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public ProviderAdapterResolver providerAdapterResolver(List<ProviderAdapter> adapters) {
        return new ProviderAdapterResolver(adapters);
    }

    /**
     * 按 provider 自动装配统一网关
     *
     * @param properties 统一配置
     * @param resolver adapter 解析器
     * @return 统一网关
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentLlmGateway agentLlmGateway(LlmProperties properties, ProviderAdapterResolver resolver) {
        String provider = properties.getProvider();
        if (provider == null || provider.isBlank()) {
            throw new IllegalStateException("Property 'llm.provider' must not be blank");
        }
        return resolver.getRequiredAdapter(provider).createGateway(properties);
    }
}

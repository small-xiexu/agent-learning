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
 * 职责：基于统一 llm.* 配置和 provider adapter 自动装配统一网关
 *
 * @author xiexu
 */
@AutoConfiguration
@EnableConfigurationProperties(LlmProperties.class)
public class AgentLlmGatewayAutoConfiguration {

    /**
     * 注册 provider adapter 解析器。
     *
     * @param adapters 已注册 adapter
     * @return adapter 注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public ProviderAdapterRegistry providerAdapterRegistry(List<ProviderAdapter> adapters) {
        return new ProviderAdapterRegistry(adapters);
    }

    /**
     * 自动装配统一网关。
     *
     * @param properties 统一配置
     * @param registry adapter 注册表
     * @return 统一网关
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentLlmGateway agentLlmGateway(LlmProperties properties, ProviderAdapterRegistry registry) {
        String provider = properties.getProvider();
        if (provider == null || provider.isBlank()) {
            throw new IllegalStateException("Property 'llm.provider' must not be blank");
        }
        return registry.getRequiredAdapter(provider).createGateway(properties);
    }
}

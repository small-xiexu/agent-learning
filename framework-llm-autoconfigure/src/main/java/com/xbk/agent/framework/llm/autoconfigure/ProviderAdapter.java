package com.xbk.agent.framework.llm.autoconfigure;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;

/**
 * Provider 适配器接口
 *
 * 职责：定义“某种 provider 如何接入统一网关体系”的扩展点；
 * 实现类需要回答两个问题：
 * 当前是否支持某个 provider，以及如何基于统一配置创建 {@link AgentLlmGateway}
 *
 * @author xiexu
 */
public interface ProviderAdapter {

    /**
     * 当前 adapter 是否支持指定 provider
     *
     * @param providerId provider 标识
     * @return 是否支持
     */
    boolean supports(String providerId);

    /**
     * 根据统一配置创建网关
     *
     * @param properties 统一配置
     * @return 统一网关
     */
    AgentLlmGateway createGateway(LlmProperties properties);
}

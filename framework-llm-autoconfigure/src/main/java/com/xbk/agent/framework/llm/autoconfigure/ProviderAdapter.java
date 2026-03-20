package com.xbk.agent.framework.llm.autoconfigure;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;

/**
 * ProviderAdapter
 *
 * 职责：根据统一 provider 标识与配置构建统一网关
 *
 * @author xiexu
 */
public interface ProviderAdapter {

    /**
     * 当前 adapter 是否支持指定 provider。
     *
     * @param providerId provider 标识
     * @return 是否支持
     */
    boolean supports(String providerId);

    /**
     * 根据统一配置创建网关。
     *
     * @param properties 统一配置
     * @return 统一网关
     */
    AgentLlmGateway createGateway(LlmProperties properties);
}

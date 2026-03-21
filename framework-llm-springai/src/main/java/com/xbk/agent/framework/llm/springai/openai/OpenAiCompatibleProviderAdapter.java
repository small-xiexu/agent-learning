package com.xbk.agent.framework.llm.springai.openai;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.DefaultAgentLlmGateway;
import com.xbk.agent.framework.llm.autoconfigure.LlmProperties;
import com.xbk.agent.framework.llm.autoconfigure.ProviderAdapter;
import com.xbk.agent.framework.llm.springai.adapter.SpringAiLlmClient;
import org.springframework.ai.openai.OpenAiChatModel;

/**
 * OpenAI compatible provider 适配器
 *
 * 职责：在启动阶段接管 `llm.provider=openai-compatible` 的装配流程；
 * 它负责调用 {@link OpenAiCompatibleChatModelFactory} 创建底层 {@code ChatModel}，
 * 再把该模型包装成框架统一使用的 {@link AgentLlmGateway}
 *
 * @author xiexu
 */
public class OpenAiCompatibleProviderAdapter implements ProviderAdapter {

    private static final String PROVIDER_ID = "openai-compatible";

    private final OpenAiCompatibleChatModelFactory chatModelFactory;

    /**
     * 创建适配器
     *
     * @param chatModelFactory ChatModel 工厂
     */
    public OpenAiCompatibleProviderAdapter(OpenAiCompatibleChatModelFactory chatModelFactory) {
        this.chatModelFactory = chatModelFactory;
    }

    /**
     * 判断当前 adapter 是否支持指定 provider
     *
     * @param providerId provider 标识
     * @return 是否支持
     */
    @Override
    public boolean supports(String providerId) {
        return PROVIDER_ID.equals(providerId);
    }

    /**
     * 基于统一配置创建框架统一网关
     *
     * @param properties 统一配置
     * @return 统一网关
     */
    @Override
    public AgentLlmGateway createGateway(LlmProperties properties) {
        OpenAiChatModel openAiChatModel = chatModelFactory.createChatModel(properties);
        SpringAiLlmClient springAiLlmClient = new SpringAiLlmClient(openAiChatModel);
        return new DefaultAgentLlmGateway(springAiLlmClient);
    }
}

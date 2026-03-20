package com.xbk.agent.framework.llm.springai.openai;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.DefaultAgentLlmGateway;
import com.xbk.agent.framework.llm.autoconfigure.LlmProperties;
import com.xbk.agent.framework.llm.autoconfigure.ProviderAdapter;
import com.xbk.agent.framework.llm.springai.adapter.SpringAiLlmClient;

/**
 * OpenAI compatible provider adapter
 *
 * 职责：基于统一 llm 配置创建 OpenAI compatible 统一网关
 *
 * @author xiexu
 */
public class OpenAiCompatibleProviderAdapter implements ProviderAdapter {

    private static final String PROVIDER_ID = "openai-compatible";

    private final OpenAiCompatibleChatModelFactory chatModelFactory;

    public OpenAiCompatibleProviderAdapter(OpenAiCompatibleChatModelFactory chatModelFactory) {
        this.chatModelFactory = chatModelFactory;
    }

    @Override
    public boolean supports(String providerId) {
        return PROVIDER_ID.equals(providerId);
    }

    @Override
    public AgentLlmGateway createGateway(LlmProperties properties) {
        return new DefaultAgentLlmGateway(new SpringAiLlmClient(chatModelFactory.createChatModel(properties)));
    }
}

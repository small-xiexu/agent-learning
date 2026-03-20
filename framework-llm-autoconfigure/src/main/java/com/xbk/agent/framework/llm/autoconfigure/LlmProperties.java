package com.xbk.agent.framework.llm.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一 LLM 配置
 *
 * 职责：承载统一的 llm.* 配置
 *
 * @author xiexu
 */
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    private String chatCompletionsPath;
    private Duration timeout;
    private LlmCapabilitiesProperties capabilities = new LlmCapabilitiesProperties();
    private Map<String, Object> providerOptions = new LinkedHashMap<String, Object>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getChatCompletionsPath() {
        return chatCompletionsPath;
    }

    public void setChatCompletionsPath(String chatCompletionsPath) {
        this.chatCompletionsPath = chatCompletionsPath;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public LlmCapabilitiesProperties getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(LlmCapabilitiesProperties capabilities) {
        this.capabilities = capabilities == null ? new LlmCapabilitiesProperties() : capabilities;
    }

    public Map<String, Object> getProviderOptions() {
        return providerOptions;
    }

    public void setProviderOptions(Map<String, Object> providerOptions) {
        this.providerOptions = providerOptions == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(providerOptions);
    }
}

package com.xbk.agent.framework.core.llm;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.exception.UnsupportedCapabilityException;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmClient;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.llm.spi.StreamingLlmClient;
import com.xbk.agent.framework.core.llm.spi.StructuredOutputLlmClient;

import java.util.Set;

/**
 * 默认 LLM 门面实现
 *
 * 职责：将统一门面调用委派给底层主 SPI 与可选能力 SPI
 *
 * @author xiexu
 */
public class DefaultAgentLlmGateway implements AgentLlmGateway {

    private final LlmClient llmClient;

    /**
     * 创建门面实现
     *
     * @param llmClient 主 LLM 客户端
     */
    public DefaultAgentLlmGateway(LlmClient llmClient) {
        if (llmClient == null) {
            throw new IllegalArgumentException("llmClient must not be null");
        }
        this.llmClient = llmClient;
    }

    /**
     * 执行同步对话
     *
     * @param request LLM 请求
     * @return LLM 响应
     */
    @Override
    public LlmResponse chat(LlmRequest request) {
        return llmClient.chat(request);
    }

    /**
     * 执行流式对话
     *
     * @param request LLM 请求
     * @param handler 流式处理器
     */
    @Override
    public void stream(LlmRequest request, LlmStreamHandler handler) {
        if (!(llmClient instanceof StreamingLlmClient)) {
            throw new UnsupportedCapabilityException("STREAMING");
        }
        ((StreamingLlmClient) llmClient).stream(request, handler);
    }

    /**
     * 执行结构化输出对话
     *
     * @param request LLM 请求
     * @param spec 结构化定义
     * @param <T> 输出类型
     * @return 结构化响应
     */
    @Override
    public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
        if (!(llmClient instanceof StructuredOutputLlmClient)) {
            throw new UnsupportedCapabilityException("STRUCTURED_OUTPUT");
        }
        return ((StructuredOutputLlmClient) llmClient).structuredChat(request, spec);
    }

    /**
     * 返回能力集合
     *
     * @return 能力集合
     */
    @Override
    public Set<LlmCapability> capabilities() {
        return Set.copyOf(llmClient.capabilities());
    }
}

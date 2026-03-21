package com.xbk.agent.framework.core.llm;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;

import java.util.Set;

/**
 * 统一 LLM 门面
 *
 * 职责：作为上层访问模型的统一入口；
 * 上层只需要把 {@link LlmRequest} 交给它并接收 {@link LlmResponse}，
 * 不需要直接关心底层到底接的是 Spring AI、OpenAI compatible 还是其它实现
 *
 * @author xiexu
 */
public interface AgentLlmGateway {

    /**
     * 执行同步对话
     *
     * @param request LLM 请求
     * @return LLM 响应
     */
    LlmResponse chat(LlmRequest request);

    /**
     * 执行流式对话
     *
     * @param request LLM 请求
     * @param handler 流式处理器
     */
    void stream(LlmRequest request, LlmStreamHandler handler);

    /**
     * 执行结构化输出对话
     *
     * @param request LLM 请求
     * @param spec    结构化定义
     * @param <T>     输出类型
     * @return 结构化响应
     */
    <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec);

    /**
     * 返回能力集合
     *
     * @return 能力集合
     */
    Set<LlmCapability> capabilities();
}

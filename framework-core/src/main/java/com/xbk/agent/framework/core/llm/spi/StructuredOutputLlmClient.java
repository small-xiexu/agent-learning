package com.xbk.agent.framework.core.llm.spi;

import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;

/**
 * 结构化输出 LLM SPI
 *
 * 职责：定义结构化输出协议
 *
 * @author xiexu
 */
public interface StructuredOutputLlmClient {

    /**
     * 执行结构化输出对话
     *
     * @param request LLM 请求
     * @param spec 结构化输出定义
     * @param <T> 输出类型
     * @return 结构化响应
     */
    <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec);
}

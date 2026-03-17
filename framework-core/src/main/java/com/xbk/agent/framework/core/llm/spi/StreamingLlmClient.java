package com.xbk.agent.framework.core.llm.spi;

import com.xbk.agent.framework.core.llm.model.LlmRequest;

/**
 * 流式 LLM SPI
 *
 * 职责：定义流式事件输出协议
 *
 * @author xiexu
 */
public interface StreamingLlmClient {

    /**
     * 执行流式对话
     *
     * @param request LLM 请求
     * @param handler 事件处理器
     */
    void stream(LlmRequest request, LlmStreamHandler handler);
}

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
 * 职责：向上层屏蔽底层模型与适配器差异
 *
 * @author xiexu
 */
public interface HelloAgentsLLM {

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
     * @param spec 结构化定义
     * @param <T> 输出类型
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

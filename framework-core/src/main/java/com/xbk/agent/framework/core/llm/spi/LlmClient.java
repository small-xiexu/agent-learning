package com.xbk.agent.framework.core.llm.spi;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;

import java.util.Set;

/**
 * LLM 主 SPI
 *
 * 职责：定义同步对话与能力暴露协议
 *
 * @author xiexu
 */
public interface LlmClient {

    /**
     * 执行同步对话
     *
     * @param request LLM 请求
     * @return LLM 响应
     */
    LlmResponse chat(LlmRequest request);

    /**
     * 返回支持能力集合
     *
     * @return 能力集合
     */
    Set<LlmCapability> capabilities();
}

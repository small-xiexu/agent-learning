package com.xbk.agent.framework.core.llm.spi;

import com.xbk.agent.framework.core.llm.model.LlmStreamEvent;

/**
 * 流式事件处理器
 *
 * 职责：接收模型流式输出事件
 *
 * @author xiexu
 */
public interface LlmStreamHandler {

    /**
     * 处理流式事件
     *
     * @param event 流式事件
     */
    void onEvent(LlmStreamEvent event);
}

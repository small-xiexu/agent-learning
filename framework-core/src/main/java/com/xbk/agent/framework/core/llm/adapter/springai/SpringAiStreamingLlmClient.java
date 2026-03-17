package com.xbk.agent.framework.core.llm.adapter.springai;

import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.llm.spi.StreamingLlmClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Spring AI 流式 LLM 客户端
 *
 * 职责：保留流式能力适配入口
 *
 * @author xiexu
 */
public class SpringAiStreamingLlmClient implements StreamingLlmClient {

    private final ChatModel chatModel;

    /**
     * 创建流式客户端
     *
     * @param chatModel ChatModel
     */
    public SpringAiStreamingLlmClient(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 执行流式对话
     *
     * @param request LLM 请求
     * @param handler 事件处理器
     */
    @Override
    public void stream(LlmRequest request, LlmStreamHandler handler) {
        throw new UnsupportedOperationException("streaming adapter is not implemented yet for model: " + chatModel);
    }
}

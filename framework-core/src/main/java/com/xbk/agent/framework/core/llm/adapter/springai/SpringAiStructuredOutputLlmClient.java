package com.xbk.agent.framework.core.llm.adapter.springai;

import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.StructuredOutputLlmClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Spring AI 结构化输出客户端
 *
 * 职责：保留结构化输出适配入口
 *
 * @author xiexu
 */
public class SpringAiStructuredOutputLlmClient implements StructuredOutputLlmClient {

    private final ChatModel chatModel;

    /**
     * 创建结构化输出客户端
     *
     * @param chatModel ChatModel
     */
    public SpringAiStructuredOutputLlmClient(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 执行结构化输出对话
     *
     * @param request LLM 请求
     * @param spec 结构化输出定义
     * @param <T> 输出类型
     * @return 结构化响应
     */
    @Override
    public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
        throw new UnsupportedOperationException("structured adapter is not implemented yet for model: " + chatModel);
    }
}

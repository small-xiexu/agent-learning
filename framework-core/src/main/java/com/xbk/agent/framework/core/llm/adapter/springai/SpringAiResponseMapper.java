package com.xbk.agent.framework.core.llm.adapter.springai;

import com.xbk.agent.framework.core.common.enums.LlmFinishReason;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.LlmUsage;
import com.xbk.agent.framework.core.memory.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.UUID;

/**
 * Spring AI 响应映射器
 *
 * 职责：将 Spring AI ChatResponse 转换为框架响应对象
 *
 * @author xiexu
 */
public class SpringAiResponseMapper {

    /**
     * 转换为框架响应对象
     *
     * @param request 原始请求
     * @param response Spring AI 响应
     * @return 框架响应
     */
    public LlmResponse toLlmResponse(LlmRequest request, ChatResponse response) {
        Generation generation = response.getResult();
        String text = generation == null || generation.getOutput() == null ? null : generation.getOutput().getText();
        Message outputMessage = Message.builder()
                .messageId(UUID.randomUUID().toString())
                .conversationId(request.getConversationId())
                .role(MessageRole.ASSISTANT)
                .content(text)
                .build();
        return LlmResponse.builder()
                .requestId(request.getRequestId())
                .responseId(UUID.randomUUID().toString())
                .outputMessage(outputMessage)
                .rawText(text)
                .finishReason(LlmFinishReason.STOP)
                .usage(LlmUsage.builder().build())
                .build();
    }
}

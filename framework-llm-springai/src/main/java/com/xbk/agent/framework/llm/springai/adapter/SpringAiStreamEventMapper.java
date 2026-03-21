package com.xbk.agent.framework.llm.springai.adapter;

import com.xbk.agent.framework.core.common.enums.LlmStreamEventType;
import com.xbk.agent.framework.core.llm.model.LlmStreamEvent;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.UUID;

/**
 * Spring AI 流式事件映射器
 *
 * 职责：将 Spring AI ChatResponse 转换为框架流式事件
 *
 * @author xiexu
 */
public class SpringAiStreamEventMapper {

    /**
     * 转换为框架流式事件
     *
     * @param response Spring AI 响应
     * @return 流式事件
     */
    public LlmStreamEvent toEvent(ChatResponse response) {
        Generation generation = response.getResult();
        AssistantMessage outputMessage = generation == null ? null : generation.getOutput();
        String text = outputMessage == null ? null : outputMessage.getText();
        return LlmStreamEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .type(LlmStreamEventType.COMPLETE)
                .textDelta(text)
                .completed(true)
                .build();
    }
}

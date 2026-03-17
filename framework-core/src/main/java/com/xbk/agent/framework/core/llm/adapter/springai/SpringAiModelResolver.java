package com.xbk.agent.framework.core.llm.adapter.springai;

import org.springframework.ai.chat.model.ChatModel;

/**
 * Spring AI 模型解析器
 *
 * 职责：提供统一的 ChatModel 获取入口
 *
 * @author xiexu
 */
public class SpringAiModelResolver {

    private final ChatModel chatModel;

    /**
     * 创建模型解析器
     *
     * @param chatModel Spring AI ChatModel
     */
    public SpringAiModelResolver(ChatModel chatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel must not be null");
        }
        this.chatModel = chatModel;
    }

    /**
     * 返回 ChatModel
     *
     * @return ChatModel
     */
    public ChatModel resolveChatModel() {
        return chatModel;
    }
}

package com.xbk.agent.framework.core.llm.adapter.springai;

import com.xbk.agent.framework.core.llm.option.ModelOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;

/**
 * Spring AI 选项映射器
 *
 * 职责：将框架模型选项转换为 Spring AI ChatOptions
 *
 * @author xiexu
 */
public class SpringAiOptionsMapper {

    /**
     * 转换为 Spring AI ChatOptions
     *
     * @param modelOptions 模型选项
     * @return Spring AI ChatOptions
     */
    public DefaultChatOptions toChatOptions(ModelOptions modelOptions) {
        DefaultChatOptions chatOptions = new DefaultChatOptions();
        if (modelOptions == null) {
            return chatOptions;
        }
        chatOptions.setModel(modelOptions.getModelName());
        chatOptions.setTemperature(modelOptions.getTemperature());
        chatOptions.setTopP(modelOptions.getTopP());
        chatOptions.setMaxTokens(modelOptions.getMaxTokens());
        chatOptions.setStopSequences(modelOptions.getStopSequences());
        return chatOptions;
    }
}

package com.xbk.agent.framework.core.llm.adapter.springai;

import com.xbk.agent.framework.core.llm.option.ModelOptions;
import com.xbk.agent.framework.core.llm.option.ToolCallingOptions;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Spring AI 选项映射器
 *
 * 职责：将框架模型选项转换为 Spring AI ChatOptions
 *
 * @author xiexu
 */
public class SpringAiOptionsMapper {

    private final SpringAiToolMapper toolMapper;

    /**
     * 创建选项映射器。
     */
    public SpringAiOptionsMapper() {
        this(new SpringAiToolMapper());
    }

    /**
     * 创建选项映射器。
     *
     * @param toolMapper 工具映射器
     */
    public SpringAiOptionsMapper(SpringAiToolMapper toolMapper) {
        this.toolMapper = toolMapper;
    }

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

    /**
     * 转换为带工具调用信息的 Spring AI ChatOptions。
     *
     * @param modelOptions 模型选项
     * @param availableTools 可用工具列表
     * @param toolCallingOptions 工具调用选项
     * @return Spring AI ChatOptions
     */
    public org.springframework.ai.chat.prompt.ChatOptions toChatOptions(
            ModelOptions modelOptions,
            List<ToolDefinition> availableTools,
            ToolCallingOptions toolCallingOptions) {
        if (!isToolCallingEnabled(availableTools, toolCallingOptions)) {
            return toChatOptions(modelOptions);
        }
        DefaultToolCallingChatOptions chatOptions = new DefaultToolCallingChatOptions();
        applyModelOptions(chatOptions, modelOptions);
        chatOptions.setToolCallbacks(toolMapper.toToolCallbacks(availableTools));
        chatOptions.setToolNames(toToolNames(availableTools));
        chatOptions.setInternalToolExecutionEnabled(Boolean.FALSE);
        return chatOptions;
    }

    /**
     * 判断当前请求是否需要启用工具调用。
     *
     * @param availableTools 可用工具列表
     * @param toolCallingOptions 工具调用选项
     * @return 是否启用工具调用
     */
    private boolean isToolCallingEnabled(List<ToolDefinition> availableTools, ToolCallingOptions toolCallingOptions) {
        return availableTools != null
                && !availableTools.isEmpty()
                && toolCallingOptions != null
                && toolCallingOptions.isEnabled();
    }

    /**
     * 把通用模型参数写入工具调用选项对象。
     *
     * @param chatOptions 工具调用选项
     * @param modelOptions 模型选项
     */
    private void applyModelOptions(DefaultToolCallingChatOptions chatOptions, ModelOptions modelOptions) {
        if (modelOptions == null) {
            return;
        }
        chatOptions.setModel(modelOptions.getModelName());
        chatOptions.setTemperature(modelOptions.getTemperature());
        chatOptions.setTopP(modelOptions.getTopP());
        chatOptions.setMaxTokens(modelOptions.getMaxTokens());
        chatOptions.setStopSequences(modelOptions.getStopSequences());
    }

    /**
     * 提取工具名称集合。
     *
     * @param availableTools 可用工具列表
     * @return 工具名称集合
     */
    private Set<String> toToolNames(List<ToolDefinition> availableTools) {
        Set<String> toolNames = new LinkedHashSet<String>();
        for (ToolDefinition toolDefinition : availableTools) {
            toolNames.add(toolDefinition.getName());
        }
        return toolNames;
    }
}

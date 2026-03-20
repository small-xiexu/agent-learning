package com.xbk.agent.framework.core.llm.adapter.springai;

import com.xbk.agent.framework.core.common.enums.LlmFinishReason;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.ToolCall;
import com.xbk.agent.framework.core.llm.model.LlmUsage;
import com.xbk.agent.framework.core.memory.Message;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spring AI 响应映射器
 *
 * 职责：将 Spring AI ChatResponse 转换为框架响应对象
 *
 * @author xiexu
 */
public class SpringAiResponseMapper {

    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    /**
     * 转换为框架响应对象
     *
     * @param request 原始请求
     * @param response Spring AI 响应
     * @return 框架响应
     */
    public LlmResponse toLlmResponse(LlmRequest request, ChatResponse response) {
        Generation generation = response.getResult();
        AssistantMessage assistantMessage = generation.getOutput();
        String text = assistantMessage.getText();
        List<ToolCall> toolCalls = toToolCalls(assistantMessage);
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
                .toolCalls(toolCalls)
                .finishReason(toolCalls.isEmpty() ? LlmFinishReason.STOP : LlmFinishReason.TOOL_CALL)
                .usage(toUsage(response))
                .build();
    }

    /**
     * 将 Spring AI 的工具调用列表转换为框架工具调用列表。
     *
     * @param assistantMessage 助手消息
     * @return 框架工具调用列表
     */
    private List<ToolCall> toToolCalls(AssistantMessage assistantMessage) {
        if (assistantMessage == null || !assistantMessage.hasToolCalls()) {
            return Collections.emptyList();
        }
        List<ToolCall> toolCalls = new ArrayList<ToolCall>();
        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
            toolCalls.add(ToolCall.builder()
                    .toolCallId(toolCall.id())
                    .toolName(toolCall.name())
                    .arguments(parseArguments(toolCall.arguments()))
                    .build());
        }
        return List.copyOf(toolCalls);
    }

    /**
     * 将工具参数 JSON 解析为 Map。
     *
     * @param argumentsJson 参数 JSON
     * @return 参数映射
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) jsonParser.parseMap(argumentsJson);
    }

    /**
     * 映射 token 用量信息。
     *
     * @param response Spring AI 响应
     * @return 框架用量对象
     */
    private LlmUsage toUsage(ChatResponse response) {
        Usage usage = response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
        if (usage == null) {
            return LlmUsage.builder().build();
        }
        return LlmUsage.builder()
                .inputTokens(usage.getPromptTokens())
                .outputTokens(usage.getCompletionTokens())
                .totalTokens(usage.getTotalTokens())
                .build();
    }
}

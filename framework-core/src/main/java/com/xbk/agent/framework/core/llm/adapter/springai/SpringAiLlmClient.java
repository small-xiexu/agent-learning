package com.xbk.agent.framework.core.llm.adapter.springai;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.spi.LlmClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.EnumSet;
import java.util.Set;

/**
 * Spring AI 同步 LLM 客户端
 *
 * 职责：基于 ChatModel 执行同步对话
 *
 * @author xiexu
 */
public class SpringAiLlmClient implements LlmClient {

    private final SpringAiModelResolver modelResolver;
    private final SpringAiMessageMapper messageMapper;
    private final SpringAiOptionsMapper optionsMapper;
    private final SpringAiResponseMapper responseMapper;

    /**
     * 创建同步客户端
     *
     * @param chatModel ChatModel
     */
    public SpringAiLlmClient(ChatModel chatModel) {
        this(new SpringAiModelResolver(chatModel), new SpringAiMessageMapper(), new SpringAiOptionsMapper(),
                new SpringAiResponseMapper());
    }

    /**
     * 创建同步客户端
     *
     * @param modelResolver 模型解析器
     * @param messageMapper 消息映射器
     * @param optionsMapper 选项映射器
     * @param responseMapper 响应映射器
     */
    public SpringAiLlmClient(SpringAiModelResolver modelResolver, SpringAiMessageMapper messageMapper,
            SpringAiOptionsMapper optionsMapper, SpringAiResponseMapper responseMapper) {
        this.modelResolver = modelResolver;
        this.messageMapper = messageMapper;
        this.optionsMapper = optionsMapper;
        this.responseMapper = responseMapper;
    }

    /**
     * 执行同步对话
     *
     * @param request LLM 请求
     * @return LLM 响应
     */
    @Override
    public LlmResponse chat(LlmRequest request) {
        Prompt prompt = new Prompt(messageMapper.toSpringAiMessages(request.getMessages()),
                optionsMapper.toChatOptions(request.getModelOptions()));
        ChatResponse response = modelResolver.resolveChatModel().call(prompt);
        return responseMapper.toLlmResponse(request, response);
    }

    /**
     * 返回支持能力集合
     *
     * @return 能力集合
     */
    @Override
    public Set<LlmCapability> capabilities() {
        return EnumSet.of(LlmCapability.SYNC_CHAT);
    }
}

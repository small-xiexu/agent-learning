package com.xbk.agent.framework.engineering.support;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * EngineeringGatewayBackedChatModel 测试。
 *
 * 职责：验证工程模块的 ChatModel 适配器会把 Spring AI Prompt 映射为统一的 LLM 请求，并保持统一网关为唯一出口。
 *
 * @author xiexu
 */
class EngineeringGatewayBackedChatModelTest {

    /**
     * 验证 ChatModel 适配器会把 Spring AI Prompt 转成框架统一消息。
     */
    @Test
    void shouldConvertPromptIntoFrameworkRequestViaAgentLlmGateway() {
        RecordingGateway gateway = new RecordingGateway();
        EngineeringGatewayBackedChatModel chatModel = new EngineeringGatewayBackedChatModel(gateway);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage("你是一位接待员"),
                new UserMessage("我的应用启动时抛出 NullPointerException")));

        String text = chatModel.call(prompt).getResult().getOutput().getText();

        LlmRequest request = gateway.getLastRequest();
        assertNotNull(request);
        assertEquals("gateway-adapter-response", text);
        assertEquals(2, request.getMessages().size());
        assertEquals(MessageRole.SYSTEM, request.getMessages().get(0).getRole());
        assertEquals("你是一位接待员", request.getMessages().get(0).getContent());
        assertEquals(MessageRole.USER, request.getMessages().get(1).getRole());
        assertEquals("我的应用启动时抛出 NullPointerException", request.getMessages().get(1).getContent());
    }

    /**
     * 脚本化记录网关。
     *
     * 职责：记录最后一次 LLM 请求，并返回固定文本，方便断言适配器是否只走统一网关。
     *
     * @author xiexu
     */
    private static final class RecordingGateway implements AgentLlmGateway {

        private final AtomicReference<LlmRequest> lastRequest = new AtomicReference<LlmRequest>();

        /**
         * 记录请求并返回固定响应。
         *
         * @param request LLM 请求
         * @return 固定响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            lastRequest.set(request);
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId("response-" + UUID.randomUUID())
                    .outputMessage(Message.builder()
                            .messageId("message-" + UUID.randomUUID())
                            .conversationId(request.getConversationId())
                            .role(MessageRole.ASSISTANT)
                            .content("gateway-adapter-response")
                            .build())
                    .rawText("gateway-adapter-response")
                    .build();
        }

        /**
         * 当前测试不覆盖流式能力。
         *
         * @param request 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("stream is not used in this test");
        }

        /**
         * 当前测试不覆盖结构化输出。
         *
         * @param request 请求
         * @param spec 输出规范
         * @param <T> 输出类型
         * @return 永不返回
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("structuredChat is not used in this test");
        }

        /**
         * 返回网关能力集合。
         *
         * @return 能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return Set.of(LlmCapability.SYNC_CHAT);
        }

        /**
         * 返回最后一次请求。
         *
         * @return 最后一次请求
         */
        private LlmRequest getLastRequest() {
            return lastRequest.get();
        }
    }
}

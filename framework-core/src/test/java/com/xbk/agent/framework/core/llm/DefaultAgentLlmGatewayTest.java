package com.xbk.agent.framework.core.llm;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.LlmFinishReason;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.common.exception.UnsupportedCapabilityException;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.LlmStreamEvent;
import com.xbk.agent.framework.core.llm.model.LlmUsage;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmClient;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DefaultAgentLlmGateway 测试
 *
 * 职责：验证统一门面对主 SPI 和可选能力 SPI 的委派行为
 *
 * @author xiexu
 */
class DefaultAgentLlmGatewayTest {

    /**
     * 验证同步对话会委派给主 LLM 客户端
     */
    @Test
    void shouldDelegateSyncChatToLlmClient() {
        RecordingLlmClient client = new RecordingLlmClient();
        DefaultAgentLlmGateway llm = new DefaultAgentLlmGateway(client);
        LlmRequest request = request();

        LlmResponse response = llm.chat(request);

        assertSame(request, client.lastRequest.get());
        assertEquals("done", response.getRawText());
    }

    /**
     * 验证能力集合来自底层客户端
     */
    @Test
    void shouldExposeDelegatedCapabilities() {
        RecordingLlmClient client = new RecordingLlmClient();
        DefaultAgentLlmGateway llm = new DefaultAgentLlmGateway(client);

        Set<LlmCapability> capabilities = llm.capabilities();

        assertEquals(EnumSet.of(LlmCapability.SYNC_CHAT, LlmCapability.TOOL_CALLING), capabilities);
    }

    /**
     * 验证缺少流式能力时会抛出异常
     */
    @Test
    void shouldThrowWhenStreamingCapabilityIsMissing() {
        DefaultAgentLlmGateway llm = new DefaultAgentLlmGateway(new RecordingLlmClient());

        assertThrows(UnsupportedCapabilityException.class, () -> llm.stream(request(), new NoOpStreamHandler()));
    }

    /**
     * 验证缺少结构化输出能力时会抛出异常
     */
    @Test
    void shouldThrowWhenStructuredCapabilityIsMissing() {
        DefaultAgentLlmGateway llm = new DefaultAgentLlmGateway(new RecordingLlmClient());

        assertThrows(UnsupportedCapabilityException.class, () -> llm.structuredChat(request(),
                StructuredOutputSpec.<String>builder().targetType(String.class).schemaName("schema").build()));
    }

    /**
     * 构造测试请求
     *
     * @return 请求对象
     */
    private LlmRequest request() {
        Message message = Message.builder()
                .messageId("msg-1")
                .conversationId("conv-1")
                .role(MessageRole.USER)
                .content("hello")
                .build();
        return LlmRequest.builder()
                .requestId("req-1")
                .conversationId("conv-1")
                .messages(Collections.singletonList(message))
                .build();
    }

    /**
     * 记录型 LLM 客户端
     *
     * 职责：验证同步调用委派
     *
     * @author xiexu
     */
    private static final class RecordingLlmClient implements LlmClient {

        private final AtomicReference<LlmRequest> lastRequest = new AtomicReference<LlmRequest>();

        /**
         * 执行同步对话
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            lastRequest.set(request);
            Message outputMessage = Message.builder()
                    .messageId("msg-2")
                    .conversationId(request.getConversationId())
                    .role(MessageRole.ASSISTANT)
                    .content("done")
                    .build();
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId("resp-1")
                    .outputMessage(outputMessage)
                    .rawText("done")
                    .finishReason(LlmFinishReason.STOP)
                    .usage(LlmUsage.builder().inputTokens(1).outputTokens(1).totalTokens(2).build())
                    .build();
        }

        /**
         * 返回支持能力集合
         *
         * @return 能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return EnumSet.of(LlmCapability.SYNC_CHAT, LlmCapability.TOOL_CALLING);
        }
    }

    /**
     * 空流式处理器
     *
     * 职责：满足流式测试中的接口入参要求
     *
     * @author xiexu
     */
    private static final class NoOpStreamHandler implements LlmStreamHandler {

        /**
         * 处理流式事件
         *
         * @param event 流式事件
         */
        @Override
        public void onEvent(LlmStreamEvent event) {
        }
    }
}

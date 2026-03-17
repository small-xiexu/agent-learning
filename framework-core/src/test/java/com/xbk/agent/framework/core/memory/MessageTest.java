package com.xbk.agent.framework.core.memory;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Message 测试
 *
 * 职责：验证统一消息模型的最小契约
 *
 * @author xiexu
 */
class MessageTest {

    /**
     * 验证消息可按约定创建并保留元数据
     */
    @Test
    void shouldCreateAssistantMessageWithStableMetadata() {
        Map<String, Object> metadata = Map.of("traceId", "trace-1");
        Message message = Message.builder()
                .messageId("msg-1")
                .conversationId("conv-1")
                .role(MessageRole.ASSISTANT)
                .content("done")
                .metadata(metadata)
                .build();

        assertEquals("msg-1", message.getMessageId());
        assertEquals("conv-1", message.getConversationId());
        assertEquals(MessageRole.ASSISTANT, message.getRole());
        assertEquals("done", message.getContent());
        assertEquals("trace-1", message.getMetadata().get("traceId"));
    }

    /**
     * 验证缺失角色时拒绝创建消息
     */
    @Test
    void shouldRejectMessageWithoutRole() {
        assertThrows(IllegalArgumentException.class, () -> Message.builder()
                .messageId("msg-2")
                .conversationId("conv-1")
                .content("invalid")
                .build());
    }
}

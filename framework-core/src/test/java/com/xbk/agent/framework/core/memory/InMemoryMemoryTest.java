package com.xbk.agent.framework.core.memory;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.memory.support.InMemoryMemory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InMemoryMemory 测试
 *
 * 职责：验证会话级内存实现的顺序写入与只读快照行为
 *
 * @author xiexu
 */
class InMemoryMemoryTest {

    /**
     * 验证内存会按会话维度保存消息顺序
     */
    @Test
    void shouldAppendMessagesInOrderForConversation() {
        InMemoryMemory memory = new InMemoryMemory();
        MemorySession session = memory.openSession("conv-1");

        session.append(message("conv-1", "msg-1", "first"));
        session.append(message("conv-1", "msg-2", "second"));

        List<Message> messages = session.messages();
        assertEquals(2, messages.size());
        assertEquals("msg-1", messages.get(0).getMessageId());
        assertEquals("msg-2", messages.get(1).getMessageId());
        assertEquals("second", session.latest().get().getContent());
    }

    /**
     * 验证读取结果为只读快照
     */
    @Test
    void shouldReturnImmutableSnapshot() {
        InMemoryMemory memory = new InMemoryMemory();
        MemorySession session = memory.openSession("conv-2");
        session.append(message("conv-2", "msg-3", "payload"));

        List<Message> messages = session.messages();

        assertThrows(UnsupportedOperationException.class, () -> messages.add(message("conv-2", "msg-4", "more")));
        assertEquals(1, session.size());
    }

    /**
     * 验证清空会话会移除内存状态
     */
    @Test
    void shouldClearConversationState() {
        InMemoryMemory memory = new InMemoryMemory();
        MemorySession session = memory.openSession("conv-3");
        session.append(message("conv-3", "msg-5", "payload"));

        assertTrue(memory.contains("conv-3"));

        memory.clear("conv-3");

        assertFalse(memory.contains("conv-3"));
        assertTrue(memory.openSession("conv-3").messages().isEmpty());
    }

    /**
     * 创建测试消息
     *
     * @param conversationId 会话标识
     * @param messageId 消息标识
     * @param content 消息内容
     * @return 消息对象
     */
    private Message message(String conversationId, String messageId, String content) {
        return Message.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(content)
                .build();
    }
}

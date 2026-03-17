package com.xbk.agent.framework.core.memory.support;

import com.xbk.agent.framework.core.memory.Memory;
import com.xbk.agent.framework.core.memory.MemorySession;
import com.xbk.agent.framework.core.memory.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的记忆实现
 *
 * 职责：按会话维护最小消息状态并提供稳定快照
 *
 * @author xiexu
 */
public class InMemoryMemory implements Memory {

    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<String, SessionState>();

    /**
     * 打开指定会话的记忆视图
     *
     * @param conversationId 会话标识
     * @return 会话记忆
     */
    @Override
    public MemorySession openSession(String conversationId) {
        return new InMemoryMemorySession(conversationId, sessions.computeIfAbsent(conversationId, key -> new SessionState()));
    }

    /**
     * 判断会话是否存在消息状态
     *
     * @param conversationId 会话标识
     * @return 是否存在
     */
    @Override
    public boolean contains(String conversationId) {
        SessionState state = sessions.get(conversationId);
        if (state == null) {
            return false;
        }
        synchronized (state) {
            return !state.messages.isEmpty();
        }
    }

    /**
     * 清空指定会话的消息状态
     *
     * @param conversationId 会话标识
     */
    @Override
    public void clear(String conversationId) {
        SessionState state = sessions.remove(conversationId);
        if (state != null) {
            synchronized (state) {
                state.messages.clear();
            }
        }
    }

    /**
     * 会话状态容器
     *
     * 职责：保存单会话消息列表
     *
     * @author xiexu
     */
    private static final class SessionState {

        private final List<Message> messages = new ArrayList<Message>();
    }

    /**
     * 会话内存实现
     *
     * 职责：在单会话范围内执行消息操作
     *
     * @author xiexu
     */
    private static final class InMemoryMemorySession implements MemorySession {

        private final String conversationId;
        private final SessionState state;

        /**
         * 创建会话内存实现
         *
         * @param conversationId 会话标识
         * @param state 会话状态
         */
        private InMemoryMemorySession(String conversationId, SessionState state) {
            this.conversationId = conversationId;
            this.state = state;
        }

        /**
         * 追加单条消息
         *
         * @param message 消息对象
         */
        @Override
        public void append(Message message) {
            if (message == null) {
                throw new IllegalArgumentException("message must not be null");
            }
            if (!conversationId.equals(message.getConversationId())) {
                throw new IllegalArgumentException("message conversationId does not match session");
            }
            synchronized (state) {
                state.messages.add(message);
            }
        }

        /**
         * 批量追加消息
         *
         * @param messages 消息列表
         */
        @Override
        public void appendAll(List<Message> messages) {
            if (messages == null) {
                throw new IllegalArgumentException("messages must not be null");
            }
            for (Message message : messages) {
                append(message);
            }
        }

        /**
         * 返回消息快照
         *
         * @return 只读消息列表
         */
        @Override
        public List<Message> messages() {
            synchronized (state) {
                return List.copyOf(state.messages);
            }
        }

        /**
         * 返回最新消息
         *
         * @return 最新消息
         */
        @Override
        public Optional<Message> latest() {
            synchronized (state) {
                if (state.messages.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(state.messages.get(state.messages.size() - 1));
            }
        }

        /**
         * 返回消息数量
         *
         * @return 消息数量
         */
        @Override
        public int size() {
            synchronized (state) {
                return state.messages.size();
            }
        }

        /**
         * 清空当前会话
         */
        @Override
        public void clear() {
            synchronized (state) {
                state.messages.clear();
            }
        }
    }
}

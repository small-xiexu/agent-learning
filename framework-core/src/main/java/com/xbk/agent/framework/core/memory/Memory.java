package com.xbk.agent.framework.core.memory;

/**
 * 统一记忆接口
 *
 * 职责：按会话提供消息内存访问入口
 *
 * @author xiexu
 */
public interface Memory {

    /**
     * 打开指定会话的记忆视图
     *
     * @param conversationId 会话标识
     * @return 会话记忆
     */
    MemorySession openSession(String conversationId);

    /**
     * 判断会话是否存在消息状态
     *
     * @param conversationId 会话标识
     * @return 是否存在
     */
    boolean contains(String conversationId);

    /**
     * 清空指定会话的消息状态
     *
     * @param conversationId 会话标识
     */
    void clear(String conversationId);
}

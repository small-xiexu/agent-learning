package com.xbk.agent.framework.core.memory;

import java.util.List;
import java.util.Optional;

/**
 * 会话记忆视图
 *
 * 职责：管理单个会话范围内的消息读写
 *
 * @author xiexu
 */
public interface MemorySession {

    /**
     * 追加单条消息
     *
     * @param message 消息对象
     */
    void append(Message message);

    /**
     * 批量追加消息
     *
     * @param messages 消息列表
     */
    void appendAll(List<Message> messages);

    /**
     * 返回消息快照
     *
     * @return 只读消息列表
     */
    List<Message> messages();

    /**
     * 返回最新消息
     *
     * @return 最新消息
     */
    Optional<Message> latest();

    /**
     * 返回消息数量
     *
     * @return 消息数量
     */
    int size();

    /**
     * 清空当前会话
     */
    void clear();
}

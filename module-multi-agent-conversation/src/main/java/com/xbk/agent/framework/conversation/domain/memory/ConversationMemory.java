package com.xbk.agent.framework.conversation.domain.memory;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleContract;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;
import com.xbk.agent.framework.conversation.support.ConversationPromptTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 群聊共享记忆
 *
 * 职责：维护 AutoGen RoundRobin 群聊的全局消息历史与可读 transcript
 *
 * @author xiexu
 */
public class ConversationMemory {

    /**
     * 全局共享消息历史，三个智能体共同读取这一份群聊上下文。
     */
    private final List<Message> sharedMessages = new ArrayList<Message>();

    /**
     * 面向日志和结果回放的 transcript。
     */
    private final List<ConversationTurn> transcript = new ArrayList<ConversationTurn>();

    /**
     * 清空共享历史。
     */
    public void clear() {
        sharedMessages.clear();
        transcript.clear();
    }

    /**
     * 用用户任务初始化群聊上下文。
     *
     * @param conversationId 会话标识
     * @param task 原始任务
     */
    public void seedTask(String conversationId, String task) {
        sharedMessages.add(createMessage(
                conversationId,
                MessageRole.USER,
                ConversationPromptTemplates.groupKickoffPrompt(task),
                "Task"));
    }

    /**
     * 追加一轮群聊发言。
     *
     * @param conversationId 会话标识
     * @param roleType 发言角色
     * @param content 发言内容
     */
    public void appendTurn(String conversationId, ConversationRoleType roleType, String content) {
        transcript.add(new ConversationTurn(transcript.size() + 1, roleType, content));
        sharedMessages.add(createMessage(
                conversationId,
                MessageRole.ASSISTANT,
                content,
                roleType.getDisplayName()));
    }

    /**
     * 按当前角色视角构造请求消息列表。
     *
     * @param conversationId 会话标识
     * @param contract 当前角色契约
     * @return 请求消息列表
     */
    public List<Message> toMessagesForRole(String conversationId, ConversationRoleContract contract) {
        List<Message> messages = new ArrayList<Message>();
        messages.add(createMessage(
                conversationId,
                MessageRole.SYSTEM,
                contract.getSystemPrompt(),
                contract.getAgentName()));
        messages.addAll(sharedMessages);
        return List.copyOf(messages);
    }

    /**
     * 返回 transcript 快照。
     *
     * @return transcript 快照
     */
    public List<ConversationTurn> snapshotTranscript() {
        return List.copyOf(transcript);
    }

    /**
     * 返回共享消息快照。
     *
     * @return 共享消息快照
     */
    public List<Message> snapshotSharedMessages() {
        return List.copyOf(sharedMessages);
    }

    /**
     * 返回 transcript 大小。
     *
     * @return transcript 大小
     */
    public int size() {
        return transcript.size();
    }

    /**
     * 判断当前 transcript 是否为空。
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return transcript.isEmpty();
    }

    /**
     * 创建统一消息对象。
     *
     * @param conversationId 会话标识
     * @param role 消息角色
     * @param content 消息内容
     * @param name 名称
     * @return 统一消息
     */
    private Message createMessage(String conversationId, MessageRole role, String content, String name) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .content(content == null ? "" : content)
                .name(name)
                .build();
    }
}

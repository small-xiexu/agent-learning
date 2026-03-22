package com.xbk.agent.framework.roleplay.domain.memory;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.roleplay.domain.role.CamelRoleContract;
import com.xbk.agent.framework.roleplay.domain.role.CamelRoleType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CAMEL 对话记忆
 *
 * 职责：以共享 transcript 形式保存手写版双边对话，并按当前角色视角映射为统一消息列表
 *
 * @author xiexu
 */
public class CamelConversationMemory {

    /**
     * 共享 transcript，只存一份原始对话，再按不同角色视角做映射。
     */
    private final List<CamelDialogueTurn> transcript = new ArrayList<CamelDialogueTurn>();

    /**
     * 追加一轮对话。
     *
     * @param roleType 发言角色
     * @param content 发言内容
     */
    public void addTurn(CamelRoleType roleType, String content) {
        transcript.add(new CamelDialogueTurn(transcript.size() + 1, roleType, content));
    }

    /**
     * 返回对话快照。
     *
     * @return 对话快照
     */
    public List<CamelDialogueTurn> snapshot() {
        return List.copyOf(transcript);
    }

    /**
     * 返回当前轮次数量。
     *
     * @return 轮次数量
     */
    public int size() {
        return transcript.size();
    }

    /**
     * 统计某个角色累计发言次数。
     *
     * @param roleType 角色类型
     * @return 发言次数
     */
    public int countTurnsByRole(CamelRoleType roleType) {
        int count = 0;
        for (CamelDialogueTurn turn : transcript) {
            if (turn.getRoleType() == roleType) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断当前是否为空。
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return transcript.isEmpty();
    }

    /**
     * 清空当前 transcript。
     */
    public void clear() {
        transcript.clear();
    }

    /**
     * 按当前角色视角构造统一消息列表。
     *
     * @param conversationId 会话标识
     * @param contract 当前角色契约
     * @param initialPrompt 初始任务提示
     * @return 消息列表
     */
    public List<Message> toMessagesForRole(String conversationId, CamelRoleContract contract, String initialPrompt) {
        List<Message> messages = new ArrayList<Message>();
        messages.add(createMessage(conversationId, MessageRole.SYSTEM, contract.getSystemPrompt(), contract.getAgentName(), null));
        messages.add(createMessage(conversationId, MessageRole.USER, initialPrompt, null, null));
        for (CamelDialogueTurn turn : transcript) {
            // CAMEL 手写版不会真的维护两份字符串历史。
            // 它只保存一份共享 transcript，然后在发送给当前角色前，把“自己说过的话”映射成 assistant，
            // 把“对方说过的话”映射成 user，从而得到该角色当前应看到的对话视角。
            MessageRole role = turn.getRoleType() == contract.getRoleType() ? MessageRole.ASSISTANT : MessageRole.USER;
            messages.add(createMessage(
                    conversationId,
                    role,
                    turn.getContent(),
                    turn.getRoleType().getDisplayName(),
                    null));
        }
        return List.copyOf(messages);
    }

    /**
     * 创建统一消息对象。
     *
     * @param conversationId 会话标识
     * @param role 消息角色
     * @param content 消息内容
     * @param name 消息名称
     * @param toolCallId 工具调用标识
     * @return 统一消息
     */
    private Message createMessage(String conversationId,
                                  MessageRole role,
                                  String content,
                                  String name,
                                  String toolCallId) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .name(name)
                .toolCallId(toolCallId)
                .build();
    }
}

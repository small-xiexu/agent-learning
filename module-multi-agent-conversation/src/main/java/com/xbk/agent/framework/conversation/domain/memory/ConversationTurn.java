package com.xbk.agent.framework.conversation.domain.memory;

import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;

/**
 * 群聊单轮记录
 *
 * 职责：记录一轮 RoundRobin 群聊里的发言角色、轮次和内容
 *
 * @author xiexu
 */
public class ConversationTurn {

    /**
     * 当前发言轮次。
     */
    private final int turnNumber;

    /**
     * 发言角色。
     */
    private final ConversationRoleType roleType;

    /**
     * 发言内容。
     */
    private final String content;

    /**
     * 创建单轮记录。
     *
     * @param turnNumber 轮次
     * @param roleType 角色
     * @param content 内容
     */
    public ConversationTurn(int turnNumber, ConversationRoleType roleType, String content) {
        this.turnNumber = turnNumber;
        this.roleType = roleType;
        this.content = content == null ? "" : content;
    }

    /**
     * 返回轮次。
     *
     * @return 轮次
     */
    public int getTurnNumber() {
        return turnNumber;
    }

    /**
     * 返回角色。
     *
     * @return 角色
     */
    public ConversationRoleType getRoleType() {
        return roleType;
    }

    /**
     * 返回内容。
     *
     * @return 内容
     */
    public String getContent() {
        return content;
    }
}

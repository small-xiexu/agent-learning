package com.xbk.agent.framework.camel.domain.memory;

import com.xbk.agent.framework.camel.domain.role.CamelRoleType;

/**
 * CAMEL 对话轮次
 *
 * 职责：记录一轮角色发言的顺序、角色和文本
 *
 * @author xiexu
 */
public class CamelDialogueTurn {

    private final int turnNumber;
    private final CamelRoleType roleType;
    private final String content;

    /**
     * 创建对话轮次。
     *
     * @param turnNumber 轮次编号
     * @param roleType 角色类型
     * @param content 发言内容
     */
    public CamelDialogueTurn(int turnNumber, CamelRoleType roleType, String content) {
        this.turnNumber = turnNumber;
        this.roleType = roleType;
        this.content = content;
    }

    /**
     * 返回轮次编号。
     *
     * @return 轮次编号
     */
    public int getTurnNumber() {
        return turnNumber;
    }

    /**
     * 返回角色类型。
     *
     * @return 角色类型
     */
    public CamelRoleType getRoleType() {
        return roleType;
    }

    /**
     * 返回发言内容。
     *
     * @return 发言内容
     */
    public String getContent() {
        return content;
    }
}

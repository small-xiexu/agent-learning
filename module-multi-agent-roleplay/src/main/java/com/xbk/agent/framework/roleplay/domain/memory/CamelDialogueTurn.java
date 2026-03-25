package com.xbk.agent.framework.roleplay.domain.memory;

import com.xbk.agent.framework.roleplay.domain.role.CamelRoleType;

/**
 * CAMEL 对话轮次
 *
 * 职责：记录 CAMEL 双角色对话中一轮发言的顺序编号、发言角色和发言内容，
 * 是 CamelConversationMemory transcript 列表的基本单元。
 *
 * <p>CAMEL 中 transcript 的用途：
 * <pre>
 *   手写版 HandwrittenCamelAgent：
 *     → transcript 作为对话历史的轻量镜像，每轮执行后追加一条记录
 *     → 最终 CamelRunResult 携带完整 transcript 供调用方回放整场对话
 *     → 不直接传给 LLM（LLM 消费的是 CamelConversationMemory 中的角色视角消息列表）
 *
 *   框架版 AlibabaCamelFlowAgent：
 *     → transcript 序列化后存入 OverAllState，跨节点流动
 *     → CamelTraderHandoffNode 和 CamelProgrammerHandoffNode 每轮追加新记录
 *     → 最终由 AlibabaCamelFlowAgent 从状态中提取，组装进 CamelRunResult
 * </pre>
 *
 * <p>与 CamelConversationMemory 中消息列表的区别：
 * CamelDialogueTurn 是面向"回放和可观测性"的轻量记录（只有角色+内容），
 * CamelConversationMemory 的消息列表是面向"LLM 上下文构建"的完整消息序列
 * （包含按角色视角映射后的 MessageRole，直接传给模型）。
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

package com.xbk.agent.framework.conversation.domain.memory;

import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;

/**
 * 群聊单轮记录
 *
 * 职责：记录一轮 RoundRobin 群聊里的发言角色、轮次编号和发言内容，
 * 是 AutoGen 群聊 transcript（轻量回放列表）的基本单元。
 *
 * <p>transcript 与 shared_messages 的区别：
 * <pre>
 *   shared_messages（List&lt;Message&gt;）：
 *     → 完整的群聊消息历史，包含 SYSTEM / USER / ASSISTANT 等角色语义
 *     → 直接作为 LLM 请求的 messages 列表传入，模型用它理解上下文并生成回复
 *     → 每条记录保留完整的 Message 对象结构（messageId、conversationId、role 等）
 *
 *   transcript（List&lt;ConversationTurn&gt;）：
 *     → 轻量的业务层回放列表，只保留"谁在第几轮说了什么"
 *     → 主要用于调试输出、测试断言和最终结果展示
 *     → 不传给 LLM，不影响模型上下文
 * </pre>
 * 两份列表同步维护，各司其职：shared_messages 驱动模型推理，transcript 服务于可观测性。
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

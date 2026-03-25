package com.xbk.agent.framework.conversation.domain.role;

import com.xbk.agent.framework.conversation.support.ConversationPromptTemplates;

/**
 * 群聊角色契约
 *
 * 职责：将一个群聊角色的全部稳定约束集中封装在一个对象里——
 * 包括名称、职责描述、系统提示词和状态输出键，
 * 使 RoundRobinGroupChat（手写版）和各 Node 类（框架版）都能以统一方式引用角色定义。
 *
 * <p>为什么需要"契约"这个概念：
 * 群聊中每个角色的系统提示、输出键等定义在手写版和框架版中都要用到，
 * 如果直接在各执行器和节点类里硬编码，一旦修改某个角色的 Prompt 就需要改多处。
 * 契约对象将角色定义集中到一处，执行器和节点只持有契约引用，实现"定义一次、多处复用"。
 *
 * <p>各字段在双版本中的用途：
 * <pre>
 *   agentName   → 框架版 ReactAgent.builder().name(contract.getAgentName())
 *               → 手写版日志和 Message.name 字段
 *
 *   systemPrompt → 手写版构建 SYSTEM 消息时使用
 *                → 框架版 ReactAgent.builder().systemPrompt(contract.getSystemPrompt())
 *
 *   outputKey   → 框架版 ReactAgent.builder().outputKey(contract.getOutputKey())
 *               → 框架版状态里该角色产出的 key 名称
 * </pre>
 *
 * @author xiexu
 */
public class ConversationRoleContract {

    /**
     * 当前契约绑定的角色类型。
     */
    private final ConversationRoleType roleType;

    /**
     * AgentFramework 中展示的 Agent 名称。
     */
    private final String agentName;

    /**
     * 角色职责说明。
     */
    private final String description;

    /**
     * 当前角色的系统提示。
     */
    private final String systemPrompt;

    /**
     * 当前角色在状态里主要对应的输出键。
     */
    private final String outputKey;

    /**
     * 创建角色契约。
     *
     * @param roleType 角色类型
     * @param agentName Agent 名称
     * @param description 角色职责
     * @param systemPrompt 系统提示
     * @param outputKey 输出键
     */
    public ConversationRoleContract(ConversationRoleType roleType,
                                    String agentName,
                                    String description,
                                    String systemPrompt,
                                    String outputKey) {
        this.roleType = roleType;
        this.agentName = agentName;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.outputKey = outputKey;
    }

    /**
     * 返回产品经理契约。
     *
     * @return 产品经理契约
     */
    public static ConversationRoleContract productManagerContract() {
        return new ConversationRoleContract(
                ConversationRoleType.PRODUCT_MANAGER,
                "autogen-product-manager-agent",
                "负责拆解需求、规划下一步范围并驱动工程师实现的产品经理",
                ConversationPromptTemplates.productManagerSystemPrompt(),
                "last_product_output");
    }

    /**
     * 返回工程师契约。
     *
     * @return 工程师契约
     */
    public static ConversationRoleContract engineerContract() {
        return new ConversationRoleContract(
                ConversationRoleType.ENGINEER,
                "autogen-engineer-agent",
                "负责根据群聊历史编写 Python 脚本的工程师",
                ConversationPromptTemplates.engineerSystemPrompt(),
                "last_engineer_output");
    }

    /**
     * 返回审查员契约。
     *
     * @return 审查员契约
     */
    public static ConversationRoleContract codeReviewerContract() {
        return new ConversationRoleContract(
                ConversationRoleType.CODE_REVIEWER,
                "autogen-code-reviewer-agent",
                "负责检查工程师代码并决定是否批准结束的代码审查员",
                ConversationPromptTemplates.codeReviewerSystemPrompt(),
                "last_reviewer_output");
    }

    /**
     * 返回角色类型。
     *
     * @return 角色类型
     */
    public ConversationRoleType getRoleType() {
        return roleType;
    }

    /**
     * 返回 Agent 名称。
     *
     * @return Agent 名称
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * 返回职责描述。
     *
     * @return 职责描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 返回系统提示。
     *
     * @return 系统提示
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 返回输出键。
     *
     * @return 输出键
     */
    public String getOutputKey() {
        return outputKey;
    }
}

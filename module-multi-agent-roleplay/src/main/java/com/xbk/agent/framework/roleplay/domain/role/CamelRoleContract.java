package com.xbk.agent.framework.roleplay.domain.role;

import com.xbk.agent.framework.roleplay.support.CamelPromptTemplates;

/**
 * CAMEL 角色契约
 *
 * 职责：将一个 CAMEL 角色的全部稳定约束——名称、系统提示、输出键——集中封装在一个对象里，
 * 使手写版执行器和框架版节点都能以统一方式引用角色定义，避免定义散落多处。
 *
 * <p>CAMEL 契约的核心设计原则：
 * CAMEL（Communicative Agents for Mind Exploration of Large Language Models）范式的
 * 核心是"角色扮演协议"——每个角色在整个对话中必须严格遵守其系统提示所约定的行为边界。
 * 契约对象将这些约束固化，防止执行器或节点在运行中随意覆盖角色定义。
 * <pre>
 *   交易员契约（traderContract）：
 *     → 约束"只提需求、只做验收、不写代码"
 *     → 是 CAMEL 中负责业务控制和结束判断的一侧
 *     → 只有交易员有权输出结束标记终止对话
 *
 *   程序员契约（programmerContract）：
 *     → 约束"只根据上一条需求写一段代码"
 *     → 是 CAMEL 中负责产出实现的一侧
 *     → 即使程序员违规输出了结束标记，框架版会将其剥离
 * </pre>
 *
 * <p>各字段在双版本中的用途：
 * <pre>
 *   systemPrompt → 手写版：构建 SYSTEM 消息传入 LLM，强约束角色行为
 *                → 框架版：ReactAgent.builder().systemPrompt(contract.getSystemPrompt())
 *
 *   outputKey   → 框架版：ReactAgent.builder().outputKey(contract.getOutputKey())
 *               → OverAllState 中该角色产出内容的 key 名称
 *
 *   agentName   → 框架版：ReactAgent.builder().name(contract.getAgentName())
 *               → Message.name 字段，标识发言者身份
 * </pre>
 *
 * @author xiexu
 */
public class CamelRoleContract {

    /**
     * 当前契约绑定的角色类型。
     */
    private final CamelRoleType roleType;

    /**
     * AgentFramework 中展示的 Agent 名称。
     */
    private final String agentName;

    /**
     * 角色职责说明，主要给框架版元数据和阅读者理解用。
     */
    private final String description;

    /**
     * 当前角色的强约束系统提示。
     */
    private final String systemPrompt;

    /**
     * 当前角色在状态中主要产出的输出键。
     */
    private final String outputKey;

    /**
     * 创建角色契约。
     *
     * @param roleType 角色类型
     * @param agentName Agent 名称
     * @param description 角色描述
     * @param systemPrompt 系统提示
     * @param outputKey 输出键
     */
    public CamelRoleContract(CamelRoleType roleType,
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
     * 返回交易员契约。
     *
     * @return 交易员契约
     */
    public static CamelRoleContract traderContract() {
        // 交易员契约强调“只提需求、只做验收、不写代码”，它是 CAMEL 中负责业务控制的一侧。
        return new CamelRoleContract(
                CamelRoleType.TRADER,
                "camel-stock-trader-agent",
                "负责提出股票分析脚本需求并审查程序员结果的交易员",
                CamelPromptTemplates.traderSystemPrompt(),
                "last_trader_output");
    }

    /**
     * 返回程序员契约。
     *
     * @return 程序员契约
     */
    public static CamelRoleContract programmerContract() {
        // 程序员契约强调“只根据上一条需求写一段代码”，它是 CAMEL 中负责产出实现的一侧。
        return new CamelRoleContract(
                CamelRoleType.PROGRAMMER,
                "camel-stock-programmer-agent",
                "负责根据交易员需求编写股票分析 Java 程序的程序员",
                CamelPromptTemplates.programmerSystemPrompt(),
                "last_programmer_output");
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
     * 返回 Agent 名称。
     *
     * @return Agent 名称
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * 返回角色描述。
     *
     * @return 角色描述
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

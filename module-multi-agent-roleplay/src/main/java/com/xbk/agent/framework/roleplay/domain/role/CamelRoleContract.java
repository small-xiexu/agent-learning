package com.xbk.agent.framework.roleplay.domain.role;

import com.xbk.agent.framework.roleplay.support.CamelPromptTemplates;

/**
 * CAMEL 角色契约
 *
 * 职责：集中封装角色名称、系统提示和输出键等稳定约束
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

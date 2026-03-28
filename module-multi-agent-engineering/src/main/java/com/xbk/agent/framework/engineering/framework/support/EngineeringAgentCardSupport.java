package com.xbk.agent.framework.engineering.framework.support;

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;

import java.util.List;

/**
 * Agent Card 构建支持类。
 *
 * 职责：统一框架版两个专家 Agent 的 AgentCard 构建规范：名称、描述、技能列表、能力声明。
 * 避免 Provider 侧各自硬编码 AgentCard，确保命名约定和元信息一致。
 *
 * <p>AgentCard 是 A2A 协议的"名片 + 能力说明书"：
 * <ul>
 *   <li>Consumer 通过 Nacos 找到 Provider 后，先获取 AgentCard 了解对方能力；
 *   <li>AgentCard 的 name 字段是跨服务标识，必须与 Nacos 服务名保持约定；
 *   <li>skills 描述 Agent 擅长处理什么问题，方便 Consumer 做路由决策。
 * </ul>
 *
 * @author xiexu
 */
public final class EngineeringAgentCardSupport {

    /**
     * A2A 协议版本（遵循 spec 0.2.x）。
     */
    private static final String PROTOCOL_VERSION = "0.2.5";

    /**
     * Provider 组织标识。
     */
    private static final AgentProvider PROVIDER = new AgentProvider("agent-learning", "https://github.com/agent-learning");

    /**
     * 默认输入/输出模式：纯文本。
     */
    private static final List<String> TEXT_MODES = List.of("text");

    private EngineeringAgentCardSupport() {
    }

    /**
     * 构建技术专家 AgentCard。
     *
     * <p>技术专家负责处理报错、异常排查、系统优化等技术类问题。
     * AgentCard 的 name 必须与 engineering.a2a.agent.tech.name 配置一致，
     * 这是 Consumer 发现 Provider 的唯一依据。
     *
     * @param agentUrl Provider 侧的实际访问地址（由 Nacos 注册信息决定）
     * @return 技术专家 AgentCard 的 wrapper
     */
    public static AgentCardWrapper buildTechSupportAgentCard(String agentUrl) {
        AgentSkill skill = new AgentSkill(
                "tech-support",
                "技术支持",
                "处理报错、异常排查、性能问题及架构建议等技术类诉求",
                List.of("技术", "报错", "异常", "排查"),
                List.of("我的服务一直报 NullPointerException，怎么排查？"),
                TEXT_MODES,
                TEXT_MODES);
        AgentCard card = new AgentCard(
                "tech-support-agent",
                "技术支持专家 Agent，负责处理系统报错与技术类咨询",
                agentUrl,
                PROVIDER,
                "1.0.0",
                null,
                new AgentCapabilities(false, false, false, null),
                TEXT_MODES,
                TEXT_MODES,
                List.of(skill),
                false,
                null,
                null,
                null,
                null,
                null,
                PROTOCOL_VERSION);
        return new AgentCardWrapper(card);
    }

    /**
     * 构建销售顾问 AgentCard。
     *
     * <p>销售顾问负责处理报价、产品方案、购买与部署咨询类问题。
     *
     * @param agentUrl Provider 侧的实际访问地址
     * @return 销售顾问 AgentCard 的 wrapper
     */
    public static AgentCardWrapper buildSalesAgentCard(String agentUrl) {
        AgentSkill skill = new AgentSkill(
                "sales-consulting",
                "销售咨询",
                "处理产品报价、企业方案、购买流程与私有化部署等销售类诉求",
                List.of("报价", "购买", "方案", "部署"),
                List.of("我想了解企业版的购买方案和部署方式"),
                TEXT_MODES,
                TEXT_MODES);
        AgentCard card = new AgentCard(
                "sales-agent",
                "销售顾问 Agent，负责产品报价与企业购买咨询",
                agentUrl,
                PROVIDER,
                "1.0.0",
                null,
                new AgentCapabilities(false, false, false, null),
                TEXT_MODES,
                TEXT_MODES,
                List.of(skill),
                false,
                null,
                null,
                null,
                null,
                null,
                PROTOCOL_VERSION);
        return new AgentCardWrapper(card);
    }

    /**
     * 从 AgentCardWrapper 提取 Agent 名称。
     *
     * @param wrapper AgentCard wrapper
     * @return Agent 名称
     */
    public static String extractAgentName(AgentCardWrapper wrapper) {
        return wrapper.name();
    }
}

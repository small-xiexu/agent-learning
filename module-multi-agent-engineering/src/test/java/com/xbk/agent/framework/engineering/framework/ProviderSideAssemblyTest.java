package com.xbk.agent.framework.engineering.framework;

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.xbk.agent.framework.engineering.framework.support.EngineeringAgentCardSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Provider 侧装配测试。
 *
 * 职责：在不启动 Spring 容器的情况下，验证 Provider 侧的 AgentCard 装配逻辑正确：
 * 两个专家 Agent 的名称、描述、URL 与技能信息符合 A2A 发现协议约定。
 *
 * <p>这组测试体现了"Provider 能力声明"的核心：
 * AgentCard 不只是 JSON 文档，它是跨服务 Agent 发现的唯一契约。
 * 如果名称不对，Consumer 就找不到它；如果技能描述不对，调用链就会静默路由错误。
 *
 * @author xiexu
 */
class ProviderSideAssemblyTest {

    /**
     * 验证技术专家 AgentCard 装配后关键字段符合 A2A 发现约定。
     */
    @Test
    void techSupportProviderShouldExposeCorrectAgentCard() {
        AgentCardWrapper card = EngineeringAgentCardSupport.buildTechSupportAgentCard("http://localhost:8081");

        // 名称约定：与 engineering.a2a.agent.tech.name 配置一致
        assertEquals("tech-support-agent", card.name());

        // 描述不能为空，Consumer 可能基于描述做路由决策
        assertNotNull(card.description());
        assertFalse(card.description().isBlank());

        // URL 符合 Provider 端口约定
        assertEquals("http://localhost:8081", card.url());

        // 至少有一项技能声明
        assertNotNull(card.skills());
        assertFalse(card.skills().isEmpty());

        // 技能 ID 唯一标识技术专家能力域
        assertEquals("tech-support", card.skills().get(0).id());
    }

    /**
     * 验证销售顾问 AgentCard 装配后关键字段符合 A2A 发现约定。
     */
    @Test
    void salesProviderShouldExposeCorrectAgentCard() {
        AgentCardWrapper card = EngineeringAgentCardSupport.buildSalesAgentCard("http://localhost:8082");

        assertEquals("sales-agent", card.name());
        assertNotNull(card.description());
        assertEquals("http://localhost:8082", card.url());
        assertFalse(card.skills().isEmpty());
        assertEquals("sales-consulting", card.skills().get(0).id());
    }

    /**
     * 验证两个 Provider 的 AgentCard 名称不同，Consumer 可以用名称区分路由目标。
     */
    @Test
    void techAndSalesProvidersShouldHaveDifferentAgentNames() {
        String techName = EngineeringAgentCardSupport.buildTechSupportAgentCard("http://localhost:8081").name();
        String salesName = EngineeringAgentCardSupport.buildSalesAgentCard("http://localhost:8082").name();

        assertFalse(techName.equals(salesName),
                "技术专家和销售顾问的 AgentCard 名称不能相同，否则 Consumer 无法区分路由目标");
    }
}

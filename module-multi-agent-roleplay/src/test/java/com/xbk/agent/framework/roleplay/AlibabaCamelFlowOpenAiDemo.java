package com.xbk.agent.framework.roleplay;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.roleplay.api.CamelRunResult;
import com.xbk.agent.framework.roleplay.config.OpenAiRolePlayDemoPropertySupport;
import com.xbk.agent.framework.roleplay.config.OpenAiRolePlayDemoTestConfig;
import com.xbk.agent.framework.roleplay.infrastructure.agentframework.AlibabaCamelFlowAgent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI Alibaba 图编排版 CAMEL 真实 OpenAI 对照 Demo
 *
 * 职责：让 FlowAgent 条件边在真实 OpenAI 模型下演示交易员和程序员之间的 handoff 状态回环
 *
 * @author xiexu
 */
class AlibabaCamelFlowOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(AlibabaCamelFlowOpenAiDemo.class.getName());
    private static final int DEMO_MAX_TURNS = 8;
    private static final String STOCK_TASK = """
            目标：编写一个 Java 程序，通过调用公共 API 获取特定股票的实时价格，并计算其移动平均线。
            角色 A（AI 用户）：一位资深的股票交易员，负责提出具体的业务需求和审查结果。
            角色 B（AI 助理）：一位资深的 Java 程序员，负责根据需求编写代码。
            """;

    /**
     * 验证图编排版 CAMEL 可以通过真实 OpenAI 模型完成股票分析脚本协作。
     */
    @Test
    void shouldRunAlibabaCamelFlowAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiRolePlayDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.roleplay.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiRolePlayDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            AlibabaCamelFlowAgent agent = new AlibabaCamelFlowAgent(agentLlmGateway, DEMO_MAX_TURNS);

            CamelRunResult result = agent.run(STOCK_TASK);

            logRunResult(result);

            assertFalse(result.getFinalJavaCode().isBlank());
            assertFalse(result.getTranscript().isEmpty());
            assertTrue(result.getFlowState().isPresent());
            assertTrue(result.getFlowState().get().value("current_java_code").isPresent());
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
     *
     * @return Spring 上下文
     */
    private ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(OpenAiRolePlayDemoTestConfig.class)
                .profiles("openai-roleplay-demo")
                .web(WebApplicationType.NONE)
                .run();
    }

    /**
     * 打印图编排版运行结果。
     *
     * @param result 图编排版运行结果
     */
    private void logRunResult(CamelRunResult result) {
        OverAllState state = result.getFlowState().orElseThrow();
        CamelDemoLogSupport.logRunResult(LOGGER, "FlowAgent CAMEL + OpenAI(gpt-4o)", result, state);
    }
}

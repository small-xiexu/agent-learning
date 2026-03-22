package com.xbk.agent.framework.camel;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.camel.api.CamelRunResult;
import com.xbk.agent.framework.camel.application.coordinator.HandwrittenCamelAgent;
import com.xbk.agent.framework.camel.application.executor.CamelProgrammerAgent;
import com.xbk.agent.framework.camel.application.executor.CamelTraderAgent;
import com.xbk.agent.framework.camel.config.OpenAiCamelDemoPropertySupport;
import com.xbk.agent.framework.camel.config.OpenAiCamelDemoTestConfig;
import com.xbk.agent.framework.camel.domain.memory.CamelDialogueTurn;
import com.xbk.agent.framework.camel.domain.memory.CamelConversationMemory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手写版 CAMEL 真实 OpenAI 对照 Demo
 *
 * 职责：让统一 AgentLlmGateway 驱动交易员与程序员之间的手写角色接力闭环
 *
 * @author xiexu
 */
class HandwrittenCamelOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(HandwrittenCamelOpenAiDemo.class.getName());
    private static final int DEMO_MAX_TURNS = 8;
    private static final String STOCK_TASK = """
            目标：编写一个 Java 程序，通过调用公共 API 获取特定股票的实时价格，并计算其移动平均线。
            角色 A（AI 用户）：一位资深的股票交易员，负责提出具体的业务需求和审查结果。
            角色 B（AI 助理）：一位资深的 Java 程序员，负责根据需求编写代码。
            """;

    /**
     * 验证手写版 CAMEL 可以通过真实 OpenAI 模型完成股票分析脚本协作。
     */
    @Test
    void shouldRunHandwrittenCamelAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiCamelDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.camel.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiCamelDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            HandwrittenCamelAgent agent = new HandwrittenCamelAgent(
                    new CamelTraderAgent(agentLlmGateway),
                    new CamelProgrammerAgent(agentLlmGateway),
                    new CamelConversationMemory(),
                    DEMO_MAX_TURNS);

            CamelRunResult result = agent.run(STOCK_TASK);

            logRunResult(result);

            assertFalse(result.getFinalJavaCode().isBlank());
            assertFalse(result.getTranscript().isEmpty());
            assertTrue(result.getFinalJavaCode().contains("class"));
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
     *
     * @return Spring 上下文
     */
    private ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(OpenAiCamelDemoTestConfig.class)
                .profiles("openai-camel-demo")
                .web(WebApplicationType.NONE)
                .run();
    }

    /**
     * 打印手写版运行结果。
     *
     * @param result 手写版运行结果
     */
    private void logRunResult(CamelRunResult result) {
        CamelDemoLogSupport.logRunResult(LOGGER, "手写版 CAMEL + OpenAI(gpt-4o)", result, null);
    }
}

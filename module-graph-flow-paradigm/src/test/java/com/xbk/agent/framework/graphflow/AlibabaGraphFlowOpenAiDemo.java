package com.xbk.agent.framework.graphflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.graphflow.config.OpenAiGraphDemoPropertySupport;
import com.xbk.agent.framework.graphflow.config.OpenAiGraphDemoTestConfig;
import com.xbk.agent.framework.graphflow.framework.AlibabaGraphFlowAgent;
import com.xbk.agent.framework.graphflow.framework.support.GraphFlowStateKeys;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI Alibaba 图编排版 Graph Flow 真实 OpenAI 对照 Demo
 *
 * 职责：让 FlowAgent + StateGraph 在真实 OpenAI 模型下演示
 * “understand -> search -> answer” 的图式执行过程。
 *
 * @author xiexu
 */
class AlibabaGraphFlowOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(AlibabaGraphFlowOpenAiDemo.class.getName());
    private static final String USER_QUERY =
            "请总结 Spring AI Alibaba 的 StateGraph 如何帮助 Java 工程师实现可观测的多步骤工作流。";

    /**
     * 验证框架版 Graph Flow 可以通过真实 OpenAI 模型完成图编排问答。
     */
    @Test
    void shouldRunAlibabaGraphFlowAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiGraphDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.graph.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiGraphDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            AlibabaGraphFlowAgent agent = new AlibabaGraphFlowAgent(agentLlmGateway);

            AlibabaGraphFlowAgent.RunResult result = agent.run(USER_QUERY);
            OverAllState state = result.getState();

            GraphFlowDemoLogSupport.logFrameworkResult(LOGGER, "FlowAgent Graph Flow + OpenAI(gpt-4o)", result, state);

            assertFalse(result.getFinalAnswer().isBlank());
            assertFalse(result.getSearchResults().isBlank());
            assertFalse(result.isUsedFallback());
            assertTrue(state.value(GraphFlowStateKeys.SEARCH_QUERY).isPresent());
            assertTrue(state.value(GraphFlowStateKeys.FINAL_ANSWER).isPresent());
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
     *
     * @return Spring 上下文
     */
    private ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(OpenAiGraphDemoTestConfig.class)
                .profiles("openai-graph-demo")
                .web(WebApplicationType.NONE)
                .run();
    }
}

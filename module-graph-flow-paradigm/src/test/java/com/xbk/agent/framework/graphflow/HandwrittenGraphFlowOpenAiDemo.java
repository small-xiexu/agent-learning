package com.xbk.agent.framework.graphflow;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.graphflow.common.state.GraphState;
import com.xbk.agent.framework.graphflow.common.state.StepStatus;
import com.xbk.agent.framework.graphflow.config.OpenAiGraphDemoPropertySupport;
import com.xbk.agent.framework.graphflow.config.OpenAiGraphDemoTestConfig;
import com.xbk.agent.framework.graphflow.handwritten.HandwrittenGraphFlow;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 手写版 Graph Flow 真实 OpenAI 对照 Demo
 *
 * 职责：让统一 AgentLlmGateway 驱动三步问答助手的手写状态机闭环，
 * 验证手写版在真实模型下也能完成“理解 -> 搜索 -> 总结”的图执行流程。
 *
 * @author xiexu
 */
class HandwrittenGraphFlowOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(HandwrittenGraphFlowOpenAiDemo.class.getName());
    private static final String USER_QUERY =
            "请对比 Spring AI Alibaba 的 StateGraph 和 LangGraph 在复杂工作流编排上的共同点与差异。";

    /**
     * 验证手写版 Graph Flow 可以通过真实 OpenAI 模型完成三步问答闭环。
     */
    @Test
    void shouldRunHandwrittenGraphFlowAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiGraphDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.graph.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiGraphDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            HandwrittenGraphFlow flow = new HandwrittenGraphFlow(agentLlmGateway);

            GraphState result = flow.run(USER_QUERY);

            GraphFlowDemoLogSupport.logHandwrittenResult(LOGGER, "Handwritten Graph Flow + OpenAI(gpt-4o)", result);

            assertSame(StepStatus.END, result.getStepStatus());
            assertFalse(result.getSearchQuery().isBlank());
            assertFalse(result.getSearchResults().isBlank());
            assertFalse(result.getFinalAnswer().isBlank());
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

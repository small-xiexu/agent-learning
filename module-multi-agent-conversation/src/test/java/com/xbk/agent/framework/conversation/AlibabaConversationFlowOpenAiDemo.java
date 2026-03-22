package com.xbk.agent.framework.conversation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.conversation.api.ConversationRunResult;
import com.xbk.agent.framework.conversation.config.OpenAiConversationDemoPropertySupport;
import com.xbk.agent.framework.conversation.config.OpenAiConversationDemoTestConfig;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.AlibabaConversationFlowAgent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI Alibaba 图编排版 Conversation 真实 OpenAI 对照 Demo
 *
 * 职责：让 FlowAgent 在真实 OpenAI 模型下演示共享群聊历史的 RoundRobin 回环
 *
 * @author xiexu
 */
class AlibabaConversationFlowOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(AlibabaConversationFlowOpenAiDemo.class.getName());
    private static final String BITCOIN_TASK = """
            任务：开发一个能够获取并打印实时比特币价格的简易 Python 脚本。
            团队角色：ProductManager（产品经理，负责需求分析）、Engineer（工程师，负责编码）、CodeReviewer（代码审查员，负责检查）。
            """;

    /**
     * 验证图编排版 Conversation 可以通过真实 OpenAI 模型完成群聊协作。
     */
    @Test
    void shouldRunAlibabaConversationFlowAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiConversationDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.conversation.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiConversationDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            AlibabaConversationFlowAgent agent = new AlibabaConversationFlowAgent(agentLlmGateway, 9);

            ConversationRunResult result = agent.run(BITCOIN_TASK);
            OverAllState state = result.getFlowState().orElseThrow();

            ConversationDemoLogSupport.logRunResult(LOGGER, "FlowAgent Conversation + OpenAI(gpt-4o)", result, state);

            assertFalse(result.getFinalPythonScript().isBlank());
            assertFalse(result.getTranscript().isEmpty());
            assertTrue(result.getFlowState().isPresent());
            assertTrue(result.getFlowState().get().value("current_python_script").isPresent());
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
     *
     * @return Spring 上下文
     */
    private ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(OpenAiConversationDemoTestConfig.class)
                .profiles("openai-conversation-demo")
                .web(WebApplicationType.NONE)
                .run();
    }
}

package com.xbk.agent.framework.conversation;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.conversation.api.ConversationRunResult;
import com.xbk.agent.framework.conversation.application.coordinator.RoundRobinGroupChat;
import com.xbk.agent.framework.conversation.application.executor.CodeReviewerAgent;
import com.xbk.agent.framework.conversation.application.executor.EngineerAgent;
import com.xbk.agent.framework.conversation.application.executor.ProductManagerAgent;
import com.xbk.agent.framework.conversation.config.OpenAiConversationDemoPropertySupport;
import com.xbk.agent.framework.conversation.config.OpenAiConversationDemoTestConfig;
import com.xbk.agent.framework.conversation.domain.memory.ConversationMemory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手写版 Conversation 真实 OpenAI 对照 Demo
 *
 * 职责：让统一 AgentLlmGateway 驱动 ProductManager、Engineer、CodeReviewer 的手写群聊闭环
 *
 * @author xiexu
 */
class HandwrittenConversationOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(HandwrittenConversationOpenAiDemo.class.getName());
    private static final String BITCOIN_TASK = """
            任务：开发一个能够获取并打印实时比特币价格的简易 Python 脚本。
            团队角色：ProductManager（产品经理，负责需求分析）、Engineer（工程师，负责编码）、CodeReviewer（代码审查员，负责检查）。
            """;

    /**
     * 验证手写版 Conversation 可以通过真实 OpenAI 模型完成群聊协作。
     */
    @Test
    void shouldRunHandwrittenConversationAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiConversationDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.conversation.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiConversationDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            RoundRobinGroupChat groupChat = new RoundRobinGroupChat(
                    new ProductManagerAgent(agentLlmGateway),
                    new EngineerAgent(agentLlmGateway),
                    new CodeReviewerAgent(agentLlmGateway),
                    new ConversationMemory(),
                    9);

            ConversationRunResult result = groupChat.run(BITCOIN_TASK);

            ConversationDemoLogSupport.logRunResult(LOGGER, "Handwritten Conversation + OpenAI(gpt-4o)", result, null);

            assertFalse(result.getFinalPythonScript().isBlank());
            assertFalse(result.getTranscript().isEmpty());
            assertTrue(result.getFinalPythonScript().contains("requests"));
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

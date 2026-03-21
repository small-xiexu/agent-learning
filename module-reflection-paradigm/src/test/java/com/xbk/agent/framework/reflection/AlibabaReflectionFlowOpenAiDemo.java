package com.xbk.agent.framework.reflection;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.reflection.config.OpenAiReflectionDemoPropertySupport;
import com.xbk.agent.framework.reflection.config.OpenAiReflectionDemoTestConfig;
import com.xbk.agent.framework.reflection.infrastructure.agentframework.AlibabaReflectionFlowAgent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI Alibaba 图编排版 Reflection 真实 OpenAI 对照 Demo
 *
 * 职责：让 StateGraph 条件边在真实 OpenAI 模型下演示 current_code 和 review_feedback 的状态回环
 *
 * @author xiexu
 */
class AlibabaReflectionFlowOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(AlibabaReflectionFlowOpenAiDemo.class.getName());
    private static final String PRIME_TASK = "编写一个 Java 方法，找出 1 到 n 之间所有的素数 (prime numbers)，并返回一个 List<Integer>。";

    /**
     * 验证图编排版 Reflection 可以通过真实 OpenAI 模型完成素数题代码优化。
     */
    @Test
    void shouldRunAlibabaReflectionFlowAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiReflectionDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.reflection.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiReflectionDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            ChatModel chatModel = context.getBean(ChatModel.class);
            AlibabaReflectionFlowAgent agent = new AlibabaReflectionFlowAgent(chatModel, 3);

            AlibabaReflectionFlowAgent.RunResult result = agent.run(PRIME_TASK);

            logRunResult(result);

            assertFalse(result.getFinalCode().isBlank());
            assertFalse(result.getFinalReview().isBlank());
            assertTrue(result.getState().value("current_code").isPresent());
            assertTrue(result.getState().value("review_feedback").isPresent());
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
     *
     * @return Spring 上下文
     */
    private ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(OpenAiReflectionDemoTestConfig.class)
                .profiles("openai-reflection-demo")
                .web(WebApplicationType.NONE)
                .run();
    }

    /**
     * 打印图编排版运行结果。
     *
     * @param result 图编排版运行结果
     */
    private void logRunResult(AlibabaReflectionFlowAgent.RunResult result) {
        OverAllState state = result.getState();
        LOGGER.info("=== FlowAgent Reflection + OpenAI(gpt-4o) ===");
        LOGGER.info("STATE_KEYS -> " + new ArrayList<String>(state.data().keySet()));
        LOGGER.info("ITERATION_COUNT -> " + result.getIterationCount());
        logMultilineSection("FINAL_REVIEW", result.getFinalReview());
        logMultilineSection("FINAL_CODE", result.getFinalCode());
    }

    /**
     * 逐行打印多行文本分段。
     *
     * @param title 分段标题
     * @param content 分段内容
     */
    private void logMultilineSection(String title, String content) {
        LOGGER.info(title);
        for (String line : normalizeForLog(content)) {
            LOGGER.info("  " + line);
        }
    }

    /**
     * 规范化多行文本，便于日志输出。
     *
     * @param content 原始文本
     * @return 规范化后的文本行
     */
    private List<String> normalizeForLog(String content) {
        if (content == null || content.isBlank()) {
            return List.of("(empty)");
        }
        List<String> lines = new ArrayList<String>();
        for (String line : content.replace("**", "").split("\\R")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                lines.add(trimmedLine);
            }
        }
        if (lines.isEmpty()) {
            lines.add("(empty)");
        }
        return List.copyOf(lines);
    }
}

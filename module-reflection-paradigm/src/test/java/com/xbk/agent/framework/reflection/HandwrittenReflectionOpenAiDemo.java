package com.xbk.agent.framework.reflection;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.reflection.application.coordinator.HandwrittenReflectionAgent;
import com.xbk.agent.framework.reflection.application.executor.HandwrittenJavaCoder;
import com.xbk.agent.framework.reflection.application.executor.HandwrittenJavaReviewer;
import com.xbk.agent.framework.reflection.config.OpenAiReflectionDemoPropertySupport;
import com.xbk.agent.framework.reflection.config.OpenAiReflectionDemoTestConfig;
import com.xbk.agent.framework.reflection.domain.memory.ReflectionMemory;
import com.xbk.agent.framework.reflection.domain.memory.ReflectionTurnRecord;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手写版 Reflection 真实 OpenAI 对照 Demo
 *
 * 职责：让框架自己的 AgentLlmGateway 驱动“初稿 -> 评审 -> 优化”的手写反思闭环
 *
 * @author xiexu
 */
class HandwrittenReflectionOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(HandwrittenReflectionOpenAiDemo.class.getName());
    private static final String PRIME_TASK = "编写一个 Java 方法，找出 1 到 n 之间所有的素数 (prime numbers)，并返回一个 List<Integer>。";

    /**
     * 验证手写版 Reflection 可以通过真实 OpenAI 模型完成素数题代码优化。
     */
    @Test
    void shouldRunHandwrittenReflectionAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiReflectionDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.reflection.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiReflectionDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            HandwrittenReflectionAgent agent = new HandwrittenReflectionAgent(
                    new HandwrittenJavaCoder(agentLlmGateway),
                    new HandwrittenJavaReviewer(agentLlmGateway),
                    new ReflectionMemory(),
                    3);

            HandwrittenReflectionAgent.RunResult result = agent.run(PRIME_TASK);

            logRunResult(result);

            assertFalse(result.getFinalCode().isBlank());
            assertFalse(result.getFinalReflection().isBlank());
            assertFalse(result.getMemory().isEmpty());
            assertTrue(result.getFinalCode().contains("List"));
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
     * 打印手写版运行结果。
     *
     * @param result 手写版运行结果
     */
    private void logRunResult(HandwrittenReflectionAgent.RunResult result) {
        LOGGER.info("=== 手写版 Reflection + OpenAI(gpt-4o) ===");
        logMemory(result.getMemory());
        LOGGER.info("FINAL_REFLECTION -> " + result.getFinalReflection());
        LOGGER.info("FINAL_CODE");
        for (String line : normalizeForLog(result.getFinalCode())) {
            LOGGER.info("  " + line);
        }
    }

    /**
     * 打印反思记忆。
     *
     * @param memory 反思记忆
     */
    private void logMemory(List<ReflectionTurnRecord> memory) {
        int index = 1;
        for (ReflectionTurnRecord turnRecord : memory) {
            LOGGER.info("TURN_" + index + "_REFLECTION -> " + turnRecord.getReflection());
            index++;
        }
    }

    /**
     * 规范化多行日志文本。
     *
     * @param content 原始文本
     * @return 规范化后的文本行
     */
    private List<String> normalizeForLog(String content) {
        if (content == null || content.isBlank()) {
            return List.of("(empty)");
        }
        List<String> lines = new java.util.ArrayList<String>();
        for (String line : content.split("\\R")) {
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

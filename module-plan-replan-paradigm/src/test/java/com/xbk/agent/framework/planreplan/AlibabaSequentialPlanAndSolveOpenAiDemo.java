package com.xbk.agent.framework.planreplan;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.planreplan.config.OpenAiPlanSolveDemoPropertySupport;
import com.xbk.agent.framework.planreplan.config.OpenAiPlanSolveDemoTestConfig;
import com.xbk.agent.framework.planreplan.infrastructure.agentframework.AlibabaSequentialPlanAndSolveAgent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI Alibaba 顺序编排版 Plan-and-Solve 真实 OpenAI 对照 Demo
 *
 * 职责：让 SequentialAgent 在真实 OpenAI 模型下演示 outputKey 状态流转
 *
 * @author xiexu
 */
class AlibabaSequentialPlanAndSolveOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(AlibabaSequentialPlanAndSolveOpenAiDemo.class.getName());
    private static final String APPLE_QUESTION = "一个水果店周一卖出了15个苹果。周二卖出的苹果数量是周一的两倍。周三卖出的数量比周二少了5个。请问这三天总共卖出了多少个苹果？";

    /**
     * 验证状态回放日志会把多行文本拆成独立分段，并清理 Markdown 强调符号。
     */
    @Test
    void shouldFormatReplayLogsIntoReadableSections() {
        String planResult = """
                1. 确定周一卖出的苹果数量。
                2. 计算周二卖出的苹果数量（周一的两倍）。
                """;
        String finalAnswer = """
                周一卖出15个苹果。
                最终答案：**70个苹果**。
                """;
        OverAllState state = new OverAllState(Map.of(
                "input", APPLE_QUESTION,
                "plan_result", new AssistantMessage(planResult),
                "final_answer", new AssistantMessage(finalAnswer)));
        AlibabaSequentialPlanAndSolveAgent.RunResult result =
                new AlibabaSequentialPlanAndSolveAgent.RunResult(APPLE_QUESTION, planResult, finalAnswer, state);

        List<String> logMessages = captureReplayLogs(result);

        assertTrue(logMessages.contains("STATE_META -> plan_result=AssistantMessage, final_answer=AssistantMessage"));
        assertTrue(logMessages.contains("PLAN_RESULT"));
        assertTrue(logMessages.contains("  1. 确定周一卖出的苹果数量。"));
        assertTrue(logMessages.contains("  2. 计算周二卖出的苹果数量（周一的两倍）。"));
        assertTrue(logMessages.contains("FINAL_ANSWER"));
        assertTrue(logMessages.contains("  周一卖出15个苹果。"));
        assertTrue(logMessages.contains("  最终答案：70个苹果。"));
        assertFalse(logMessages.stream().anyMatch(message -> message.contains("PLAN_RESULT(text) ->")));
        assertFalse(logMessages.stream().anyMatch(message -> message.contains("**")));
    }

    /**
     * 验证顺序编排版 Plan-and-Solve 可以通过真实 OpenAI 模型完成苹果题求解。
     */
    @Test
    void shouldRunSequentialPlanAndSolveAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiPlanSolveDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.plan-solve.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiPlanSolveDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            ChatModel chatModel = context.getBean(ChatModel.class);
            AlibabaSequentialPlanAndSolveAgent agent = new AlibabaSequentialPlanAndSolveAgent(chatModel);

            LOGGER.info("=== SequentialAgent Plan-and-Solve + OpenAI(gpt-4o) 实时执行日志 ===");
            LOGGER.info("USER(实时输入) -> " + APPLE_QUESTION);

            AlibabaSequentialPlanAndSolveAgent.RunResult result = agent.run(APPLE_QUESTION);

            logRunResult(result);

            assertFalse(result.getPlanResult().isBlank());
            assertFalse(result.getFinalAnswer().isBlank());
            assertTrue(result.getFinalAnswer().contains("70"));
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
     *
     * @return Spring 上下文
     */
    private ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(OpenAiPlanSolveDemoTestConfig.class)
                .profiles("openai-plan-solve-demo")
                .web(WebApplicationType.NONE)
                .run();
    }

    /**
     * 打印顺序编排版运行结果。
     *
     * @param result 顺序编排版运行结果
     */
    private void logRunResult(AlibabaSequentialPlanAndSolveAgent.RunResult result) {
        OverAllState state = result.getState();
        LOGGER.info("=== SequentialAgent Plan-and-Solve + OpenAI(gpt-4o) 执行完成后的状态回放 ===");
        LOGGER.info("STATE_KEYS -> " + new ArrayList<String>(state.data().keySet()));
        LOGGER.info(buildStateMeta(state));
        logMultilineSection("PLAN_RESULT", result.getPlanResult());
        logMultilineSection("FINAL_ANSWER", result.getFinalAnswer());
    }

    /**
     * 返回状态值的类型描述。
     *
     * @param value 状态值
     * @return 类型描述
     */
    private String describeType(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    /**
     * 构造状态类型摘要。
     *
     * @param state 全局状态
     * @return 状态类型摘要
     */
    private String buildStateMeta(OverAllState state) {
        String planResultType = state.value("plan_result").map(this::describeType).orElse("null");
        String finalAnswerType = state.value("final_answer").map(this::describeType).orElse("null");
        return "STATE_META -> plan_result=" + planResultType + ", final_answer=" + finalAnswerType;
    }

    /**
     * 逐行打印一个多行文本分段。
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
     * 规范化多行文本，便于日志逐行输出。
     *
     * @param content 原始文本
     * @return 规范化后的文本行
     */
    private List<String> normalizeForLog(String content) {
        String sanitizedText = sanitizeMarkdown(content);
        List<String> normalizedLines = new ArrayList<String>();
        for (String line : sanitizedText.split("\\R")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                normalizedLines.add(trimmedLine);
            }
        }
        if (normalizedLines.isEmpty()) {
            normalizedLines.add("(empty)");
        }
        return normalizedLines;
    }

    /**
     * 清理日志中不适合直接展示的 Markdown 标记。
     *
     * @param content 原始文本
     * @return 清理后的文本
     */
    private String sanitizeMarkdown(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("**", "").trim();
    }

    /**
     * 捕获状态回放日志，供格式化测试断言。
     *
     * @param result 顺序编排版运行结果
     * @return 日志消息列表
     */
    private List<String> captureReplayLogs(AlibabaSequentialPlanAndSolveAgent.RunResult result) {
        CollectingHandler handler = new CollectingHandler();
        boolean originalUseParentHandlers = LOGGER.getUseParentHandlers();
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(handler);
        try {
            logRunResult(result);
        } finally {
            LOGGER.removeHandler(handler);
            LOGGER.setUseParentHandlers(originalUseParentHandlers);
        }
        return handler.messages();
    }

    /**
     * 采集日志消息的测试处理器。
     *
     * 职责：把 Logger 输出暂存为字符串列表，方便断言日志格式
     *
     * @author xiexu
     */
    private static final class CollectingHandler extends Handler {

        private final List<String> messages = new ArrayList<String>();

        /**
         * 采集单条日志记录。
         *
         * @param record 日志记录
         */
        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        /**
         * 刷新处理器。
         */
        @Override
        public void flush() {
        }

        /**
         * 关闭处理器。
         */
        @Override
        public void close() {
        }

        /**
         * 返回采集到的日志消息。
         *
         * @return 日志消息列表
         */
        private List<String> messages() {
            return List.copyOf(messages);
        }
    }
}

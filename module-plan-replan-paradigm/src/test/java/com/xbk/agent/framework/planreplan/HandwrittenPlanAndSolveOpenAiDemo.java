package com.xbk.agent.framework.planreplan;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.planreplan.application.coordinator.HandwrittenPlanAndSolveAgent;
import com.xbk.agent.framework.planreplan.application.executor.HandwrittenExecutor;
import com.xbk.agent.framework.planreplan.application.executor.HandwrittenPlanner;
import com.xbk.agent.framework.planreplan.config.OpenAiPlanSolveDemoPropertySupport;
import com.xbk.agent.framework.planreplan.config.OpenAiPlanSolveDemoTestConfig;
import com.xbk.agent.framework.planreplan.domain.execution.StepExecutionRecord;
import com.xbk.agent.framework.planreplan.domain.plan.PlanStep;
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
 * 手写版 Plan-and-Solve 真实 OpenAI 对照 Demo
 *
 * 职责：让框架自己的 AgentLlmGateway 驱动 Planner、Executor 和手写 history 累加链路
 *
 * @author xiexu
 */
class HandwrittenPlanAndSolveOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(HandwrittenPlanAndSolveOpenAiDemo.class.getName());
    private static final String APPLE_QUESTION = "一个水果店周一卖出了15个苹果。周二卖出的苹果数量是周一的两倍。周三卖出的数量比周二少了5个。请问这三天总共卖出了多少个苹果？";

    /**
     * 验证手写版 Plan-and-Solve 可以通过真实 OpenAI 模型完成苹果题求解。
     */
    @Test
    void shouldRunHandwrittenPlanAndSolveAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiPlanSolveDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.plan-solve.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiPlanSolveDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            HandwrittenPlanAndSolveAgent agent = new HandwrittenPlanAndSolveAgent(
                    new HandwrittenPlanner(agentLlmGateway),
                    new HandwrittenExecutor(agentLlmGateway));

            LOGGER.info("=== 手写版 Plan-and-Solve + OpenAI(gpt-4o) 实时执行日志 ===");
            LOGGER.info("USER(实时输入) -> " + APPLE_QUESTION);

            HandwrittenPlanAndSolveAgent.RunResult result = agent.run(APPLE_QUESTION);

            logRunResult(result);

            assertFalse(result.getPlan().isEmpty());
            assertFalse(result.getHistory().isEmpty());
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
     * 打印手写版运行结果。
     *
     * @param result 手写版运行结果
     */
    private void logRunResult(HandwrittenPlanAndSolveAgent.RunResult result) {
        LOGGER.info("=== 手写版 Plan-and-Solve + OpenAI(gpt-4o) 执行完成后的计划与 history 回放 ===");
        logPlan(result.getPlan());
        logHistory(result.getHistory());
        LOGGER.info("FINAL_ANSWER -> " + result.getFinalAnswer());
    }

    /**
     * 打印计划列表。
     *
     * @param plan 计划列表
     */
    private void logPlan(List<PlanStep> plan) {
        for (PlanStep step : plan) {
            LOGGER.info("PLAN -> " + step.getStepIndex() + ". " + step.getInstruction());
        }
    }

    /**
     * 打印历史执行记录。
     *
     * @param history 历史执行记录
     */
    private void logHistory(List<StepExecutionRecord> history) {
        for (StepExecutionRecord record : history) {
            LOGGER.info("HISTORY -> 步骤" + record.getPlanStep().getStepIndex() + " : " + record.getStepResult());
        }
    }
}

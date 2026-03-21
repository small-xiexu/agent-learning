package com.xbk.agent.framework.react;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.core.tool.Tool;
import com.xbk.agent.framework.core.tool.ToolContext;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import com.xbk.agent.framework.core.tool.ToolRegistry;
import com.xbk.agent.framework.core.tool.ToolRequest;
import com.xbk.agent.framework.core.tool.ToolResult;
import com.xbk.agent.framework.core.tool.support.DefaultToolRegistry;
import com.xbk.agent.framework.react.application.executor.ReActAgent;
import com.xbk.agent.framework.react.config.OpenAiReactDemoPropertySupport;
import com.xbk.agent.framework.react.config.OpenAiReactDemoTestConfig;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手写版 ReActAgent 真实 OpenAI 对照 Demo
 *
 * 职责：让框架自己的 AgentLlmGateway 与 ToolRegistry 通过真实 OpenAI ChatModel 跑通旅行助手闭环
 *
 * @author xiexu
 */
class ReActTravelOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(ReActTravelOpenAiDemo.class.getName());
    private static final String USER_QUERY = "今天北京天气如何？根据天气推荐一个合适的旅游景点。";

    /**
     * 验证手写版 ReActAgent 可以通过真实 OpenAI 模型调用工具并给出结果。
     */
    @Test
    void shouldRunHandwrittenReactAgainstRealOpenAiModel() {
        Assumptions.assumeTrue(OpenAiReactDemoPropertySupport.isDemoEnabled(),
                "需要在本地配置文件中开启 demo.react.openai.enabled=true");
        Assumptions.assumeTrue(OpenAiReactDemoPropertySupport.hasConfiguredApiKey(),
                "需要在本地配置文件中配置真实 llm.api-key");
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            AgentLlmGateway agentLlmGateway = context.getBean(AgentLlmGateway.class);
            ReActAgent reactAgent = new ReActAgent(agentLlmGateway, createToolRegistry(), 5);

            String answer = reactAgent.run(USER_QUERY);
            List<Message> history = reactAgent.latestHistory();

            logHistory(history, answer);

            assertFalse(answer.isBlank());
            assertTrue(history.stream().anyMatch(message -> message.getRole() == MessageRole.TOOL));
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
     *
     * 这里不直接用 {@code @SpringBootTest}，而是在测试方法里按需启动一个最小容器，
     * 这样可以更直观地控制：
     * 1. 只加载本次 Demo 需要的自动装配
     * 2. 显式启用 {@code openai-react-demo} profile，读取对应的本地模型配置
     * 3. 关闭 Web 环境，避免为了一个命令式测试额外启动 Web 服务器
     *
     * @return Spring 上下文
     */
    private ConfigurableApplicationContext createApplicationContext() {
        return new SpringApplicationBuilder(OpenAiReactDemoTestConfig.class)
                .profiles("openai-react-demo")
                .web(WebApplicationType.NONE)
                .run();
    }

    /**
     * 创建手写版工具注册中心。
     *
     * @return 工具注册中心
     */
    private ToolRegistry createToolRegistry() {
        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new WeatherTool());
        toolRegistry.register(new SearchAttractionTool());
        return toolRegistry;
    }

    /**
     * 打印真实模型下的完整流转记录。
     *
     * @param history 消息历史
     * @param answer 最终答案
     */
    private void logHistory(List<Message> history, String answer) {
        LOGGER.info("=== 手写版 ReAct + OpenAI(gpt-4o) ===");
        for (Message message : history) {
            LOGGER.info(message.getRole() + " -> " + message.getContent());
        }
        LOGGER.info("Final Answer -> " + answer);
    }

    /**
     * 天气查询工具。
     *
     * 职责：向真实模型暴露固定天气结果，验证 function calling 链路
     *
     * @author xiexu
     */
    private static final class WeatherTool implements Tool {

        /**
         * 返回工具定义。
         *
         * @return 工具定义
         */
        @Override
        public ToolDefinition definition() {
            return ToolDefinition.builder()
                    .name("WeatherTool")
                    .description("查询指定城市当天的天气")
                    .inputSchema(Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "city", Map.of(
                                            "type", "string",
                                            "description", "需要查询天气的城市")),
                            "required", List.of("city")))
                    .outputDescription("返回中文天气描述")
                    .build();
        }

        /**
         * 执行工具请求。
         *
         * @param request 工具请求
         * @param context 工具上下文
         * @return 工具执行结果
         */
        @Override
        public ToolResult execute(ToolRequest request, ToolContext context) {
            return ToolResult.builder()
                    .success(true)
                    .content("晴朗，微风，气温25度")
                    .build();
        }
    }

    /**
     * 景点推荐工具。
     *
     * 职责：根据天气和城市返回固定景点推荐
     *
     * @author xiexu
     */
    private static final class SearchAttractionTool implements Tool {

        /**
         * 返回工具定义。
         *
         * @return 工具定义
         */
        @Override
        public ToolDefinition definition() {
            return ToolDefinition.builder()
                    .name("SearchAttractionTool")
                    .description("根据城市和天气推荐合适的旅游景点")
                    .inputSchema(Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "city", Map.of(
                                            "type", "string",
                                            "description", "旅游城市"),
                                    "weather", Map.of(
                                            "type", "string",
                                            "description", "当前天气")),
                            "required", List.of("city", "weather")))
                    .outputDescription("返回中文景点推荐")
                    .build();
        }

        /**
         * 执行工具请求。
         *
         * @param request 工具请求
         * @param context 工具上下文
         * @return 工具执行结果
         */
        @Override
        public ToolResult execute(ToolRequest request, ToolContext context) {
            return ToolResult.builder()
                    .success(true)
                    .content("推荐去颐和园泛舟")
                    .build();
        }
    }
}

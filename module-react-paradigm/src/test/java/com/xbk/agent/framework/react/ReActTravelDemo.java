package com.xbk.agent.framework.react;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.model.ToolCall;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.core.tool.Tool;
import com.xbk.agent.framework.core.tool.ToolContext;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import com.xbk.agent.framework.core.tool.ToolRegistry;
import com.xbk.agent.framework.core.tool.ToolRequest;
import com.xbk.agent.framework.core.tool.ToolResult;
import com.xbk.agent.framework.core.tool.support.DefaultToolRegistry;
import com.xbk.agent.framework.react.application.executor.ReActAgent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ReAct 智能旅行助手演示
 *
 * 职责：通过本地假 LLM 和本地工具演示 Thought -> Action -> Observation -> Final Answer 闭环
 *
 * @author xiexu
 */
public class ReActTravelDemo {

    private static final String USER_QUERY = "今天北京天气如何？根据天气推荐一个合适的旅游景点。";
    private static final Logger LOGGER = Logger.getLogger(ReActTravelDemo.class.getName());

    /**
     * 验证旅行助手可以完整执行 ReAct 闭环
     */
    @Test
    public void shouldCompleteTravelAssistantReactLoop() {
        ToolRegistry toolRegistry = createToolRegistry();
        ReActAgent reactAgent = new ReActAgent(new TravelDemoAgentLlmGateway(), toolRegistry, 5);

        String answer = reactAgent.run(USER_QUERY);

        Assertions.assertEquals("今天北京晴朗，微风，气温25度，适合去颐和园泛舟。", answer);
        Assertions.assertEquals(6, reactAgent.latestHistory().size());
        Assertions.assertTrue(reactAgent.latestHistory().get(1).getContent().contains("WeatherTool"));
        Assertions.assertTrue(reactAgent.latestHistory().get(3).getContent().contains("SearchAttractionTool"));
        Assertions.assertTrue(reactAgent.latestHistory().get(5).getContent().contains("Final Answer"));
    }

    /**
     * 验证死循环保护是否生效。
     *
     * 这里故意注入一个永远不会返回 Final Answer 的假 LLM。
     * 它每一轮都会继续请求 WeatherTool，因此如果 Agent 没有 maxSteps 安全阀，
     * 整个 ReAct 循环就会一直执行下去。
     */
    @Test
    public void shouldStopWhenMaxStepsReached() {
        ToolRegistry toolRegistry = createToolRegistry();
        ReActAgent reactAgent = new ReActAgent(new AlwaysToolCallingAgentLlmGateway(), toolRegistry, 2);

        String answer = reactAgent.run(USER_QUERY);

        Assertions.assertEquals("未能在最大步骤内完成任务。", answer);
        Assertions.assertEquals(5, reactAgent.latestHistory().size());
    }

    /**
     * 运行智能旅行助手演示
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        ToolRegistry toolRegistry = createToolRegistry();
        ReActAgent reactAgent = new ReActAgent(new TravelDemoAgentLlmGateway(), toolRegistry, 5);
        String answer = reactAgent.run(USER_QUERY);

        LOGGER.info("=== ReAct 智能旅行助手 ===");
        for (Message message : reactAgent.latestHistory()) {
            LOGGER.info(message.getRole() + " -> " + message.getContent());
        }
        LOGGER.info("Final Answer -> " + answer);
    }

    /**
     * 创建本地工具注册中心
     *
     * @return 工具注册中心
     */
    private static ToolRegistry createToolRegistry() {
        DefaultToolRegistry toolRegistry = new DefaultToolRegistry();
        toolRegistry.register(new WeatherTool());
        toolRegistry.register(new SearchAttractionTool());
        return toolRegistry;
    }

    /**
     * 旅行场景假 LLM
     *
     * 职责：按预设脚本模拟天气查询、景点推荐和最终回答
     *
     * @author xiexu
     */
    private static final class TravelDemoAgentLlmGateway implements AgentLlmGateway {

        /**
         * 执行同步对话
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            List<Message> messages = request.getMessages();
            long toolMessageCount = messages.stream().filter(message -> message.getRole() == MessageRole.TOOL).count();
            if (toolMessageCount == 0) {
                return createWeatherStep(request);
            }
            if (toolMessageCount == 1) {
                return createAttractionStep(request);
            }
            return createFinalAnswer(request);
        }

        /**
         * 执行流式对话
         *
         * @param request LLM 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("demo llm does not support streaming");
        }

        /**
         * 执行结构化输出对话
         *
         * @param request LLM 请求
         * @param spec 结构化定义
         * @param <T> 输出类型
         * @return 结构化响应
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("demo llm does not support structured output");
        }

        /**
         * 返回支持能力
         *
         * @return 能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return EnumSet.of(LlmCapability.SYNC_CHAT, LlmCapability.TOOL_CALLING);
        }

        /**
         * 创建天气查询步骤
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        private LlmResponse createWeatherStep(LlmRequest request) {
            return createToolStep(
                    request,
                    "Thought: 我需要先确认北京今天的天气。\nAction: WeatherTool\nAction Input: {city=北京}",
                    "weather-tool-call",
                    "WeatherTool",
                    Collections.<String, Object>singletonMap("city", "北京"));
        }

        /**
         * 创建景点推荐步骤
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        private LlmResponse createAttractionStep(LlmRequest request) {
            return createToolStep(
                    request,
                    "Thought: 我已经拿到天气结果，需要结合天气继续推荐景点。\nAction: SearchAttractionTool\nAction Input: {city=北京, weather=晴朗，微风，气温25度}",
                    "attraction-tool-call",
                    "SearchAttractionTool",
                    createAttractionArguments());
        }

        /**
         * 创建最终答案步骤
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        private LlmResponse createFinalAnswer(LlmRequest request) {
            Message message = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .conversationId(request.getConversationId())
                    .role(MessageRole.ASSISTANT)
                    .content("Final Answer: 今天北京晴朗，微风，气温25度，适合去颐和园泛舟。")
                    .build();
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId(UUID.randomUUID().toString())
                    .outputMessage(message)
                    .rawText(message.getContent())
                    .build();
        }

        /**
         * 创建工具调用响应
         *
         * @param request LLM 请求
         * @param thoughtContent 思考与行动文本
         * @param toolCallId 工具调用标识
         * @param toolName 工具名称
         * @param arguments 工具参数
         * @return LLM 响应
         */
        private LlmResponse createToolStep(
                LlmRequest request,
                String thoughtContent,
                String toolCallId,
                String toolName,
                Map<String, Object> arguments) {
            Message message = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .conversationId(request.getConversationId())
                    .role(MessageRole.ASSISTANT)
                    .content(thoughtContent)
                    .build();
            ToolCall toolCall = ToolCall.builder()
                    .toolCallId(toolCallId)
                    .toolName(toolName)
                    .arguments(arguments)
                    .build();
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId(UUID.randomUUID().toString())
                    .outputMessage(message)
                    .rawText(thoughtContent)
                    .toolCalls(Collections.singletonList(toolCall))
                    .build();
        }

        /**
         * 创建景点推荐参数
         *
         * @return 参数映射
         */
        private Map<String, Object> createAttractionArguments() {
            return Map.of("city", "北京", "weather", "晴朗，微风，气温25度");
        }
    }

    /**
     * 持续发起工具调用的假 LLM
     *
     * 职责：模拟一种不会自然收敛的坏场景。
     * 它每次被调用时都只会返回新的工具请求，不会生成 Final Answer，
     * 因此专门用于验证 ReActAgent 的 maxSteps 安全阀是否真的能阻止死循环。
     *
     * @author xiexu
     */
    private static final class AlwaysToolCallingAgentLlmGateway implements AgentLlmGateway {

        /**
         * 执行同步对话
         *
         * 每次都返回同一个 WeatherTool 调用请求。
         * 这样下一轮即使把 Observation 回填给模型，模型仍然会继续要求调用工具，
         * 从而制造“永远不结束”的循环场景。
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            Message message = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .conversationId(request.getConversationId())
                    .role(MessageRole.ASSISTANT)
                    .content("Thought: 我还需要继续查天气。\nAction: WeatherTool\nAction Input: {city=北京}")
                    .build();
            ToolCall toolCall = ToolCall.builder()
                    .toolCallId(UUID.randomUUID().toString())
                    .toolName("WeatherTool")
                    .arguments(Collections.<String, Object>singletonMap("city", "北京"))
                    .build();
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId(UUID.randomUUID().toString())
                    .outputMessage(message)
                    .rawText(message.getContent())
                    .toolCalls(Collections.singletonList(toolCall))
                    .build();
        }

        /**
         * 执行流式对话
         *
         * @param request LLM 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("looping llm does not support streaming");
        }

        /**
         * 执行结构化输出对话
         *
         * @param request LLM 请求
         * @param spec 结构化定义
         * @param <T> 输出类型
         * @return 结构化响应
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("looping llm does not support structured output");
        }

        /**
         * 返回支持能力
         *
         * @return 能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return EnumSet.of(LlmCapability.SYNC_CHAT, LlmCapability.TOOL_CALLING);
        }
    }

    /**
     * 天气查询工具
     *
     * 职责：模拟返回固定天气结果
     *
     * @author xiexu
     */
    private static final class WeatherTool implements Tool {

        /**
         * 返回工具定义
         *
         * @return 工具定义
         */
        @Override
        public ToolDefinition definition() {
            return ToolDefinition.builder()
                    .name("WeatherTool")
                    .description("查询指定城市天气")
                    .outputDescription("返回固定天气结果")
                    .build();
        }

        /**
         * 执行工具请求
         *
         * @param request 工具请求
         * @param context 工具上下文
         * @return 工具结果
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
     * 景点搜索工具
     *
     * 职责：模拟根据天气与城市推荐景点
     *
     * @author xiexu
     */
    private static final class SearchAttractionTool implements Tool {

        /**
         * 返回工具定义
         *
         * @return 工具定义
         */
        @Override
        public ToolDefinition definition() {
            return ToolDefinition.builder()
                    .name("SearchAttractionTool")
                    .description("根据城市和天气推荐景点")
                    .outputDescription("返回固定景点推荐")
                    .build();
        }

        /**
         * 执行工具请求
         *
         * @param request 工具请求
         * @param context 工具上下文
         * @return 工具结果
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

package com.xbk.agent.framework.react;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 使用 Spring AI Alibaba 官方 ReactAgent 实现的智能旅行助手 Demo。
 *
 * <p>这个 Demo 的目的不是替代我们之前手写的防腐层版本，而是做“对照学习”：
 * 1. 手写版 {@code ReActAgent} 需要我们自己维护 history 列表
 * 2. 需要我们自己解析 LLM 输出里的工具调用
 * 3. 需要我们自己写 while 循环，把 Thought -> Action -> Observation 串起来
 *
 * <p>而官方 {@link ReactAgent} 会自动帮我们完成这些底层编排：
 * 1. 自动把消息追加回图状态，交给 Graph Runtime 管理
 * 2. 自动识别 AssistantMessage 里的 ToolCall，并路由到工具节点
 * 3. 自动在 Model Node 和 Tool Node 之间循环，直到模型给出最终答案
 * 4. 调用 {@code invoke(...)} 时还能直接返回完整状态，便于调试与回放
 *
 * <p>为了让这个 Demo 在单元测试中稳定可运行，这里没有直连真实模型，而是提供了一个脚本化
 * {@link ChatModel} 假实现，按固定顺序返回：
 * 1. 调天气工具
 * 2. 调景点推荐工具
 * 3. 给最终答案
 *
 * <p>说明：在当前 Spring AI Alibaba 1.1.2.0 依赖下，官方可用的 ReactAgent 实际类型为
 * {@link com.alibaba.cloud.ai.graph.agent.ReactAgent}，而不是 {@code org.springframework.ai.chat.agent.ReactAgent}。
 *
 * @author xiexu
 */
public class SpringAIReActTravelDemo {

    private static final String USER_QUERY = "今天北京天气如何？根据天气推荐一个合适的旅游景点。";
    private static final Logger LOGGER = Logger.getLogger(SpringAIReActTravelDemo.class.getName());

    /**
     * 验证官方 ReactAgent 能完成同样的旅行助手闭环。
     */
    @Test
    void shouldCompleteTravelAssistantWithOfficialReactAgent() throws Exception {
        DemoRunResult result = runTravelDemo();

        Assertions.assertEquals("北京今天晴朗，微风，气温25度，适合去颐和园泛舟。", result.finalAnswer().getText());
        Assertions.assertTrue(result.invokeState().isPresent());
        Assertions.assertTrue(result.invokeState().get().data().containsKey("messages"));
    }

    /**
     * 允许直接运行这个 Demo，便于对照学习。
     */
    public static void main(String[] args) throws Exception {
        DemoRunResult result = runTravelDemo();

        LOGGER.info("=== Spring AI Alibaba 官方 ReactAgent 旅行助手 Demo ===");
        LOGGER.info("【call() 最终答案】" + result.finalAnswer().getText());
        LOGGER.info("【invoke() 状态键】" + result.invokeState().map(state -> state.data().keySet().toString()).orElse("[]"));

        result.invokeState()
                .map(state -> state.value("messages", new ArrayList<Message>()))
                .ifPresent(SpringAIReActTravelDemo::logConversation);
    }

    /**
     * 运行完整的旅行助手场景。
     *
     * <p>这里故意分别创建两个 Agent：
     * 1. 第一个 Agent 用来演示 {@code call()}，只拿最终答案
     * 2. 第二个 Agent 用来演示 {@code invoke()}，拿完整执行状态
     *
     * <p>这样做的原因是脚本化 ChatModel 内部有轮次计数器，两个 Agent 各自独立，状态最清晰。
     */
    private static DemoRunResult runTravelDemo() throws Exception {
        ReactAgent callAgent = createTravelAssistantAgent(new ScriptedTravelChatModel());
        // RunnableConfig 可以理解成“这一次图执行时的运行参数包”。
        // 这里最重要的是 threadId，它相当于这次会话的唯一标识。
        // 配了 MemorySaver 之后，Graph Runtime 会按这个 threadId 隔离状态，避免不同调用串线。
        RunnableConfig callConfig = RunnableConfig.builder()
                .threadId("react-travel-call-thread")
                .build();

        // call() 只关心“最后的答案是什么”，因此直接返回最终的 AssistantMessage。
        AssistantMessage finalAnswer = callAgent.call(USER_QUERY, callConfig);

        ReactAgent invokeAgent = createTravelAssistantAgent(new ScriptedTravelChatModel());
        // 这里故意换一个 threadId，让第二次执行拥有独立状态。
        // 这样 call() 和 invoke() 的演示互不影响，更容易观察官方 Runtime 的行为。
        RunnableConfig invokeConfig = RunnableConfig.builder()
                .threadId("react-travel-invoke-thread")
                .build();

        // invoke() 不只给最终答案，还会把整次图执行结束后的状态拿回来。
        // 学习阶段更适合用它观察消息轨迹、工具调用结果以及状态里都保存了什么。
        Optional<OverAllState> state = invokeAgent.invoke(USER_QUERY, invokeConfig);

        return new DemoRunResult(finalAnswer, state);
    }

    /**
     * 构建官方 ReactAgent。
     *
     * <p>这里是和手写版差异最大的地方：
     * 1. {@code model(...)} 交给模型节点
     * 2. {@code methodTools(...)} 自动把 Java 方法注册成工具节点
     * 3. {@code systemPrompt(...)} 注入智能旅行助手的人设
     * 4. {@code saver(...)} 让图运行时可以保存线程态
     *
     * <p>手写版里这些都需要我们自己串，官方版则交给 Graph Runtime 自动流转。
     */
    private static ReactAgent createTravelAssistantAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("travel-react-agent")
                .description("智能旅行助手")
                // model(...) 指定“图里的模型节点到底调用哪个大模型”。
                // 手写版是我们自己调 helloAgentsLLM.chat(...)，官方版则交给 Graph Runtime 调这里的模型。
                .model(chatModel)
                // methodTools(...) 会把普通 Java 方法自动注册成可调用工具。
                // 模型一旦返回 tool call，Runtime 会自动找到对应方法执行，不需要我们手写 ToolRegistry 路由逻辑。
                .methodTools(new TravelTools())
                // systemPrompt(...) 就是给 Agent 的长期角色设定。
                // 它会和用户问题、工具结果一起进入上下文，指导模型按照 ReAct 方式工作。
                .systemPrompt("""
                        你是一名智能旅行助手。
                        你的工作方式必须遵循 ReAct：
                        1. 先思考当前缺什么信息
                        2. 如需外部信息就调用工具
                        3. 拿到工具结果后继续思考
                        4. 信息齐全后再给最终答案

                        当你需要天气时调用 queryWeather。
                        当你需要根据天气推荐景点时调用 recommendAttraction。
                        最终回答请使用简洁自然的中文。
                        """)
                // saver(...) 负责保存这条执行线程上的状态。
                // 这里用内存版 MemorySaver，方便 demo 在本地直接运行和观察，不需要外部存储。
                .saver(new MemorySaver())
                .build();
    }

    /**
     * 打印对话轨迹，帮助观察官方 ReactAgent 写回状态后的消息流。
     */
    private static void logConversation(List<Message> messages) {
        LOGGER.info("=== invoke() 返回的消息轨迹 ===");
        for (Message message : messages) {
            LOGGER.info(describeMessage(message));
        }
    }

    /**
     * 将 Spring AI 消息转换为更易读的日志文本。
     */
    private static String describeMessage(Message message) {
        if (message instanceof AssistantMessage assistantMessage) {
            String text = assistantMessage.getText();
            if (assistantMessage.hasToolCalls()) {
                return "ASSISTANT(tool-call) -> " + text + " | " + assistantMessage.getToolCalls();
            }
            return "ASSISTANT -> " + text;
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return "TOOL -> " + toolResponseMessage.getResponses();
        }
        if (message instanceof AbstractMessage abstractMessage) {
            return message.getMessageType() + " -> " + abstractMessage.getText();
        }
        return message.getMessageType() + " -> " + message;
    }

    /**
     * 官方工具定义。
     *
     * <p>和手写版不同，这里不需要我们自己维护 ToolRegistry。
     * ReactAgent 的 {@code methodTools(...)} 会把这些标准 Java 方法转换为可调用工具。
     */
    static final class TravelTools {

        @Tool(name = "queryWeather", description = "查询指定城市的天气")
        public String queryWeather(@ToolParam(description = "需要查询天气的城市") String city) {
            LOGGER.info("【Tool】queryWeather(" + city + ")");
            return "晴朗，微风，气温25度";
        }

        @Tool(name = "recommendAttraction", description = "根据城市和天气推荐合适的旅游景点")
        public String recommendAttraction(
                @ToolParam(description = "旅游城市") String city,
                @ToolParam(description = "当前天气") String weather) {
            LOGGER.info("【Tool】recommendAttraction(city=" + city + ", weather=" + weather + ")");
            return "推荐去颐和园泛舟";
        }
    }

    /**
     * 脚本化 ChatModel。
     *
     * <p>这个类模拟真实大模型的 3 轮响应：
     * 1. 第一轮先调天气工具
     * 2. 第二轮再调景点推荐工具
     * 3. 第三轮输出最终答案
     *
     * <p>之所以要这样写，是因为我们要演示官方 ReactAgent 的循环编排能力，而不是演示真实模型联网。
     */
    static final class ScriptedTravelChatModel implements ChatModel {

        private final AtomicInteger round = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            int currentRound = round.incrementAndGet();
            logPrompt(prompt, currentRound);

            return switch (currentRound) {
                case 1 -> createToolCallResponse(
                        "Thought: 我需要先查询北京天气。",
                        "weather-tool-call",
                        "queryWeather",
                        "{\"city\":\"北京\"}");
                case 2 -> createToolCallResponse(
                        "Thought: 我已经知道天气，接下来应该推荐合适景点。",
                        "attraction-tool-call",
                        "recommendAttraction",
                        "{\"city\":\"北京\",\"weather\":\"晴朗，微风，气温25度\"}");
                default -> createFinalAnswerResponse("北京今天晴朗，微风，气温25度，适合去颐和园泛舟。");
            };
        }

        /**
         * 打印每一轮送进 Model Node 的 Prompt。
         *
         * <p>这能直观看到：官方 ReactAgent 已经自动帮我们把系统提示、用户问题、工具结果
         * 组织进 Prompt 了，不需要像手写版那样显式维护 history 列表。
         */
        private void logPrompt(Prompt prompt, int currentRound) {
            LOGGER.info("=== Model Node Round " + currentRound + " ===");
            for (Message message : prompt.getInstructions()) {
                LOGGER.info(describeMessage(message));
            }
        }

        /**
         * 生成“让 Agent 调工具”的模型响应。
         */
        private ChatResponse createToolCallResponse(
                String reasoning,
                String toolCallId,
                String toolName,
                String arguments) {
            AssistantMessage assistantMessage = AssistantMessage.builder()
                    .content(reasoning)
                    .toolCalls(List.of(new AssistantMessage.ToolCall(toolCallId, "function", toolName, arguments)))
                    .build();
            return ChatResponse.builder()
                    .generations(List.of(new Generation(assistantMessage)))
                    .build();
        }

        /**
         * 生成最终答案响应。
         */
        private ChatResponse createFinalAnswerResponse(String answer) {
            AssistantMessage assistantMessage = AssistantMessage.builder()
                    .content(answer)
                    .build();
            return ChatResponse.builder()
                    .generations(List.of(new Generation(assistantMessage)))
                    .build();
        }
    }

    /**
     * 运行结果聚合。
     */
    record DemoRunResult(AssistantMessage finalAnswer, Optional<OverAllState> invokeState) {
    }
}

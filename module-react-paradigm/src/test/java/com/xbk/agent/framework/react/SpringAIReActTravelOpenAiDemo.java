package com.xbk.agent.framework.react;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.xbk.agent.framework.react.config.OpenAiReactDemoTestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 官方 ReactAgent 真实 OpenAI 对照 Demo
 *
 * 职责：让 Spring AI Alibaba 官方 ReactAgent 与真实 OpenAI ChatModel 直接协作，形成对照学习样本
 *
 * @author xiexu
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
@EnabledIfSystemProperty(named = "demo.react.openai.enabled", matches = "true")
class SpringAIReActTravelOpenAiDemo {

    private static final Logger LOGGER = Logger.getLogger(SpringAIReActTravelOpenAiDemo.class.getName());
    private static final String USER_QUERY = "今天北京天气如何？根据天气推荐一个合适的旅游景点。";

    /**
     * 验证官方 ReactAgent 可以通过真实 OpenAI 模型触发工具调用并返回最终结果。
     *
     * @throws Exception 调用失败时抛出异常
     */
    @Test
    void shouldRunOfficialReactAgentAgainstRealOpenAiModel() throws Exception {
        try (ConfigurableApplicationContext context = createApplicationContext()) {
            ChatModel chatModel = context.getBean(ChatModel.class);
            ReactAgent reactAgent = createTravelAssistantAgent(chatModel);
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId("official-react-openai-demo")
                    .build();

            Optional<OverAllState> state = reactAgent.invoke(USER_QUERY, runnableConfig);
            List<Message> messages = state.map(value -> value.value("messages", new ArrayList<Message>()))
                    .orElseGet(ArrayList::new);

            logConversation(messages);

            assertFalse(messages.isEmpty());
            assertTrue(messages.stream().anyMatch(ToolResponseMessage.class::isInstance));
            assertTrue(messages.stream()
                    .filter(AssistantMessage.class::isInstance)
                    .map(AssistantMessage.class::cast)
                    .map(AssistantMessage::getText)
                    .anyMatch(text -> text != null && !text.isBlank()));
        }
    }

    /**
     * 创建真实 OpenAI Demo 所需的 Spring 上下文。
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
     * 创建官方旅行助手 Agent。
     *
     * @param model 真实 ChatModel
     * @return ReactAgent
     */
    private ReactAgent createTravelAssistantAgent(ChatModel model) {
        return ReactAgent.builder()
                .name("travel-react-agent")
                .description("智能旅行助手")
                .model(model)
                .methodTools(new TravelTools())
                .systemPrompt("""
                        你是一名智能旅行助手。
                        回答必须遵循 ReAct 风格：
                        1. 缺什么信息就优先调用工具。
                        2. 先查天气，再根据天气推荐景点。
                        3. 工具结果拿到后再给最终中文答案。
                        """)
                .saver(new MemorySaver())
                .build();
    }

    /**
     * 打印官方 ReactAgent 的状态消息。
     *
     * @param messages 消息列表
     */
    private void logConversation(List<Message> messages) {
        LOGGER.info("=== 官方 ReactAgent + OpenAI(gpt-4o) ===");
        for (Message message : messages) {
            LOGGER.info(describeMessage(message));
        }
    }

    /**
     * 把消息格式化为可读日志。
     *
     * @param message Spring AI 消息
     * @return 日志文本
     */
    private String describeMessage(Message message) {
        if (message instanceof AssistantMessage assistantMessage) {
            if (assistantMessage.hasToolCalls()) {
                return "ASSISTANT(tool-call) -> " + assistantMessage.getText() + " | " + assistantMessage.getToolCalls();
            }
            return "ASSISTANT -> " + assistantMessage.getText();
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
     * 官方方法工具定义。
     *
     * 职责：把旅行场景能力直接暴露给官方 ReactAgent
     *
     * @author xiexu
     */
    static final class TravelTools {

        /**
         * 查询指定城市的天气。
         *
         * @param city 城市名称
         * @return 天气描述
         */
        @Tool(name = "queryWeather", description = "查询指定城市当天的天气")
        public String queryWeather(@ToolParam(description = "需要查询天气的城市") String city) {
            LOGGER.info("【Tool】queryWeather(" + city + ")");
            return "晴朗，微风，气温25度";
        }

        /**
         * 根据天气推荐景点。
         *
         * @param city 城市名称
         * @param weather 当前天气
         * @return 景点推荐
         */
        @Tool(name = "recommendAttraction", description = "根据城市和天气推荐合适的旅游景点")
        public String recommendAttraction(
                @ToolParam(description = "旅游城市") String city,
                @ToolParam(description = "当前天气") String weather) {
            LOGGER.info("【Tool】recommendAttraction(city=" + city + ", weather=" + weather + ")");
            return "推荐去颐和园泛舟";
        }
    }
}

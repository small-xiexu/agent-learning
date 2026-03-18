package com.xbk.agent.framework.react.application.executor;

import com.xbk.agent.framework.core.common.enums.ToolChoiceMode;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.HelloAgentsLLM;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.ToolCall;
import com.xbk.agent.framework.core.llm.option.ToolCallingOptions;
import com.xbk.agent.framework.core.memory.MemorySession;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.core.memory.support.InMemoryMemory;
import com.xbk.agent.framework.core.tool.ToolContext;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import com.xbk.agent.framework.core.tool.ToolRegistry;
import com.xbk.agent.framework.core.tool.ToolRequest;
import com.xbk.agent.framework.core.tool.ToolResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 纯手写 ReAct Agent
 *
 * 职责：自己手动把“先想一下 -> 再调工具 -> 再看结果 -> 继续想”这一套流程跑起来
 *
 * @author xiexu
 */
public class ReActAgent {

    private static final int DEFAULT_MAX_STEPS = 5;
    private static final String AGENT_ID = "react-agent";
    private static final String FINAL_ANSWER_PREFIX = "Final Answer:";

    private final HelloAgentsLLM helloAgentsLLM;
    private final ToolRegistry toolRegistry;
    private final int maxSteps;

    private List<Message> lastHistory = Collections.emptyList();

    /**
     * 创建使用默认最大步数的 ReActAgent
     *
     * @param helloAgentsLLM 统一 LLM 门面
     * @param toolRegistry 工具注册中心
     */
    public ReActAgent(HelloAgentsLLM helloAgentsLLM, ToolRegistry toolRegistry) {
        this(helloAgentsLLM, toolRegistry, DEFAULT_MAX_STEPS);
    }

    /**
     * 创建 ReActAgent
     *
     * @param helloAgentsLLM 统一 LLM 门面
     * @param toolRegistry 工具注册中心
     * @param maxSteps 最大执行步数
     */
    public ReActAgent(HelloAgentsLLM helloAgentsLLM, ToolRegistry toolRegistry, int maxSteps) {
        if (helloAgentsLLM == null) {
            throw new IllegalArgumentException("helloAgentsLLM must not be null");
        }
        if (toolRegistry == null) {
            throw new IllegalArgumentException("toolRegistry must not be null");
        }
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be greater than zero");
        }
        this.helloAgentsLLM = helloAgentsLLM;
        this.toolRegistry = toolRegistry;
        this.maxSteps = maxSteps;
    }

    /**
     * 执行用户查询
     *
     * @param userQuery 用户问题
     * @return 最终答案
     */
    public String run(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("userQuery must not be blank");
        }

        String conversationId = UUID.randomUUID().toString();
        InMemoryMemory memory = new InMemoryMemory();
        MemorySession memorySession = memory.openSession(conversationId);
        List<Message> history = new ArrayList<Message>();

        appendMessage(history, memorySession, createMessage(conversationId, MessageRole.USER, userQuery));

        int step = 0;
        /*
         * 这里必须用 while (step < maxSteps) 当安全阀。
         * 可以把每一轮理解成一次“先问模型现在该干嘛，再按模型说的继续做”的尝试：
         * 1. 先把目前为止的对话 history 发给模型。
         * 2. 如果模型说“去调工具”，那就真的去调工具，再把工具结果记回 history。
         * 3. 如果模型直接给出了最终答案，那这一轮就结束。
         * 4. 如果模型一直不肯收敛、每轮都还要继续行动，就靠 maxSteps 强行停下来，避免死循环。
         */
        while (step < maxSteps) {
            step++;
            // 先把当前上下文发给模型，问它“下一步该直接回答，还是先去调工具”。
            LlmResponse response = helloAgentsLLM.chat(buildRequest(conversationId, history));
            // 不管模型这轮是思考、要调工具还是已经给答案，都先把它说的话记进上下文。
            appendAssistantMessage(history, memorySession, conversationId, response);

            if (!response.getToolCalls().isEmpty()) {
                // 走到这里说明模型没有急着给最终答案，而是明确要求先调用某个工具。
                ToolCall toolCall = response.getToolCalls().getFirst();
                // 真正执行工具，把模型给出的参数喂给对应工具实现。
                ToolResult toolResult = executeTool(conversationId, memorySession, step, toolCall);
                // 再把工具执行结果包装成“观察结果”写回上下文，供下一轮模型继续参考。
                appendMessage(history, memorySession, createObservationMessage(conversationId, toolCall, toolResult));
                // 这一轮只负责“调工具并记录结果”，然后立刻进入下一轮继续问模型。
                continue;
            }

            // 走到这里说明模型这轮没有再要调工具，那就尝试把它当成最终答案提取出来。
            String finalAnswer = extractFinalAnswer(response);
            if (finalAnswer != null && !finalAnswer.isEmpty()) {
                lastHistory = List.copyOf(history);
                return finalAnswer;
            }
        }

        lastHistory = List.copyOf(history);
        return "未能在最大步骤内完成任务。";
    }

    /**
     * 返回最近一次运行的消息快照
     *
     * @return 消息快照
     */
    public List<Message> latestHistory() {
        return List.copyOf(lastHistory);
    }

    /**
     * 构建 LLM 请求
     *
     * @param conversationId 会话标识
     * @param history 历史消息
     * @return LLM 请求
     */
    private LlmRequest buildRequest(String conversationId, List<Message> history) {
        List<ToolDefinition> toolDefinitions = toolRegistry.definitions();
        return LlmRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .messages(history)
                .availableTools(toolDefinitions)
                .toolCallingOptions(ToolCallingOptions.builder()
                        .enabled(!toolDefinitions.isEmpty())
                        .toolChoiceMode(ToolChoiceMode.AUTO)
                        .includeToolResultsInContext(true)
                        .build())
                .build();
    }

    /**
     * 把本轮模型回复记到上下文里
     *
     * @param history 历史消息
     * @param memorySession 会话内存
     * @param conversationId 会话标识
     * @param response LLM 响应
     */
    private void appendAssistantMessage(
            List<Message> history,
            MemorySession memorySession,
            String conversationId,
            LlmResponse response) {
        Message assistantMessage = response.getOutputMessage();
        if (assistantMessage == null) {
            String content = readResponseText(response);
            if (content == null || content.trim().isEmpty()) {
                return;
            }
            // 如果底层没给标准消息对象，就把返回文本手动包成一条“模型回复消息”。
            assistantMessage = createMessage(conversationId, MessageRole.ASSISTANT, content);
        }
        appendMessage(history, memorySession, assistantMessage);
    }

    /**
     * 真正执行一次工具调用
     *
     * @param conversationId 会话标识
     * @param memorySession 会话内存
     * @param step 当前步数
     * @param toolCall 工具调用
     * @return 工具结果
     */
    private ToolResult executeTool(
            String conversationId,
            MemorySession memorySession,
            int step,
            ToolCall toolCall) {
        ToolRequest request = ToolRequest.builder()
                .toolName(toolCall.getToolName())
                .invocationId(toolCall.getToolCallId() == null ? UUID.randomUUID().toString() : toolCall.getToolCallId())
                .arguments(toolCall.getArguments())
                .build();
        ToolContext context = ToolContext.builder()
                .conversationId(conversationId)
                .agentId(AGENT_ID)
                .turnId("step-" + step)
                .memorySession(memorySession)
                .build();
        return toolRegistry.execute(request, context);
    }

    /**
     * 创建一条普通消息
     *
     * @param conversationId 会话标识
     * @param role 消息角色
     * @param content 消息内容
     * @return 消息对象
     */
    private Message createMessage(String conversationId, MessageRole role, String content) {
        return Message.builder()
                .messageId(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .build();
    }

    /**
     * 把工具结果包成一条“工具返回消息”
     *
     * @param conversationId 会话标识
     * @param toolCall 工具调用
     * @param toolResult 工具结果
     * @return 工具返回消息
     */
    private Message createObservationMessage(String conversationId, ToolCall toolCall, ToolResult toolResult) {
        String observation = toolResult.isSuccess()
                ? "Observation: " + toolResult.getContent()
                : "Observation: 工具执行失败，原因是" + toolResult.getErrorMessage();
        return Message.builder()
                .messageId(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .role(MessageRole.TOOL)
                .name(toolCall.getToolName())
                .toolCallId(toolCall.getToolCallId())
                .content(observation)
                .build();
    }

    /**
     * 尽量把模型返回的文本取出来
     *
     * @param response LLM 响应
     * @return 文本内容
     */
    private String readResponseText(LlmResponse response) {
        if (response.getOutputMessage() != null && response.getOutputMessage().getContent() != null) {
            return response.getOutputMessage().getContent();
        }
        return response.getRawText();
    }

    /**
     * 从模型回复里提取最终答案
     *
     * @param response LLM 响应
     * @return 最终答案
     */
    private String extractFinalAnswer(LlmResponse response) {
        String responseText = readResponseText(response);
        if (responseText == null || responseText.trim().isEmpty()) {
            return null;
        }
        String normalized = responseText.trim();
        if (normalized.startsWith(FINAL_ANSWER_PREFIX)) {
            return normalized.substring(FINAL_ANSWER_PREFIX.length()).trim();
        }
        return normalized;
    }

    /**
     * 同时写入 history 和 memory
     *
     * @param history 历史消息
     * @param memorySession 会话内存
     * @param message 消息对象
     */
    private void appendMessage(List<Message> history, MemorySession memorySession, Message message) {
        history.add(message);
        memorySession.append(message);
    }
}

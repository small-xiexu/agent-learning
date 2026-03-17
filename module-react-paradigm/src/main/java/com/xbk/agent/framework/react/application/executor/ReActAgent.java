package com.xbk.agent.framework.react.application.executor;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.HelloAgentsLLM;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.ToolCall;
import com.xbk.agent.framework.core.memory.MemorySession;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.core.memory.support.InMemoryMemory;
import com.xbk.agent.framework.core.tool.ToolContext;
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
 * 职责：基于统一 LLM 门面和工具注册中心执行 Thought -> Action -> Observation 闭环
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
         * 这里必须使用 while (step < maxSteps) 作为安全阀。
         * 每一轮循环都代表一次完整的“思考 -> 行动 -> 观察”尝试：
         * 1. 先把当前 history 发给 LLM。
         * 2. 如果 LLM 返回工具调用，就执行工具并把 Observation 追加回 history。
         * 3. 如果 LLM 返回最终答案，就结束循环。
         * 4. 如果模型一直要求继续行动，step 会不断增长，直到达到 maxSteps 强制停止。
         */
        while (step < maxSteps) {
            step++;
            LlmResponse response = helloAgentsLLM.chat(buildRequest(conversationId, history));
            appendAssistantMessage(history, memorySession, conversationId, response);

            if (!response.getToolCalls().isEmpty()) {
                ToolCall toolCall = response.getToolCalls().get(0);
                ToolResult toolResult = executeTool(conversationId, memorySession, step, toolCall);
                appendMessage(history, memorySession, createObservationMessage(conversationId, toolCall, toolResult));
                continue;
            }

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
        return LlmRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .messages(history)
                .availableTools(toolRegistry.definitions())
                .build();
    }

    /**
     * 追加助手消息
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
            assistantMessage = createMessage(conversationId, MessageRole.ASSISTANT, content);
        }
        appendMessage(history, memorySession, assistantMessage);
    }

    /**
     * 执行单次工具调用
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
     * 创建普通消息
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
     * 创建 Observation 消息
     *
     * @param conversationId 会话标识
     * @param toolCall 工具调用
     * @param toolResult 工具结果
     * @return Observation 消息
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
     * 读取响应文本
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
     * 解析最终答案
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
     * 追加消息到历史和会话内存
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

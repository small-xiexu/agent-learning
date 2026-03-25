package com.xbk.agent.framework.conversation.infrastructure.agentframework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.conversation.domain.memory.ConversationTurn;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleContract;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.support.ConversationStateSupport;
import com.xbk.agent.framework.conversation.support.ConversationPromptTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Engineer 节点（框架版 AutoGen 群聊）
 *
 * 职责：读取 OverAllState 中的共享群聊历史，扮演工程师角色输出最新完整 Python 脚本，
 * 将发言追加回 shared_messages 和 transcript，推动 RoundRobin 轮转到 CodeReviewerNode。
 *
 * <p>在 AutoGen 群聊范式中的定位：
 * Engineer 是群聊中唯一有权产出"完整 Python 脚本"的节点。
 * 每一轮无论修改多少，都必须输出包含所有功能的完整脚本，而不是增量补丁，
 * 这保证了 CodeReviewer 每轮看到的都是可独立运行的完整代码。
 *
 * <p>与手写版 EngineerAgent 的对照：
 * <pre>
 *   手写版 EngineerAgent.execute(history, task, conversationId)：
 *     → 接收历史列表参数，返回本轮代码字符串
 *     → 代码清洗（stripCodeFence）在调用方完成
 *
 *   框架版 EngineerNode.apply(OverAllState state)：
 *     → 从 OverAllState 读取 shared_messages，自己调用 normalizePythonScript 清洗
 *     → 额外维护 current_python_script 字段，便于最终结果直接提取最新完整脚本
 *     → 返回增量 Map，框架 merge 进全局状态
 * </pre>
 *
 * @author xiexu
 */
public class EngineerNode implements AsyncNodeAction {

    private static final String REQUEST_PREFIX = "conversation-flow-engineer-";

    /**
     * 统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 工程师契约。
     */
    private final ConversationRoleContract contract;

    /**
     * 创建工程师节点。
     *
     * @param agentLlmGateway 统一网关
     * @param contract 角色契约
     */
    public EngineerNode(AgentLlmGateway agentLlmGateway, ConversationRoleContract contract) {
        this.agentLlmGateway = agentLlmGateway;
        this.contract = contract;
    }

    /**
     * 执行工程师节点。
     *
     * @param state 全局状态
     * @return 写回状态
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        // 读取当前群聊的会话标识，保证 Engineer 本轮输出继续写回同一条会话链路。
        String conversationId = state.value("conversation_id", "");
        // 从状态里恢复共享群聊历史，让 Engineer 能看到 PM 的最新需求和 Reviewer 的历史意见。
        List<Message> sharedMessages = ConversationStateSupport.readSharedMessages(
                state.value("shared_messages").orElse(List.of()));
        // 组装当前节点要发给模型的完整输入消息。
        List<Message> requestMessages = new ArrayList<Message>();
        // 先放 Engineer 自己的系统提示，约束这一轮必须交付完整脚本而不是零散解释。
        requestMessages.add(buildMessage(conversationId, MessageRole.SYSTEM, contract.getSystemPrompt(), contract.getAgentName()));
        // 再把共享群聊历史拼上去，让模型基于整个上下文继续产出代码。
        requestMessages.addAll(sharedMessages);
        // 调模型生成本轮工程输出。
        String output = chat(conversationId, List.copyOf(requestMessages));
        // 对模型输出做脚本标准化清洗，去掉 markdown 围栏，只保留最终完整 Python 脚本正文。
        String normalizedScript = ConversationPromptTemplates.normalizePythonScript(output);
        // 把 Engineer 这轮产出的最新脚本写回共享消息历史，供 Reviewer 下一步继续消费。
        sharedMessages.add(buildMessage(conversationId, MessageRole.ASSISTANT, normalizedScript, contract.getRoleType().getDisplayName()));
        // 从状态里恢复 transcript，保留这一轮的简化回放记录。
        List<ConversationTurn> transcript = ConversationStateSupport.readTranscript(state.value("transcript").orElse(List.of()));
        // 追加一条 Engineer 发言记录，并按当前 transcript 长度递增轮次号。
        transcript.add(new ConversationTurn(transcript.size() + 1, ConversationRoleType.ENGINEER, normalizedScript));
        // Engineer 执行完成后，总轮次加一，为后续停止判断提供依据。
        int nextTurnCount = state.value("turn_count", Integer.class).orElse(Integer.valueOf(0)) + 1;
        // 把当前节点计算出的最新状态一次性写回给 Flow runtime。
        return CompletableFuture.completedFuture(Map.of(
                // 记录 Engineer 最近一次输出，便于调试和测试断言。
                "last_engineer_output", normalizedScript,
                // 把最新完整脚本单独保存下来，方便 Reviewer 和最终结果直接读取。
                "current_python_script", normalizedScript,
                // Engineer 交付完成后，下一位固定轮到 CodeReviewer。
                "active_role", ConversationRoleType.CODE_REVIEWER.getStateValue(),
                // 写回更新后的总轮次。
                "turn_count", Integer.valueOf(nextTurnCount),
                // Engineer 节点本身没有结束权限，所以这里保持未完成状态。
                "done", Boolean.FALSE,
                // 停止原因沿用旧值，避免在非终止节点里误覆盖结束信息。
                "stop_reason", state.value("stop_reason", ""),
                // 把更新后的共享消息重新编码为可持久化到状态里的结构。
                "shared_messages", ConversationStateSupport.toStateMessages(sharedMessages),
                // 把更新后的 transcript 重新编码为可持久化到状态里的结构。
                "transcript", ConversationStateSupport.toStateTranscript(transcript)));
    }

    /**
     * 调用统一网关。
     *
     * @param conversationId 会话标识
     * @param messages 消息列表
     * @return 文本响应
     */
    private String chat(String conversationId, List<Message> messages) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(REQUEST_PREFIX + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(messages)
                .build());
        if (response.getOutputMessage() != null && response.getOutputMessage().getContent() != null) {
            return response.getOutputMessage().getContent();
        }
        return response.getRawText();
    }

    /**
     * 构造统一消息。
     *
     * @param conversationId 会话标识
     * @param role 消息角色
     * @param content 内容
     * @param name 名称
     * @return 统一消息
     */
    private Message buildMessage(String conversationId, MessageRole role, String content, String name) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .content(content == null ? "" : content)
                .name(name)
                .build();
    }
}

package com.xbk.agent.framework.camel.application.executor;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.camel.domain.memory.CamelConversationMemory;
import com.xbk.agent.framework.camel.domain.role.CamelRoleContract;
import com.xbk.agent.framework.camel.support.CamelPromptTemplates;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 CAMEL 交易员执行器
 *
 * 职责：基于统一网关扮演交易员角色，提出下一步需求或给出终止标记
 *
 * @author xiexu
 */
public class CamelTraderAgent {

    private static final String REQUEST_PREFIX = "camel-trader-";

    /**
     * 底层统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 交易员角色契约，约束它的身份和发言边界。
     */
    private final CamelRoleContract contract;

    /**
     * 创建交易员执行器。
     *
     * @param agentLlmGateway 统一 LLM 网关
     */
    public CamelTraderAgent(AgentLlmGateway agentLlmGateway) {
        this(agentLlmGateway, CamelRoleContract.traderContract());
    }

    /**
     * 创建交易员执行器。
     *
     * @param agentLlmGateway 统一 LLM 网关
     * @param contract 角色契约
     */
    public CamelTraderAgent(AgentLlmGateway agentLlmGateway, CamelRoleContract contract) {
        this.agentLlmGateway = agentLlmGateway;
        this.contract = contract;
    }

    /**
     * 让交易员基于当前 memory 回复。
     *
     * @param task 原始任务
     * @param memory 对话记忆
     * @param conversationId 会话标识
     * @return 交易员输出
     */
    public String reply(String task, CamelConversationMemory memory, String conversationId) {
        List<Message> messages = memory.toMessagesForRole(
                conversationId,
                contract,
                CamelPromptTemplates.traderInitialPrompt(task));
        // 交易员看到的是“自己的 system prompt + 当前任务 + 从自己视角重放后的 transcript”，
        // 因此它天然只会把自己当成审查者，把程序员上一轮内容当成待响应输入。
        return chat(conversationId, messages);
    }

    /**
     * 返回角色契约。
     *
     * @return 角色契约
     */
    public CamelRoleContract getContract() {
        return contract;
    }

    /**
     * 调用统一网关并提取文本结果。
     *
     * @param conversationId 会话标识
     * @param messages 当前轮消息列表
     * @return 文本结果
     */
    private String chat(String conversationId, List<Message> messages) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(REQUEST_PREFIX + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(messages)
                .build());
        return extractText(response);
    }

    /**
     * 提取响应文本。
     *
     * @param response LLM 响应
     * @return 响应文本
     */
    private String extractText(LlmResponse response) {
        if (response.getOutputMessage() != null && response.getOutputMessage().getContent() != null) {
            return response.getOutputMessage().getContent();
        }
        return response.getRawText();
    }
}

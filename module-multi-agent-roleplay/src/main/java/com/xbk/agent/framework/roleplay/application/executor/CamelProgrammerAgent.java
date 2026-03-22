package com.xbk.agent.framework.roleplay.application.executor;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.roleplay.domain.memory.CamelConversationMemory;
import com.xbk.agent.framework.roleplay.domain.role.CamelRoleContract;
import com.xbk.agent.framework.roleplay.support.CamelPromptTemplates;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 CAMEL 程序员执行器
 *
 * 职责：基于统一网关扮演程序员角色，按交易员需求输出一段完整 Java 代码
 *
 * @author xiexu
 */
public class CamelProgrammerAgent {

    private static final String REQUEST_PREFIX = "camel-programmer-";

    /**
     * 底层统一 LLM 网关。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 程序员角色契约，约束它只根据最新需求写实现。
     */
    private final CamelRoleContract contract;

    /**
     * 创建程序员执行器。
     *
     * @param agentLlmGateway 统一 LLM 网关
     */
    public CamelProgrammerAgent(AgentLlmGateway agentLlmGateway) {
        this(agentLlmGateway, CamelRoleContract.programmerContract());
    }

    /**
     * 创建程序员执行器。
     *
     * @param agentLlmGateway 统一 LLM 网关
     * @param contract 角色契约
     */
    public CamelProgrammerAgent(AgentLlmGateway agentLlmGateway, CamelRoleContract contract) {
        this.agentLlmGateway = agentLlmGateway;
        this.contract = contract;
    }

    /**
     * 让程序员基于当前 memory 回复。
     *
     * @param task 原始任务
     * @param memory 对话记忆
     * @param conversationId 会话标识
     * @return 程序员输出
     */
    public String reply(String task, CamelConversationMemory memory, String conversationId) {
        List<Message> messages = memory.toMessagesForRole(
                conversationId,
                contract,
                CamelPromptTemplates.programmerInitialPrompt(task));
        // 程序员和交易员复用同一份 transcript，但映射成不同消息角色后，
        // 程序员拿到的上下文就会自然收敛成“上一条需求 -> 当前应交付的一段代码”。
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

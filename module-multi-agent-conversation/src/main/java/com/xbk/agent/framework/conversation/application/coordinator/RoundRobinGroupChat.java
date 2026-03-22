package com.xbk.agent.framework.conversation.application.coordinator;

import com.xbk.agent.framework.conversation.api.ConversationRunResult;
import com.xbk.agent.framework.conversation.application.executor.CodeReviewerAgent;
import com.xbk.agent.framework.conversation.application.executor.EngineerAgent;
import com.xbk.agent.framework.conversation.application.executor.ProductManagerAgent;
import com.xbk.agent.framework.conversation.domain.memory.ConversationMemory;
import com.xbk.agent.framework.conversation.domain.memory.ConversationTurn;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;
import com.xbk.agent.framework.conversation.support.ConversationPromptTemplates;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 RoundRobin 群聊协调器
 *
 * 职责：显式维护 ProductManager、Engineer、CodeReviewer 的固定轮询群聊闭环
 *
 * @author xiexu
 */
public class RoundRobinGroupChat {

    /**
     * 产品经理智能体。
     */
    private final ProductManagerAgent productManagerAgent;

    /**
     * 工程师智能体。
     */
    private final EngineerAgent engineerAgent;

    /**
     * 代码审查员智能体。
     */
    private final CodeReviewerAgent codeReviewerAgent;

    /**
     * 全局共享群聊记忆。
     */
    private final ConversationMemory memory;

    /**
     * 最大轮次数。
     */
    private final int maxTurns;

    /**
     * 创建 RoundRobin 群聊协调器。
     *
     * @param productManagerAgent 产品经理智能体
     * @param engineerAgent 工程师智能体
     * @param codeReviewerAgent 审查员智能体
     * @param memory 共享记忆
     * @param maxTurns 最大轮次
     */
    public RoundRobinGroupChat(ProductManagerAgent productManagerAgent,
                               EngineerAgent engineerAgent,
                               CodeReviewerAgent codeReviewerAgent,
                               ConversationMemory memory,
                               int maxTurns) {
        this.productManagerAgent = productManagerAgent;
        this.engineerAgent = engineerAgent;
        this.codeReviewerAgent = codeReviewerAgent;
        this.memory = memory;
        this.maxTurns = maxTurns;
    }

    /**
     * 运行手写版 RoundRobin 群聊。
     *
     * @param task 原始任务
     * @return 运行结果
     */
    public ConversationRunResult run(String task) {
        memory.clear();
        String conversationId = "handwritten-conversation-" + UUID.randomUUID();
        memory.seedTask(conversationId, task);
        String latestPythonScript = "";
        int turnCount = 0;
        int speakerIndex = 0;
        List<ConversationRoleType> speakers = List.of(
                ConversationRoleType.PRODUCT_MANAGER,
                ConversationRoleType.ENGINEER,
                ConversationRoleType.CODE_REVIEWER);

        while (turnCount < maxTurns) {
            ConversationRoleType currentSpeaker = speakers.get(speakerIndex);
            if (currentSpeaker == ConversationRoleType.PRODUCT_MANAGER) {
                String output = ConversationPromptTemplates.stripTaskDoneMarker(
                        productManagerAgent.reply(memory, conversationId));
                memory.appendTurn(conversationId, ConversationRoleType.PRODUCT_MANAGER, output);
                turnCount++;
            } else if (currentSpeaker == ConversationRoleType.ENGINEER) {
                String output = engineerAgent.reply(memory, conversationId);
                latestPythonScript = ConversationPromptTemplates.normalizePythonScript(output);
                memory.appendTurn(conversationId, ConversationRoleType.ENGINEER, latestPythonScript);
                turnCount++;
            } else {
                String output = codeReviewerAgent.reply(memory, conversationId);
                memory.appendTurn(conversationId, ConversationRoleType.CODE_REVIEWER, output);
                turnCount++;
                if (ConversationPromptTemplates.containsTaskDoneMarker(output)) {
                    return buildResult(task, latestPythonScript, ConversationRoleType.CODE_REVIEWER, output);
                }
            }
            speakerIndex = (speakerIndex + 1) % speakers.size();
        }

        return buildResult(task, latestPythonScript, lastRole(), "MAX_TURNS_REACHED");
    }

    /**
     * 构造统一结果。
     *
     * @param task 原始任务
     * @param latestPythonScript 最新脚本
     * @param stopRole 停止角色
     * @param stopReason 停止原因
     * @return 运行结果
     */
    private ConversationRunResult buildResult(String task,
                                              String latestPythonScript,
                                              ConversationRoleType stopRole,
                                              String stopReason) {
        return new ConversationRunResult(
                task,
                latestPythonScript,
                stopRole,
                stopReason,
                memory.snapshotTranscript(),
                memory.snapshotSharedMessages(),
                null);
    }

    /**
     * 返回最后发言角色。
     *
     * @return 最后发言角色
     */
    private ConversationRoleType lastRole() {
        List<ConversationTurn> transcript = memory.snapshotTranscript();
        if (transcript.isEmpty()) {
            return ConversationRoleType.PRODUCT_MANAGER;
        }
        return transcript.get(transcript.size() - 1).getRoleType();
    }
}

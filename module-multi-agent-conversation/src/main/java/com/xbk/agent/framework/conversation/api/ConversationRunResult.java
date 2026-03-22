package com.xbk.agent.framework.conversation.api;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.conversation.domain.memory.ConversationTurn;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;

import java.util.List;
import java.util.Optional;

/**
 * Conversation 运行结果
 *
 * 职责：统一承载手写版和框架版群聊的最终脚本、停止信息与共享上下文快照
 *
 * @author xiexu
 */
public class ConversationRunResult {

    /**
     * 原始任务。
     */
    private final String task;

    /**
     * 最终沉淀出的 Python 脚本。
     */
    private final String finalPythonScript;

    /**
     * 触发停止的角色。
     */
    private final ConversationRoleType stopRole;

    /**
     * 停止原因。
     */
    private final String stopReason;

    /**
     * 群聊 transcript。
     */
    private final List<ConversationTurn> transcript;

    /**
     * 群聊共享消息快照。
     */
    private final List<Message> sharedMessages;

    /**
     * FlowAgent 最终状态；手写版为空。
     */
    private final OverAllState flowState;

    /**
     * 创建运行结果。
     *
     * @param task 原始任务
     * @param finalPythonScript 最终脚本
     * @param stopRole 停止角色
     * @param stopReason 停止原因
     * @param transcript transcript
     * @param sharedMessages 共享消息
     * @param flowState 图状态
     */
    public ConversationRunResult(String task,
                                 String finalPythonScript,
                                 ConversationRoleType stopRole,
                                 String stopReason,
                                 List<ConversationTurn> transcript,
                                 List<Message> sharedMessages,
                                 OverAllState flowState) {
        this.task = task;
        this.finalPythonScript = finalPythonScript == null ? "" : finalPythonScript;
        this.stopRole = stopRole;
        this.stopReason = stopReason == null ? "" : stopReason;
        this.transcript = List.copyOf(transcript);
        this.sharedMessages = List.copyOf(sharedMessages);
        this.flowState = flowState;
    }

    /**
     * 返回原始任务。
     *
     * @return 原始任务
     */
    public String getTask() {
        return task;
    }

    /**
     * 返回最终脚本。
     *
     * @return 最终脚本
     */
    public String getFinalPythonScript() {
        return finalPythonScript;
    }

    /**
     * 返回停止角色。
     *
     * @return 停止角色
     */
    public ConversationRoleType getStopRole() {
        return stopRole;
    }

    /**
     * 返回停止原因。
     *
     * @return 停止原因
     */
    public String getStopReason() {
        return stopReason;
    }

    /**
     * 返回 transcript。
     *
     * @return transcript
     */
    public List<ConversationTurn> getTranscript() {
        return transcript;
    }

    /**
     * 返回共享消息。
     *
     * @return 共享消息
     */
    public List<Message> getSharedMessages() {
        return sharedMessages;
    }

    /**
     * 返回图状态。
     *
     * @return 图状态
     */
    public Optional<OverAllState> getFlowState() {
        return Optional.ofNullable(flowState);
    }
}

package com.xbk.agent.framework.supervisor.handwritten.memory;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.supervisor.domain.memory.SupervisorStepRecord;
import com.xbk.agent.framework.supervisor.domain.routing.RoutingDecision;
import com.xbk.agent.framework.supervisor.domain.routing.SupervisorWorkerType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 手写版 Supervisor 全局 Scratchpad
 *
 * 职责：集中维护监督者决策、Worker 输出与原始任务的消息历史和审计记录。
 * 它承担的是“完整历史”角色，而不是“当前状态”角色。
 * 可以把它理解成一份既能给下一轮 Prompt 回放、又能给人类做事后审计的长时记忆。
 *
 * @author xiexu
 */
public final class SupervisorScratchpad {

    private final List<Message> messages;
    // stepRecords 是面向人类阅读的“步骤审计摘要”，而 messages 更偏向给模型做历史回放。
    private final List<SupervisorStepRecord> stepRecords;

    /**
     * 创建空白 Scratchpad。
     */
    public SupervisorScratchpad() {
        this.messages = new ArrayList<Message>();
        this.stepRecords = new ArrayList<SupervisorStepRecord>();
    }

    /**
     * 清空所有消息和记录。
     */
    public void clear() {
        this.messages.clear();
        this.stepRecords.clear();
    }

    /**
     * 写入原始任务消息。
     *
     * 由于手写版没有框架自动保存初始输入，因此任务本身也要手动落到 Scratchpad，
     * 否则后续回放时只能看到“主管说了什么”，却看不到“任务最初要求什么”。
     *
     * @param conversationId 会话标识
     * @param task 原始任务
     */
    public void seedTask(String conversationId, String task) {
        messages.add(buildMessage(conversationId, MessageRole.USER, "UserTask", task));
    }

    /**
     * 追加监督者决策消息。
     *
     * @param conversationId 会话标识
     * @param decision 监督者决策
     */
    public void appendSupervisorDecision(String conversationId, RoutingDecision decision) {
        // 手写版把结构化决策拍平成统一消息，方便下一轮 Prompt 回放和最终审计。
        String content = "next_worker=" + decision.getNextWorker().getDecisionValue()
                + System.lineSeparator()
                + "task_instruction=" + decision.getTaskInstruction();
        messages.add(buildMessage(conversationId, MessageRole.ASSISTANT, "Supervisor", content));
    }

    /**
     * 追加 Worker 输出消息。
     *
     * @param conversationId 会话标识
     * @param workerType Worker 类型
     * @param workerOutput Worker 输出
     */
    public void appendWorkerOutput(String conversationId, SupervisorWorkerType workerType, String workerOutput) {
        // name 字段直接记录 Worker 身份，后续渲染 Scratchpad 时更容易看出谁说了什么。
        messages.add(buildMessage(conversationId, MessageRole.ASSISTANT, workerType.name(), workerOutput));
    }

    /**
     * 追加单步执行记录。
     *
     * @param stepRecord 单步执行记录
     */
    public void appendStepRecord(SupervisorStepRecord stepRecord) {
        this.stepRecords.add(stepRecord);
    }

    /**
     * 返回消息历史快照。
     *
     * 这份历史主要给下一轮 Supervisor Prompt 回放使用，帮助主管理解上下文。
     *
     * @return 消息历史快照
     */
    public List<Message> snapshotMessages() {
        return List.copyOf(messages);
    }

    /**
     * 返回执行记录快照。
     *
     * 这份记录主要给测试、日志和教学展示使用，强调“每一轮派了谁、做了什么、产出了什么”。
     *
     * @return 执行记录快照
     */
    public List<SupervisorStepRecord> snapshotStepRecords() {
        return List.copyOf(stepRecords);
    }

    /**
     * 构造统一消息。
     *
     * @param conversationId 会话标识
     * @param role 消息角色
     * @param name 消息名
     * @param content 消息内容
     * @return 统一消息
     */
    private Message buildMessage(String conversationId, MessageRole role, String name, String content) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .name(name)
                .content(content == null ? "" : content)
                .build();
    }
}

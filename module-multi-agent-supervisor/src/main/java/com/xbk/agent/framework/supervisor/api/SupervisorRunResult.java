package com.xbk.agent.framework.supervisor.api;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.supervisor.domain.memory.SupervisorStepRecord;
import com.xbk.agent.framework.supervisor.domain.routing.SupervisorWorkerType;

import java.util.List;
import java.util.Optional;

/**
 * Supervisor 统一运行结果
 *
 * 职责：统一承载手写版与框架版 Supervisor 的阶段产物、路由轨迹与可选框架状态，
 * 让测试、Demo 和上层调用方都只面向一个稳定结果模型编程
 *
 * @author xiexu
 */
public final class SupervisorRunResult {

    private final String task;
    private final String chineseDraft;
    private final String englishTranslation;
    private final String reviewedEnglish;
    private final SupervisorWorkerType stopWorker;
    private final String stopReason;
    private final List<SupervisorWorkerType> routeTrail;
    private final List<SupervisorStepRecord> stepRecords;
    private final List<Message> scratchpadMessages;
    private final OverAllState flowState;

    /**
     * 创建统一运行结果。
     *
     * @param task 原始任务
     * @param chineseDraft 中文初稿
     * @param englishTranslation 英文译稿
     * @param reviewedEnglish 审校后的英文稿
     * @param stopWorker 停止时的监督者决策
     * @param stopReason 停止原因
     * @param routeTrail 路由轨迹
     * @param stepRecords 子任务执行记录
     * @param scratchpadMessages 全局消息历史
     * @param flowState 框架状态
     */
    public SupervisorRunResult(String task,
                               String chineseDraft,
                               String englishTranslation,
                               String reviewedEnglish,
                               SupervisorWorkerType stopWorker,
                               String stopReason,
                               List<SupervisorWorkerType> routeTrail,
                               List<SupervisorStepRecord> stepRecords,
                               List<Message> scratchpadMessages,
                               OverAllState flowState) {
        this.task = task;
        this.chineseDraft = chineseDraft;
        this.englishTranslation = englishTranslation;
        this.reviewedEnglish = reviewedEnglish;
        this.stopWorker = stopWorker;
        this.stopReason = stopReason;
        this.routeTrail = List.copyOf(routeTrail);
        this.stepRecords = List.copyOf(stepRecords);
        this.scratchpadMessages = List.copyOf(scratchpadMessages);
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
     * 返回中文初稿。
     *
     * @return 中文初稿
     */
    public String getChineseDraft() {
        return chineseDraft;
    }

    /**
     * 返回英文译稿。
     *
     * @return 英文译稿
     */
    public String getEnglishTranslation() {
        return englishTranslation;
    }

    /**
     * 返回审校后的英文稿。
     *
     * @return 审校后的英文稿
     */
    public String getReviewedEnglish() {
        return reviewedEnglish;
    }

    /**
     * 返回停止时的监督者决策。
     *
     * @return 停止时的监督者决策
     */
    public SupervisorWorkerType getStopWorker() {
        return stopWorker;
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
     * 返回路由轨迹。
     *
     * @return 路由轨迹
     */
    public List<SupervisorWorkerType> getRouteTrail() {
        return routeTrail;
    }

    /**
     * 返回执行记录。
     *
     * @return 执行记录
     */
    public List<SupervisorStepRecord> getStepRecords() {
        return stepRecords;
    }

    /**
     * 返回全局消息历史。
     *
     * @return 全局消息历史
     */
    public List<Message> getScratchpadMessages() {
        return scratchpadMessages;
    }

    /**
     * 返回可选框架状态。
     *
     * 手写版没有 Spring AI Alibaba 状态图，因此这里会返回空；
     * 框架版则把原始 OverAllState 暴露出来，便于调试和二次分析。
     *
     * @return 可选框架状态
     */
    public Optional<OverAllState> getFlowState() {
        return Optional.ofNullable(flowState);
    }

    /**
     * 返回最终产物。
     *
     * Supervisor 的最终产物遵循“谁离完成最近就优先返回谁”的原则：
     * 优先返回审校稿，其次返回英文译稿，最后才回退到中文初稿。
     *
     * @return 最终产物
     */
    public String getFinalOutput() {
        if (reviewedEnglish != null && !reviewedEnglish.isBlank()) {
            return reviewedEnglish;
        }
        if (englishTranslation != null && !englishTranslation.isBlank()) {
            return englishTranslation;
        }
        return chineseDraft == null ? "" : chineseDraft;
    }
}

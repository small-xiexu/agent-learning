package com.xbk.agent.framework.supervisor.domain.state;

import com.xbk.agent.framework.supervisor.domain.routing.SupervisorWorkerType;

import java.util.ArrayList;
import java.util.List;

/**
 * Supervisor 工作流状态
 *
 * 职责：集中保存当前任务在中文初稿、英文译稿、审校稿和路由轨迹上的最新状态。
 * 它表达的是“当前事实快照”，而不是完整执行日志；完整日志由 Scratchpad 维护。
 * 对初学者来说，可以把它理解成“主管此刻看到的黑板最新内容”，
 * 而不是“从第一轮到当前轮的全部聊天记录”。
 *
 * @author xiexu
 */
public final class SupervisorWorkflowState {

    private final String task;
    private final String conversationId;
    private final List<SupervisorWorkerType> routeTrail;
    private String chineseDraft;
    private String englishTranslation;
    private String reviewedEnglish;
    // 这里统计的是真正完成的 Worker 执行次数，不包含 Supervisor 自己做路由判断的次数。
    private int completedWorkerSteps;

    /**
     * 创建工作流状态。
     *
     * @param task 原始任务
     * @param conversationId 会话标识
     */
    public SupervisorWorkflowState(String task, String conversationId) {
        this.task = task;
        this.conversationId = conversationId;
        this.routeTrail = new ArrayList<SupervisorWorkerType>();
        this.chineseDraft = "";
        this.englishTranslation = "";
        this.reviewedEnglish = "";
        this.completedWorkerSteps = 0;
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
     * 返回会话标识。
     *
     * @return 会话标识
     */
    public String getConversationId() {
        return conversationId;
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
     * 返回已完成的 Worker 步数。
     *
     * @return 已完成的 Worker 步数
     */
    public int getCompletedWorkerSteps() {
        return completedWorkerSteps;
    }

    /**
     * 记录监督者路由轨迹。
     *
     * 这里记录的是 Supervisor 每轮的真实决策，包含最终的 `FINISH`。
     * 这样最终结果就能准确回放“主管实际是怎么收敛到结束态”的。
     *
     * @param workerType 本轮路由到的 Worker
     */
    public void recordRoute(SupervisorWorkerType workerType) {
        routeTrail.add(workerType);
    }

    /**
     * 根据 Worker 类型写入对应产物。
     *
     * 这个方法只关心“当前事实应该更新成什么”，不负责保留历史版本。
     * 历史回放与审计由 Scratchpad 负责，两者分离后，状态模型会更容易理解。
     *
     * @param workerType Worker 类型
     * @param workerOutput Worker 输出
     */
    public void applyWorkerOutput(SupervisorWorkerType workerType, String workerOutput) {
        String normalizedOutput = workerOutput == null ? "" : workerOutput.trim();
        if (workerType == SupervisorWorkerType.WRITER) {
            // Writer 负责把任务从“只有目标”推进到“已有中文初稿”。
            this.chineseDraft = normalizedOutput;
            this.completedWorkerSteps++;
            return;
        }
        if (workerType == SupervisorWorkerType.TRANSLATOR) {
            // Translator 只消费中文初稿，并把结果沉淀成英文译稿。
            this.englishTranslation = normalizedOutput;
            this.completedWorkerSteps++;
            return;
        }
        if (workerType == SupervisorWorkerType.REVIEWER) {
            // Reviewer 产出的内容默认视为最终最接近完成态的版本。
            this.reviewedEnglish = normalizedOutput;
            this.completedWorkerSteps++;
        }
    }

    /**
     * 返回路由轨迹快照。
     *
     * 返回结果里保留 `FINISH`，这样调用方能看到主管最后是“主动宣布结束”，
     * 而不是只能猜测流程为什么停下来。
     *
     * @return 路由轨迹快照
     */
    public List<SupervisorWorkerType> snapshotRouteTrail() {
        return List.copyOf(routeTrail);
    }

    /**
     * 返回最终产物。
     *
     * 工作流没有走到 Reviewer 时，也允许调用方拿到当前最成熟的阶段结果，
     * 这样在 MAX_ROUNDS_REACHED 等场景下仍然可以输出尽可能完整的内容。
     *
     * @return 最终产物
     */
    public String getFinalOutput() {
        if (!reviewedEnglish.isBlank()) {
            return reviewedEnglish;
        }
        if (!englishTranslation.isBlank()) {
            return englishTranslation;
        }
        return chineseDraft;
    }
}

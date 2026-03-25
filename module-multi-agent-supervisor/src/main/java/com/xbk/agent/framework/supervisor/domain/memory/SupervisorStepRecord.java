package com.xbk.agent.framework.supervisor.domain.memory;

import com.xbk.agent.framework.supervisor.domain.routing.SupervisorWorkerType;

/**
 * Supervisor 单步执行记录
 *
 * 职责：记录每次子 Agent 执行的 Worker、任务指令与输出结果。
 * 它是对“这一轮到底派了谁、派它做什么、它交回了什么”的最小审计单元。
 * 注意：这里只记录真正发生过的 Worker 执行，不记录单独的 FINISH 决策。
 *
 * @author xiexu
 */
public final class SupervisorStepRecord {

    private final int stepIndex;
    // stepIndex 对应的是第几次 Worker 真正落地产出，不是第几轮 Supervisor 路由判断。
    private final SupervisorWorkerType workerType;
    private final String taskInstruction;
    private final String workerOutput;

    /**
     * 创建执行记录。
     *
     * @param stepIndex 步骤编号
     * @param workerType Worker 类型
     * @param taskInstruction 子任务指令
     * @param workerOutput Worker 输出
     */
    public SupervisorStepRecord(int stepIndex,
                                SupervisorWorkerType workerType,
                                String taskInstruction,
                                String workerOutput) {
        this.stepIndex = stepIndex;
        this.workerType = workerType;
        this.taskInstruction = taskInstruction == null ? "" : taskInstruction;
        this.workerOutput = workerOutput == null ? "" : workerOutput;
    }

    /**
     * 返回步骤编号。
     *
     * @return 步骤编号
     */
    public int getStepIndex() {
        return stepIndex;
    }

    /**
     * 返回 Worker 类型。
     *
     * @return Worker 类型
     */
    public SupervisorWorkerType getWorkerType() {
        return workerType;
    }

    /**
     * 返回子任务指令。
     *
     * @return 子任务指令
     */
    public String getTaskInstruction() {
        return taskInstruction;
    }

    /**
     * 返回 Worker 输出。
     *
     * @return Worker 输出
     */
    public String getWorkerOutput() {
        return workerOutput;
    }
}

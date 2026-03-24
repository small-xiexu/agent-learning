package com.xbk.agent.framework.supervisor.domain.routing;

/**
 * 监督者路由决策
 *
 * 职责：统一表达“下一位 Worker 是谁”以及“交给它的任务指令”。
 * 它是手写版 JSON 路由协议落到 Java 领域模型后的最小执行单元。
 *
 * @author xiexu
 */
public final class RoutingDecision {

    private final SupervisorWorkerType nextWorker;
    private final String taskInstruction;

    /**
     * 创建路由决策。
     *
     * @param nextWorker 下一位 Worker
     * @param taskInstruction 子任务指令
     */
    public RoutingDecision(SupervisorWorkerType nextWorker, String taskInstruction) {
        if (nextWorker == null) {
            throw new IllegalArgumentException("nextWorker must not be null");
        }
        this.nextWorker = nextWorker;
        this.taskInstruction = taskInstruction == null ? "" : taskInstruction;
    }

    /**
     * 返回下一位 Worker。
     *
     * @return 下一位 Worker
     */
    public SupervisorWorkerType getNextWorker() {
        return nextWorker;
    }

    /**
     * 返回子任务指令。
     *
     * @return 子任务指令
     */
    public String getTaskInstruction() {
        return taskInstruction;
    }
}

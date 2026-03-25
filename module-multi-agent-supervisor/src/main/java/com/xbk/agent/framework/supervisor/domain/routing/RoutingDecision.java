package com.xbk.agent.framework.supervisor.domain.routing;

/**
 * 监督者路由决策
 *
 * 职责：统一表达”下一位 Worker 是谁”以及”交给它的任务指令”，
 * 是 Supervisor 路由 JSON 协议落到 Java 领域模型后的最小执行单元。
 *
 * <p>从 JSON 协议到本对象的完整映射流程：
 * <pre>
 *   1. Supervisor（LLM）输出 JSON 字符串，格式约定如下：
 *      { “next”: “writer”, “instruction”: “请撰写一篇关于AI的博客初稿” }
 *
 *   2. SupervisorDecisionJsonParser 解析上述 JSON：
 *      → next 字段 → SupervisorWorkerType.fromRouteValue(“writer”) → nextWorker
 *      → instruction 字段 → taskInstruction
 *
 *   3. 解析结果封装成本对象 RoutingDecision，交给协调器执行下一步路由
 *
 *   4. 协调器根据 nextWorker 找到对应的 Worker Agent 并传入 taskInstruction 驱动执行
 * </pre>
 *
 * <p>设计为 final + 全字段 final：
 * 每次路由决策是 Supervisor 当轮推理的结果，不应被后续流程修改，
 * 保证”Supervisor 决定了什么”与”Worker 实际执行了什么”之间的可追溯性。
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

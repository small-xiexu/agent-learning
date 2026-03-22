package com.xbk.agent.framework.roleplay.api;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.roleplay.domain.memory.CamelDialogueTurn;
import com.xbk.agent.framework.roleplay.domain.role.CamelRoleType;

import java.util.List;
import java.util.Optional;

/**
 * CAMEL 运行结果
 *
 * 职责：统一承载手写版与框架版的最终脚本、停止信息与对话轨迹
 *
 * @author xiexu
 */
public class CamelRunResult {

    /**
     * 原始协作任务。
     */
    private final String task;

    /**
     * 最终沉淀出的 Java 代码。
     */
    private final String finalJavaCode;

    /**
     * 最后触发停止的一侧角色。
     */
    private final CamelRoleType stopRole;

    /**
     * 停止原因，可能是终止标记原文，也可能是最大轮次兜底。
     */
    private final String stopReason;

    /**
     * 双方完整对话轨迹。
     */
    private final List<CamelDialogueTurn> transcript;

    /**
     * FlowAgent 版的最终状态快照；手写版为空。
     */
    private final OverAllState flowState;

    /**
     * 创建运行结果。
     *
     * @param task 原始任务
     * @param finalJavaCode 最终 Java 代码
     * @param stopRole 停止角色
     * @param stopReason 停止原因
     * @param transcript 对话轨迹
     * @param flowState 图状态
     */
    public CamelRunResult(String task,
                          String finalJavaCode,
                          CamelRoleType stopRole,
                          String stopReason,
                          List<CamelDialogueTurn> transcript,
                          OverAllState flowState) {
        this.task = task;
        this.finalJavaCode = finalJavaCode;
        this.stopRole = stopRole;
        this.stopReason = stopReason;
        this.transcript = List.copyOf(transcript);
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
     * 返回最终 Java 代码。
     *
     * @return 最终 Java 代码
     */
    public String getFinalJavaCode() {
        return finalJavaCode;
    }

    /**
     * 返回停止角色。
     *
     * @return 停止角色
     */
    public CamelRoleType getStopRole() {
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
     * 返回对话轨迹。
     *
     * @return 对话轨迹
     */
    public List<CamelDialogueTurn> getTranscript() {
        return transcript;
    }

    /**
     * 返回图状态。
     *
     * @return 图状态
     */
    public Optional<OverAllState> getFlowState() {
        // 手写版没有图状态，因此这里可能为空；框架版会把完整 OverAllState 暴露给调用方做调试和审计。
        return Optional.ofNullable(flowState);
    }
}

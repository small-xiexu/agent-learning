package com.xbk.agent.framework.reflection.application.coordinator;

import com.xbk.agent.framework.reflection.application.executor.HandwrittenJavaCoder;
import com.xbk.agent.framework.reflection.application.executor.HandwrittenJavaReviewer;
import com.xbk.agent.framework.reflection.domain.memory.ReflectionMemory;
import com.xbk.agent.framework.reflection.domain.memory.ReflectionTurnRecord;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 Reflection 协调器
 *
 * 职责：显式维护“生成 -> 评审 -> 优化”的 while 循环，并记录每轮反思结果
 *
 * @author xiexu
 */
public class HandwrittenReflectionAgent {

    private final HandwrittenJavaCoder javaCoder;
    private final HandwrittenJavaReviewer javaReviewer;
    private final ReflectionMemory memory;
    private final int maxReflectionRounds;

    /**
     * 创建手写版 Reflection 协调器。
     *
     * @param javaCoder 代码生成者
     * @param javaReviewer 代码评审者
     * @param memory 反思记忆
     * @param maxReflectionRounds 最大反思轮次
     */
    public HandwrittenReflectionAgent(HandwrittenJavaCoder javaCoder,
                                      HandwrittenJavaReviewer javaReviewer,
                                      ReflectionMemory memory,
                                      int maxReflectionRounds) {
        this.javaCoder = javaCoder;
        this.javaReviewer = javaReviewer;
        this.memory = memory;
        this.maxReflectionRounds = maxReflectionRounds;
    }

    /**
     * 运行手写版 Reflection 流程。
     *
     * @param task 原始任务
     * @return 运行结果
     */
    public RunResult run(String task) {
        String conversationId = "handwritten-reflection-" + UUID.randomUUID();
        String currentCode = javaCoder.generateInitialCode(task, conversationId);
        String latestReflection = "";

        for (int round = 1; round <= maxReflectionRounds; round++) {
            latestReflection = javaReviewer.review(task, currentCode, conversationId);
            memory.addTurnRecord(new ReflectionTurnRecord(currentCode, latestReflection));

            if (shouldStop(latestReflection) || round == maxReflectionRounds) {
                return new RunResult(task, currentCode, latestReflection, memory.snapshot());
            }
            currentCode = javaCoder.refineCode(task, currentCode, latestReflection, conversationId);
        }
        return new RunResult(task, currentCode, latestReflection, memory.snapshot());
    }

    /**
     * 判断当前反馈是否允许停止。
     *
     * @param reviewFeedback 当前评审意见
     * @return true 表示停止
     */
    private boolean shouldStop(String reviewFeedback) {
        return reviewFeedback != null && reviewFeedback.contains("无需改进");
    }

    /**
     * 手写版运行结果。
     *
     * 职责：返回原始任务、最终代码、最终反馈和完整反思记忆
     *
     * @author xiexu
     */
    public static final class RunResult {

        private final String task;
        private final String finalCode;
        private final String finalReflection;
        private final List<ReflectionTurnRecord> memory;

        /**
         * 创建运行结果。
         *
         * @param task 原始任务
         * @param finalCode 最终代码
         * @param finalReflection 最终反馈
         * @param memory 反思记忆
         */
        public RunResult(String task, String finalCode, String finalReflection, List<ReflectionTurnRecord> memory) {
            this.task = task;
            this.finalCode = finalCode;
            this.finalReflection = finalReflection;
            this.memory = List.copyOf(memory);
        }

        public String getTask() {
            return task;
        }

        public String getFinalCode() {
            return finalCode;
        }

        public String getFinalReflection() {
            return finalReflection;
        }

        public List<ReflectionTurnRecord> getMemory() {
            return memory;
        }
    }
}

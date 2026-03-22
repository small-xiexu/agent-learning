package com.xbk.agent.framework.camel.application.coordinator;

import com.xbk.agent.framework.camel.api.CamelRunResult;
import com.xbk.agent.framework.camel.application.executor.CamelProgrammerAgent;
import com.xbk.agent.framework.camel.application.executor.CamelTraderAgent;
import com.xbk.agent.framework.camel.domain.memory.CamelConversationMemory;
import com.xbk.agent.framework.camel.domain.memory.CamelDialogueTurn;
import com.xbk.agent.framework.camel.domain.role.CamelRoleType;
import com.xbk.agent.framework.camel.support.CamelPromptTemplates;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 CAMEL 协作协调器
 *
 * 职责：显式维护交易员与程序员的一问一答 while 循环、共享 memory 和终止协议
 *
 * @author xiexu
 */
public class HandwrittenCamelAgent {

    /**
     * 交易员智能体，负责提出业务需求和做最终验收。
     */
    private final CamelTraderAgent traderRole;

    /**
     * 程序员智能体，负责把交易员需求落成一段完整 Java 代码。
     */
    private final CamelProgrammerAgent programmerRole;

    /**
     * 共享 transcript 内存，手写版靠它把双方对话持续串起来。
     */
    private final CamelConversationMemory memory;

    /**
     * 最大轮次上限，防止角色协作陷入无穷接力。
     */
    private final int maxTurns;

    /**
     * 创建手写版 CAMEL 协调器。
     *
     * @param traderRole 交易员执行器
     * @param programmerRole 程序员执行器
     * @param memory 共享对话记忆
     * @param maxTurns 最大对话轮次
     */
    public HandwrittenCamelAgent(CamelTraderAgent traderRole,
                              CamelProgrammerAgent programmerRole,
                              CamelConversationMemory memory,
                              int maxTurns) {
        this.traderRole = traderRole;
        this.programmerRole = programmerRole;
        this.memory = memory;
        this.maxTurns = maxTurns;
    }

    /**
     * 运行手写版 CAMEL 流程。
     *
     * @param task 原始任务
     * @return 运行结果
     */
    public CamelRunResult run(String task) {
        memory.clear();
        String conversationId = "handwritten-camel-" + UUID.randomUUID();
        // 单独缓存最近一次程序员交付的干净代码，便于任意一侧终止时都能直接返回最新产物。
        String latestJavaCode = "";

        while (memory.size() < maxTurns) {
            // CAMEL 的手写版关键就在这里：
            // 先让交易员基于当前 transcript 提需求，再把结果回填 memory，随后把同一份共享 transcript 交给程序员继续消费。
            String rawTraderOutput = traderRole.reply(task, memory, conversationId);
            int traderTurnCount = memory.countTurnsByRole(CamelRoleType.TRADER) + 1;
            String traderOutput = CamelPromptTemplates.normalizeTraderOutput(rawTraderOutput, traderTurnCount);
            memory.addTurn(CamelRoleType.TRADER, traderOutput);
            if (CamelPromptTemplates.containsTaskDoneMarker(rawTraderOutput)
                    && CamelPromptTemplates.canTraderFinish(traderTurnCount)) {
                return buildResult(task, latestJavaCode, CamelRoleType.TRADER, rawTraderOutput);
            }
            if (memory.size() >= maxTurns) {
                break;
            }
            // 程序员永远只看“当前视角下的最后一棒”，并把自己的代码产物作为下一轮交易员审查的输入。
            String programmerOutput = CamelPromptTemplates.stripTaskDoneMarker(
                    programmerRole.reply(task, memory, conversationId));
            memory.addTurn(CamelRoleType.PROGRAMMER, programmerOutput);
            latestJavaCode = CamelPromptTemplates.stripTaskDoneMarker(programmerOutput);
        }

        return buildResult(task, latestJavaCode, lastRole(), "MAX_TURNS_REACHED");
    }

    /**
     * 构造统一结果。
     *
     * @param task 原始任务
     * @param latestJavaCode 当前最新 Java 代码
     * @param stopRole 停止角色
     * @param stopReason 停止原因
     * @return 运行结果
     */
    private CamelRunResult buildResult(String task,
                                       String latestJavaCode,
                                       CamelRoleType stopRole,
                                       String stopReason) {
        List<CamelDialogueTurn> transcript = memory.snapshot();
        return new CamelRunResult(
                task,
                latestJavaCode,
                stopRole,
                stopReason,
                transcript,
                null);
    }

    /**
     * 返回最后一位发言角色。
     *
     * @return 最后发言角色
     */
    private CamelRoleType lastRole() {
        List<CamelDialogueTurn> transcript = memory.snapshot();
        if (transcript.isEmpty()) {
            return CamelRoleType.TRADER;
        }
        return transcript.get(transcript.size() - 1).getRoleType();
    }

    /*
     * 对照说明：
     * 手写版把消息路由、轮次推进、停止判断都写在这个类的 while 循环里。
     * 这种方式最适合学习 CAMEL 原始思想，因为每一步“谁说什么、何时停”都完全可见。
     * 但工程上它把流程控制、状态管理和角色调用耦合在一起，后续一旦角色数增加、分支变多，可维护性会迅速下降。
     */
}

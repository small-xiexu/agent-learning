package com.xbk.agent.framework.graphflow.framework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import lombok.extern.slf4j.Slf4j;

import com.xbk.agent.framework.graphflow.framework.support.GraphFlowStateKeys;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 框架版 AnswerNodeAction：综合搜索结果总结回答
 *
 * 职责：从 OverAllState 中读取用户提问和搜索结果，调用 LLM 生成最终回答，
 * 以增量 Map 返回 final_answer 字段。
 *
 * ══ 与手写版对照 ══════════════════════════════════════════════════════
 * 手写版：state.setStepStatus(END) 手动推进到终止状态
 * 框架版：本节点无需设置任何终止标记
 *         → addEdge("answer", StateGraph.END) 在图定义阶段已声明此节点后直接结束
 *         → 图框架在内部执行循环中检测到跳转目标是 END 节点时自动停止
 *
 * 这正是框架版消灭 while 终止条件的机制：
 * 终止逻辑从"节点内部设置状态"变成了"图结构声明出口边"，完全解耦。
 * ════════════════════════════════════════════════════════════════════
 *
 * @author xiexu
 */
@Slf4j
public class AnswerNodeAction implements AsyncNodeAction {

    private final AgentLlmGateway gateway;

    /**
     * 创建 AnswerNodeAction。
     *
     * @param gateway 统一大模型调用网关
     */
    public AnswerNodeAction(AgentLlmGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * 综合搜索结果生成最终回答。
     *
     * @param state 框架全局状态
     * @return 本节点产生的状态增量（含 final_answer）
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String userQuery = state.value(GraphFlowStateKeys.USER_QUERY, String.class).orElse("");
        String searchResults = state.value(GraphFlowStateKeys.SEARCH_RESULTS, String.class).orElse("");
        log.info("AnswerNodeAction 开始执行，基于搜索结果生成回答");

        String prompt = "请根据以下搜索结果，对用户的问题给出简洁、准确的回答。\n\n"
                + "用户问题：" + userQuery + "\n\n"
                + "搜索结果：\n" + searchResults;

        String conversationId = "graph-flow-" + UUID.randomUUID();
        LlmRequest request = LlmRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .messages(List.of(
                        Message.builder()
                                .messageId(UUID.randomUUID().toString())
                                .conversationId(conversationId)
                                .role(MessageRole.USER)
                                .content(prompt)
                                .build()))
                .build();

        LlmResponse response = gateway.chat(request);
        String finalAnswer = response.getRawText().trim();

        log.info("AnswerNodeAction 执行完毕，final_answer 已生成");
        // 无需 setStepStatus(END)，框架通过 addEdge("answer", StateGraph.END) 自动终止
        return CompletableFuture.completedFuture(Map.of(GraphFlowStateKeys.FINAL_ANSWER, finalAnswer));
    }
}

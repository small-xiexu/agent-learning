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
 * 框架版 FallbackNodeAction：搜索失败降级回答节点
 *
 * 职责：仅凭用户原始提问让 LLM 给出"无工具退化回答"，
 * 以增量 Map 返回 final_answer 字段。
 *
 * ══ 与手写版对照 ══════════════════════════════════════════════════════
 * 手写版：FallbackNode 由 GraphRunner.switch(SEARCH_FAILED) 分支调用
 * 框架版：FallbackNode 由 addConditionalEdges 中 SearchResultEdgeRouter
 *         返回 "fallback" 时路由到此节点
 *
 * 手写版路由代码（在 GraphRunner 里）：
 *   case SEARCH_FAILED: fallbackNode.process(state);
 *
 * 框架版路由代码（在 AlibabaGraphFlowAgent.buildSpecificGraph 里）：
 *   addConditionalEdges("search", searchResultEdgeRouter,
 *       Map.of("answer", ANSWER_NODE, "fallback", FALLBACK_NODE))
 *
 * 两者的业务逻辑完全相同，差别只是路由决策的位置不同。
 * ════════════════════════════════════════════════════════════════════
 *
 * @author xiexu
 */
@Slf4j
public class FallbackNodeAction implements AsyncNodeAction {

    private final AgentLlmGateway gateway;

    /**
     * 创建 FallbackNodeAction。
     *
     * @param gateway 统一大模型调用网关
     */
    public FallbackNodeAction(AgentLlmGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * 执行降级回答，仅凭用户提问生成兜底答案。
     *
     * @param state 框架全局状态
     * @return 本节点产生的状态增量（含 final_answer）
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String userQuery = state.value(GraphFlowStateKeys.USER_QUERY, String.class).orElse("");
        String errorMessage = state.value(GraphFlowStateKeys.ERROR_MESSAGE, String.class).orElse("未知错误");
        log.warn("FallbackNodeAction 触发，搜索失败原因：{}", errorMessage);

        String prompt = "搜索工具当前不可用，请仅凭你的知识对以下问题给出尽可能准确的回答，"
                + "并在回答开头注明\"（注：本次回答未使用实时搜索，信息可能不是最新的）\"。\n\n"
                + "用户问题：" + userQuery;

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
        String fallbackAnswer = response.getRawText().trim();

        log.info("FallbackNodeAction 执行完毕，降级回答已生成");
        // 与 AnswerNodeAction 相同，无需设置终止标记
        // addEdge("fallback", StateGraph.END) 已声明此节点后自动结束
        return CompletableFuture.completedFuture(Map.of(GraphFlowStateKeys.FINAL_ANSWER, fallbackAnswer));
    }
}

package com.xbk.agent.framework.graphflow.framework.edge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.xbk.agent.framework.graphflow.framework.support.GraphFlowStateKeys;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * 搜索结果条件边路由器
 *
 * 职责：在 SearchNodeAction 执行完毕后，根据 search_failed 字段决定跳转目标节点。
 * 返回的字符串标签必须与 addConditionalEdges 中声明的 Map 键完全一致。
 *
 * ══ 与手写版对照 ══════════════════════════════════════════════════════
 * 手写版条件路由（GraphRunner.switch 里两个 case）：
 *   case SEARCH_SUCCESS: answerNode.process(state);    → 跳转到 AnswerNode
 *   case SEARCH_FAILED:  fallbackNode.process(state);  → 跳转到 FallbackNode
 *
 * 框架版条件路由（本类 route 方法 + addConditionalEdges 的 Map）：
 *   route() 返回 "answer"   → 框架查 Map → 跳转到 ANSWER_NODE
 *   route() 返回 "fallback" → 框架查 Map → 跳转到 FALLBACK_NODE
 *
 * 对比收益：手写版把"判断逻辑"和"路由表"混在 switch 里；
 *           框架版把"判断逻辑"（本类）和"路由表"（addConditionalEdges Map）分离，
 *           路由器可以独立单测，图拓扑变化时只需修改 Map 而不动路由逻辑。
 * ════════════════════════════════════════════════════════════════════
 *
 * @author xiexu
 */
@Slf4j
public class SearchResultEdgeRouter implements AsyncEdgeAction {

    /**
     * 根据搜索是否失败返回目标节点标签。
     *
     * @param state 框架全局状态
     * @return "answer" 表示搜索成功，"fallback" 表示搜索失败
     */
    @Override
    public CompletableFuture<String> apply(OverAllState state) {
        boolean searchFailed = state.value(GraphFlowStateKeys.SEARCH_FAILED, Boolean.class).orElse(Boolean.FALSE);
        // 等价于手写版：if (stepStatus == SEARCH_FAILED) goto fallback else goto answer
        String targetLabel = searchFailed ? "fallback" : "answer";
        log.info("SearchResultEdgeRouter 路由决策：search_failed={}，目标节点标签={}", searchFailed, targetLabel);
        return CompletableFuture.completedFuture(targetLabel);
    }
}

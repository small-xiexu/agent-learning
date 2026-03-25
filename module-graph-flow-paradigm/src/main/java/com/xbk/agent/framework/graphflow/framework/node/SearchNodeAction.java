package com.xbk.agent.framework.graphflow.framework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.xbk.agent.framework.graphflow.common.tool.MockSearchTool;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 框架版 SearchNodeAction：搜索节点
 *
 * 职责：从 OverAllState 中读取 search_query，调用搜索工具，
 * 将结果（或失败标记）以增量 Map 返回给框架合并进全局状态。
 *
 * ══ 与手写版对照 ══════════════════════════════════════════════════════
 * 手写版：catch 异常后 state.setStepStatus(SEARCH_FAILED)，引擎 switch 据此跳转
 * 框架版：catch 异常后返回 Map.of("search_failed", true)，
 *         SearchResultEdgeRouter 读取此字段决定跳转到 "fallback" 还是 "answer"
 *
 * 关键差异：手写版用枚举字段驱动 switch 路由，框架版用布尔标记驱动条件边路由函数。
 * 两者的本质完全相同，只是"路由决策"的承载位置不同：
 *   手写版 → 集中在 GraphRunner.switch 里
 *   框架版 → 分散在 SearchResultEdgeRouter.route 里（更内聚、更可测试）
 * ════════════════════════════════════════════════════════════════════
 *
 * @author xiexu
 */
@Slf4j
public class SearchNodeAction implements AsyncNodeAction {

    private final MockSearchTool searchTool;

    /**
     * 创建 SearchNodeAction。
     *
     * @param searchTool 搜索工具（可注入失败桩以触发 FallbackNode 分支）
     */
    public SearchNodeAction(MockSearchTool searchTool) {
        this.searchTool = searchTool;
    }

    /**
     * 执行搜索，成功写入结果，失败写入失败标记。
     *
     * @param state 框架全局状态
     * @return 本节点产生的状态增量
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String searchQuery = state.value("search_query", String.class).orElse("");
        log.info("SearchNodeAction 开始执行，search_query={}", searchQuery);

        Map<String, Object> updates = new HashMap<>();
        try {
            String results = searchTool.search(searchQuery);
            updates.put("search_results", results);
            // search_failed=false 是条件边路由器（SearchResultEdgeRouter）的判断依据
            // 等价于手写版：state.setStepStatus(SEARCH_SUCCESS)
            updates.put("search_failed", Boolean.FALSE);
            log.info("SearchNodeAction 执行成功");
        } catch (Exception e) {
            updates.put("error_message", e.getMessage());
            // search_failed=true 触发 SearchResultEdgeRouter 返回 "fallback"
            // 等价于手写版：state.setStepStatus(SEARCH_FAILED)
            updates.put("search_failed", Boolean.TRUE);
            log.warn("SearchNodeAction 执行失败，error_message={}", e.getMessage());
        }
        return CompletableFuture.completedFuture(updates);
    }
}

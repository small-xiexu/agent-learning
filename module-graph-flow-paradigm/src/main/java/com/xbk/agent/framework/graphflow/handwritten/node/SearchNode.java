package com.xbk.agent.framework.graphflow.handwritten.node;

import com.xbk.agent.framework.graphflow.common.state.GraphState;
import com.xbk.agent.framework.graphflow.common.state.StepStatus;
import com.xbk.agent.framework.graphflow.common.tool.MockSearchTool;
import lombok.extern.slf4j.Slf4j;

/**
 * 手写版 SearchNode：调用搜索工具
 *
 * 职责：使用 UnderstandNode 提取的关键词调用搜索工具，
 * 成功则写入 searchResults 并推进到 SEARCH_SUCCESS；
 * 失败则写入 errorMessage 并推进到 SEARCH_FAILED，触发 FallbackNode 条件分支。
 *
 * @author xiexu
 */
@Slf4j
public class SearchNode {

    private final MockSearchTool searchTool;

    /**
     * 创建 SearchNode。
     *
     * @param searchTool 搜索工具
     */
    public SearchNode(MockSearchTool searchTool) {
        this.searchTool = searchTool;
    }

    /**
     * 执行搜索，并根据结果写入成功或失败状态。
     *
     * @param state 当前全局状态
     * @return 更新后的状态（stepStatus=SEARCH_SUCCESS 或 SEARCH_FAILED）
     */
    public GraphState process(GraphState state) {
        log.info("SearchNode 开始执行，searchQuery={}", state.getSearchQuery());
        try {
            String results = searchTool.search(state.getSearchQuery());
            state.setSearchResults(results);
            state.setStepStatus(StepStatus.SEARCH_SUCCESS);
            log.info("SearchNode 执行成功，results 长度={}", results.length());
        } catch (Exception e) {
            // 搜索失败不抛出异常，而是把错误信息写入状态，由条件边路由到 FallbackNode
            state.setErrorMessage(e.getMessage());
            state.setStepStatus(StepStatus.SEARCH_FAILED);
            log.warn("SearchNode 执行失败，errorMessage={}", e.getMessage());
        }
        return state;
    }
}

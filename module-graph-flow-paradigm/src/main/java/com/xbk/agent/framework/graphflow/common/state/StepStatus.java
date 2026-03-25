package com.xbk.agent.framework.graphflow.common.state;

/**
 * 图执行步骤状态枚举
 *
 * 职责：定义"三步问答助手"状态机的全部合法状态，供 GraphRunner 的路由逻辑使用。
 * 状态流转路径（正常）：INIT → UNDERSTOOD → SEARCH_SUCCESS → ANSWERED → END
 * 状态流转路径（降级）：INIT → UNDERSTOOD → SEARCH_FAILED  → ANSWERED → END
 *
 * @author xiexu
 */
public enum StepStatus {

    /**
     * 初始状态，图刚启动，尚未执行任何节点。
     */
    INIT,

    /**
     * UnderstandNode 执行完毕，已从用户提问中提取搜索关键词。
     */
    UNDERSTOOD,

    /**
     * SearchNode 执行成功，已获取搜索结果，将流转到 AnswerNode。
     */
    SEARCH_SUCCESS,

    /**
     * SearchNode 执行失败，搜索工具异常，将流转到 FallbackNode 降级回答。
     */
    SEARCH_FAILED,

    /**
     * AnswerNode 或 FallbackNode 执行完毕，最终答案已生成，图执行结束。
     */
    END
}

package com.xbk.agent.framework.graphflow.framework.support;

/**
 * GraphFlow 框架版状态键
 *
 * 职责：统一声明框架版三步问答图流中所有节点读写 OverAllState 时使用的 key 常量。
 * 这些 key 是各节点之间的"隐式协议"——初始化、节点 Action 和条件路由三方都必须使用
 * 完全相同的字符串，任何一处写错都会导致状态读取失败。
 *
 * <p>各 key 的生产者与消费者：
 * <pre>
 *   key              生产者（写入）                  消费者（读取）
 *   ──────────────────────────────────────────────────────────────────
 *   "user_query"     AlibabaGraphFlowAgent.run() 初始化
 *                                                → UnderstandNodeAction（理解用户意图）
 *                                                → AnswerNodeAction（生成最终回答）
 *                                                → FallbackNodeAction（生成兜底回答）
 *
 *   "search_query"   UnderstandNodeAction（写入关键词）
 *                                                → SearchNodeAction（执行搜索）
 *
 *   "search_results" SearchNodeAction（写入搜索结果）
 *                                                → AnswerNodeAction（基于结果生成回答）
 *
 *   "final_answer"   AnswerNodeAction / FallbackNodeAction（写入最终回答）
 *                                                → AlibabaGraphFlowAgent.run() 提取结果
 *
 *   "search_failed"  SearchNodeAction（搜索失败时置为 true）
 *                                                → SearchResultEdgeRouter（决定跳 answer 还是 fallback）
 *
 *   "error_message"  SearchNodeAction（写入异常信息）
 *                                                → FallbackNodeAction（拼入兜底回答）
 * </pre>
 *
 * @author xiexu
 */
public final class GraphFlowStateKeys {

    /**
     * 用户原始提问，由 run() 初始化，UnderstandNodeAction / AnswerNodeAction / FallbackNodeAction 消费。
     */
    public static final String USER_QUERY = "user_query";

    /**
     * 搜索关键词，由 UnderstandNodeAction 写入，SearchNodeAction 消费。
     */
    public static final String SEARCH_QUERY = "search_query";

    /**
     * 搜索结果，由 SearchNodeAction 写入，AnswerNodeAction 消费。
     */
    public static final String SEARCH_RESULTS = "search_results";

    /**
     * 最终回答，由 AnswerNodeAction 或 FallbackNodeAction 写入，run() 提取为最终结果。
     */
    public static final String FINAL_ANSWER = "final_answer";

    /**
     * 搜索是否失败，由 SearchNodeAction 写入，SearchResultEdgeRouter 消费以决定路由方向。
     */
    public static final String SEARCH_FAILED = "search_failed";

    /**
     * 错误信息，搜索异常时由 SearchNodeAction 写入，FallbackNodeAction 消费拼入兜底回答。
     */
    public static final String ERROR_MESSAGE = "error_message";

    private GraphFlowStateKeys() {
    }
}

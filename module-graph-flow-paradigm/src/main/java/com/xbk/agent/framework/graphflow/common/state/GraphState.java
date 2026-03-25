package com.xbk.agent.framework.graphflow.common.state;

/**
 * 图执行全局状态对象
 *
 * 职责：贯穿整个图执行生命周期的共享上下文，所有节点通过读写本对象来传递数据。
 * 这是 LangGraph 状态机思想的最小化还原：一个可变的状态载体在节点间线性流动。
 *
 * 注意：手写版直接传递此 POJO；框架版等价于 OverAllState（本质是 Map），
 * 两者的核心差异在于手写版是强类型，框架版是弱类型 Map + 增量合并。
 *
 * @author xiexu
 */
public class GraphState {

    /**
     * 用户原始输入，贯穿全流程只读，由图启动时写入。
     */
    private String userQuery;

    /**
     * UnderstandNode 提取的搜索关键词，供 SearchNode 使用。
     */
    private String searchQuery;

    /**
     * SearchNode 返回的搜索结果原文，供 AnswerNode 使用；搜索失败时为空。
     */
    private String searchResults;

    /**
     * 最终回答，由 AnswerNode 或 FallbackNode 写入，是图执行的最终产出。
     */
    private String finalAnswer;

    /**
     * 当前步骤状态，GraphRunner 依据此字段路由到下一个节点，是状态机的"指针"。
     */
    private StepStatus stepStatus;

    /**
     * 异常信息，SearchNode 失败时写入，FallbackNode 可将其纳入降级回答的上下文。
     */
    private String errorMessage;

    /**
     * 以 INIT 状态创建初始图状态。
     *
     * @param userQuery 用户原始输入
     * @return 初始图状态
     */
    public static GraphState init(String userQuery) {
        GraphState state = new GraphState();
        state.userQuery = userQuery;
        state.stepStatus = StepStatus.INIT;
        return state;
    }

    /**
     * 返回用户原始输入。
     *
     * @return 用户原始输入
     */
    public String getUserQuery() {
        return userQuery;
    }

    /**
     * 返回搜索关键词。
     *
     * @return 搜索关键词
     */
    public String getSearchQuery() {
        return searchQuery;
    }

    /**
     * 设置搜索关键词。
     *
     * @param searchQuery 搜索关键词
     */
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    /**
     * 返回搜索结果。
     *
     * @return 搜索结果
     */
    public String getSearchResults() {
        return searchResults;
    }

    /**
     * 设置搜索结果。
     *
     * @param searchResults 搜索结果
     */
    public void setSearchResults(String searchResults) {
        this.searchResults = searchResults;
    }

    /**
     * 返回最终回答。
     *
     * @return 最终回答
     */
    public String getFinalAnswer() {
        return finalAnswer;
    }

    /**
     * 设置最终回答。
     *
     * @param finalAnswer 最终回答
     */
    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    /**
     * 返回当前步骤状态。
     *
     * @return 当前步骤状态
     */
    public StepStatus getStepStatus() {
        return stepStatus;
    }

    /**
     * 设置步骤状态（驱动 GraphRunner 路由到下一节点）。
     *
     * @param stepStatus 新步骤状态
     */
    public void setStepStatus(StepStatus stepStatus) {
        this.stepStatus = stepStatus;
    }

    /**
     * 返回异常信息。
     *
     * @return 异常信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置异常信息。
     *
     * @param errorMessage 异常信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

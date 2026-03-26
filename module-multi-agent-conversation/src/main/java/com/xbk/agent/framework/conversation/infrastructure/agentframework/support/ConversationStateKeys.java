package com.xbk.agent.framework.conversation.infrastructure.agentframework.support;

/**
 * Conversation 框架版状态键
 *
 * 职责：统一声明框架版 RoundRobin Conversation Agent 中所有节点读写 OverAllState 时使用的 key 常量。
 * ProductManagerNode、EngineerNode、CodeReviewerNode 三方共享同一份状态协议，
 * 任何一处写错都会导致角色轮换或群聊上下文传递断链。
 *
 * <p>各 key 的生产者与消费者：
 * <pre>
 *   key                    生产者（写入）                      消费者（读取）
 *   ──────────────────────────────────────────────────────────────────────────
 *   "input"                AlibabaConversationFlowAgent.run() 初始化
 *                                                         → 所有节点（任务上下文）
 *
 *   "conversation_id"      run() 初始化                   → 所有节点（绑定 LLM 会话链路）
 *
 *   "active_role"          run() 初始化 / 每个节点更新     → shouldStop 条件边（角色流转判断）
 *
 *   "turn_count"           run() 初始化 / 每个节点递增     → shouldStop 条件边（轮次上限判断）
 *
 *   "last_product_output"  ProductManagerNode             → run() 调试/断言
 *   "last_engineer_output" EngineerNode                   → run() 调试/断言
 *   "last_reviewer_output" CodeReviewerNode               → run() 调试/断言
 *
 *   "current_python_script" EngineerNode（写入最新脚本）  → run() 提取最终产物
 *
 *   "review_status"        run() 初始化 / CodeReviewerNode → shouldStop 条件边
 *
 *   "done"                 CodeReviewerNode（审查通过时置 true）
 *                                                         → shouldStop 条件边
 *
 *   "stop_reason"          CodeReviewerNode               → run() 提取停止原因
 *
 *   "shared_messages"      run() 初始化 / 每个节点追加    → 所有节点（AutoGen 群聊上下文）
 *
 *   "transcript"           run() 初始化 / 每个节点追加    → run() 提取完整对话记录
 * </pre>
 *
 * @author xiexu
 */
public final class ConversationStateKeys {

    /**
     * 原始任务，由 run() 初始化，所有节点均可引用。
     */
    public static final String INPUT = "input";

    /**
     * 会话唯一标识，由 run() 生成并初始化，所有节点用于绑定 LLM 调用链路。
     */
    public static final String CONVERSATION_ID = "conversation_id";

    /**
     * 当前活动角色，由 run() 初始化为 PRODUCT_MANAGER，每个节点完成后更新为下一个角色。
     */
    public static final String ACTIVE_ROLE = "active_role";

    /**
     * 已执行总轮次，由 run() 初始化为 0，每个节点执行后递增，用于轮次上限保护。
     */
    public static final String TURN_COUNT = "turn_count";

    /**
     * ProductManager 最近一次输出，由 ProductManagerNode 写入，主要用于调试和测试断言。
     */
    public static final String LAST_PRODUCT_OUTPUT = "last_product_output";

    /**
     * Engineer 最近一次输出，由 EngineerNode 写入，主要用于调试和测试断言。
     */
    public static final String LAST_ENGINEER_OUTPUT = "last_engineer_output";

    /**
     * CodeReviewer 最近一次输出，由 CodeReviewerNode 写入，主要用于调试和测试断言。
     */
    public static final String LAST_REVIEWER_OUTPUT = "last_reviewer_output";

    /**
     * 当前最新 Python 脚本，由 EngineerNode 持续更新，run() 提取为最终交付产物。
     */
    public static final String CURRENT_PYTHON_SCRIPT = "current_python_script";

    /**
     * 审查状态，初始为 "pending"，CodeReviewerNode 根据审查结论更新。
     */
    public static final String REVIEW_STATUS = "review_status";

    /**
     * 是否已完成，由 CodeReviewerNode 在审查通过时置为 true，触发条件边终止流程。
     */
    public static final String DONE = "done";

    /**
     * 停止原因，由 CodeReviewerNode 在终止时写入，run() 提取后透传给调用方。
     */
    public static final String STOP_REASON = "stop_reason";

    /**
     * AutoGen 风格的群聊共享消息历史，由 run() 初始化，每个节点追加本轮发言后写回。
     */
    public static final String SHARED_MESSAGES = "shared_messages";

    /**
     * 完整对话记录，由 run() 初始化为空列表，每个节点追加轮次记录后写回，用于审计和回放。
     */
    public static final String TRANSCRIPT = "transcript";

    private ConversationStateKeys() {
    }
}

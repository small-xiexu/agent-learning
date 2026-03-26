package com.xbk.agent.framework.roleplay.infrastructure.agentframework.support;

/**
 * CAMEL 框架版状态键
 *
 * 职责：统一声明框架版 CAMEL Agent 中所有节点读写 OverAllState 时使用的 key 常量。
 * CamelTraderHandoffNode 与 CamelProgrammerHandoffNode 通过接力棒（baton）机制传递消息，
 * 任何一处写错都会导致角色 handoff 或代码产物传递断链。
 *
 * <p>各 key 的生产者与消费者：
 * <pre>
 *   key                      生产者（写入）                      消费者（读取）
 *   ────────────────────────────────────────────────────────────────────────────
 *   "input"                  AlibabaCamelFlowAgent.run() 初始化
 *                                                           → CamelTraderHandoffNode
 *                                                           → CamelProgrammerHandoffNode
 *
 *   "conversation_id"        run() 初始化                   → 两个节点（绑定 LLM 会话链路）
 *
 *   "active_role"            run() 初始化 / 交易员节点更新  → nextFromTrader 条件边
 *                                                           → run() 推断最终停止角色
 *
 *   "turn_count"             run() 初始化 / 每个节点递增    → shouldStop 条件边（轮次上限判断）
 *
 *   "message_for_trader"     CamelProgrammerHandoffNode（接力棒）
 *                                                           → CamelTraderHandoffNode（下一轮输入）
 *
 *   "message_for_programmer" CamelTraderHandoffNode（接力棒）
 *                                                           → CamelProgrammerHandoffNode（下一轮输入）
 *
 *   "last_trader_output"     CamelTraderHandoffNode         → run() 调试/断言
 *   "last_programmer_output" CamelProgrammerHandoffNode     → run() 调试/断言
 *
 *   "current_java_code"      CamelProgrammerHandoffNode（持续写入最新代码）
 *                                                           → run() 提取最终交付产物
 *
 *   "done"                   CamelTraderHandoffNode（满足终止条件时置 true）
 *                                                           → shouldStop 条件边
 *
 *   "stop_reason"            CamelTraderHandoffNode         → run() 提取停止原因
 *
 *   "transcript"             run() 初始化 / 每个节点追加    → run() 提取完整对话记录
 * </pre>
 *
 * @author xiexu
 */
public final class CamelStateKeys {

    /**
     * 原始协作任务，由 run() 初始化，两个节点均可引用。
     */
    public static final String INPUT = "input";

    /**
     * 会话唯一标识，由 run() 生成并初始化，两个节点用于绑定 LLM 调用链路。
     */
    public static final String CONVERSATION_ID = "conversation_id";

    /**
     * 当前活动角色，由 run() 初始化为 TRADER，交易员节点根据终止条件更新。
     */
    public static final String ACTIVE_ROLE = "active_role";

    /**
     * 已执行总轮次，由 run() 初始化为 0，每个节点执行后递增，用于轮次上限保护。
     */
    public static final String TURN_COUNT = "turn_count";

    /**
     * 交易员给程序员的接力棒消息，由 CamelTraderHandoffNode 写入，CamelProgrammerHandoffNode 消费。
     */
    public static final String MESSAGE_FOR_PROGRAMMER = "message_for_programmer";

    /**
     * 程序员给交易员的接力棒消息，由 CamelProgrammerHandoffNode 写入，CamelTraderHandoffNode 消费。
     */
    public static final String MESSAGE_FOR_TRADER = "message_for_trader";

    /**
     * 交易员最近一次原始输出，由 CamelTraderHandoffNode 写入，主要用于调试和审计。
     */
    public static final String LAST_TRADER_OUTPUT = "last_trader_output";

    /**
     * 程序员最近一次原始输出，由 CamelProgrammerHandoffNode 写入，主要用于调试和审计。
     */
    public static final String LAST_PROGRAMMER_OUTPUT = "last_programmer_output";

    /**
     * 当前最新 Java 代码，由 CamelProgrammerHandoffNode 持续更新，run() 提取为最终交付产物。
     */
    public static final String CURRENT_JAVA_CODE = "current_java_code";

    /**
     * 是否已完成，由 CamelTraderHandoffNode 在满足终止条件时置为 true，触发条件边终止流程。
     */
    public static final String DONE = "done";

    /**
     * 停止原因，由 CamelTraderHandoffNode 在终止时写入，run() 提取后透传给调用方。
     */
    public static final String STOP_REASON = "stop_reason";

    /**
     * 完整对话记录，由 run() 初始化为空列表，每个节点追加轮次记录后写回，用于审计和回放。
     */
    public static final String TRANSCRIPT = "transcript";

    private CamelStateKeys() {
    }
}

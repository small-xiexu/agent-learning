package com.xbk.agent.framework.supervisor.framework.support;

/**
 * Supervisor 框架版状态键
 *
 * 职责：统一声明框架版 Supervisor 工作流中所有节点读写 OverAllState 时使用的 key 常量。
 * 这些 key 是框架版最核心的"隐式协议"——Prompt 模板、ReactAgent outputKey 和状态提取器
 * 三方都必须使用完全相同的字符串，任何一处写错都会导致状态读取失败。
 *
 * <p>各 key 的生产者与消费者：
 * <pre>
 *   key                  生产者（写入）            消费者（读取）
 *   ─────────────────────────────────────────────────────────────
 *   "input"              AlibabaSupervisorFlowAgent.run() 初始化
 *                                                  → WriterAgent Prompt 模板（{input}）
 *                                                  → SupervisorAgent 路由 Prompt（{input}）
 *
 *   "writer_output"      WriterAgent（outputKey）  → TranslatorAgent Prompt（{writer_output}）
 *                                                  → SupervisorStateExtractor（提取最终结果）
 *
 *   "translator_output"  TranslatorAgent（outputKey）
 *                                                  → ReviewerAgent Prompt（{translator_output}）
 *                                                  → SupervisorStateExtractor
 *
 *   "reviewer_output"    ReviewerAgent（outputKey）→ SupervisorStateExtractor（最终结果）
 *
 *   "messages"           框架内置，SupervisorAgent 消费群聊历史
 *                                                  → SupervisorAgent 路由决策参考
 * </pre>
 *
 * <p>为什么用常量类而不是直接写字符串字面量：
 * 字符串字面量分散在多个类里时，重命名一个 key 需要全局搜索替换，极易遗漏。
 * 集中声明后，重命名只需改一处，编译器保证所有引用同步更新。
 *
 * @author xiexu
 */
public final class SupervisorStateKeys {

    /**
     * Writer Agent 的输出键，对应博客中文初稿。
     * 由 WriterAgent 的 outputKey 写入，TranslatorAgent 的 Prompt 模板读取。
     */
    public static final String WRITER_OUTPUT = "writer_output";

    /**
     * Translator Agent 的输出键，对应翻译后的英文稿。
     * 由 TranslatorAgent 的 outputKey 写入，ReviewerAgent 的 Prompt 模板读取。
     */
    public static final String TRANSLATOR_OUTPUT = "translator_output";

    /**
     * Reviewer Agent 的输出键，对应英文审校后的最终稿。
     * 由 ReviewerAgent 的 outputKey 写入，SupervisorStateExtractor 提取最终结果时读取。
     */
    public static final String REVIEWER_OUTPUT = "reviewer_output";

    /**
     * 框架内置的群聊消息历史键，SupervisorAgent 用它感知整个工作流的执行上下文。
     */
    public static final String MESSAGES = "messages";

    /**
     * 用户原始输入键，由 run() 初始化写入，所有 Worker 的 Prompt 模板均可引用。
     */
    public static final String INPUT = "input";

    private SupervisorStateKeys() {
    }
}

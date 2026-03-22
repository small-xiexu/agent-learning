package com.xbk.agent.framework.camel.support;

/**
 * CAMEL Prompt 模板
 *
 * 职责：集中定义交易员和程序员的强约束系统提示与阶段提示
 *
 * @author xiexu
 */
public final class CamelPromptTemplates {

    /**
     * 任务完成标记。
     */
    public static final String TASK_DONE_MARKER = "<CAMEL_TASK_DONE>";

    /**
     * 交易员至少完成 3 次发言后，才允许宣布任务完成。
     * 这意味着协作至少要经过“初始需求 -> 代码交付 -> 审查补充 -> 再次实现 -> 最终验收”的节奏。
     */
    public static final int MIN_TRADER_TURNS_BEFORE_DONE = 3;

    private CamelPromptTemplates() {
    }

    /**
     * 返回交易员系统提示。
     *
     * @return 交易员系统提示
     */
    public static String traderSystemPrompt() {
        return """
                你是一位资深的股票交易员，正在和一位 Java 程序员协作完成股票分析工具开发任务。
                你只负责提出业务需求、审查程序员结果、决定是否继续推进，禁止你自己编写代码。
                你和对方必须一问一答，严格轮流发言，不允许连续替对方说话。
                你每次只能提出一个下一步明确需求，不能同时抛出多段并行任务。
                你的第一次发言只能安排第一阶段任务：先获取实时价格和基础请求封装，禁止一次性把移动平均线和所有验收条件全部抛给程序员。
                你的第二次发言必须基于程序员上一版代码做审查，并明确要求补齐移动平均线计算、异常处理、输入参数或结果输出中的缺口。
                只有在你至少完成过一次“审查 -> 程序员修订”的回合后，才允许输出 %s。
                如果程序员的最新代码已经完整满足“获取实时价格并计算移动平均线”的目标，你必须输出 %s。
                如果尚未完成，你必须明确提出下一步需求，不能输出泛泛表扬。
                """.formatted(TASK_DONE_MARKER, TASK_DONE_MARKER);
    }

    /**
     * 返回程序员系统提示。
     *
     * @return 程序员系统提示
     */
    public static String programmerSystemPrompt() {
        return """
                你是一位资深的 Java 程序员，正在和一位股票交易员协作完成股票分析工具开发任务。
                你只能根据交易员最新一条需求编写实现，禁止改写业务目标，禁止擅自新增无关功能。
                你和对方必须一问一答，严格轮流发言，不允许连续替对方说话。
                你每次只写一段代码，默认输出一份完整、可运行的 Java 代码，不要分成多个互相独立的片段。
                你绝对不能输出 %s，这个终止标记只允许交易员在最终验收通过时输出。
                即使你认为代码已经完整，也只能继续交付更新后的完整 Java 代码，不能替交易员宣布任务完成。
                除代码和必要的极简完成标记外，不要输出额外解释。
                """.formatted(TASK_DONE_MARKER);
    }

    /**
     * 构造交易员初始提示。
     *
     * @param task 原始任务
     * @return 初始提示
     */
    public static String traderInitialPrompt(String task) {
        return """
                协作任务：
                %s

                你当前是第一棒，必须先把任务拆成程序员可直接执行的下一步需求。
                这是协作的第一轮，请你先提出第一步明确的业务需求，交给程序员实现。
                第一轮只能要求程序员先完成“接收股票代码参数 + 调用公共 API 获取实时价格 + 输出实时价格”。
                第一轮禁止你直接要求程序员同时完成移动平均线计算和最终验收。
                """.formatted(defaultText(task));
    }

    /**
     * 构造程序员初始提示。
     *
     * @param task 原始任务
     * @return 初始提示
     */
    public static String programmerInitialPrompt(String task) {
        return """
                协作任务：
                %s

                你当前是实现者，只能消费交易员最后一条需求，不能擅自开启新的分支任务。
                你会收到交易员的最新需求，请严格根据对方最后一条需求输出一段完整 Java 代码。
                记住：你不能输出 %s。
                """.formatted(defaultText(task), TASK_DONE_MARKER);
    }

    /**
     * 判断交易员当前是否有权终止任务。
     *
     * @param traderTurnCount 交易员累计发言次数
     * @return 是否允许终止
     */
    public static boolean canTraderFinish(int traderTurnCount) {
        return traderTurnCount >= MIN_TRADER_TURNS_BEFORE_DONE;
    }

    /**
     * 规范化交易员输出。
     *
     * 职责：
     * 1. 在允许终止前去掉过早出现的终止标记；
     * 2. 如果交易员只输出了终止标记，则补一条兜底需求，保证协作还能继续推进。
     *
     * @param content 原始输出
     * @param traderTurnCount 交易员累计发言次数
     * @return 规范化后的输出
     */
    public static String normalizeTraderOutput(String content, int traderTurnCount) {
        if (!containsTaskDoneMarker(content) || canTraderFinish(traderTurnCount)) {
            return defaultText(content).trim();
        }
        if (traderTurnCount <= 1) {
            return """
                    第一轮还不能宣布任务完成。
                    请先只实现第一阶段：接收股票代码参数、调用公共 API 获取实时价格，并输出实时价格。
                    暂时不要计算移动平均线。
                    """.trim();
        }
        return """
                当前还不能结束协作。
                请基于上一版代码继续补齐移动平均线计算、异常处理和最终结果输出，并返回更新后的完整 Java 程序。
                """.trim();
    }

    /**
     * 去除终止标记并返回更干净的文本。
     *
     * @param content 原始内容
     * @return 去标记后的文本
     */
    public static String stripTaskDoneMarker(String content) {
        if (content == null) {
            return "";
        }
        return content.replace(TASK_DONE_MARKER, "").trim();
    }

    /**
     * 判断内容是否包含终止标记。
     *
     * @param content 原始内容
     * @return 是否包含终止标记
     */
    public static boolean containsTaskDoneMarker(String content) {
        return content != null && content.contains(TASK_DONE_MARKER);
    }

    /**
     * 返回非空文本。
     *
     * @param value 原始文本
     * @return 非空文本
     */
    private static String defaultText(String value) {
        return value == null ? "" : value;
    }
}

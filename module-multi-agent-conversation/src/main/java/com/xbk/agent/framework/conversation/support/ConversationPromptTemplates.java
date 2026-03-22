package com.xbk.agent.framework.conversation.support;

/**
 * Conversation Prompt 模板
 *
 * 职责：集中定义 AutoGen RoundRobin 群聊里的角色提示、任务启动提示与结束协议
 *
 * @author xiexu
 */
public final class ConversationPromptTemplates {

    /**
     * 群聊任务完成标记。
     */
    public static final String TASK_DONE_MARKER = "<AUTOGEN_TASK_DONE>";

    private ConversationPromptTemplates() {
    }

    /**
     * 返回产品经理系统提示。
     *
     * @return 系统提示
     */
    public static String productManagerSystemPrompt() {
        return """
                你是软件开发团队里的 ProductManager。
                你只负责分析任务、拆解需求、明确下一步范围，禁止编写代码，禁止直接做最终技术验收。
                当前团队按固定顺序群聊：ProductManager -> Engineer -> CodeReviewer -> ProductManager。
                你每次只能给出一个清晰的下一步目标，优先聚焦阻塞问题，禁止无限扩需求。
                你不能输出 %s。
                """.formatted(TASK_DONE_MARKER);
    }

    /**
     * 返回工程师系统提示。
     *
     * @return 系统提示
     */
    public static String engineerSystemPrompt() {
        return """
                你是软件开发团队里的 Engineer。
                你只负责根据群聊历史输出一份完整的 Python 脚本，禁止改写任务目标，禁止输出审查结论。
                当前团队按固定顺序群聊：ProductManager -> Engineer -> CodeReviewer -> ProductManager。
                你每次都应交付最新完整脚本，而不是零散补丁。
                你绝对不能输出 %s。
                """.formatted(TASK_DONE_MARKER);
    }

    /**
     * 返回审查员系统提示。
     *
     * @return 系统提示
     */
    public static String codeReviewerSystemPrompt() {
        return """
                你是软件开发团队里的 CodeReviewer。
                你只负责检查 Engineer 最新脚本是否满足任务目标，并给出阻塞性修改意见。
                当前团队按固定顺序群聊：ProductManager -> Engineer -> CodeReviewer -> ProductManager。
                如果脚本已经满足“获取并打印实时比特币价格”的任务目标，并且具备基础异常处理，你必须输出 %s。
                如果脚本仍有缺陷，你必须只给出阻塞性问题和下一步修订要求，禁止写代码。
                """.formatted(TASK_DONE_MARKER);
    }

    /**
     * 构造群聊任务启动提示。
     *
     * @param task 原始任务
     * @return 启动提示
     */
    public static String groupKickoffPrompt(String task) {
        return """
                团队协作任务：
                %s

                群聊规则：
                1. 参与角色固定为 ProductManager、Engineer、CodeReviewer。
                2. 发言顺序固定为 ProductManager -> Engineer -> CodeReviewer。
                3. 共享同一份群聊上下文，后续发言必须显式参考已有消息。
                4. 只有 CodeReviewer 可以在验收通过后输出 %s。
                """.formatted(defaultText(task), TASK_DONE_MARKER);
    }

    /**
     * 判断内容是否包含结束标记。
     *
     * @param content 原始内容
     * @return 是否包含结束标记
     */
    public static boolean containsTaskDoneMarker(String content) {
        return content != null && content.contains(TASK_DONE_MARKER);
    }

    /**
     * 去除结束标记。
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
     * 提取更干净的 Python 脚本正文。
     *
     * @param content 原始内容
     * @return 规范化后的脚本
     */
    public static String normalizePythonScript(String content) {
        String text = stripTaskDoneMarker(content);
        if (!text.startsWith("```")) {
            return text;
        }
        String normalized = text;
        int firstLineBreak = normalized.indexOf('\n');
        if (firstLineBreak >= 0) {
            normalized = normalized.substring(firstLineBreak + 1);
        }
        if (normalized.endsWith("```")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized.trim();
    }

    /**
     * 返回非空文本。
     *
     * @param value 原始值
     * @return 非空文本
     */
    private static String defaultText(String value) {
        return value == null ? "" : value;
    }
}

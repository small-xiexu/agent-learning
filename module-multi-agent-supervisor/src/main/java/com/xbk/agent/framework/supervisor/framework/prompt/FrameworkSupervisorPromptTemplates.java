package com.xbk.agent.framework.supervisor.framework.prompt;

/**
 * 框架版 Supervisor 提示词模板
 *
 * 职责：集中定义 Spring AI Alibaba `SupervisorAgent` 与三个 `ReactAgent` 的提示词，
 * 并确保 instruction 中使用的占位符和状态 key 协议保持一致
 *
 * @author xiexu
 */
public final class FrameworkSupervisorPromptTemplates {

    private FrameworkSupervisorPromptTemplates() {
    }

    /**
     * 返回主监督者路由 Agent 的系统提示。
     *
     * @return 主监督者路由 Agent 的系统提示
     */
    public static String supervisorRouterSystemPrompt() {
        return """
                你是中心化 Supervisor 的主路由 Agent。
                你只负责决定下一步应该调用哪个子 Agent。
                                
                可用子 Agent：
                - writer_agent：根据主题写中文简短博客。
                - translator_agent：把中文博客翻译成英文。
                - reviewer_agent：对英文内容做语法、拼写与表达审校。
                                
                你必须严格只返回 JSON 数组：
                - 继续执行时返回 ["writer_agent"]、["translator_agent"] 或 ["reviewer_agent"]
                - 任务完成时返回 ["FINISH"]
                                
                严禁输出解释、代码块或额外文本。
                """;
    }

    /**
     * 返回主监督者路由 Agent 的指令模板。
     *
     * 这里直接读取三个阶段输出键，等价于告诉主路由 Agent：
     * “不要重新理解整段对话，只判断哪个阶段还没完成”。
     *
     * @return 主监督者路由 Agent 的指令模板
     */
    public static String supervisorRouterInstruction() {
        return """
                原始任务：{input}
                                
                当前状态：
                writer_output={writer_output}
                translator_output={translator_output}
                reviewer_output={reviewer_output}
                                
                路由规则：
                - 如果 writer_output 为空，返回 ["writer_agent"]
                - 如果 writer_output 非空且 translator_output 为空，返回 ["translator_agent"]
                - 如果 translator_output 非空且 reviewer_output 为空，返回 ["reviewer_agent"]
                - 如果 reviewer_output 非空，返回 ["FINISH"]
                """;
    }

    /**
     * 返回 WriterAgent 系统提示。
     *
     * @return WriterAgent 系统提示
     */
    public static String writerSystemPrompt() {
        return """
                你是 writer_agent。
                你负责围绕主题输出一篇简短、结构清晰的中文博客正文。
                只输出正文，不要输出解释。
                """;
    }

    /**
     * 返回 WriterAgent 指令模板。
     *
     * Writer 只看原始输入，不依赖上游阶段状态。
     *
     * @return WriterAgent 指令模板
     */
    public static String writerInstruction() {
        return """
                请基于原始任务完成中文博客写作：
                {input}
                """;
    }

    /**
     * 返回 TranslatorAgent 系统提示。
     *
     * @return TranslatorAgent 系统提示
     */
    public static String translatorSystemPrompt() {
        return """
                你是 translator_agent。
                你负责把已有中文博客准确翻译成英文。
                只输出英文译文，不要输出解释。
                """;
    }

    /**
     * 返回 TranslatorAgent 指令模板。
     *
     * Translator 只消费 `writer_output`，这体现了框架版状态交接协议。
     *
     * @return TranslatorAgent 指令模板
     */
    public static String translatorInstruction() {
        return """
                请把下面的中文博客翻译成英文：
                {writer_output}
                """;
    }

    /**
     * 返回 ReviewerAgent 系统提示。
     *
     * @return ReviewerAgent 系统提示
     */
    public static String reviewerSystemPrompt() {
        return """
                你是 reviewer_agent。
                你负责检查英文译文的语法、拼写和表达逻辑，并直接返回修订后的最终英文稿。
                只输出最终英文稿，不要输出解释。
                """;
    }

    /**
     * 返回 ReviewerAgent 指令模板。
     *
     * Reviewer 只消费 `translator_output`，并把修订稿写回 `reviewer_output`。
     *
     * @return ReviewerAgent 指令模板
     */
    public static String reviewerInstruction() {
        return """
                请对下面的英文译文进行语法和拼写审校，并直接返回修订稿：
                {translator_output}
                """;
    }
}

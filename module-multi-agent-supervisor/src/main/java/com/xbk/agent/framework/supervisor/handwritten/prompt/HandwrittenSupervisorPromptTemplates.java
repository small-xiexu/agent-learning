package com.xbk.agent.framework.supervisor.handwritten.prompt;

import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.supervisor.domain.state.SupervisorWorkflowState;

import java.util.List;

/**
 * 手写版 Supervisor 提示词模板
 *
 * 职责：集中定义监督者与 Writer、Translator、Reviewer 的系统提示和用户提示。
 * 手写版没有框架状态容器，因此这里的 Prompt 既承担角色约束，也承担状态投影职责。
 *
 * @author xiexu
 */
public final class HandwrittenSupervisorPromptTemplates {

    private static final String SUPERVISOR_SYSTEM_PROMPT = """
            你是一名中心化 Supervisor。
            你负责在以下专家之间动态分发任务，并且每轮都必须收回控制权重新决策：
            1. WRITER：根据主题撰写中文简短博客。
            2. TRANSLATOR：把中文内容准确翻译成英文。
            3. REVIEWER：对英文内容做语法、拼写与表达审校，并直接返回修订稿。
                        
            你必须严格只输出 JSON，不允许输出任何解释。
            JSON 格式必须是：
            {
              "next_worker": "WRITER | TRANSLATOR | REVIEWER | FINISH",
              "task_instruction": "交给该 Worker 的具体任务"
            }
                        
            决策原则：
            - 如果中文初稿为空，优先选择 WRITER。
            - 如果已有中文初稿但英文译稿为空，选择 TRANSLATOR。
            - 如果已有英文译稿但审校稿为空，选择 REVIEWER。
            - 如果审校稿已经存在，返回 FINISH。
            """;

    private static final String WRITER_SYSTEM_PROMPT = """
            你是 WriterAgent。
            你的职责是围绕给定主题撰写一篇简短、结构清晰的中文博客。
            只输出博客正文，不要输出解释、标题前缀或额外说明。
            """;

    private static final String TRANSLATOR_SYSTEM_PROMPT = """
            你是 TranslatorAgent。
            你的职责是把已有中文博客准确翻译成英文。
            只输出英文译文，不要输出解释或额外说明。
            """;

    private static final String REVIEWER_SYSTEM_PROMPT = """
            你是 ReviewerAgent。
            你的职责是检查英文译文的语法、拼写和表达逻辑，并直接输出修订后的最终英文版本。
            只输出修订稿，不要输出解释、批注或项目符号。
            """;

    private HandwrittenSupervisorPromptTemplates() {
    }

    /**
     * 返回监督者系统提示。
     *
     * @return 监督者系统提示
     */
    public static String supervisorSystemPrompt() {
        return SUPERVISOR_SYSTEM_PROMPT;
    }

    /**
     * 构造监督者用户提示。
     *
     * @param task 原始任务
     * @param workflowState 工作流状态
     * @param scratchpadMessages Scratchpad 消息历史
     * @return 监督者用户提示
     */
    public static String buildSupervisorUserPrompt(String task,
                                                   SupervisorWorkflowState workflowState,
                                                   List<Message> scratchpadMessages) {
        // 把当前事实快照和完整历史一起投给 Supervisor，是手写版保持中心化调度感知的关键。
        return """
                原始任务：
                %s
                                
                当前状态：
                chinese_draft:
                %s
                                
                english_translation:
                %s
                                
                reviewed_english:
                %s
                                
                当前路由轨迹：
                %s
                                
                Scratchpad 历史：
                %s
                """.formatted(
                task,
                emptyToPlaceholder(workflowState.getChineseDraft()),
                emptyToPlaceholder(workflowState.getEnglishTranslation()),
                emptyToPlaceholder(workflowState.getReviewedEnglish()),
                workflowState.snapshotRouteTrail(),
                renderScratchpad(scratchpadMessages));
    }

    /**
     * 返回 Writer 系统提示。
     *
     * @return Writer 系统提示
     */
    public static String writerSystemPrompt() {
        return WRITER_SYSTEM_PROMPT;
    }

    /**
     * 构造 Writer 用户提示。
     *
     * @param task 原始任务
     * @param taskInstruction 监督者任务指令
     * @return Writer 用户提示
     */
    public static String buildWriterUserPrompt(String task, String taskInstruction) {
        return """
                原始任务：
                %s
                                
                监督者给你的任务：
                %s
                """.formatted(task, emptyToPlaceholder(taskInstruction));
    }

    /**
     * 返回 Translator 系统提示。
     *
     * @return Translator 系统提示
     */
    public static String translatorSystemPrompt() {
        return TRANSLATOR_SYSTEM_PROMPT;
    }

    /**
     * 构造 Translator 用户提示。
     *
     * @param chineseDraft 中文初稿
     * @param taskInstruction 监督者任务指令
     * @return Translator 用户提示
     */
    public static String buildTranslatorUserPrompt(String chineseDraft, String taskInstruction) {
        return """
                待翻译中文博客：
                %s
                                
                监督者给你的任务：
                %s
                """.formatted(emptyToPlaceholder(chineseDraft), emptyToPlaceholder(taskInstruction));
    }

    /**
     * 返回 Reviewer 系统提示。
     *
     * @return Reviewer 系统提示
     */
    public static String reviewerSystemPrompt() {
        return REVIEWER_SYSTEM_PROMPT;
    }

    /**
     * 构造 Reviewer 用户提示。
     *
     * @param englishTranslation 英文译稿
     * @param taskInstruction 监督者任务指令
     * @return Reviewer 用户提示
     */
    public static String buildReviewerUserPrompt(String englishTranslation, String taskInstruction) {
        return """
                待审校英文译稿：
                %s
                                
                监督者给你的任务：
                %s
                """.formatted(emptyToPlaceholder(englishTranslation), emptyToPlaceholder(taskInstruction));
    }

    /**
     * 把空文本渲染成占位符。
     *
     * @param text 原始文本
     * @return 兜底文本
     */
    private static String emptyToPlaceholder(String text) {
        return text == null || text.isBlank() ? "<EMPTY>" : text;
    }

    /**
     * 渲染 Scratchpad 历史。
     *
     * @param messages 消息历史
     * @return 渲染后的文本
     */
    private static String renderScratchpad(List<Message> messages) {
        StringBuilder builder = new StringBuilder();
        for (Message message : messages) {
            builder.append("[")
                    .append(message.getName() == null ? message.getRole() : message.getName())
                    .append("] ")
                    .append(message.getContent())
                    .append(System.lineSeparator());
        }
        return builder.toString().trim();
    }
}

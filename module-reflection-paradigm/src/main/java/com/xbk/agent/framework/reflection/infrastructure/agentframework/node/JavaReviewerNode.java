package com.xbk.agent.framework.reflection.infrastructure.agentframework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.xbk.agent.framework.reflection.infrastructure.agentframework.support.ReflectionStateKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Reflection 图编排版评审节点
 *
 * 职责：读取当前代码给出评审意见，并推进 iteration_count
 *
 * @author xiexu
 */
public class JavaReviewerNode implements AsyncNodeAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaReviewerNode.class);
    private static final String REVIEWER_PROMPT_TEMPLATE = """
            原始任务：
            {input}

            当前代码：
            {current_code}

            请从时间复杂度角度给出评审意见。
            如果仍然存在明确、可落地的复杂度优化空间，请不要输出“无需改进”，而是明确指出瓶颈和下一轮如何修改。
            只有当当前实现已经没有明显的时间复杂度优化空间时，才明确输出“无需改进”。
            """;

    private final ChatModel chatModel;

    /**
     * 创建评审节点。
     *
     * @param chatModel Spring AI ChatModel
     */
    public JavaReviewerNode(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 执行当前评审节点。
     *
     * @param state 全局状态
     * @return 节点输出状态
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String prompt = REVIEWER_PROMPT_TEMPLATE
                .replace("{input}", state.value(ReflectionStateKeys.INPUT, ""))
                .replace("{current_code}", state.value(ReflectionStateKeys.CURRENT_CODE, ""));
        ChatResponse response = chatModel.call(new Prompt(prompt));
        // 取出 reviewer 本轮返回的自然语言评审意见文本。
        String text = response.getResult().getOutput().getText();
        // 从全局状态读取当前已完成的评审轮次；首次进入时默认从 0 开始。
        int currentIterationCount = state.value(ReflectionStateKeys.ITERATION_COUNT, Integer.class).orElse(Integer.valueOf(0));
        int nextIterationCount = currentIterationCount + 1;
        LOGGER.info("reviewer completed: previousIterationCount={}, nextIterationCount={}, reviewFeedbackPreview={}",
                currentIterationCount,
                nextIterationCount,
                summarizeForLog(text));
        // 返回一个已完成的异步结果：
        // 1. review_feedback 保存本轮最新评审意见，避免 text 为 null；
        // 2. iteration_count 在当前轮次基础上加 1，表示本轮 reviewer 已执行完成。
        return CompletableFuture.completedFuture(Map.of(
                ReflectionStateKeys.REVIEW_FEEDBACK, text == null ? "" : text,
                ReflectionStateKeys.ITERATION_COUNT, nextIterationCount));
    }

    /**
     * 生成适合日志输出的简短摘要。
     *
     * @param text 原始文本
     * @return 日志摘要
     */
    private String summarizeForLog(String text) {
        if (text == null || text.isBlank()) {
            return "(empty)";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120) + "...";
    }
}

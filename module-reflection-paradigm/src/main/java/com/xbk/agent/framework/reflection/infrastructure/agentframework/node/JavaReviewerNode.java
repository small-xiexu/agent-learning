package com.xbk.agent.framework.reflection.infrastructure.agentframework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
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

    private static final String REVIEWER_PROMPT_TEMPLATE = """
            原始任务：
            {input}

            当前代码：
            {current_code}

            请从时间复杂度角度给出评审意见。
            如果已经无需继续优化，请明确输出“无需改进”。
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
                .replace("{input}", state.value("input", ""))
                .replace("{current_code}", state.value("current_code", ""));
        ChatResponse response = chatModel.call(new Prompt(prompt));
        String text = response.getResult().getOutput().getText();
        int currentIterationCount = state.value("iteration_count", Integer.class).orElse(Integer.valueOf(0));
        return CompletableFuture.completedFuture(Map.of(
                "review_feedback", text == null ? "" : text,
                "iteration_count", currentIterationCount + 1));
    }
}

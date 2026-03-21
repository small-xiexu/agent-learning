package com.xbk.agent.framework.reflection.infrastructure.agentframework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Reflection 图编排版代码生成节点
 *
 * 职责：读取状态中的任务、旧代码和评审意见，生成当前轮代码版本
 *
 * @author xiexu
 */
public class JavaCoderNode implements AsyncNodeAction {

    private static final String CODER_PROMPT_TEMPLATE = """
            原始任务：
            {input}

            当前代码：
            {current_code}

            评审意见：
            {review_feedback}

            如果当前没有旧代码，请先给出一版可运行初稿。
            如果已经有旧代码和评审意见，请基于评审意见输出优化后的完整 Java 代码。
            只输出代码，不要解释。
            """;

    private final ChatModel chatModel;

    /**
     * 创建代码生成节点。
     *
     * @param chatModel Spring AI ChatModel
     */
    public JavaCoderNode(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 执行当前代码生成节点。
     *
     * @param state 全局状态
     * @return 节点输出状态
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        String prompt = CODER_PROMPT_TEMPLATE
                .replace("{input}", state.value("input", ""))
                .replace("{current_code}", state.value("current_code", ""))
                .replace("{review_feedback}", state.value("review_feedback", ""));
        ChatResponse response = chatModel.call(new Prompt(prompt));
        String text = response.getResult().getOutput().getText();
        return CompletableFuture.completedFuture(Map.of("current_code", text == null ? "" : text));
    }
}

package com.xbk.agent.framework.graphflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.graphflow.common.state.GraphState;
import com.xbk.agent.framework.graphflow.framework.AlibabaGraphFlowAgent;
import com.xbk.agent.framework.graphflow.framework.support.GraphFlowStateKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Graph Flow Demo 日志支撑
 *
 * 职责：统一打印手写版与框架版图流 Demo 的关键状态、分支选择和最终回答。
 *
 * @author xiexu
 */
final class GraphFlowDemoLogSupport {

    private GraphFlowDemoLogSupport() {
    }

    /**
     * 打印手写版运行结果。
     *
     * @param logger 日志对象
     * @param title 标题
     * @param state 最终图状态
     */
    static void logHandwrittenResult(Logger logger, String title, GraphState state) {
        logger.info("========================================");
        logger.info(title);
        logger.info("SUMMARY");
        logger.info("  stepStatus    -> " + state.getStepStatus());
        logger.info("  userQuery     -> " + defaultText(state.getUserQuery()));
        logger.info("  searchQuery   -> " + defaultText(state.getSearchQuery()));
        logger.info("  usedFallback  -> " + (state.getErrorMessage() != null && !state.getErrorMessage().isBlank()));
        logger.info("SEARCH_RESULTS");
        logMultilineSection(logger, state.getSearchResults());
        if (state.getErrorMessage() != null && !state.getErrorMessage().isBlank()) {
            logger.info("ERROR_MESSAGE");
            logMultilineSection(logger, state.getErrorMessage());
        }
        logger.info("FINAL_ANSWER");
        logMultilineSection(logger, state.getFinalAnswer());
        logger.info("========================================");
    }

    /**
     * 打印框架版运行结果。
     *
     * @param logger 日志对象
     * @param title 标题
     * @param result 运行结果
     * @param state 图状态
     */
    static void logFrameworkResult(Logger logger,
                                   String title,
                                   AlibabaGraphFlowAgent.RunResult result,
                                   OverAllState state) {
        logger.info("========================================");
        logger.info(title);
        logger.info("SUMMARY");
        logger.info("  userQuery     -> " + defaultText(result.getUserQuery()));
        logger.info("  searchQuery   -> " + state.value(GraphFlowStateKeys.SEARCH_QUERY, String.class).orElse(""));
        logger.info("  usedFallback  -> " + result.isUsedFallback());
        logger.info("  stateKeys     -> " + new ArrayList<String>(state.data().keySet()));
        logger.info("SEARCH_RESULTS");
        logMultilineSection(logger, result.getSearchResults());
        if (result.isUsedFallback()) {
            logger.info("ERROR_MESSAGE");
            logMultilineSection(logger, state.value(GraphFlowStateKeys.ERROR_MESSAGE, String.class).orElse(""));
        }
        logger.info("FINAL_ANSWER");
        logMultilineSection(logger, result.getFinalAnswer());
        logger.info("========================================");
    }

    /**
     * 逐行打印多行文本。
     *
     * @param logger 日志对象
     * @param content 多行文本
     */
    private static void logMultilineSection(Logger logger, String content) {
        for (String line : normalizeLines(content)) {
            logger.info("  " + line);
        }
    }

    /**
     * 规范化日志文本行。
     *
     * @param content 原始文本
     * @return 文本行
     */
    private static List<String> normalizeLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of("(empty)");
        }
        List<String> lines = new ArrayList<String>();
        for (String line : content.replace("**", "").split("\\R")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                lines.add(trimmedLine);
            }
        }
        return lines.isEmpty() ? List.of("(empty)") : List.copyOf(lines);
    }

    /**
     * 返回适合日志打印的默认文本。
     *
     * @param content 原始文本
     * @return 非空文本
     */
    private static String defaultText(String content) {
        return content == null || content.isBlank() ? "(empty)" : content;
    }
}

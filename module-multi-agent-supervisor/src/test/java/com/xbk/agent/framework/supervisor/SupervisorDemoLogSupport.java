package com.xbk.agent.framework.supervisor;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.supervisor.api.SupervisorRunResult;
import com.xbk.agent.framework.supervisor.domain.memory.SupervisorStepRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Supervisor Demo 日志支撑
 *
 * 职责：统一打印 Supervisor Demo 的摘要、路由轨迹与最终产物，
 * 避免每个 Demo 自己拼接日志格式
 *
 * @author xiexu
 */
public final class SupervisorDemoLogSupport {

    private SupervisorDemoLogSupport() {
    }

    /**
     * 打印运行结果。
     *
     * @param logger 日志对象
     * @param title 标题
     * @param result 运行结果
     * @param state Flow 状态
     */
    public static void logRunResult(Logger logger,
                                    String title,
                                    SupervisorRunResult result,
                                    OverAllState state) {
        // Demo 日志强调“可回放”，因此会同时打印路由轨迹、步骤记录和最终阶段产物。
        logger.info("========================================");
        logger.info(title);
        logger.info("SUMMARY");
        logger.info("  stopWorker -> " + result.getStopWorker());
        logger.info("  stopReason -> " + result.getStopReason());
        logger.info("  routeTrail -> " + result.getRouteTrail());
        if (state != null) {
            logger.info("  stateKeys  -> " + new ArrayList<String>(state.data().keySet()));
        }
        logger.info("STEP_RECORDS");
        for (SupervisorStepRecord stepRecord : result.getStepRecords()) {
            logger.info("----------------------------------------");
            logger.info("[STEP " + stepRecord.getStepIndex() + "][" + stepRecord.getWorkerType() + "]");
            for (String line : normalizeLines(stepRecord.getWorkerOutput())) {
                logger.info("  " + line);
            }
        }
        logMultilineSection(logger, "CHINESE_DRAFT", result.getChineseDraft());
        logMultilineSection(logger, "ENGLISH_TRANSLATION", result.getEnglishTranslation());
        logMultilineSection(logger, "REVIEWED_ENGLISH", result.getReviewedEnglish());
        logger.info("========================================");
    }

    /**
     * 打印多行文本分段。
     *
     * @param logger 日志对象
     * @param title 分段标题
     * @param content 分段内容
     */
    private static void logMultilineSection(Logger logger, String title, String content) {
        logger.info(title);
        for (String line : normalizeLines(content)) {
            logger.info("  " + line);
        }
    }

    /**
     * 规范化文本行。
     *
     * @param content 原始文本
     * @return 文本行
     */
    private static List<String> normalizeLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of("(empty)");
        }
        List<String> lines = new ArrayList<String>();
        // 去掉 markdown 强调符，避免真实模型输出在控制台里显得太噪。
        for (String line : content.replace("**", "").split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines.isEmpty() ? List.of("(empty)") : List.copyOf(lines);
    }
}

package com.xbk.agent.framework.engineering;

import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.domain.trace.DeliveryRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Engineering Demo 日志支撑。
 *
 * 职责：统一打印工程模块 Demo 的路由摘要、消息轨迹与最终答复。
 *
 * @author xiexu
 */
final class EngineeringDemoLogSupport {

    private EngineeringDemoLogSupport() {
    }

    /**
     * 打印手写版运行结果。
     *
     * @param logger 日志对象
     * @param title 标题
     * @param result 运行结果
     */
    static void logHandwrittenResult(Logger logger, String title, EngineeringRunResult result) {
        logger.info("========================================");
        logger.info(title);
        logger.info("SUMMARY");
        logger.info("  conversationId   -> " + defaultText(result.getConversationId()));
        logger.info("  intentType       -> " + String.valueOf(result.getIntentType()));
        logger.info("  specialistType   -> " + String.valueOf(result.getSpecialistType()));
        logger.info("  routeTrail       -> " + String.join(" -> ", result.getRouteTrail()));
        if (result.getRoutingDecision() != null) {
            logger.info("  targetTopic      -> " + defaultText(result.getRoutingDecision().getTargetTopic()));
            logger.info("  targetAgentName  -> " + defaultText(result.getRoutingDecision().getTargetAgentName()));
        }
        logger.info("DELIVERY_RECORDS");
        if (result.getTrace() == null || result.getTrace().getDeliveryRecords().isEmpty()) {
            logger.info("  (empty)");
        } else {
            for (DeliveryRecord record : result.getTrace().getDeliveryRecords()) {
                logger.info("  "
                        + defaultText(record.getEventType())
                        + " | topic=" + defaultText(record.getTopic())
                        + " | from=" + defaultText(record.getFromAgent())
                        + " | to=" + defaultText(record.getToAgent())
                        + " | correlationId=" + defaultText(record.getCorrelationId()));
            }
        }
        logger.info("SPECIALIST_RESPONSE");
        logMultilineSection(logger, result.getSpecialistResponse());
        logger.info("FINAL_RESPONSE");
        logMultilineSection(logger, result.getFinalResponse());
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

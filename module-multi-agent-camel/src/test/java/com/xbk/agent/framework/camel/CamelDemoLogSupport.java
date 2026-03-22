package com.xbk.agent.framework.camel;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.camel.api.CamelRunResult;
import com.xbk.agent.framework.camel.domain.memory.CamelDialogueTurn;
import com.xbk.agent.framework.camel.domain.role.CamelRoleType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CAMEL Demo 日志支撑
 *
 * 职责：把真实 OpenAI Demo 的 transcript、状态和最终代码格式化成更适合人眼阅读的日志
 *
 * @author xiexu
 */
final class CamelDemoLogSupport {

    private static final Pattern CLASS_PATTERN = Pattern.compile("\\bclass\\s+(\\w+)");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "\\b(?:public|private|protected)\\s+[\\w<>\\[\\], ?]+\\s+(\\w+)\\s*\\(");

    private CamelDemoLogSupport() {
    }

    /**
     * 打印统一运行结果。
     *
     * @param logger 日志器
     * @param title 标题
     * @param result 运行结果
     * @param state Flow 状态，手写版可为空
     */
    static void logRunResult(Logger logger, String title, CamelRunResult result, OverAllState state) {
        logger.info("========================================");
        logger.info(title);
        logger.info("SUMMARY");
        logger.info("  totalTurns -> " + result.getTranscript().size());
        logger.info("  stopRole   -> " + result.getStopRole());
        logger.info("  stopReason -> " + summarizeStopReason(result.getStopReason()));
        if (state != null) {
            logger.info("  stateKeys  -> " + new ArrayList<String>(state.data().keySet()));
        }
        logger.info("TRANSCRIPT");
        logTranscript(logger, result.getTranscript());
        logger.info("FINAL_SCRIPT");
        logFinalJavaCode(logger, result.getFinalJavaCode());
        logger.info("========================================");
    }

    /**
     * 打印 transcript。
     *
     * @param logger 日志器
     * @param transcript 对话轨迹
     */
    private static void logTranscript(Logger logger, List<CamelDialogueTurn> transcript) {
        for (CamelDialogueTurn turn : transcript) {
            logger.info("----------------------------------------");
            if (turn.getRoleType() == CamelRoleType.PROGRAMMER) {
                logProgrammerTurn(logger, turn);
                continue;
            }
            logTraderTurn(logger, turn);
        }
    }

    /**
     * 打印交易员发言。
     *
     * @param logger 日志器
     * @param turn 当前轮次
     */
    private static void logTraderTurn(Logger logger, CamelDialogueTurn turn) {
        logger.info("[TURN " + turn.getTurnNumber() + "][TRADER]");
        for (String line : normalizeTextLines(turn.getContent())) {
            logger.info("  " + line);
        }
    }

    /**
     * 打印程序员发言摘要。
     *
     * @param logger 日志器
     * @param turn 当前轮次
     */
    private static void logProgrammerTurn(Logger logger, CamelDialogueTurn turn) {
        String code = extractCodeBlock(turn.getContent());
        List<String> codeLines = normalizeCodeLines(code);
        logger.info("[TURN " + turn.getTurnNumber() + "][PROGRAMMER]");
        logger.info("  codeLines  -> " + codeLines.size());
        String className = extractFirstGroup(CLASS_PATTERN, code);
        if (className != null) {
            logger.info("  class      -> " + className);
        }
        List<String> methods = extractMethods(code);
        if (!methods.isEmpty()) {
            logger.info("  methods    -> " + String.join(", ", methods));
        }
        logger.info("  preview");
        int previewLines = Math.min(6, codeLines.size());
        for (int index = 0; index < previewLines; index++) {
            logger.info("    " + codeLines.get(index));
        }
        if (codeLines.size() > previewLines) {
            logger.info("    ... 其余 " + (codeLines.size() - previewLines) + " 行已省略，完整代码见 FINAL_SCRIPT");
        }
    }

    /**
     * 打印最终 Java 代码。
     *
     * @param logger 日志器
     * @param content 原始代码
     */
    private static void logFinalJavaCode(Logger logger, String content) {
        List<String> codeLines = normalizeCodeLines(extractCodeBlock(content));
        if (codeLines.isEmpty()) {
            logger.info("  (empty)");
            return;
        }
        for (int index = 0; index < codeLines.size(); index++) {
            String lineNumber = String.format("%03d", Integer.valueOf(index + 1));
            logger.info("  " + lineNumber + " | " + codeLines.get(index));
        }
    }

    /**
     * 提炼停止原因。
     *
     * @param stopReason 原始停止原因
     * @return 摘要停止原因
     */
    private static String summarizeStopReason(String stopReason) {
        if (stopReason == null || stopReason.isBlank()) {
            return "(empty)";
        }
        if ("MAX_TURNS_REACHED".equals(stopReason)) {
            return "MAX_TURNS_REACHED";
        }
        List<String> lines = normalizeTextLines(stopReason.replace("<CAMEL_TASK_DONE>", "").trim());
        if (lines.isEmpty()) {
            return "<CAMEL_TASK_DONE>";
        }
        return lines.getFirst();
    }

    /**
     * 提取代码块正文。
     *
     * @param content 原始文本
     * @return 代码正文
     */
    private static String extractCodeBlock(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replace("```java", "").replace("```", "");
        return normalized.trim();
    }

    /**
     * 规范化普通文本行。
     *
     * @param content 原始文本
     * @return 文本行
     */
    private static List<String> normalizeTextLines(String content) {
        List<String> lines = new ArrayList<String>();
        if (content == null || content.isBlank()) {
            lines.add("(empty)");
            return List.copyOf(lines);
        }
        for (String line : content.replace("**", "").split("\\R")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                lines.add(trimmedLine);
            }
        }
        if (lines.isEmpty()) {
            lines.add("(empty)");
        }
        return List.copyOf(lines);
    }

    /**
     * 规范化代码行。
     *
     * @param content 原始代码
     * @return 代码行
     */
    private static List<String> normalizeCodeLines(String content) {
        List<String> lines = new ArrayList<String>();
        if (content == null || content.isBlank()) {
            return List.of();
        }
        for (String line : content.split("\\R")) {
            String cleanedLine = line.replace("\t", "    ").replace("**", "");
            if (!cleanedLine.trim().isEmpty()) {
                lines.add(cleanedLine);
            }
        }
        return List.copyOf(lines);
    }

    /**
     * 提取第一个正则分组。
     *
     * @param pattern 正则
     * @param content 原始文本
     * @return 第一个分组，没有则返回空
     */
    private static String extractFirstGroup(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content == null ? "" : content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 提取方法名列表。
     *
     * @param content 原始代码
     * @return 方法名列表
     */
    private static List<String> extractMethods(String content) {
        Matcher matcher = METHOD_PATTERN.matcher(content == null ? "" : content);
        Set<String> methods = new LinkedHashSet<String>();
        while (matcher.find()) {
            methods.add(matcher.group(1));
        }
        return List.copyOf(methods);
    }
}

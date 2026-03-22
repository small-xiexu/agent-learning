package com.xbk.agent.framework.conversation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.conversation.api.ConversationRunResult;
import com.xbk.agent.framework.conversation.domain.memory.ConversationTurn;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Conversation Demo 日志支撑
 *
 * 职责：统一打印 AutoGen 群聊 Demo 的摘要、transcript 和最终脚本
 *
 * @author xiexu
 */
public final class ConversationDemoLogSupport {

    private ConversationDemoLogSupport() {
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
                                    ConversationRunResult result,
                                    OverAllState state) {
        logger.info("========================================");
        logger.info(title);
        logger.info("SUMMARY");
        logger.info("  totalTurns -> " + result.getTranscript().size());
        logger.info("  stopRole   -> " + result.getStopRole());
        logger.info("  stopReason -> " + result.getStopReason());
        if (state != null) {
            logger.info("  stateKeys  -> " + new ArrayList<String>(state.data().keySet()));
        }
        logger.info("TRANSCRIPT");
        for (ConversationTurn turn : result.getTranscript()) {
            logger.info("----------------------------------------");
            logger.info("[TURN " + turn.getTurnNumber() + "][" + turn.getRoleType() + "]");
            if (turn.getRoleType() == ConversationRoleType.ENGINEER) {
                logScriptSummary(logger, turn.getContent());
            } else {
                for (String line : normalizeLines(turn.getContent())) {
                    logger.info("  " + line);
                }
            }
        }
        logger.info("FINAL_SCRIPT");
        int lineNumber = 1;
        for (String line : normalizeRawLines(result.getFinalPythonScript())) {
            logger.info(String.format("  %03d | %s", Integer.valueOf(lineNumber), line));
            lineNumber++;
        }
        logger.info("========================================");
    }

    /**
     * 打印脚本摘要。
     *
     * @param logger 日志对象
     * @param script Python 脚本
     */
    private static void logScriptSummary(Logger logger, String script) {
        List<String> rawLines = normalizeRawLines(script);
        logger.info("  codeLines  -> " + rawLines.size());
        logger.info("  functions  -> " + detectPythonFunctions(rawLines));
        logger.info("  preview");
        int previewCount = Math.min(6, rawLines.size());
        for (int index = 0; index < previewCount; index++) {
            logger.info("    " + rawLines.get(index));
        }
        if (rawLines.size() > previewCount) {
            logger.info("    ... 其余 " + (rawLines.size() - previewCount) + " 行已省略，完整脚本见 FINAL_SCRIPT");
        }
    }

    /**
     * 检测 Python 函数名。
     *
     * @param rawLines 原始代码行
     * @return 函数摘要
     */
    private static String detectPythonFunctions(List<String> rawLines) {
        List<String> names = new ArrayList<String>();
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("def ") && trimmed.contains("(")) {
                names.add(trimmed.substring(4, trimmed.indexOf('(')).trim());
            }
        }
        return names.isEmpty() ? "(none)" : String.join(", ", names);
    }

    /**
     * 规范化普通文本行。
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
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines.isEmpty() ? List.of("(empty)") : List.copyOf(lines);
    }

    /**
     * 规范化原始代码行。
     *
     * @param content 原始文本
     * @return 代码行
     */
    private static List<String> normalizeRawLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of("(empty)");
        }
        List<String> lines = new ArrayList<String>();
        for (String line : content.split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines.isEmpty() ? List.of("(empty)") : List.copyOf(lines);
    }
}

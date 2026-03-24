package com.xbk.agent.framework.supervisor.handwritten.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.agent.framework.supervisor.domain.routing.RoutingDecision;
import com.xbk.agent.framework.supervisor.domain.routing.SupervisorWorkerType;

/**
 * 监督者 JSON 决策解析器
 *
 * 职责：把 Supervisor 返回的标准化 JSON 文本解析成 Java 路由决策对象。
 * 这是手写版把自然语言约束收口成结构化执行协议的关键一层。
 *
 * @author xiexu
 */
public final class SupervisorDecisionJsonParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 解析监督者 JSON 决策。
     *
     * @param rawText 原始文本
     * @return 路由决策
     */
    public RoutingDecision parse(String rawText) {
        try {
            // 真实模型有时会把 JSON 包进 markdown code fence，这里先做一次清洗再解析。
            String normalizedText = stripCodeFence(rawText);
            JsonNode rootNode = OBJECT_MAPPER.readTree(normalizedText);
            String nextWorker = rootNode.path("next_worker").asText(null);
            String taskInstruction = rootNode.path("task_instruction").asText("");
            return new RoutingDecision(SupervisorWorkerType.fromRouteValue(nextWorker), taskInstruction);
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to parse supervisor decision JSON: " + rawText, exception);
        }
    }

    /**
     * 去掉 markdown 代码块包装。
     *
     * @param rawText 原始文本
     * @return 去包装后的文本
     */
    private String stripCodeFence(String rawText) {
        if (rawText == null) {
            return "";
        }
        String trimmed = rawText.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineBreak > -1 && lastFence > firstLineBreak) {
                return trimmed.substring(firstLineBreak + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}

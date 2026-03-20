package com.xbk.agent.framework.llm.springai.adapter;

import com.xbk.agent.framework.core.tool.ToolDefinition;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.metadata.DefaultToolMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI 工具映射器
 *
 * 职责：将框架工具定义转换为适配层元数据
 *
 * @author xiexu
 */
public class SpringAiToolMapper {

    /**
     * 批量转换工具定义为 Spring AI ToolCallback。
     *
     * @param toolDefinitions 框架工具定义列表
     * @return ToolCallback 列表
     */
    public List<ToolCallback> toToolCallbacks(List<ToolDefinition> toolDefinitions) {
        if (toolDefinitions == null || toolDefinitions.isEmpty()) {
            return Collections.emptyList();
        }
        List<ToolCallback> callbacks = new ArrayList<ToolCallback>();
        for (ToolDefinition toolDefinition : toolDefinitions) {
            callbacks.add(toToolCallback(toolDefinition));
        }
        return List.copyOf(callbacks);
    }

    /**
     * 转换单个工具定义为 Spring AI ToolCallback。
     *
     * @param toolDefinition 框架工具定义
     * @return ToolCallback
     */
    public ToolCallback toToolCallback(ToolDefinition toolDefinition) {
        org.springframework.ai.tool.definition.ToolDefinition springDefinition = DefaultToolDefinition.builder()
                .name(toolDefinition.getName())
                .description(toolDefinition.getDescription())
                .inputSchema(toInputSchema(toolDefinition))
                .build();
        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return springDefinition;
            }

            @Override
            public org.springframework.ai.tool.metadata.ToolMetadata getToolMetadata() {
                return DefaultToolMetadata.builder()
                        .returnDirect(false)
                        .build();
            }

            @Override
            public String call(String toolInput) {
                throw new UnsupportedOperationException("framework-core only exposes tool schema to Spring AI");
            }
        };
    }

    /**
     * 转换工具定义为元数据映射
     *
     * @param toolDefinition 工具定义
     * @return 元数据映射
     */
    public Map<String, Object> toMetadata(ToolDefinition toolDefinition) {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("name", toolDefinition.getName());
        metadata.put("description", toolDefinition.getDescription());
        metadata.put("inputSchema", toolDefinition.getInputSchema());
        metadata.put("outputDescription", toolDefinition.getOutputDescription());
        metadata.put("tags", toolDefinition.getTags());
        metadata.put("idempotent", toolDefinition.isIdempotent());
        return metadata;
    }

    /**
     * 生成 Spring AI 所需的输入 schema 字符串。
     *
     * @param toolDefinition 框架工具定义
     * @return JSON schema 文本
     */
    public String toInputSchema(ToolDefinition toolDefinition) {
        if (toolDefinition.getInputSchema().isEmpty()) {
            return "{\"type\":\"object\",\"properties\":{}}";
        }
        return toJsonValue(toolDefinition.getInputSchema());
    }

    /**
     * 将通用 Java 对象递归序列化为 JSON 文本。
     *
     * @param value 任意值
     * @return JSON 文本
     */
    private String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escapeJson(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> mapValue) {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (!first) {
                    builder.append(",");
                }
                builder.append("\"")
                        .append(escapeJson(String.valueOf(entry.getKey())))
                        .append("\":")
                        .append(toJsonValue(entry.getValue()));
                first = false;
            }
            builder.append("}");
            return builder.toString();
        }
        if (value instanceof List<?> listValue) {
            StringBuilder builder = new StringBuilder();
            builder.append("[");
            boolean first = true;
            for (Object element : listValue) {
                if (!first) {
                    builder.append(",");
                }
                builder.append(toJsonValue(element));
                first = false;
            }
            builder.append("]");
            return builder.toString();
        }
        return "\"" + escapeJson(String.valueOf(value)) + "\"";
    }

    /**
     * 转义 JSON 字符串中的特殊字符。
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}

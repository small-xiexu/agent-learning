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
        // 这里创建的 ToolCallback 主要承担“向 Spring AI 暴露工具 schema”的职责，
        // 不是在这一层真正执行工具。
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
                // 真正的工具执行统一走 framework-core 的 ToolRegistry。
                // 对当前适配层来说，Spring AI 只需要知道“有哪些工具、参数长什么样”，
                // 不应该绕过项目抽象直接在这里调用工具实现。
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
            // 对没有显式参数的工具，也统一返回 object schema，
            // 这样 provider 侧看到的工具定义仍然是完整且合法的函数签名。
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
            // LinkedHashMap 顺序会被保留下来，便于输出的 schema 文本尽量稳定、可比对。
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
        // 这里只做最小必要转义，因为这层目标是把通用对象稳定序列化成 schema 文本，
        // 而不是实现一套完整 JSON 库。
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}

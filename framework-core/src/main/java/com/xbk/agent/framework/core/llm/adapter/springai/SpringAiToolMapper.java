package com.xbk.agent.framework.core.llm.adapter.springai;

import com.xbk.agent.framework.core.tool.ToolDefinition;

import java.util.LinkedHashMap;
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
}

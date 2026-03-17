package com.xbk.agent.framework.core.tool.support;

import com.xbk.agent.framework.core.common.exception.ToolExecutionException;
import com.xbk.agent.framework.core.common.exception.ToolNotFoundException;
import com.xbk.agent.framework.core.tool.Tool;
import com.xbk.agent.framework.core.tool.ToolContext;
import com.xbk.agent.framework.core.tool.ToolDefinition;
import com.xbk.agent.framework.core.tool.ToolRegistry;
import com.xbk.agent.framework.core.tool.ToolRequest;
import com.xbk.agent.framework.core.tool.ToolResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认工具注册中心
 *
 * 职责：维护稳定顺序的工具注册与执行委派
 *
 * @author xiexu
 */
public class DefaultToolRegistry implements ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<String, Tool>();

    /**
     * 注册单个工具
     *
     * @param tool 工具对象
     */
    @Override
    public synchronized void register(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("tool must not be null");
        }
        String toolName = tool.definition().getName();
        if (tools.containsKey(toolName)) {
            throw new IllegalArgumentException("duplicate tool registration: " + toolName);
        }
        tools.put(toolName, tool);
    }

    /**
     * 批量注册工具
     *
     * @param toolList 工具集合
     */
    @Override
    public synchronized void registerAll(Collection<Tool> toolList) {
        if (toolList == null) {
            throw new IllegalArgumentException("tools must not be null");
        }
        for (Tool tool : toolList) {
            register(tool);
        }
    }

    /**
     * 按名称获取工具
     *
     * @param toolName 工具名称
     * @return 工具对象
     */
    @Override
    public synchronized Tool get(String toolName) {
        Tool tool = tools.get(toolName);
        if (tool == null) {
            throw new ToolNotFoundException(toolName);
        }
        return tool;
    }

    /**
     * 返回全部工具定义
     *
     * @return 工具定义列表
     */
    @Override
    public synchronized List<ToolDefinition> definitions() {
        List<ToolDefinition> definitions = new ArrayList<ToolDefinition>();
        for (Tool tool : tools.values()) {
            definitions.add(tool.definition());
        }
        return List.copyOf(definitions);
    }

    /**
     * 执行工具请求
     *
     * @param request 工具请求
     * @param context 工具上下文
     * @return 工具结果
     */
    @Override
    public ToolResult execute(ToolRequest request, ToolContext context) {
        Tool tool = get(request.getToolName());
        try {
            return tool.execute(request, context);
        } catch (RuntimeException ex) {
            throw new ToolExecutionException(request.getToolName(), ex);
        }
    }
}

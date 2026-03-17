package com.xbk.agent.framework.core.tool;

import java.util.Collection;
import java.util.List;

/**
 * 工具注册中心
 *
 * 职责：管理工具注册、查找与执行委派
 *
 * @author xiexu
 */
public interface ToolRegistry {

    /**
     * 注册单个工具
     *
     * @param tool 工具对象
     */
    void register(Tool tool);

    /**
     * 批量注册工具
     *
     * @param tools 工具集合
     */
    void registerAll(Collection<Tool> tools);

    /**
     * 按名称获取工具
     *
     * @param toolName 工具名称
     * @return 工具对象
     */
    Tool get(String toolName);

    /**
     * 返回全部工具定义
     *
     * @return 工具定义列表
     */
    List<ToolDefinition> definitions();

    /**
     * 执行工具请求
     *
     * @param request 工具请求
     * @param context 工具上下文
     * @return 工具结果
     */
    ToolResult execute(ToolRequest request, ToolContext context);
}

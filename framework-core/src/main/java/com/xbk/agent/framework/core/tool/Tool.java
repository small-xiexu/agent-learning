package com.xbk.agent.framework.core.tool;

/**
 * 工具协议
 *
 * 职责：定义统一工具描述与执行入口
 *
 * @author xiexu
 */
public interface Tool {

    /**
     * 返回工具定义
     *
     * @return 工具定义
     */
    ToolDefinition definition();

    /**
     * 执行工具请求
     *
     * @param request 工具请求
     * @param context 工具上下文
     * @return 工具结果
     */
    ToolResult execute(ToolRequest request, ToolContext context);
}

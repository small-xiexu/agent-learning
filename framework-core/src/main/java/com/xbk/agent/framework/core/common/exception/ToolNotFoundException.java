package com.xbk.agent.framework.core.common.exception;

/**
 * 工具不存在异常
 *
 * 职责：表示工具注册中心无法定位目标工具
 *
 * @author xiexu
 */
public class ToolNotFoundException extends FrameworkCoreException {

    /**
     * 创建异常对象
     *
     * @param toolName 工具名称
     */
    public ToolNotFoundException(String toolName) {
        super("tool not found: " + toolName);
    }
}

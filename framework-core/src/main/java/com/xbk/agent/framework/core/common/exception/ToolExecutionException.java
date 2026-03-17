package com.xbk.agent.framework.core.common.exception;

/**
 * 工具执行异常
 *
 * 职责：封装工具运行过程中的异常
 *
 * @author xiexu
 */
public class ToolExecutionException extends FrameworkCoreException {

    /**
     * 创建异常对象
     *
     * @param toolName 工具名称
     * @param cause 异常原因
     */
    public ToolExecutionException(String toolName, Throwable cause) {
        super("tool execution failed: " + toolName, cause);
    }
}

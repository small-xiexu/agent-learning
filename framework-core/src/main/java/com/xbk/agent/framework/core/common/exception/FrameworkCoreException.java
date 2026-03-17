package com.xbk.agent.framework.core.common.exception;

/**
 * 框架核心异常
 *
 * 职责：定义 framework-core 统一异常基类
 *
 * @author xiexu
 */
public class FrameworkCoreException extends RuntimeException {

    /**
     * 创建异常对象
     *
     * @param message 异常消息
     */
    public FrameworkCoreException(String message) {
        super(message);
    }

    /**
     * 创建异常对象
     *
     * @param message 异常消息
     * @param cause 异常原因
     */
    public FrameworkCoreException(String message, Throwable cause) {
        super(message, cause);
    }
}

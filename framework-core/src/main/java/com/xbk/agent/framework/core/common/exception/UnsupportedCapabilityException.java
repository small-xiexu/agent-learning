package com.xbk.agent.framework.core.common.exception;

/**
 * 不支持能力异常
 *
 * 职责：表示当前 LLM 客户端未实现指定能力
 *
 * @author xiexu
 */
public class UnsupportedCapabilityException extends FrameworkCoreException {

    /**
     * 创建异常对象
     *
     * @param capability 能力名称
     */
    public UnsupportedCapabilityException(String capability) {
        super("unsupported capability: " + capability);
    }
}

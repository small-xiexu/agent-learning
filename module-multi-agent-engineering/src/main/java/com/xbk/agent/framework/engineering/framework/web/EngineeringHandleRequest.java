package com.xbk.agent.framework.engineering.framework.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 接待员 HTTP 入口请求体。
 *
 * 职责：承载 `/engineering/handle` 接口收到的用户原始问题文本，
 * 作为 HTTP 层到接待员服务之间的最小数据传输对象。
 *
 * @author xiexu
 */
public final class EngineeringHandleRequest {

    /** 用户原始问题文本。 */
    @NotBlank(message = "request 不能为空")
    private String request;

    /**
     * 创建空请求体，供 Jackson 反序列化使用。
     */
    public EngineeringHandleRequest() {
    }

    /**
     * 读取用户原始问题文本。
     *
     * @return 用户原始问题文本
     */
    public String getRequest() {
        return request;
    }

    /**
     * 设置用户原始问题文本。
     *
     * @param request 用户原始问题文本
     */
    public void setRequest(String request) {
        this.request = request;
    }
}

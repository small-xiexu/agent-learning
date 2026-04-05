package com.xbk.agent.framework.engineering.domain.ticket;

/**
 * 用户客服请求。
 *
 * 职责：承载用户原始诉求文本，作为外部入口投递给 Receptionist 的业务 payload。
 *
 * @author xiexu
 */
public final class CustomerServiceRequest {

    /** 用户原始诉求文本。是整条链的输入来源，由 Coordinator 封装后投递给 Receptionist。 */
    private final String requestText;

    /**
     * 创建用户请求。
     *
     * @param requestText 原始诉求
     */
    public CustomerServiceRequest(String requestText) {
        this.requestText = requestText;
    }

    public String getRequestText() {
        return requestText;
    }
}

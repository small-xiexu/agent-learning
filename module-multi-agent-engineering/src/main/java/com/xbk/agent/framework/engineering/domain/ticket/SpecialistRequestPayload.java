package com.xbk.agent.framework.engineering.domain.ticket;

import com.xbk.agent.framework.engineering.domain.routing.CustomerIntentType;

/**
 * 专家请求载荷。
 *
 * 职责：作为 Receptionist 发给专业客服的标准业务 payload，包含原始请求和接待员分析结论。
 *
 * @author xiexu
 */
public final class SpecialistRequestPayload {

    /** 用户原始请求文本，直接透传自 CustomerServiceRequest，作为专家回答的原始输入。 */
    private final String originalRequest;

    /** Receptionist 对用户诉求的分析说明（即路由理由），让专家了解前置判断上下文。 */
    private final String receptionistAnalysis;

    /** 已识别的意图类型，让专家明确当前请求的业务类别，可用于 prompt 中的条件引导。 */
    private final CustomerIntentType intentType;

    /**
     * 创建专家请求载荷。
     *
     * @param originalRequest 原始请求
     * @param receptionistAnalysis 接待员分析
     * @param intentType 意图类型
     */
    public SpecialistRequestPayload(String originalRequest, String receptionistAnalysis, CustomerIntentType intentType) {
        this.originalRequest = originalRequest;
        this.receptionistAnalysis = receptionistAnalysis;
        this.intentType = intentType;
    }

    public String getOriginalRequest() {
        return originalRequest;
    }

    public String getReceptionistAnalysis() {
        return receptionistAnalysis;
    }

    public CustomerIntentType getIntentType() {
        return intentType;
    }
}

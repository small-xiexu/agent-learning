package com.xbk.agent.framework.engineering.domain.ticket;

import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;

/**
 * 专家响应载荷。
 *
 * 职责：封装专业客服处理后的最终文本结果，并明确它来自哪一类专家。
 *
 * @author xiexu
 */
public final class SpecialistResponsePayload {

    private final SpecialistType specialistType;
    private final String resolvedText;

    /**
     * 创建专家响应载荷。
     *
     * @param specialistType 专家类型
     * @param resolvedText 处理结果
     */
    public SpecialistResponsePayload(SpecialistType specialistType, String resolvedText) {
        this.specialistType = specialistType;
        this.resolvedText = resolvedText;
    }

    public SpecialistType getSpecialistType() {
        return specialistType;
    }

    public String getResolvedText() {
        return resolvedText;
    }
}

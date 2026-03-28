package com.xbk.agent.framework.engineering.handwritten.support;

import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.support.EngineeringPromptTemplates;

/**
 * 手写版 Agent 提示词支持。
 *
 * 职责：为手写版三类 Agent 提供可复用提示词和最终客服转述模板。
 *
 * @author xiexu
 */
public final class HandwrittenAgentPromptTemplates {

    private HandwrittenAgentPromptTemplates() {
    }

    /**
     * 返回技术专家提示词。
     *
     * @return 技术专家提示词
     */
    public static String techSupportSystemPrompt() {
        return EngineeringPromptTemplates.techSupportSystemPrompt();
    }

    /**
     * 返回销售专家提示词。
     *
     * @return 销售专家提示词
     */
    public static String salesSystemPrompt() {
        return EngineeringPromptTemplates.salesConsultingSystemPrompt();
    }

    /**
     * 构造接待员最终转述。
     *
     * @param specialistType 专家类型
     * @param specialistResponse 专家响应
     * @return 接待员最终话术
     */
    public static String receptionistSummary(SpecialistType specialistType, String specialistResponse) {
        if (specialistType == SpecialistType.SALES) {
            return "接待员结论：销售顾问已处理你的诉求。答复如下：" + specialistResponse;
        }
        return "接待员结论：技术专家已处理你的诉求。答复如下：" + specialistResponse;
    }
}

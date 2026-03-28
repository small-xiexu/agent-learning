package com.xbk.agent.framework.engineering.support;

/**
 * 工程模块提示词模板。
 *
 * 职责：统一维护接待员和专业客服使用的核心系统提示词，避免各类 Agent 自己散落拼接。
 *
 * @author xiexu
 */
public final class EngineeringPromptTemplates {

    private EngineeringPromptTemplates() {
    }

    /**
     * 返回意图分类提示词。
     *
     * @return 接待员意图分类提示词
     */
    public static String intentClassifierSystemPrompt() {
        return """
                你是一位企业智能客服系统的接待员。
                你的任务只有一个：判断当前用户诉求属于哪一类。
                你只能输出以下三个枚举值之一：
                TECH_SUPPORT / SALES_CONSULTING / UNKNOWN
                不要输出解释，不要输出额外文字。
                """;
    }

    /**
     * 返回技术专家提示词。
     *
     * @return 技术专家提示词
     */
    public static String techSupportSystemPrompt() {
        return """
                你是一位资深技术支持专家。
                请聚焦错误现象、排查方向和修复建议，给出简洁且可执行的技术答复。
                """;
    }

    /**
     * 返回销售专家提示词。
     *
     * @return 销售专家提示词
     */
    public static String salesConsultingSystemPrompt() {
        return """
                你是一位资深销售顾问。
                请围绕报价、购买方案、部署选项和服务支持范围给出简洁清晰的商务答复。
                """;
    }
}

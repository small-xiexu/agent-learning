package com.xbk.agent.framework.engineering.domain.routing;

/**
 * 客服意图类型。
 * <p>
 * 职责：描述接待员识别出的用户诉求大类。
 *
 * @author xiexu
 */
public enum CustomerIntentType {

    /**
     * 技术支持诉求。用户遇到服务异常、报错、功能故障等技术类问题，路由到技术专家。
     */
    TECH_SUPPORT,

    /**
     * 销售咨询诉求。用户询问产品方案、报价、采购流程等商务类问题，路由到销售顾问。
     */
    SALES_CONSULTING,

    /**
     * 意图无法识别。LLM 分析后无法归入已知类别，通常需要人工介入或兜底处理。
     */
    UNKNOWN
}

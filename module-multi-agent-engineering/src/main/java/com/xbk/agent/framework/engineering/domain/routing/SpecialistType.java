package com.xbk.agent.framework.engineering.domain.routing;

/**
 * 专家类型。
 *
 * 职责：标识当前请求最终应该转交给哪一类专业客服。
 *
 * @author xiexu
 */
public enum SpecialistType {

    /** 技术支持专家。负责处理服务异常、报错排查、技术方案咨询等技术类请求。 */
    TECH_SUPPORT,

    /** 销售顾问。负责处理产品报价、方案选型、采购合同等商务类请求。 */
    SALES
}

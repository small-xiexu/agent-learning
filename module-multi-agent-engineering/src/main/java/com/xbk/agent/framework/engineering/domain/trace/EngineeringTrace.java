package com.xbk.agent.framework.engineering.domain.trace;

import java.util.List;

/**
 * 工程轨迹。
 *
 * 职责：聚合某次客服处理链路下的全部消息投递记录，便于演示和对照测试。
 *
 * @author xiexu
 */
public final class EngineeringTrace {

    private final List<DeliveryRecord> deliveryRecords;

    /**
     * 创建工程轨迹。
     *
     * @param deliveryRecords 投递记录列表
     */
    public EngineeringTrace(List<DeliveryRecord> deliveryRecords) {
        this.deliveryRecords = deliveryRecords == null ? List.<DeliveryRecord>of() : List.copyOf(deliveryRecords);
    }

    public List<DeliveryRecord> getDeliveryRecords() {
        return deliveryRecords;
    }
}

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

    /**
     * 本次客服链路的全部投递记录（不可变快照）。
     *
     * 每条记录对应一个 publish / deliver / consume 事件，三条记录合为一条消息的完整投递轨迹。
     * 整个列表按时间顺序排列，可直接用于教学演示中的消息流转可视化。
     */
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

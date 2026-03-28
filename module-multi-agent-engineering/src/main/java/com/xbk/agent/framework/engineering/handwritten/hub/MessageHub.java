package com.xbk.agent.framework.engineering.handwritten.hub;

import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.trace.DeliveryRecord;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenMessageAgent;

import java.util.List;

/**
 * 中心消息代理接口。
 *
 * 职责：抽象主题订阅、消息发布与轨迹查询，让手写版能够在内存实现与 MQ 实现之间切换而不改 Agent 协议。
 *
 * @author xiexu
 */
public interface MessageHub extends AutoCloseable {

    /**
     * 订阅主题。
     *
     * @param topic 主题
     * @param agent 订阅方 Agent
     */
    void subscribe(String topic, HandwrittenMessageAgent agent);

    /**
     * 发布消息。
     *
     * @param message 工程消息
     */
    void publish(EngineeringMessage message);

    /**
     * 返回当前审计记录快照。
     *
     * @return 审计记录列表
     */
    List<DeliveryRecord> snapshotAuditRecords();

    /**
     * 关闭消息中心。
     */
    @Override
    void close();
}

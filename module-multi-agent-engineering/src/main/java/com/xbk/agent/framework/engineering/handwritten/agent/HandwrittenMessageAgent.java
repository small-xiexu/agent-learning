package com.xbk.agent.framework.engineering.handwritten.agent;

import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;

/**
 * 手写版消息 Agent 协议。
 *
 * 职责：统一定义所有手写版 Agent 的接收与发送接口，体现消息驱动协作而不是方法直调。
 *
 * @author xiexu
 */
public interface HandwrittenMessageAgent {

    /**
     * 返回 Agent 名称。
     *
     * @return Agent 名称
     */
    String getAgentName();

    /**
     * 接收消息。
     *
     * @param message 工程消息
     */
    void receive(EngineeringMessage message);

    /**
     * 发送消息。
     *
     * @param message 工程消息
     */
    void send(EngineeringMessage message);
}

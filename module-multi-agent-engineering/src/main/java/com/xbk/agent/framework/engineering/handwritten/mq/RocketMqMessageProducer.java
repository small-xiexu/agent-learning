package com.xbk.agent.framework.engineering.handwritten.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

/**
 * RocketMQ 消息生产者。
 *
 * 职责：把 EngineeringMessage 序列化为 JSON 字符串，并通过 RocketMQTemplate 投递到对应的 MQ 目的地。
 * Agent 代码不需要感知 RocketMQ 的 destination 规则，只需传入逻辑主题即可。
 *
 * @author xiexu
 */
public class RocketMqMessageProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建消息生产者。
     *
     * @param rocketMQTemplate RocketMQ 模板
     */
    public RocketMqMessageProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 发送工程消息到 RocketMQ。
     *
     * <p>消息序列化为 JSON 字符串，目的地由逻辑主题映射得到，例如：
     * "customer.request" → "ENGINEERING_MSG:CUSTOMER_REQUEST"。
     *
     * @param message 工程消息
     * @throws RuntimeException 序列化失败时抛出
     */
    public void send(EngineeringMessage message) {
        String destination = RocketMqTopicBindingSupport.toRocketMqDestination(message.getTopic());
        String body = serialize(message);
        rocketMQTemplate.convertAndSend(destination, body);
    }

    /**
     * 序列化工程消息。
     *
     * @param message 工程消息
     * @return JSON 字符串
     */
    private String serialize(EngineeringMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        }
        catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize EngineeringMessage: " + message.getMessageId(), ex);
        }
    }
}

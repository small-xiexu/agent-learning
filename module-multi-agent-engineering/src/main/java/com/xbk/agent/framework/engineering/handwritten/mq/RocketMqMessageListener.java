package com.xbk.agent.framework.engineering.handwritten.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RocketMQ 消息监听器抽象基类。
 *
 * 职责：把 RocketMQ 原始字符串消息反序列化为 EngineeringMessage，并交给子类或回调处理。
 *
 * <p>使用方式：继承本类并添加 @RocketMQMessageListener 注解，指定 topic 和消费者组。
 * 在 onEngineeringMessage 中调用 MqBackedMessageHub 的 receiveFromMq 方法即可完成闭环。
 *
 * <p>示例：
 * <pre>
 *   {@literal @}Component
 *   {@literal @}RocketMQMessageListener(
 *       topic = RocketMqTopicBindingSupport.ENGINEERING_TOPIC,
 *       consumerGroup = "engineering-hub-consumer",
 *       selectorExpression = "*"
 *   )
 *   public class EngineeringMessageRocketMqListener extends RocketMqMessageListener {
 *       public EngineeringMessageRocketMqListener(MqBackedMessageHub hub) {
 *           super(hub::receiveFromMq);
 *       }
 *   }
 * </pre>
 *
 * @author xiexu
 */
public abstract class RocketMqMessageListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(RocketMqMessageListener.class);

    private final java.util.function.Consumer<EngineeringMessage> messageConsumer;
    private final ObjectMapper objectMapper;

    /**
     * 创建监听器。
     *
     * @param messageConsumer 接收反序列化后消息的回调，通常指向 MqBackedMessageHub::receiveFromMq
     */
    protected RocketMqMessageListener(java.util.function.Consumer<EngineeringMessage> messageConsumer) {
        this.messageConsumer = messageConsumer;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 接收 RocketMQ 原始字符串消息，反序列化后交给回调处理。
     *
     * @param messageBody RocketMQ 原始消息体（JSON 字符串）
     */
    @Override
    public void onMessage(String messageBody) {
        try {
            EngineeringMessage message = objectMapper.readValue(messageBody, EngineeringMessage.class);
            messageConsumer.accept(message);
        }
        catch (Exception ex) {
            log.error("Failed to process RocketMQ engineering message: {}", messageBody, ex);
        }
    }
}

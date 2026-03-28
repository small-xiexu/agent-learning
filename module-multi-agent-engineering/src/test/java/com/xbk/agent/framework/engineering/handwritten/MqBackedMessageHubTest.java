package com.xbk.agent.framework.engineering.handwritten;

import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.message.MessageType;
import com.xbk.agent.framework.engineering.handwritten.agent.AbstractHandwrittenAgent;
import com.xbk.agent.framework.engineering.handwritten.hub.MqBackedMessageHub;
import com.xbk.agent.framework.engineering.handwritten.mq.RocketMqMessageProducer;
import com.xbk.agent.framework.engineering.handwritten.mq.RocketMqTopicBindingSupport;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * MqBackedMessageHub 契约测试。
 *
 * 职责：验证 MqBackedMessageHub 与 InMemoryMessageHub 对外语义保持同构：
 * 相同的 subscribe/publish 协议，相同的审计记录，相同的异步分发行为。
 *
 * <p>测试策略说明：
 * 本测试不启动真实 RocketMQ Broker，而是：
 * <ol>
 *   <li>Mock RocketMQTemplate，捕获 convertAndSend 调用，验证目的地和 body 格式正确；
 *   <li>直接调用 hub.receiveFromMq(message) 模拟 Broker 回调，绕过实际网络传输；
 *   <li>验证 receiveFromMq 触发的分发与内存版完全一致。
 * </ol>
 * 这套测试方式可以在 CI 中无依赖运行，同时验证了两套实现的"行为同构"约定。
 *
 * @author xiexu
 */
class MqBackedMessageHubTest {

    /**
     * 验证 publish 会向 RocketMQ 发送序列化消息，目的地格式正确。
     */
    @Test
    void shouldSendMessageToRocketMqOnPublish() {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        RocketMqMessageProducer producer = new RocketMqMessageProducer(rocketMQTemplate);
        MqBackedMessageHub hub = new MqBackedMessageHub(producer);

        EngineeringMessage message = EngineeringMessage.builder()
                .messageId("message-1")
                .conversationId("conversation-1")
                .correlationId("correlation-1")
                .fromAgent("receptionist_agent")
                .topic("support.tech.request")
                .messageType(MessageType.SPECIALIST_REQUEST)
                .payload("请排查数据库连接异常")
                .build();

        hub.publish(message);

        // 验证 RocketMQ 发送目的地格式为 "ENGINEERING_MSG:SUPPORT_TECH_REQUEST"
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(rocketMQTemplate).convertAndSend(destinationCaptor.capture(), any(Object.class));
        assertEquals(RocketMqTopicBindingSupport.ENGINEERING_TOPIC + ":SUPPORT_TECH_REQUEST",
                destinationCaptor.getValue());

        // 验证审计日志记录了发布事件
        assertTrue(hub.snapshotAuditRecords().stream()
                .anyMatch(record -> "publish".equals(record.getEventType())));

        hub.close();
    }

    /**
     * 验证 receiveFromMq 触发的订阅者分发与 InMemoryMessageHub 行为同构。
     *
     * @throws Exception 等待异步消息时抛出的异常
     */
    @Test
    void shouldDeliverMessageToSubscribersViaReceiveFromMq() throws Exception {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        MqBackedMessageHub hub = new MqBackedMessageHub(new RocketMqMessageProducer(rocketMQTemplate));
        CountDownLatch latch = new CountDownLatch(2);
        RecordingAgent techAgent = new RecordingAgent("tech_support_agent", hub, latch);
        RecordingAgent backupAgent = new RecordingAgent("backup_support_agent", hub, latch);
        hub.subscribe("support.tech.request", techAgent);
        hub.subscribe("support.tech.request", backupAgent);

        EngineeringMessage message = EngineeringMessage.builder()
                .messageId("message-mq-1")
                .conversationId("conversation-mq-1")
                .correlationId("correlation-mq-1")
                .fromAgent("receptionist_agent")
                .topic("support.tech.request")
                .messageType(MessageType.SPECIALIST_REQUEST)
                .payload("请排查数据库连接异常")
                .build();

        // 模拟 RocketMQ Broker 回调：receiveFromMq 就是 @RocketMQMessageListener 触发的入口
        hub.receiveFromMq(message);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(List.of("请排查数据库连接异常"), techAgent.getReceivedPayloads());
        assertEquals(List.of("请排查数据库连接异常"), backupAgent.getReceivedPayloads());

        // 验证审计日志记录了 deliver 和 consume 事件
        assertTrue(hub.snapshotAuditRecords().stream()
                .anyMatch(record -> "deliver".equals(record.getEventType())));
        assertTrue(hub.snapshotAuditRecords().stream()
                .anyMatch(record -> "consume".equals(record.getEventType())));

        hub.close();
    }

    /**
     * 验证主题绑定正确：customer.request 的订阅者收不到 support.tech.request 消息。
     *
     * @throws Exception 等待异步消息时抛出的异常
     */
    @Test
    void shouldOnlyDeliverToMatchingTopicSubscribers() throws Exception {
        RocketMQTemplate rocketMQTemplate = mock(RocketMQTemplate.class);
        MqBackedMessageHub hub = new MqBackedMessageHub(new RocketMqMessageProducer(rocketMQTemplate));
        CountDownLatch latch = new CountDownLatch(1);
        RecordingAgent salesAgent = new RecordingAgent("sales_agent", hub, latch);
        RecordingAgent wrongAgent = new RecordingAgent("wrong_agent", hub, new CountDownLatch(0));
        hub.subscribe("support.sales.request", salesAgent);
        hub.subscribe("support.tech.request", wrongAgent);

        hub.receiveFromMq(EngineeringMessage.builder()
                .messageId("message-mq-2")
                .conversationId("conversation-mq-2")
                .correlationId("correlation-mq-2")
                .fromAgent("receptionist_agent")
                .topic("support.sales.request")
                .messageType(MessageType.SPECIALIST_REQUEST)
                .payload("请给我企业版报价")
                .build());

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(List.of("请给我企业版报价"), salesAgent.getReceivedPayloads());
        // wrongAgent 订阅的是技术主题，不应该收到销售消息
        assertTrue(wrongAgent.getReceivedPayloads().isEmpty());

        hub.close();
    }

    /**
     * 记录收到消息的测试 Agent。
     *
     * @author xiexu
     */
    private static final class RecordingAgent extends AbstractHandwrittenAgent {

        private final CountDownLatch latch;
        private final List<String> receivedPayloads = new CopyOnWriteArrayList<String>();

        private RecordingAgent(String agentName, MqBackedMessageHub hub, CountDownLatch latch) {
            super(agentName, null, hub);
            this.latch = latch;
        }

        @Override
        public void receive(EngineeringMessage message) {
            receivedPayloads.add(String.valueOf(message.getPayload()));
            latch.countDown();
        }

        private List<String> getReceivedPayloads() {
            return receivedPayloads;
        }
    }
}

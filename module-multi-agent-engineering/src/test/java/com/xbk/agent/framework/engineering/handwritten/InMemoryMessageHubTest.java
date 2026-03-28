package com.xbk.agent.framework.engineering.handwritten;

import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.message.MessageType;
import com.xbk.agent.framework.engineering.handwritten.agent.AbstractHandwrittenAgent;
import com.xbk.agent.framework.engineering.handwritten.hub.InMemoryMessageHub;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InMemoryMessageHub 测试。
 *
 * 职责：钉住手写版中心消息代理的最小行为，包括主题订阅、异步投递和审计轨迹记录。
 *
 * @author xiexu
 */
class InMemoryMessageHubTest {

    /**
     * 验证相同主题的多个订阅者都能收到异步消息。
     *
     * @throws Exception 等待异步消息时抛出的异常
     */
    @Test
    void shouldPublishMessageToAllSubscribersAsynchronously() throws Exception {
        InMemoryMessageHub messageHub = new InMemoryMessageHub();
        CountDownLatch latch = new CountDownLatch(2);
        RecordingAgent techAgent = new RecordingAgent("tech_support_agent", messageHub, latch);
        RecordingAgent backupAgent = new RecordingAgent("backup_support_agent", messageHub, latch);
        messageHub.subscribe("support.tech.request", techAgent);
        messageHub.subscribe("support.tech.request", backupAgent);

        messageHub.publish(EngineeringMessage.builder()
                .messageId("message-1")
                .conversationId("conversation-1")
                .correlationId("correlation-1")
                .fromAgent("receptionist_agent")
                .topic("support.tech.request")
                .messageType(MessageType.SPECIALIST_REQUEST)
                .payload("请排查数据库连接异常")
                .build());

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(List.of("请排查数据库连接异常"), techAgent.getReceivedPayloads());
        assertEquals(List.of("请排查数据库连接异常"), backupAgent.getReceivedPayloads());
        assertTrue(messageHub.snapshotAuditRecords().size() >= 3);
        assertTrue(messageHub.snapshotAuditRecords().stream()
                .anyMatch(record -> "publish".equals(record.getEventType())));
        assertTrue(messageHub.snapshotAuditRecords().stream()
                .anyMatch(record -> "deliver".equals(record.getEventType())));
        messageHub.close();
    }

    /**
     * 验证 MessageHub 会把消息投递给主题订阅者，并记录消费事件。
     *
     * @throws Exception 等待异步消息时抛出的异常
     */
    @Test
    void shouldRecordConsumeTrailForDeliveredMessage() throws Exception {
        InMemoryMessageHub messageHub = new InMemoryMessageHub();
        CountDownLatch latch = new CountDownLatch(1);
        RecordingAgent salesAgent = new RecordingAgent("sales_agent", messageHub, latch);
        messageHub.subscribe("support.sales.request", salesAgent);

        messageHub.publish(EngineeringMessage.builder()
                .messageId("message-2")
                .conversationId("conversation-2")
                .correlationId("correlation-2")
                .fromAgent("receptionist_agent")
                .topic("support.sales.request")
                .messageType(MessageType.SPECIALIST_REQUEST)
                .payload("请给我企业版报价")
                .build());

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(messageHub.snapshotAuditRecords().stream()
                .anyMatch(record -> "consume".equals(record.getEventType())
                        && "sales_agent".equals(record.getToAgent())));
        messageHub.close();
    }

    /**
     * 记录收到消息的测试 Agent。
     *
     * 职责：模拟主题订阅方，并把收到的 payload 存到列表里用于断言。
     *
     * @author xiexu
     */
    private static final class RecordingAgent extends AbstractHandwrittenAgent {

        private final CountDownLatch latch;
        private final List<String> receivedPayloads = new CopyOnWriteArrayList<String>();

        /**
         * 创建测试 Agent。
         *
         * @param agentName Agent 名称
         * @param messageHub 消息中心
         * @param latch 倒计时门闩
         */
        private RecordingAgent(String agentName, InMemoryMessageHub messageHub, CountDownLatch latch) {
            super(agentName, null, messageHub);
            this.latch = latch;
        }

        /**
         * 接收消息并记录 payload。
         *
         * @param message 工程消息
         */
        @Override
        public void receive(EngineeringMessage message) {
            receivedPayloads.add(String.valueOf(message.getPayload()));
            latch.countDown();
        }

        /**
         * 返回收到的 payload 列表。
         *
         * @return payload 列表
         */
        private List<String> getReceivedPayloads() {
            return receivedPayloads;
        }
    }
}

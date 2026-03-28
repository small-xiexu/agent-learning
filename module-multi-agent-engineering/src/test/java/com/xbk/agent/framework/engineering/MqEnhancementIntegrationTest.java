package com.xbk.agent.framework.engineering;

import com.xbk.agent.framework.engineering.domain.routing.CustomerIntentType;
import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;
import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.framework.config.EngineeringMqEnhancementConfig;
import com.xbk.agent.framework.engineering.framework.messaging.AsyncResultCallbackListener;
import com.xbk.agent.framework.engineering.framework.messaging.RoutingAuditEventPublisher;
import com.xbk.agent.framework.engineering.framework.messaging.SpecialistEscalationPublisher;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * MQ 增强层集成测试。
 *
 * 职责：验证 MQ 增强层三个核心组件（路由审计、专家升级、异步回调）的行为正确性。
 *
 * <p>测试策略：
 * 不启动真实 RocketMQ Broker，使用 Mock RocketMQTemplate 验证目的地和消息内容。
 * 这确保 MQ 增强层测试在 CI 中无外部依赖即可运行。
 *
 * <p>核心验证点：
 * <ol>
 *   <li>enabled=true 时，审计/升级消息发布到正确主题；
 *   <li>enabled=false 时，所有发布者都是 no-op，不与 MQ 产生任何交互；
 *   <li>AsyncResultCallbackListener 能正确分发回调事件并捕获异常。
 * </ol>
 *
 * @author xiexu
 */
@SuppressWarnings("unchecked")
class MqEnhancementIntegrationTest {

    // ─── RoutingAuditEventPublisher ─────────────────────────────────────────────

    /**
     * 验证 MQ 启用时，路由审计事件被发布到 audit 主题。
     */
    @Test
    void shouldPublishRoutingAuditEventWhenMqEnabled() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        EngineeringMqEnhancementConfig config = enabledConfig();
        RoutingAuditEventPublisher publisher = new RoutingAuditEventPublisher(template, config);

        RoutingDecision decision = new RoutingDecision(
                CustomerIntentType.TECH_SUPPORT,
                SpecialistType.TECH_SUPPORT,
                "用户询问技术问题",
                "support.tech.request",
                "tech-support-agent");

        publisher.publishRoutingAudit("conv-001", "我的服务报 NullPointerException", decision);

        // 使用 LinkedHashMap captor 避免与 MessagePostProcessor 重载的歧义
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LinkedHashMap> eventCaptor = ArgumentCaptor.forClass(LinkedHashMap.class);
        verify(template).convertAndSend(topicCaptor.capture(), eventCaptor.capture());
        assertEquals("engineering.audit", topicCaptor.getValue());

        Map<String, Object> event = eventCaptor.getValue();
        assertEquals("conv-001", event.get("conversationId"));
        assertEquals("TECH_SUPPORT", event.get("intentType"));
        assertEquals("TECH_SUPPORT", event.get("specialistType"));
        assertEquals("tech-support-agent", event.get("targetAgentName"));
        assertNotNull(event.get("timestamp"));
    }

    /**
     * 验证 MQ 禁用时，路由审计发布者是 no-op，不与 RocketMQ 交互。
     */
    @Test
    void shouldNotPublishRoutingAuditWhenMqDisabled() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        EngineeringMqEnhancementConfig config = disabledConfig();
        RoutingAuditEventPublisher publisher = new RoutingAuditEventPublisher(template, config);

        RoutingDecision decision = new RoutingDecision(
                CustomerIntentType.TECH_SUPPORT,
                SpecialistType.TECH_SUPPORT,
                "any reason",
                "support.tech.request",
                "tech-support-agent");

        publisher.publishRoutingAudit("conv-002", "任何请求", decision);

        // 禁用状态下不应有任何 MQ 交互
        verifyNoInteractions(template);
    }

    /**
     * 验证长用户请求在审计事件中被截断为 200 字符。
     */
    @Test
    void shouldTruncateLongUserRequestInAuditEvent() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        EngineeringMqEnhancementConfig config = enabledConfig();
        RoutingAuditEventPublisher publisher = new RoutingAuditEventPublisher(template, config);

        String longRequest = "A".repeat(300);
        RoutingDecision decision = new RoutingDecision(
                CustomerIntentType.SALES_CONSULTING, SpecialistType.SALES,
                "r", "support.sales.request", "sales-agent");

        publisher.publishRoutingAudit("conv-003", longRequest, decision);

        ArgumentCaptor<LinkedHashMap> eventCaptor = ArgumentCaptor.forClass(LinkedHashMap.class);
        verify(template).convertAndSend(ArgumentCaptor.forClass(String.class).capture(), eventCaptor.capture());
        String truncated = (String) eventCaptor.getValue().get("userRequest");
        // 截断后长度应 ≤ 203（200 字符 + "..."）
        assertTrue(truncated.length() <= 203);
        assertTrue(truncated.endsWith("..."));
    }

    // ─── SpecialistEscalationPublisher ──────────────────────────────────────────

    /**
     * 验证 MQ 启用时，专家升级任务被发布到 escalation 主题，包含必要字段。
     */
    @Test
    void shouldPublishEscalationTaskWhenMqEnabled() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        EngineeringMqEnhancementConfig config = enabledConfig();
        SpecialistEscalationPublisher publisher = new SpecialistEscalationPublisher(template, config);

        publisher.publishEscalation("conv-101", "用户反映系统长期宕机", "tech-support-agent", "A2A 调用超时");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LinkedHashMap> eventCaptor = ArgumentCaptor.forClass(LinkedHashMap.class);
        verify(template).convertAndSend(topicCaptor.capture(), eventCaptor.capture());
        assertEquals("engineering.escalation", topicCaptor.getValue());

        Map<String, Object> event = eventCaptor.getValue();
        assertEquals("conv-101", event.get("conversationId"));
        assertEquals("tech-support-agent", event.get("failedAgent"));
        assertEquals("A2A 调用超时", event.get("reason"));
        assertEquals(0, event.get("retryCount"));
        assertNotNull(event.get("timestamp"));
    }

    /**
     * 验证 MQ 禁用时，升级任务发布者是 no-op。
     */
    @Test
    void shouldNotPublishEscalationWhenMqDisabled() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        EngineeringMqEnhancementConfig config = disabledConfig();
        SpecialistEscalationPublisher publisher = new SpecialistEscalationPublisher(template, config);

        publisher.publishEscalation("conv-102", "任何请求", "tech-support-agent", "超时");

        verifyNoInteractions(template);
    }

    // ─── AsyncResultCallbackListener ────────────────────────────────────────────

    /**
     * 验证 processCallback 正确分发到 onCallback 子类实现。
     */
    @Test
    void shouldDispatchCallbackEventToOnCallback() {
        AtomicReference<Map<String, Object>> received = new AtomicReference<>();
        AsyncResultCallbackListener listener = new AsyncResultCallbackListener() {
            @Override
            protected void onCallback(Map<String, Object> callbackEvent) {
                received.set(callbackEvent);
            }
        };

        Map<String, Object> event = new HashMap<>();
        event.put("conversationId", "conv-201");
        event.put("correlationId", "corr-201");
        event.put("result", "专家处理完成，建议更换连接池配置。");

        listener.processCallback(event);

        assertNotNull(received.get());
        assertEquals("conv-201", received.get().get("conversationId"));
        assertEquals("专家处理完成，建议更换连接池配置。", received.get().get("result"));
    }

    /**
     * 验证 null 回调事件不触发 onCallback，且不抛出异常。
     */
    @Test
    void shouldSkipNullCallbackEventGracefully() {
        AtomicReference<Map<String, Object>> received = new AtomicReference<>();
        AsyncResultCallbackListener listener = new AsyncResultCallbackListener() {
            @Override
            protected void onCallback(Map<String, Object> callbackEvent) {
                received.set(callbackEvent);
            }
        };

        // null 事件不应触发 onCallback，且方法应正常返回
        listener.processCallback(null);

        assertNull(received.get(), "null 事件不应分发到 onCallback");
    }

    /**
     * 验证 onCallback 抛出异常时 processCallback 能捕获并不重新抛出。
     *
     * <p>这是防止 MQ 消费失败导致消息重投的关键守卫。
     */
    @Test
    void shouldCaptureExceptionInOnCallbackToPreventRedelivery() {
        AsyncResultCallbackListener listener = new AsyncResultCallbackListener() {
            @Override
            protected void onCallback(Map<String, Object> callbackEvent) {
                throw new RuntimeException("模拟回调处理异常");
            }
        };

        Map<String, Object> event = new HashMap<>();
        event.put("conversationId", "conv-202");

        // 不应抛出异常：捕获异常是防止 MQ 重投的设计意图
        listener.processCallback(event);
    }

    // ─── 配置工厂 ────────────────────────────────────────────────────────────────

    /**
     * 创建 MQ 启用的测试配置。
     */
    private EngineeringMqEnhancementConfig enabledConfig() {
        EngineeringMqEnhancementConfig config = new EngineeringMqEnhancementConfig();
        config.setEnabled(true);
        config.setNameServer("127.0.0.1:9876");
        EngineeringMqEnhancementConfig.TopicConfig topics = new EngineeringMqEnhancementConfig.TopicConfig();
        topics.setAudit("engineering.audit");
        topics.setEscalation("engineering.escalation");
        topics.setCallback("engineering.callback");
        config.setTopic(topics);
        return config;
    }

    /**
     * 创建 MQ 禁用的测试配置。
     */
    private EngineeringMqEnhancementConfig disabledConfig() {
        EngineeringMqEnhancementConfig config = new EngineeringMqEnhancementConfig();
        config.setEnabled(false);
        return config;
    }
}

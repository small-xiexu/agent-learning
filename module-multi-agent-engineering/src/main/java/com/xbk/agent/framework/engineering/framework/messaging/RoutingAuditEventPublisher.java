package com.xbk.agent.framework.engineering.framework.messaging;

import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;
import com.xbk.agent.framework.engineering.framework.config.EngineeringMqEnhancementConfig;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 路由审计事件发布者。
 *
 * 职责：把框架版每次路由决策异步发布到 MQ 审计主题，供后续消费审计、监控和数据分析。
 *
 * <p>设计边界（重要）：
 * 本类是 MQ 增强层的组成部分，不参与 A2A 主通信链路。
 * 即使 MQ 不可用或发布失败，A2A 调用链路也不应受影响。
 * 这就是配置中 {@code engineering.mq.enabled} 开关存在的原因。
 *
 * <p>与 A2aInvocationTraceSupport 的区别：
 * <ul>
 *   <li>A2aInvocationTraceSupport 记录同步调用日志（单机日志文件）；
 *   <li>RoutingAuditEventPublisher 把审计事件发布到 MQ（可被多个消费者订阅）。
 * </ul>
 *
 * @author xiexu
 */
public class RoutingAuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RoutingAuditEventPublisher.class);

    private final RocketMQTemplate rocketMQTemplate;
    private final EngineeringMqEnhancementConfig mqConfig;

    /**
     * 创建路由审计事件发布者。
     *
     * @param rocketMQTemplate RocketMQ 模板
     * @param mqConfig MQ 增强层配置
     */
    public RoutingAuditEventPublisher(RocketMQTemplate rocketMQTemplate,
                                       EngineeringMqEnhancementConfig mqConfig) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.mqConfig = mqConfig;
    }

    /**
     * 发布路由决策审计事件。
     *
     * <p>如果 MQ 增强层未启用（engineering.mq.enabled=false），本方法是 no-op。
     * 这确保了 A2A 主链路在没有 RocketMQ 的环境下也能正常运行。
     *
     * @param conversationId 会话标识
     * @param userRequest 原始用户请求（用于审计记录）
     * @param decision 路由决策
     */
    public void publishRoutingAudit(String conversationId,
                                     String userRequest,
                                     RoutingDecision decision) {
        if (!mqConfig.isEnabled()) {
            return;
        }
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("conversationId", conversationId);
        event.put("userRequest", truncate(userRequest, 200));
        event.put("intentType", decision.getIntentType().name());
        event.put("specialistType", decision.getSpecialistType().name());
        event.put("targetAgentName", decision.getTargetAgentName());
        event.put("reason", decision.getReason());
        event.put("timestamp", Instant.now().toString());
        try {
            rocketMQTemplate.convertAndSend(mqConfig.getTopic().getAudit(), event);
            log.debug("[MQ-AUDIT] Published routing audit event for conversation={}", conversationId);
        }
        catch (Exception ex) {
            // MQ 审计失败不影响主链路，降级为日志
            log.warn("[MQ-AUDIT] Failed to publish routing audit event for conversation={}: {}",
                    conversationId, ex.getMessage());
        }
    }

    /**
     * 截断字符串用于审计记录。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}

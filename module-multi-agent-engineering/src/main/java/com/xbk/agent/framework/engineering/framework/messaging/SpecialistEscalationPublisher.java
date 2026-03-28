package com.xbk.agent.framework.engineering.framework.messaging;

import com.xbk.agent.framework.engineering.framework.config.EngineeringMqEnhancementConfig;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 专家升级任务发布者。
 *
 * 职责：当 A2A 调用超时、专家无法立即处理或问题复杂度超出当前专家范围时，
 * 把升级任务投递到 MQ escalation 主题，供后台异步处理。
 *
 * <p>escalation 场景的典型案例：
 * <ul>
 *   <li>一线技术专家无法解决，需要转给资深架构师；
 *   <li>销售顾问询价超时，需要转给大客户团队；
 *   <li>问题需要多个专家协同（暂不在第一版实现，但接口预留）。
 * </ul>
 *
 * <p>这是 MQ 在多智能体工程中"异步治理"角色的体现：
 * 不是替代 A2A 同步调用，而是在同步链路之外提供补偿和升级能力。
 *
 * @author xiexu
 */
public class SpecialistEscalationPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpecialistEscalationPublisher.class);

    private final RocketMQTemplate rocketMQTemplate;
    private final EngineeringMqEnhancementConfig mqConfig;

    /**
     * 创建升级任务发布者。
     *
     * @param rocketMQTemplate RocketMQ 模板
     * @param mqConfig MQ 增强层配置
     */
    public SpecialistEscalationPublisher(RocketMQTemplate rocketMQTemplate,
                                          EngineeringMqEnhancementConfig mqConfig) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.mqConfig = mqConfig;
    }

    /**
     * 发布专家升级任务。
     *
     * <p>如果 MQ 增强层未启用，本方法是 no-op。
     *
     * @param conversationId 会话标识
     * @param userRequest 原始用户请求
     * @param targetAgentName 当前专家名称（已失败或超时）
     * @param reason 升级原因
     */
    public void publishEscalation(String conversationId,
                                   String userRequest,
                                   String targetAgentName,
                                   String reason) {
        if (!mqConfig.isEnabled()) {
            return;
        }
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("conversationId", conversationId);
        event.put("userRequest", truncate(userRequest, 200));
        event.put("failedAgent", targetAgentName);
        event.put("reason", reason);
        event.put("timestamp", Instant.now().toString());
        event.put("retryCount", 0);
        try {
            rocketMQTemplate.convertAndSend(mqConfig.getTopic().getEscalation(), event);
            log.info("[MQ-ESCALATION] Published escalation task for conversation={}, agent={}",
                    conversationId, targetAgentName);
        }
        catch (Exception ex) {
            log.warn("[MQ-ESCALATION] Failed to publish escalation for conversation={}: {}",
                    conversationId, ex.getMessage());
        }
    }

    /**
     * 截断字符串。
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}

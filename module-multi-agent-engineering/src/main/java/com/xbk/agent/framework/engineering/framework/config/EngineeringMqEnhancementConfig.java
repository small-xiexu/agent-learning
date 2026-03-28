package com.xbk.agent.framework.engineering.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQ 增强层公共配置。
 *
 * 职责：收敛框架版 MQ 增强层（审计事件、升级任务、异步回调）的配置项。
 * MQ 增强层与 A2A 主通信链路是解耦的：A2A 不可用时 MQ 增强层仍可独立控制开关。
 *
 * <p>配置示例（application-engineering-mq-local.yml）：
 * <pre>
 *   engineering:
 *     mq:
 *       enabled: true
 *       name-server: 127.0.0.1:9876
 *       topic:
 *         audit: engineering.audit
 *         escalation: engineering.escalation
 *         callback: engineering.callback
 * </pre>
 *
 * @author xiexu
 */
@ConfigurationProperties(prefix = "engineering.mq")
public class EngineeringMqEnhancementConfig {

    /**
     * 是否启用 MQ 增强层。
     *
     * <p>设置为 false 时，RoutingAuditEventPublisher / SpecialistEscalationPublisher 等组件
     * 不发布任何消息，A2A 主链路不受影响。这保证了 MQ 增强层是"增强"而非"替代"。
     */
    private boolean enabled = false;

    /**
     * RocketMQ NameServer 地址。
     */
    private String nameServer = "127.0.0.1:9876";

    /**
     * MQ 主题配置。
     */
    private TopicConfig topic = new TopicConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public TopicConfig getTopic() {
        return topic;
    }

    public void setTopic(TopicConfig topic) {
        this.topic = topic;
    }

    /**
     * MQ 主题命名配置。
     *
     * <p>三条主题对应 MQ 增强层的三类职责：
     * <ul>
     *   <li>audit：记录每次路由决策的审计事件，供运营/排查使用；
     *   <li>escalation：长耗时问题或超时后的升级任务投递；
     *   <li>callback：专家处理完成后异步通知 Receptionist 的回调主题。
     * </ul>
     */
    public static class TopicConfig {

        /** 路由审计事件主题 */
        private String audit = "engineering.audit";

        /** 升级任务投递主题 */
        private String escalation = "engineering.escalation";

        /** 异步结果回调主题 */
        private String callback = "engineering.callback";

        public String getAudit() {
            return audit;
        }

        public void setAudit(String audit) {
            this.audit = audit;
        }

        public String getEscalation() {
            return escalation;
        }

        public void setEscalation(String escalation) {
            this.escalation = escalation;
        }

        public String getCallback() {
            return callback;
        }

        public void setCallback(String callback) {
            this.callback = callback;
        }
    }
}

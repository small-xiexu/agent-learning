package com.xbk.agent.framework.engineering.framework.config;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.engineering.application.routing.CustomerIntentClassifier;
import com.xbk.agent.framework.engineering.framework.agent.FrameworkReceptionistService;
import com.xbk.agent.framework.engineering.framework.client.SalesRemoteAgentFacade;
import com.xbk.agent.framework.engineering.framework.client.SpecialistRemoteAgentLocator;
import com.xbk.agent.framework.engineering.framework.client.TechSupportRemoteAgentFacade;
import com.xbk.agent.framework.engineering.framework.messaging.RoutingAuditEventPublisher;
import com.xbk.agent.framework.engineering.framework.messaging.SpecialistEscalationPublisher;
import com.xbk.agent.framework.engineering.framework.support.A2aInvocationTraceSupport;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 接待员 Consumer 侧 A2A 客户端配置。
 *
 * 职责：把 Nacos DiscoveryClient、A2A Facade、意图分类器和接待员服务组装起来，
 * 形成完整的 Consumer 侧 Bean 图。
 *
 * <p>Consumer 侧 Bean 依赖图（教学说明）：
 * <pre>
 *   FrameworkReceptionistService
 *     ├── CustomerIntentClassifier → AgentLlmGateway
 *     ├── TechSupportRemoteAgentFacade
 *     │    ├── SpecialistRemoteAgentLocator → DiscoveryClient (Nacos)
 *     │    ├── A2aNacosCommonConfig
 *     │    └── A2aInvocationTraceSupport
 *     └── SalesRemoteAgentFacade（同上）
 * </pre>
 *
 * @author xiexu
 */
@Profile("a2a-receptionist-consumer")
@Configuration
@EnableConfigurationProperties({A2aNacosCommonConfig.class, EngineeringMqEnhancementConfig.class})
public class ReceptionistA2aClientConfig {

    /**
     * A2A 调用追踪支持。
     *
     * @return 追踪支持实例
     */
    @Bean
    public A2aInvocationTraceSupport a2aInvocationTraceSupport() {
        return new A2aInvocationTraceSupport();
    }

    /**
     * 专家 Agent 远端定位器。
     *
     * <p>DiscoveryClient 由 Spring Cloud Nacos 自动注入。
     * 它维护着 Nacos 返回的服务实例列表，Consumer 通过它把逻辑 Agent 名转成 URL。
     *
     * @param discoveryClient Nacos 服务发现客户端
     * @return 定位器
     */
    @Bean
    public SpecialistRemoteAgentLocator specialistRemoteAgentLocator(DiscoveryClient discoveryClient) {
        return new SpecialistRemoteAgentLocator(discoveryClient);
    }

    /**
     * 技术专家远端 Facade。
     *
     * @param locator 定位器
     * @param config A2A 配置
     * @param traceSupport 追踪支持
     * @return 技术专家 Facade
     */
    @Bean
    public TechSupportRemoteAgentFacade techSupportRemoteAgentFacade(
            SpecialistRemoteAgentLocator locator,
            A2aNacosCommonConfig config,
            A2aInvocationTraceSupport traceSupport) {
        return new TechSupportRemoteAgentFacade(locator, config, traceSupport);
    }

    /**
     * 销售顾问远端 Facade。
     *
     * @param locator 定位器
     * @param config A2A 配置
     * @param traceSupport 追踪支持
     * @return 销售顾问 Facade
     */
    @Bean
    public SalesRemoteAgentFacade salesRemoteAgentFacade(
            SpecialistRemoteAgentLocator locator,
            A2aNacosCommonConfig config,
            A2aInvocationTraceSupport traceSupport) {
        return new SalesRemoteAgentFacade(locator, config, traceSupport);
    }

    /**
     * 意图分类器（Consumer 侧本地运行，不通过 A2A）。
     *
     * <p>意图分类不是"交给专家 Agent"的任务，而是接待员自己的判断能力。
     * 因此它在 Consumer 侧本地调用 AgentLlmGateway，不走 A2A 链路。
     *
     * @param agentLlmGateway 统一网关
     * @return 意图分类器
     */
    @Bean
    public CustomerIntentClassifier customerIntentClassifier(AgentLlmGateway agentLlmGateway) {
        return new CustomerIntentClassifier(agentLlmGateway);
    }

    /**
     * 路由审计事件发布者。
     *
     * <p>RocketMQTemplate 不存在时仍创建 Bean，内部自动降级为 no-op，
     * 这样本地默认环境无需 RocketMQ 也能正常启动。
     *
     * @param rocketMQTemplateProvider RocketMQ 模板提供者
     * @param mqConfig MQ 增强层配置
     * @return 路由审计发布者
     */
    @Bean
    public RoutingAuditEventPublisher routingAuditEventPublisher(
            ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider,
            EngineeringMqEnhancementConfig mqConfig) {
        return new RoutingAuditEventPublisher(rocketMQTemplateProvider.getIfAvailable(), mqConfig);
    }

    /**
     * 专家升级任务发布者。
     *
     * @param rocketMQTemplateProvider RocketMQ 模板提供者
     * @param mqConfig MQ 增强层配置
     * @return 升级任务发布者
     */
    @Bean
    public SpecialistEscalationPublisher specialistEscalationPublisher(
            ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider,
            EngineeringMqEnhancementConfig mqConfig) {
        return new SpecialistEscalationPublisher(rocketMQTemplateProvider.getIfAvailable(), mqConfig);
    }

    /**
     * 框架版接待员服务（Consumer 侧核心）。
     *
     * @param intentClassifier 意图分类器
     * @param techFacade 技术专家 Facade
     * @param salesFacade 销售顾问 Facade
     * @param auditPublisher 路由审计发布者
     * @param escalationPublisher 专家升级发布者
     * @return 接待员服务
     */
    @Bean
    public FrameworkReceptionistService frameworkReceptionistService(
            CustomerIntentClassifier intentClassifier,
            TechSupportRemoteAgentFacade techFacade,
            SalesRemoteAgentFacade salesFacade,
            RoutingAuditEventPublisher auditPublisher,
            SpecialistEscalationPublisher escalationPublisher) {
        return new FrameworkReceptionistService(
                intentClassifier,
                techFacade,
                salesFacade,
                auditPublisher,
                escalationPublisher);
    }
}

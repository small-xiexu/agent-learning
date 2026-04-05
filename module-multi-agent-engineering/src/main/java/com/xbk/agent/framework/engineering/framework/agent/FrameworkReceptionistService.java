package com.xbk.agent.framework.engineering.framework.agent;

import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.application.routing.CustomerIntentClassifier;
import com.xbk.agent.framework.engineering.domain.routing.CustomerIntentType;
import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;
import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.framework.client.SalesRemoteAgentFacade;
import com.xbk.agent.framework.engineering.framework.client.TechSupportRemoteAgentFacade;
import com.xbk.agent.framework.engineering.framework.messaging.RoutingAuditEventPublisher;
import com.xbk.agent.framework.engineering.framework.messaging.SpecialistEscalationPublisher;
import com.xbk.agent.framework.engineering.handwritten.support.HandwrittenAgentPromptTemplates;

import java.util.List;
import java.util.UUID;

/**
 * 框架版接待员服务。
 * <p>
 * 职责：本地分类意图，然后调用对应的专家 Facade 发起 A2A 远端请求，最终组装结果。
 *
 * <p>与手写版 HandwrittenReceptionistAgent 的对照：
 * <ul>
 *   <li>手写版：通过 MessageHub.publish() 把消息投给内存/MQ，等异步回包；
 *   <li>框架版：通过 A2A Facade 同步发起 HTTP 请求，直接拿到专家答复。
 * </ul>
 * 两者的意图分类逻辑完全相同（都用 CustomerIntentClassifier），差异只在"通信机制"层。
 *
 * @author xiexu
 */
public class FrameworkReceptionistService {

    /**
     * 意图分类器。手写版与框架版共用同一实现，是两套方案中唯一完全相同的组件。
     */
    private final CustomerIntentClassifier intentClassifier;

    /**
     * 技术专家远端 Facade。
     * 封装对 TechSupportProviderApplication（端口 8081）的 A2A HTTP 调用，
     * 调用方无需感知网络细节，行为与手写版调用本地 TechSupportAgent 一致。
     */
    private final TechSupportRemoteAgentFacade techSupportFacade;

    /**
     * 销售顾问远端 Facade。
     * 封装对 SalesProviderApplication（端口 8082）的 A2A HTTP 调用，
     * 调用方无需感知网络细节，行为与手写版调用本地 SalesAgent 一致。
     */
    private final SalesRemoteAgentFacade salesFacade;

    /**
     * 路由审计事件发布者。
     * 在意图分类完成后异步把路由决策发到 MQ 审计主题；未接 MQ 或禁用时自动 no-op。
     */
    private final RoutingAuditEventPublisher routingAuditEventPublisher;

    /**
     * 专家升级任务发布者。
     * 在远端专家调用失败后把升级任务发到 MQ，供后台异步补偿处理。
     */
    private final SpecialistEscalationPublisher specialistEscalationPublisher;

    /**
     * 创建框架版接待员服务。
     *
     * @param intentClassifier  意图分类器
     * @param techSupportFacade 技术专家 Facade
     * @param salesFacade       销售顾问 Facade
     */
    public FrameworkReceptionistService(CustomerIntentClassifier intentClassifier,
                                        TechSupportRemoteAgentFacade techSupportFacade,
                                        SalesRemoteAgentFacade salesFacade) {
        this(intentClassifier, techSupportFacade, salesFacade, null, null);
    }

    /**
     * 创建带 MQ 增强层的框架版接待员服务。
     *
     * @param intentClassifier 意图分类器
     * @param techSupportFacade 技术专家 Facade
     * @param salesFacade 销售顾问 Facade
     * @param routingAuditEventPublisher 路由审计发布者
     * @param specialistEscalationPublisher 专家升级发布者
     */
    public FrameworkReceptionistService(CustomerIntentClassifier intentClassifier,
                                        TechSupportRemoteAgentFacade techSupportFacade,
                                        SalesRemoteAgentFacade salesFacade,
                                        RoutingAuditEventPublisher routingAuditEventPublisher,
                                        SpecialistEscalationPublisher specialistEscalationPublisher) {
        this.intentClassifier = intentClassifier;
        this.techSupportFacade = techSupportFacade;
        this.salesFacade = salesFacade;
        this.routingAuditEventPublisher = routingAuditEventPublisher;
        this.specialistEscalationPublisher = specialistEscalationPublisher;
    }

    /**
     * 处理用户请求，完成意图分类 → 远端专家调用 → 结果组装。
     *
     * @param userRequest 用户输入文本
     * @return 工程运行结果（与手写版结构一致）
     */
    public EngineeringRunResult handle(String userRequest) {
        String conversationId = "framework-conversation-" + UUID.randomUUID();

        // 与手写版相同的意图分类逻辑，两套实现的路由判断能力完全对等。
        RoutingDecision decision = intentClassifier.classify(conversationId, userRequest);
        publishRoutingAudit(conversationId, userRequest, decision);

        // 框架版没有异步消息链，直接同步调用远端专家（HTTP A2A）并等待返回。
        // 手写版此处是 messageHub.publish() + future.get() 的异步等待组合。
        String specialistResponse;
        try {
            specialistResponse = callSpecialist(conversationId, userRequest, decision);
        } catch (RuntimeException ex) {
            publishEscalation(conversationId, userRequest, decision, ex);
            throw ex;
        }

        // 复用手写版的 Prompt 模板，保证两套实现的最终回复风格一致。
        String finalResponse = HandwrittenAgentPromptTemplates.receptionistSummary(
                decision.getSpecialistType(), specialistResponse);

        // routeTrail 简化为两跳（框架版没有消息追踪），保持与手写版结果结构兼容。
        return EngineeringRunResult.builder()
                .conversationId(conversationId)
                .requestText(userRequest)
                .intentType(decision.getIntentType())
                .specialistType(decision.getSpecialistType())
                .routingDecision(decision)
                .specialistResponse(specialistResponse)
                .finalResponse(finalResponse)
                .routeTrail(List.of("framework_receptionist", decision.getTargetAgentName()))
                .build();
    }

    /**
     * 根据路由决策调用对应专家 Facade。
     *
     * @param conversationId 会话标识
     * @param userRequest    用户请求
     * @param decision       路由决策
     * @return 专家答复文本
     */
    private String callSpecialist(String conversationId, String userRequest, RoutingDecision decision) {
        // 根据路由决策选择对应 Facade，对调用方透明：
        // Facade 内部通过 Nacos 查找 Provider 地址并发起 A2A HTTP 请求。
        if (decision.getSpecialistType() == SpecialistType.SALES) {
            return salesFacade.call(conversationId, userRequest);
        }
        // 默认走技术支持（含 UNKNOWN 意图的兜底），与手写版的路由兜底逻辑对齐。
        return techSupportFacade.call(conversationId, userRequest);
    }

    /**
     * 发布路由审计事件。
     *
     * @param conversationId 会话标识
     * @param userRequest 用户请求
     * @param decision 路由决策
     */
    private void publishRoutingAudit(String conversationId, String userRequest, RoutingDecision decision) {
        if (routingAuditEventPublisher == null) {
            return;
        }
        routingAuditEventPublisher.publishRoutingAudit(conversationId, userRequest, decision);
    }

    /**
     * 发布专家升级任务。
     *
     * @param conversationId 会话标识
     * @param userRequest 用户请求
     * @param decision 路由决策
     * @param ex 调用异常
     */
    private void publishEscalation(String conversationId,
                                   String userRequest,
                                   RoutingDecision decision,
                                   RuntimeException ex) {
        if (specialistEscalationPublisher == null) {
            return;
        }
        specialistEscalationPublisher.publishEscalation(
                conversationId,
                userRequest,
                decision.getTargetAgentName(),
                ex.getMessage());
    }
}

package com.xbk.agent.framework.engineering.framework.agent;

import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.application.routing.CustomerIntentClassifier;
import com.xbk.agent.framework.engineering.domain.routing.CustomerIntentType;
import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;
import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.framework.client.SalesRemoteAgentFacade;
import com.xbk.agent.framework.engineering.framework.client.TechSupportRemoteAgentFacade;
import com.xbk.agent.framework.engineering.handwritten.support.HandwrittenAgentPromptTemplates;

import java.util.List;
import java.util.UUID;

/**
 * 框架版接待员服务。
 *
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

    private final CustomerIntentClassifier intentClassifier;
    private final TechSupportRemoteAgentFacade techSupportFacade;
    private final SalesRemoteAgentFacade salesFacade;

    /**
     * 创建框架版接待员服务。
     *
     * @param intentClassifier 意图分类器
     * @param techSupportFacade 技术专家 Facade
     * @param salesFacade 销售顾问 Facade
     */
    public FrameworkReceptionistService(CustomerIntentClassifier intentClassifier,
                                         TechSupportRemoteAgentFacade techSupportFacade,
                                         SalesRemoteAgentFacade salesFacade) {
        this.intentClassifier = intentClassifier;
        this.techSupportFacade = techSupportFacade;
        this.salesFacade = salesFacade;
    }

    /**
     * 处理用户请求，完成意图分类 → 远端专家调用 → 结果组装。
     *
     * @param userRequest 用户输入文本
     * @return 工程运行结果（与手写版结构一致）
     */
    public EngineeringRunResult handle(String userRequest) {
        String conversationId = "framework-conversation-" + UUID.randomUUID();
        RoutingDecision decision = intentClassifier.classify(conversationId, userRequest);
        String specialistResponse = callSpecialist(conversationId, userRequest, decision);
        String finalResponse = HandwrittenAgentPromptTemplates.receptionistSummary(
                decision.getSpecialistType(), specialistResponse);
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
     * @param userRequest 用户请求
     * @param decision 路由决策
     * @return 专家答复文本
     */
    private String callSpecialist(String conversationId, String userRequest, RoutingDecision decision) {
        if (decision.getSpecialistType() == SpecialistType.SALES) {
            return salesFacade.call(conversationId, userRequest);
        }
        return techSupportFacade.call(conversationId, userRequest);
    }
}

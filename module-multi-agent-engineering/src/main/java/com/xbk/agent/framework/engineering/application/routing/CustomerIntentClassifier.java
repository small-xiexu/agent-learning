package com.xbk.agent.framework.engineering.application.routing;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.engineering.domain.message.MessageTopic;
import com.xbk.agent.framework.engineering.domain.routing.CustomerIntentType;
import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;
import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.support.EngineeringPromptTemplates;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 用户意图分类器。
 *
 * 职责：调用统一网关识别用户诉求，并把识别结果映射为统一的 RoutingDecision。
 *
 * @author xiexu
 */
public class CustomerIntentClassifier {

    private final AgentLlmGateway agentLlmGateway;

    /**
     * 创建分类器。
     *
     * @param agentLlmGateway 统一网关
     */
    public CustomerIntentClassifier(AgentLlmGateway agentLlmGateway) {
        this.agentLlmGateway = agentLlmGateway;
    }

    /**
     * 识别用户诉求并返回路由决策。
     *
     * @param conversationId 会话标识
     * @param userRequest 用户诉求
     * @return 路由决策
     */
    public RoutingDecision classify(String conversationId, String userRequest) {
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId("engineering-intent-" + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(List.of(
                        systemMessage(conversationId, EngineeringPromptTemplates.intentClassifierSystemPrompt()),
                        userMessage(conversationId, userRequest)))
                .build());
        String text = response.getOutputMessage() != null && response.getOutputMessage().getContent() != null
                ? response.getOutputMessage().getContent()
                : response.getRawText();
        CustomerIntentType intentType = parseIntent(text, userRequest);
        return toRoutingDecision(intentType);
    }

    /**
     * 把文本解析为意图类型。
     *
     * @param text 模型文本
     * @param fallbackRequest 原始请求
     * @return 意图类型
     */
    private CustomerIntentType parseIntent(String text, String fallbackRequest) {
        String normalized = text == null ? "" : text.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains(CustomerIntentType.TECH_SUPPORT.name())) {
            return CustomerIntentType.TECH_SUPPORT;
        }
        if (normalized.contains(CustomerIntentType.SALES_CONSULTING.name())) {
            return CustomerIntentType.SALES_CONSULTING;
        }
        String request = fallbackRequest == null ? "" : fallbackRequest;
        if (request.contains("报错") || request.contains("异常") || request.contains("错误")
                || request.contains("NullPointerException")) {
            return CustomerIntentType.TECH_SUPPORT;
        }
        if (request.contains("报价") || request.contains("购买") || request.contains("方案")
                || request.contains("部署")) {
            return CustomerIntentType.SALES_CONSULTING;
        }
        return CustomerIntentType.UNKNOWN;
    }

    /**
     * 把意图映射为统一路由决策。
     *
     * @param intentType 意图类型
     * @return 路由决策
     */
    private RoutingDecision toRoutingDecision(CustomerIntentType intentType) {
        if (intentType == CustomerIntentType.SALES_CONSULTING) {
            return new RoutingDecision(
                    intentType,
                    SpecialistType.SALES,
                    "用户诉求聚焦报价、购买或部署方案，应该转给销售顾问。",
                    MessageTopic.SUPPORT_SALES_REQUEST,
                    "sales_agent");
        }
        return new RoutingDecision(
                intentType,
                SpecialistType.TECH_SUPPORT,
                intentType == CustomerIntentType.UNKNOWN
                        ? "未能稳定识别意图，先按保守策略转给技术支持。"
                        : "用户诉求聚焦报错、排查或修复建议，应该转给技术支持。",
                MessageTopic.SUPPORT_TECH_REQUEST,
                "tech_support_agent");
    }

    /**
     * 构造系统消息。
     *
     * @param conversationId 会话标识
     * @param content 文本内容
     * @return 系统消息
     */
    private Message systemMessage(String conversationId, String content) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.SYSTEM)
                .content(content)
                .build();
    }

    /**
     * 构造用户消息。
     *
     * @param conversationId 会话标识
     * @param content 文本内容
     * @return 用户消息
     */
    private Message userMessage(String conversationId, String content) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(content)
                .build();
    }
}

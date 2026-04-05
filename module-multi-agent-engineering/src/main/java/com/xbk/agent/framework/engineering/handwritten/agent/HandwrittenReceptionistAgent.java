package com.xbk.agent.framework.engineering.handwritten.agent;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.application.routing.CustomerIntentClassifier;
import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.message.MessageType;
import com.xbk.agent.framework.engineering.domain.message.MessageTopic;
import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;
import com.xbk.agent.framework.engineering.domain.ticket.CustomerServiceRequest;
import com.xbk.agent.framework.engineering.domain.ticket.SpecialistRequestPayload;
import com.xbk.agent.framework.engineering.domain.ticket.SpecialistResponsePayload;
import com.xbk.agent.framework.engineering.domain.trace.DeliveryRecord;
import com.xbk.agent.framework.engineering.domain.trace.EngineeringTrace;
import com.xbk.agent.framework.engineering.handwritten.hub.MessageHub;
import com.xbk.agent.framework.engineering.handwritten.runtime.ConversationContextStore;
import com.xbk.agent.framework.engineering.handwritten.runtime.PendingResponseRegistry;
import com.xbk.agent.framework.engineering.handwritten.support.HandwrittenAgentPromptTemplates;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 手写版接待员 Agent。
 *
 * <p>前台就是这个类。它是整条链里最忙的角色，要处理两种情况：
 * <ol>
 *   <li>用户进来问问题 → 前台判断要找哪位专家，然后把问题转发出去；
 *   <li>专家把答复发回来 → 前台收到后组装最终回复，然后把结果塞进"信封"，
 *       唤醒在 Coordinator 那边等待的线程。
 * </ol>
 *
 * <p>这两种情况都走同一个入口 {@code receive()}，
 * 靠消息里的 {@code messageType} 字段来区分当前是"用户来的"还是"专家回来的"。
 *
 * @author xiexu
 */
public class HandwrittenReceptionistAgent extends AbstractHandwrittenAgent {

    /**
     * 调用 LLM 判断用户问题属于技术支持还是销售咨询。
     */
    private final CustomerIntentClassifier customerIntentClassifier;

    /**
     * 存放 Coordinator 放好的"信封"（CompletableFuture）。
     * 前台处理完专家回包后，调用 complete() 把结果塞进信封，
     * Coordinator 那边等待的 future.get() 就会立刻拿到结果。
     */
    private final PendingResponseRegistry pendingResponseRegistry;

    /**
     * 跨消息保存本次会话的中间状态。
     * 前台先收到用户请求，之后才收到专家回包——两次消息之间的间隔里，
     * 路由决策、原始请求文本、路由轨迹都存在这里，不会丢。
     */
    private final ConversationContextStore conversationContextStore;

    /**
     * 创建接待员 Agent。
     *
     * @param agentLlmGateway          统一网关
     * @param customerIntentClassifier 意图分类器
     * @param messageHub               消息中心
     * @param pendingResponseRegistry  待回包注册表
     * @param conversationContextStore 会话上下文存储
     */
    public HandwrittenReceptionistAgent(AgentLlmGateway agentLlmGateway,
                                        CustomerIntentClassifier customerIntentClassifier,
                                        MessageHub messageHub,
                                        PendingResponseRegistry pendingResponseRegistry,
                                        ConversationContextStore conversationContextStore) {
        super("receptionist_agent", agentLlmGateway, messageHub);
        this.customerIntentClassifier = customerIntentClassifier;
        this.pendingResponseRegistry = pendingResponseRegistry;
        this.conversationContextStore = conversationContextStore;
    }

    /**
     * 接收消息。
     *
     * <p>前台有两条收件通道，靠消息类型区分：
     * <ul>
     *   <li>{@code CUSTOMER_REQUEST}：用户发过来的，前台判断转给哪位专家；
     *   <li>{@code SPECIALIST_RESPONSE}：专家发回来的，前台负责组装结果、唤醒 Coordinator。
     * </ul>
     *
     * @param message 工程消息
     */
    @Override
    public void receive(EngineeringMessage message) {
        if (message.getMessageType() == MessageType.CUSTOMER_REQUEST) {
            handleCustomerRequest(message);
            return;
        }
        if (message.getMessageType() == MessageType.SPECIALIST_RESPONSE) {
            handleSpecialistResponse(message);
        }
    }

    /**
     * 处理用户请求（第一阶段）。
     *
     * <p>前台收到用户问题后做三件事：
     * <ol>
     *   <li>调用 LLM 判断意图——是技术支持还是销售咨询；
     *   <li>把路由决策、原始请求都存到 contextStore，等专家回包时还要用；
     *   <li>把问题打包发给对应专家，同时在消息里附上"回寄地址"——告诉专家：
     *       "回复请发到 RECEPTIONIST_REPLY 频道，前台在那里等你"。
     * </ol>
     *
     * @param message 用户请求消息
     */
    private void handleCustomerRequest(EngineeringMessage message) {
        String requestText = extractRequestText(message.getPayload());

        // 先把原始请求文本存到上下文。
        // 专家回包时只带了答复，不带原始问题；前台那时还需要原始问题来组装完整结果，
        // 所以必须现在就存好，跨越两次消息调用保留住。
        conversationContextStore.registerConversation(message.getConversationId(), requestText);
        // 路由轨迹第一跳：前台自己。
        conversationContextStore.recordRoute(message.getConversationId(), getAgentName());

        // 调用 LLM 判断：用户这个问题应该转给技术专家还是销售顾问。
        RoutingDecision routingDecision = customerIntentClassifier.classify(message.getConversationId(), requestText);
        conversationContextStore.recordRoutingDecision(message.getConversationId(), routingDecision);
        // 路由轨迹第二跳：命中的专家。
        conversationContextStore.recordRoute(message.getConversationId(), routingDecision.getTargetAgentName());

        SpecialistRequestPayload payload = new SpecialistRequestPayload(
                requestText,
                routingDecision.getReason(),
                routingDecision.getIntentType());

        // replyTo = RECEPTIONIST_REPLY：这是"回寄地址"。
        // 专家处理完问题后会读取这个字段，把答复投到 RECEPTIONIST_REPLY 频道，
        // 前台在那里等着，收到后再做下一步。
        //
        // correlationId 必须原样透传，专家回包时带着同一个 correlationId，
        // 前台才能凭它找到 PendingResponseRegistry 里对应的"信封"，把结果塞进去。
        send(EngineeringMessage.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(message.getConversationId())
                .correlationId(message.getCorrelationId())
                .fromAgent(getAgentName())
                .toAgent(routingDecision.getTargetAgentName())
                .topic(routingDecision.getTargetTopic())
                .messageType(MessageType.SPECIALIST_REQUEST)
                .replyTo(MessageTopic.RECEPTIONIST_REPLY)
                .payload(payload)
                .build());
    }

    /**
     * 处理专家回包（第二阶段）。
     *
     * <p>前台收到专家答复后做两件事：
     * <ol>
     *   <li>从 contextStore 取回第一阶段存好的路由决策和原始请求，加上专家答复，组装完整结果；
     *   <li>用 correlationId 找到"信封"，把结果塞进去——
     *       Coordinator 那边阻塞的 future.get() 立刻解除，整条链到此结束。
     * </ol>
     *
     * @param message 专家回包消息
     */
    private void handleSpecialistResponse(EngineeringMessage message) {
        SpecialistResponsePayload payload = (SpecialistResponsePayload) message.getPayload();

        // 从 contextStore 取回第一阶段存进去的路由决策，里面有意图类型、专家类型等信息。
        RoutingDecision routingDecision = conversationContextStore.getRoutingDecision(message.getConversationId());

        // specialistResponse：专家 LLM 的原始回答，没有加任何包装。
        String specialistResponse = payload.getResolvedText();
        // finalResponse：前台对专家回答做二次整理，加上"由哪位专家回答"等说明，
        // 这才是最终展示给用户看的内容。
        String finalResponse = HandwrittenAgentPromptTemplates.receptionistSummary(
                payload.getSpecialistType(),
                specialistResponse);

        // 取出 MessageHub 记录的所有投递日志，用于教学展示消息流转的全过程。
        List<DeliveryRecord> records = getMessageHub().snapshotAuditRecords().stream()
                .filter(record -> message.getConversationId().equals(findConversationId(record, message)))
                .collect(Collectors.toList());

        // 关键一步：把组装好的结果塞进"信封"。
        // correlationId 就是信封上的快递单号，pendingResponseRegistry 凭它找到对应的信封，
        // 调用 complete() 后，Coordinator 线程里阻塞的 future.get() 立刻拿到结果并返回，
        // 整条消息驱动异步链在这里完全闭合。
        pendingResponseRegistry.complete(message.getCorrelationId(), EngineeringRunResult.builder()
                .conversationId(message.getConversationId())
                .requestText(conversationContextStore.getRequestText(message.getConversationId()))
                .intentType(routingDecision.getIntentType())
                .specialistType(payload.getSpecialistType())
                .routingDecision(routingDecision)
                .specialistResponse(specialistResponse)
                .finalResponse(finalResponse)
                .routeTrail(conversationContextStore.getRouteTrail(message.getConversationId()))
                .trace(new EngineeringTrace(records))
                .build());
    }

    /**
     * 提取原始请求文本。
     *
     * @param payload payload
     * @return 请求文本
     */
    private String extractRequestText(Object payload) {
        if (payload instanceof CustomerServiceRequest) {
            return ((CustomerServiceRequest) payload).getRequestText();
        }
        return String.valueOf(payload);
    }

    /**
     * 读取记录所属会话。
     *
     * @param record  投递记录
     * @param message 当前消息
     * @return 会话标识
     */
    private String findConversationId(DeliveryRecord record, EngineeringMessage message) {
        return record.getCorrelationId() != null && record.getCorrelationId().equals(message.getCorrelationId())
                ? message.getConversationId()
                : message.getConversationId();
    }
}

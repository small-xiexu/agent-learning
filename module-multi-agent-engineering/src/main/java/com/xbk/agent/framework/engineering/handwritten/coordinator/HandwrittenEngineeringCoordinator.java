package com.xbk.agent.framework.engineering.handwritten.coordinator;

import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.domain.message.MessageTopic;
import com.xbk.agent.framework.engineering.domain.message.MessageType;
import com.xbk.agent.framework.engineering.domain.ticket.CustomerServiceRequest;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenReceptionistAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenSalesAgent;
import com.xbk.agent.framework.engineering.handwritten.agent.HandwrittenTechSupportAgent;
import com.xbk.agent.framework.engineering.handwritten.hub.MessageHub;
import com.xbk.agent.framework.engineering.handwritten.runtime.ConversationContextStore;
import com.xbk.agent.framework.engineering.handwritten.runtime.PendingResponseRegistry;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 手写版工程协调器。
 *
 * <p>它对外只暴露一个方法：{@code run(requestText)}，调用方传入问题，直接拿到结果。
 * 看起来是同步的，但内部整条链是消息驱动异步的。
 *
 * <p>完整流程：
 * <pre>
 *   你调用 run("我的服务报NPE")
 *     → Coordinator 把问题发到"客户请求"频道
 *     → 前台（Receptionist）收到，判断是技术问题，转发到"技术支持请求"频道
 *     → 技术专家收到，调用 LLM 生成答复，发到"前台回包"频道
 *     → 前台收到专家答复，组装最终结果
 *     → Coordinator 的等待解除，run() 返回结果
 * </pre>
 *
 * @author xiexu
 */
public class HandwrittenEngineeringCoordinator {

    private final MessageHub messageHub;
    private final PendingResponseRegistry pendingResponseRegistry;
    private final ConversationContextStore conversationContextStore;

    /**
     * 创建协调器并完成主题订阅。
     *
     * @param messageHub 消息中心
     * @param receptionistAgent 接待员
     * @param techSupportAgent 技术专家
     * @param salesAgent 销售顾问
     * @param pendingResponseRegistry 待回包注册表
     * @param conversationContextStore 会话上下文
     */
    public HandwrittenEngineeringCoordinator(MessageHub messageHub,
                                             HandwrittenReceptionistAgent receptionistAgent,
                                             HandwrittenTechSupportAgent techSupportAgent,
                                             HandwrittenSalesAgent salesAgent,
                                             PendingResponseRegistry pendingResponseRegistry,
                                             ConversationContextStore conversationContextStore) {
        this.messageHub = messageHub;
        this.pendingResponseRegistry = pendingResponseRegistry;
        this.conversationContextStore = conversationContextStore;

        // 在这里完成所有"谁监听哪个频道"的绑定，必须在 run() 之前做完，
        // 否则消息发出去了，但还没人订阅，消息就丢了。
        //
        // 订阅关系（谁负责哪个频道）：
        //   "客户请求"频道      → 前台（分析意图 + 转发专家）
        //   "前台回包"频道      → 前台（收专家答复 + 组装最终结果）
        //   "技术支持请求"频道  → 技术专家
        //   "销售咨询请求"频道  → 销售顾问
        this.messageHub.subscribe(MessageTopic.CUSTOMER_REQUEST, receptionistAgent);
        this.messageHub.subscribe(MessageTopic.RECEPTIONIST_REPLY, receptionistAgent);
        this.messageHub.subscribe(MessageTopic.SUPPORT_TECH_REQUEST, techSupportAgent);
        this.messageHub.subscribe(MessageTopic.SUPPORT_SALES_REQUEST, salesAgent);
    }

    /**
     * 同步运行客服路由场景。
     *
     * @param requestText 用户请求
     * @return 运行结果
     */
    public EngineeringRunResult run(String requestText) {
        // conversationId：这次对话的唯一编号，整条链用它识别"这是同一次对话"。
        // correlationId：这次请求的快递单号，每条消息都带着它，
        //   最后 Receptionist 用它找到信封，把结果塞回来。
        String conversationId = "engineering-conversation-" + UUID.randomUUID();
        String correlationId = "engineering-correlation-" + UUID.randomUUID();

        // 先放好信封（Future），再发消息，顺序不能反。
        conversationContextStore.registerConversation(conversationId, requestText);
        CompletableFuture<EngineeringRunResult> future = pendingResponseRegistry.register(correlationId);

        // 把问题发到"客户请求"频道，前台会收到并开始处理。
        // 发完之后这个线程不做任何事，下面的 future.get() 会让它阻塞等待。
        messageHub.publish(EngineeringMessage.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .correlationId(correlationId)
                .fromAgent("user")
                .toAgent("receptionist_agent")
                .topic(MessageTopic.CUSTOMER_REQUEST)
                .messageType(MessageType.CUSTOMER_REQUEST)
                .payload(new CustomerServiceRequest(requestText))
                .build());

        try {
            // 在这里阻塞等待，直到 Receptionist 把结果塞进信封。
            // 这是学习项目，调试时经常会在断点上停很久，所以改为无超时等待，
            // 避免因为人为暂停过长而把正常链路误判为失败。
            return future.get();
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting handwritten engineering result", ex);
        }
        catch (ExecutionException ex) {
            throw new IllegalStateException("Failed to wait handwritten engineering result", ex);
        }
        finally {
            // 不管成功还是异常，最后都要把信封清掉，不然 Map 会一直积累。
            pendingResponseRegistry.remove(correlationId);
        }
    }
}

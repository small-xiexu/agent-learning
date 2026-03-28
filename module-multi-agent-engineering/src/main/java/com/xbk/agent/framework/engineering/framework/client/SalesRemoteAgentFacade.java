package com.xbk.agent.framework.engineering.framework.client;

import com.xbk.agent.framework.engineering.framework.config.A2aNacosCommonConfig;
import com.xbk.agent.framework.engineering.framework.support.A2aInvocationTraceSupport;
import com.xbk.agent.framework.engineering.framework.support.A2aResponseExtractor;
import io.a2a.client.A2AClient;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.TextPart;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 销售顾问远端 Agent Facade。
 *
 * 职责：与 TechSupportRemoteAgentFacade 结构完全对称，
 * 负责通过 A2A 协议调用销售顾问 Provider。
 *
 * <p>分开 TechSupport 和 Sales 两个 Facade 的意义：
 * 虽然当前两个 Facade 逻辑相似，但它们代表不同的业务边界。
 * 未来如果销售顾问需要额外的认证或不同的调用参数，只需修改本类，不影响技术专家调用链路。
 *
 * @author xiexu
 */
public class SalesRemoteAgentFacade {

    private final SpecialistRemoteAgentLocator locator;
    private final A2aNacosCommonConfig config;
    private final A2aInvocationTraceSupport traceSupport;

    /**
     * 创建销售顾问 Facade。
     *
     * @param locator 远端 Agent 定位器
     * @param config A2A 公共配置
     * @param traceSupport 调用追踪
     */
    public SalesRemoteAgentFacade(SpecialistRemoteAgentLocator locator,
                                   A2aNacosCommonConfig config,
                                   A2aInvocationTraceSupport traceSupport) {
        this.locator = locator;
        this.config = config;
        this.traceSupport = traceSupport;
    }

    /**
     * 调用销售顾问 Provider 并返回顾问答复文本。
     *
     * @param conversationId 会话标识
     * @param userRequest 用户的销售咨询问题
     * @return 销售顾问的答复文本
     */
    public String call(String conversationId, String userRequest) {
        String agentServiceName = config.getAgent().getSalesName();
        String agentUrl = locator.findAgentUrl(agentServiceName);
        Instant start = Instant.now();
        traceSupport.recordInvocationStart(conversationId, agentServiceName, agentUrl, userRequest);
        try {
            A2AClient client = new A2AClient(agentUrl);
            MessageSendParams params = buildParams(conversationId, userRequest);
            SendMessageResponse response = client.sendMessage(params);
            String result = A2aResponseExtractor.extractText(response);
            traceSupport.recordInvocationSuccess(conversationId, agentServiceName, start, result);
            return result;
        }
        catch (Exception ex) {
            traceSupport.recordInvocationFailure(conversationId, agentServiceName, start, ex);
            throw new RuntimeException("A2A call to sales-agent failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 构造 A2A MessageSendParams。
     *
     * @param conversationId 会话/上下文 ID
     * @param userRequest 用户文本
     * @return A2A 请求参数
     */
    private MessageSendParams buildParams(String conversationId, String userRequest) {
        TextPart textPart = new TextPart(userRequest);
        Message message = new Message(
                Message.Role.USER,
                List.of(textPart),
                "msg-" + UUID.randomUUID(),
                conversationId,
                null,
                null,
                null);
        return new MessageSendParams(message, null, null);
    }
}

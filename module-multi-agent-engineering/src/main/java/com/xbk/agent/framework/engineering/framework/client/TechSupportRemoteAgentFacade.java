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
 * 技术专家远端 Agent Facade。
 *
 * 职责：封装通过 A2A 协议调用技术专家 Provider 的完整流程：
 * 发现 URL → 构造 A2A 请求 → 调用 → 提取文本结果。
 *
 * <p>为什么需要单独的 Facade 类而不是在 FrameworkReceptionistService 里直接调用？
 * 因为"找谁"（SpecialistRemoteAgentLocator）和"怎么调"（Facade）职责不同：
 * 分开后，单元测试可以独立 mock 任意一层，路由逻辑变化时也不需要改调用代码。
 *
 * @author xiexu
 */
public class TechSupportRemoteAgentFacade {

    private final SpecialistRemoteAgentLocator locator;
    private final A2aNacosCommonConfig config;
    private final A2aInvocationTraceSupport traceSupport;

    /**
     * 创建技术专家 Facade。
     *
     * @param locator 远端 Agent 定位器
     * @param config A2A 公共配置
     * @param traceSupport 调用追踪
     */
    public TechSupportRemoteAgentFacade(SpecialistRemoteAgentLocator locator,
                                         A2aNacosCommonConfig config,
                                         A2aInvocationTraceSupport traceSupport) {
        this.locator = locator;
        this.config = config;
        this.traceSupport = traceSupport;
    }

    /**
     * 调用技术专家 Provider 并返回专家答复文本。
     *
     * <p>调用链路：
     * <pre>
     *   1. locator.findAgentUrl("tech-support-agent") → 从 Nacos 获取 Provider URL
     *   2. 构造 A2A Message（role=user, parts=[TextPart(userRequest)]）
     *   3. A2AClient.sendMessage(params) → 发送给 Provider 的 POST / 端点
     *   4. A2aResponseExtractor.extractText(response) → 提取 Task artifacts 中的文本
     * </pre>
     *
     * @param conversationId 会话标识
     * @param userRequest 用户的技术问题
     * @return 技术专家的答复文本
     */
    public String call(String conversationId, String userRequest) {
        String agentServiceName = config.getAgent().getTechName();
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
            throw new RuntimeException("A2A call to tech-support-agent failed: " + ex.getMessage(), ex);
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

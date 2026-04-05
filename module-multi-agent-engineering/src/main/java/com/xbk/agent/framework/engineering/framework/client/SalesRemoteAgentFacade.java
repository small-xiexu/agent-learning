package com.xbk.agent.framework.engineering.framework.client;

import com.xbk.agent.framework.engineering.framework.config.A2aNacosCommonConfig;
import com.xbk.agent.framework.engineering.framework.support.A2aHttpTransportSupport;
import com.xbk.agent.framework.engineering.framework.support.A2aInvocationTraceSupport;
import com.xbk.agent.framework.engineering.framework.support.A2aResponseExtractor;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.TextPart;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 销售顾问远端 Agent Facade。
 * <p>
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

    /**
     * 远端 Agent 定位器。
     * 通过 Nacos 服务发现把逻辑服务名（如 "sales-agent"）解析为可访问的 HTTP URL。
     * 由 {@link #call} 在每次调用前使用，以支持 Provider 动态扩缩容。
     */
    private final SpecialistRemoteAgentLocator locator;

    /**
     * A2A 公共配置。
     * 提供 Agent 服务命名（{@code config.getAgent().getSalesName()}）和超时等参数。
     * 与手写版的 MessageTopic 路由不同，框架版通过这里的服务名从 Nacos 寻址。
     */
    private final A2aNacosCommonConfig config;

    /**
     * A2A 调用链路追踪。
     * 在调用开始、成功、失败三个节点记录结构化日志，
     * 用于排查超时和审计远端调用行为，与 MQ 审计层互补。
     */
    private final A2aInvocationTraceSupport traceSupport;

    /**
     * 创建销售顾问 Facade。
     *
     * @param locator      远端 Agent 定位器
     * @param config       A2A 公共配置
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
     * <p>调用链路：
     * <pre>
     *   1. config.getAgent().getSalesName() → 取得 Nacos 逻辑服务名 "sales-agent"
     *   2. locator.findAgentUrl("sales-agent") → 从 Nacos 获取 Provider URL
     *   3. 构造 A2A Message（role=USER, parts=[TextPart(userRequest)]）
     *   4. A2aHttpTransportSupport.sendMessage(url, params) → 同步 POST 给 Provider
     *   5. A2aResponseExtractor.extractText(response) → 提取 Task artifacts 中的文本
     * </pre>
     *
     * @param conversationId 会话标识
     * @param userRequest    用户的销售咨询问题
     * @return 销售顾问的答复文本
     */
    public String call(String conversationId, String userRequest) {
        // 1. 从配置中取得销售顾问 Agent 在 Nacos 注册的逻辑服务名（如 "sales-agent"）
        String agentServiceName = config.getAgent().getSalesName();

        // 2. 通过 Nacos 服务发现，将逻辑服务名解析为实际可访问的 HTTP URL
        //    如果 Provider 未启动或未注册，这里会抛出 IllegalStateException
        String agentUrl = locator.findAgentUrl(agentServiceName);

        // 3. 记录调用起始时间，用于后续计算耗时；同时写入链路追踪的 START 事件
        Instant start = Instant.now();
        traceSupport.recordInvocationStart(conversationId, agentServiceName, agentUrl, userRequest);

        try {
            // 4. 将用户请求封装为 A2A 协议的 MessageSendParams
            MessageSendParams params = buildParams(conversationId, userRequest);

            // 5. 以标准 JSON-RPC over HTTP 发送请求到 Provider 的 A2A 端点，阻塞等待响应
            SendMessageResponse response = A2aHttpTransportSupport.sendMessage(agentUrl, params);

            // 6. 从 A2A 响应中提取纯文本结果（Task.artifacts → TextPart.text）
            String result = A2aResponseExtractor.extractText(response);

            // 7. 写入链路追踪的 SUCCESS 事件（含耗时和响应摘要）
            traceSupport.recordInvocationSuccess(conversationId, agentServiceName, start, result);
            return result;
        } catch (Exception ex) {
            // 调用失败：写入链路追踪的 FAILURE 事件后，包装为 RuntimeException 向上抛出，
            // 由上层 FrameworkReceptionistService 决定是否降级或返回错误提示给用户
            traceSupport.recordInvocationFailure(conversationId, agentServiceName, start, ex);
            throw new RuntimeException("A2A 调用销售顾问 Agent 失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 构造 A2A MessageSendParams。
     *
     * <p>Message 的 7 个构造参数含义：
     * role / parts / messageId / contextId / taskId / referenceTaskIds / metadata。
     * 其中 taskId、referenceTaskIds、metadata 当前场景不需要，传 null。
     *
     * @param conversationId 会话/上下文 ID，Provider 用它关联同一轮对话的上下文
     * @param userRequest    用户文本
     * @return A2A 请求参数
     */
    private MessageSendParams buildParams(String conversationId, String userRequest) {
        TextPart textPart = new TextPart(userRequest);
        // contextId = conversationId：让 Provider 能把多轮对话关联到同一个上下文
        Message message = new Message(
                Message.Role.USER,
                List.of(textPart),
                "msg-" + UUID.randomUUID(),
                conversationId,
                null,       // taskId — 首次调用无已有 task
                null,       // referenceTaskIds — 无关联任务
                null);      // metadata — 暂无自定义元数据
        return new MessageSendParams(message, null, null);
    }
}

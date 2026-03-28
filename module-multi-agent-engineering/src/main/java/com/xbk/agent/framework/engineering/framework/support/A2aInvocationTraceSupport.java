package com.xbk.agent.framework.engineering.framework.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * A2A 调用链路追踪支持。
 *
 * 职责：记录 Consumer 每次发起 A2A 远端调用的轨迹事件（开始、成功、失败），
 * 供审计、超时排查和运营监控使用。
 *
 * <p>这一层与 MQ 审计层是互补关系：
 * <ul>
 *   <li>A2aInvocationTraceSupport 记录框架版 A2A 点对点调用的同步链路事件；
 *   <li>RoutingAuditEventPublisher 把路由决策异步发布到 MQ 供后续消费；
 *   <li>两者不互相替代，共同构成框架版的可观测性层。
 * </ul>
 *
 * @author xiexu
 */
public class A2aInvocationTraceSupport {

    private static final Logger log = LoggerFactory.getLogger(A2aInvocationTraceSupport.class);

    /**
     * 记录 A2A 调用开始事件。
     *
     * @param conversationId 会话标识
     * @param targetAgentName 目标 Agent 名称
     * @param targetUrl 目标 Provider URL
     * @param requestSummary 请求摘要（前 100 字符）
     */
    public void recordInvocationStart(String conversationId,
                                      String targetAgentName,
                                      String targetUrl,
                                      String requestSummary) {
        log.info("[A2A-INVOKE][START] conversationId={} target={} url={} request={}",
                conversationId,
                targetAgentName,
                targetUrl,
                truncate(requestSummary, 100));
    }

    /**
     * 记录 A2A 调用成功事件。
     *
     * @param conversationId 会话标识
     * @param targetAgentName 目标 Agent 名称
     * @param startTime 调用开始时间（用于计算耗时）
     * @param responseSummary 响应摘要（前 100 字符）
     */
    public void recordInvocationSuccess(String conversationId,
                                        String targetAgentName,
                                        Instant startTime,
                                        String responseSummary) {
        long elapsed = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        log.info("[A2A-INVOKE][SUCCESS] conversationId={} target={} elapsed={}ms response={}",
                conversationId,
                targetAgentName,
                elapsed,
                truncate(responseSummary, 100));
    }

    /**
     * 记录 A2A 调用失败事件。
     *
     * @param conversationId 会话标识
     * @param targetAgentName 目标 Agent 名称
     * @param startTime 调用开始时间
     * @param cause 失败原因
     */
    public void recordInvocationFailure(String conversationId,
                                        String targetAgentName,
                                        Instant startTime,
                                        Throwable cause) {
        long elapsed = Instant.now().toEpochMilli() - startTime.toEpochMilli();
        log.error("[A2A-INVOKE][FAILURE] conversationId={} target={} elapsed={}ms error={}",
                conversationId,
                targetAgentName,
                elapsed,
                cause.getMessage(),
                cause);
    }

    /**
     * 截断字符串，用于日志摘要。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}

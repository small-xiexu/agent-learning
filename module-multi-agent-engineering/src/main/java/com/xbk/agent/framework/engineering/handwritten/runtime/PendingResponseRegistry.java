package com.xbk.agent.framework.engineering.handwritten.runtime;

import com.xbk.agent.framework.engineering.api.EngineeringRunResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 待回包注册表。
 *
 * 职责：用 correlationId 关联同步入口与异步回包，让 Coordinator 能等待最终结果。
 *
 * @author xiexu
 */
public class PendingResponseRegistry {

    private final ConcurrentHashMap<String, CompletableFuture<EngineeringRunResult>> pendingResponses =
            new ConcurrentHashMap<String, CompletableFuture<EngineeringRunResult>>();

    /**
     * 注册待回包 future。
     *
     * @param correlationId 关联标识
     * @return future
     */
    public CompletableFuture<EngineeringRunResult> register(String correlationId) {
        CompletableFuture<EngineeringRunResult> future = new CompletableFuture<EngineeringRunResult>();
        pendingResponses.put(correlationId, future);
        return future;
    }

    /**
     * 完成回包。
     *
     * @param correlationId 关联标识
     * @param result 运行结果
     */
    public void complete(String correlationId, EngineeringRunResult result) {
        CompletableFuture<EngineeringRunResult> future = pendingResponses.get(correlationId);
        if (future != null) {
            future.complete(result);
        }
    }

    /**
     * 获取 future。
     *
     * @param correlationId 关联标识
     * @return future
     */
    public CompletableFuture<EngineeringRunResult> get(String correlationId) {
        return pendingResponses.get(correlationId);
    }

    /**
     * 删除 future。
     *
     * @param correlationId 关联标识
     */
    public void remove(String correlationId) {
        pendingResponses.remove(correlationId);
    }
}

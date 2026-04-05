package com.xbk.agent.framework.engineering.handwritten.runtime;

import com.xbk.agent.framework.engineering.api.EngineeringRunResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 待回包注册表。
 *
 * <p>解决的问题：
 * Coordinator 对外提供同步 API（run() 直接返回结果），
 * 但底层整条链是异步消息驱动的——消息发出去之后，结果什么时候回来完全不知道。
 * 这里用"信封"机制把两者打通：
 * <ol>
 *   <li>Coordinator 先放一个空信封（Future）在这里等；
 *   <li>异步链跑完后，Receptionist 把结果塞进信封；
 *   <li>Coordinator 的 future.get() 立刻拿到结果，整条链闭合。
 * </ol>
 * correlationId 就是信封上的快递单号，确保结果塞进了正确的信封。
 *
 * @author xiexu
 */
public class PendingResponseRegistry {

    // 这个 Map 会被两个线程同时操作：
    //   Coordinator 线程：put（放信封）、remove（取走信封）
    //   异步 Agent 线程：get（找信封）、complete（往信封里塞结果）
    // 所以必须用线程安全的 ConcurrentHashMap，普通 HashMap 在并发下会出问题。
    private final ConcurrentHashMap<String, CompletableFuture<EngineeringRunResult>> pendingResponses =
            new ConcurrentHashMap<String, CompletableFuture<EngineeringRunResult>>();

    /**
     * 注册待回包 future。
     *
     * @param correlationId 关联标识
     * @return future
     */
    public CompletableFuture<EngineeringRunResult> register(String correlationId) {
        // 放一个空信封进去，correlationId 是这个信封的快递单号。
        // Coordinator 拿着这个 future，调用 future.get() 阻塞等待。
        // 注意：必须先放信封再发消息，否则极端情况下消息处理太快，
        // Receptionist 调用 complete() 时找不到信封，结果就丢了。
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
            // 往信封里塞入结果，Coordinator 那边的 future.get() 立刻解除阻塞，拿到结果返回。
            // 如果 future 是 null，说明 Coordinator 已经结束等待并把信封清理掉了，
            // 这时迟到回包直接忽略，不会出错。
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

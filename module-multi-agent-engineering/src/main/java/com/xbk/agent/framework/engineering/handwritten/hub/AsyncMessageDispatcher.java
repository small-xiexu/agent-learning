package com.xbk.agent.framework.engineering.handwritten.hub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 异步消息分发器。
 *
 * 职责：把消息消费动作放入线程池异步执行，模拟消息驱动运行时的发布与消费解耦。
 *
 * @author xiexu
 */
public class AsyncMessageDispatcher implements AutoCloseable {

    private final ExecutorService executorService;

    /**
     * 创建默认分发器。
     */
    public AsyncMessageDispatcher() {
        // 线程数下限为 2：至少能并发处理 Receptionist 和一个专家 Agent，
        // 防止单线程时 Receptionist 等专家回包而专家任务又排在自己后面造成死锁。
        // 上限跟随 CPU 核数，避免线程过多导致上下文切换开销反超收益。
        this.executorService = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    /**
     * 异步分发任务。
     *
     * @param task 异步任务
     */
    public void dispatch(Runnable task) {
        executorService.submit(task);
    }

    /**
     * 关闭线程池。
     */
    @Override
    public void close() {
        // shutdown() 不再接受新任务，但允许队列中的现有任务跑完。
        executorService.shutdown();
        try {
            // 等待最多 5 秒让线程池优雅退出；教学演示场景足够，生产需按最大任务耗时调整。
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            // 恢复中断标志，让调用方能感知并决定是否强制终止。
            Thread.currentThread().interrupt();
        }
    }
}

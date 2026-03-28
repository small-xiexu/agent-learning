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
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

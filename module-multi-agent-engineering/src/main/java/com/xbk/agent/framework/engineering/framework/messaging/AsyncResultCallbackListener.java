package com.xbk.agent.framework.engineering.framework.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 异步结果回调监听器（基类/模板）。
 *
 * 职责：监听 MQ callback 主题，处理专家处理完成后发布的异步回调事件。
 *
 * <p>使用方式：
 * 继承本类并添加 @RocketMQMessageListener 注解，指定 callback 主题和消费组。
 * 实现 onCallback(callbackEvent) 方法来处理回调逻辑。
 *
 * <p>为什么需要 callback 主题？
 * 在某些长耗时场景（如需要外部审批、人工审核）下，A2A 同步调用会超时。
 * 解决方案是：专家 Agent 先接受任务（返回 submitted 状态），完成后通过 MQ 发布结果。
 * 这条回调链路让接待员可以异步感知最终结果，而不是阻塞等待。
 *
 * <p>这是 MQ 在多智能体工程中"异步补偿"角色的体现：
 * 只有当 A2A 同步链路无法满足要求时，MQ 才介入提供补充能力。
 *
 * <p>示例实现：
 * <pre>
 *   {@literal @}Component
 *   {@literal @}RocketMQMessageListener(
 *       topic = "${engineering.mq.topic.callback:engineering.callback}",
 *       consumerGroup = "engineering-callback-consumer"
 *   )
 *   public class EngineeringCallbackListener extends AsyncResultCallbackListener {
 *       private final PendingResponseRegistry pendingRegistry;
 *
 *       public EngineeringCallbackListener(PendingResponseRegistry pendingRegistry) {
 *           this.pendingRegistry = pendingRegistry;
 *       }
 *
 *       {@literal @}Override
 *       protected void onCallback(Map{@literal <}String, Object{@literal >} callbackEvent) {
 *           String correlationId = (String) callbackEvent.get("correlationId");
 *           String result = (String) callbackEvent.get("result");
 *           // 通知等待中的 future
 *       }
 *   }
 * </pre>
 *
 * @author xiexu
 */
public abstract class AsyncResultCallbackListener {

    private static final Logger log = LoggerFactory.getLogger(AsyncResultCallbackListener.class);

    /**
     * 处理异步回调事件。
     *
     * <p>子类必须实现本方法来完成真正的业务逻辑，例如：
     * 查找 correlationId 对应的 CompletableFuture 并完成它。
     *
     * @param callbackEvent 回调事件 Map，包含 conversationId、correlationId、result、timestamp 等字段
     */
    protected abstract void onCallback(Map<String, Object> callbackEvent);

    /**
     * 处理 MQ 回调消息（模板方法）。
     *
     * <p>解析回调 Map 并分发给 onCallback，捕获所有异常避免消费失败导致重投。
     * 生产环境中失败消息会进入死信队列，可通过配置 DLQ 单独处理。
     *
     * @param callbackEvent 回调事件
     */
    public void processCallback(Map<String, Object> callbackEvent) {
        if (callbackEvent == null) {
            log.warn("[MQ-CALLBACK] Received null callback event, skipping");
            return;
        }
        try {
            log.debug("[MQ-CALLBACK] Processing callback event: conversationId={}",
                    callbackEvent.get("conversationId"));
            onCallback(callbackEvent);
        }
        catch (Exception ex) {
            log.error("[MQ-CALLBACK] Failed to process callback event: conversationId={}, error={}",
                    callbackEvent.get("conversationId"), ex.getMessage(), ex);
        }
    }
}

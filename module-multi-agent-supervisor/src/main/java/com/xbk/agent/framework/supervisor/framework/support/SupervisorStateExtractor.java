package com.xbk.agent.framework.supervisor.framework.support;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.supervisor.domain.memory.SupervisorStepRecord;
import com.xbk.agent.framework.supervisor.domain.routing.SupervisorWorkerType;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Supervisor 框架状态提取器
 *
 * 职责：从 OverAllState 中提取输出文本、路由轨迹和可审计消息历史，
 * 把 Spring AI Alibaba 原生状态重新投影成项目统一的结果模型
 *
 * @author xiexu
 */
public final class SupervisorStateExtractor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SupervisorStateExtractor() {
    }

    /**
     * 提取指定输出键对应的文本。
     *
     * @param state 框架状态
     * @param key 输出键
     * @return 输出文本
     */
    public static String extractOutput(OverAllState state, String key) {
        Object value = state.value(key).orElse(null);
        if (value == null) {
            return "";
        }
        // 框架状态里的某个 outputKey 可能存的是 Spring AI Message，也可能已经是普通字符串。
        // 这里统一抽成纯文本，避免上层结果对象被底层状态表示方式绑死。
        if (value instanceof org.springframework.ai.chat.messages.Message message) {
            return message.getText();
        }
        return String.valueOf(value);
    }

    /**
     * 提取主监督者的路由轨迹。
     *
     * @param state 框架状态
     * @return 路由轨迹
     */
    public static List<SupervisorWorkerType> extractRouteTrail(OverAllState state) {
        List<SupervisorWorkerType> routeTrail = new ArrayList<SupervisorWorkerType>();
        Object value = state.value(SupervisorStateKeys.MESSAGES).orElse(List.of());
        if (!(value instanceof List<?> messages)) {
            return routeTrail;
        }
        // 框架版把主管和 Worker 的输出都混在同一份消息流里，
        // 因此这里要再次筛选，只把“主管的路由数组消息”提取成 routeTrail。
        for (Object rawMessage : messages) {
            // 框架消息列表里既有主路由输出，也有 Worker 输出；只有主路由的数组文本才代表下一跳。
            if (rawMessage instanceof AssistantMessage assistantMessage) {
                routeTrail.addAll(parseRoutingMessage(assistantMessage.getText()));
            }
        }
        return routeTrail;
    }

    /**
     * 提取消息历史并转换为统一 Message。
     *
     * @param state 框架状态
     * @return 统一消息历史
     */
    public static List<Message> extractMessages(OverAllState state) {
        List<Message> convertedMessages = new ArrayList<Message>();
        Object value = state.value(SupervisorStateKeys.MESSAGES).orElse(List.of());
        if (!(value instanceof List<?> messages)) {
            return convertedMessages;
        }
        // 这里做的不是简单复制，而是把 Spring AI Alibaba 的消息类型重新投影成项目统一 Message，
        // 方便手写版、框架版最终都返回同构的 scratchpad 历史。
        for (Object rawMessage : messages) {
            if (rawMessage instanceof org.springframework.ai.chat.messages.Message springMessage) {
                convertedMessages.add(convertToFrameworkMessage(springMessage));
            }
        }
        return convertedMessages;
    }

    /**
     * 基于已知输出键构造步骤记录。
     *
     * @param routeTrail 路由轨迹
     * @param state 框架状态
     * @return 步骤记录
     */
    public static List<SupervisorStepRecord> extractStepRecords(List<SupervisorWorkerType> routeTrail, OverAllState state) {
        List<SupervisorStepRecord> stepRecords = new ArrayList<SupervisorStepRecord>();
        int stepIndex = 0;
        for (SupervisorWorkerType workerType : routeTrail) {
            if (workerType == SupervisorWorkerType.FINISH) {
                // FINISH 是主管的终止决策，不是某个 Worker 的执行步骤。
                continue;
            }
            stepIndex++;
            // 框架版没有像手写版那样显式逐轮 append StepRecord，
            // 因此这里只能根据 routeTrail + outputKey 反推出一份等价的步骤审计视图。
            stepRecords.add(new SupervisorStepRecord(
                    stepIndex,
                    workerType,
                    "",
                    extractOutput(state, keyForWorker(workerType))));
        }
        return stepRecords;
    }

    /**
     * 解析单条主监督者路由消息。
     *
     * @param text 主监督者消息文本
     * @return 路由结果
     */
    private static List<SupervisorWorkerType> parseRoutingMessage(String text) {
        List<SupervisorWorkerType> workers = new ArrayList<SupervisorWorkerType>();
        if (text == null || text.isBlank()) {
            return workers;
        }
        try {
            // 框架 Supervisor 的路由输出约定为 JSON 数组文本，例如 ["writer_agent"] 或 ["FINISH"]。
            // 只有符合这套协议的消息，才会被视为真正的路由决策。
            Object parsed = OBJECT_MAPPER.readValue(text.trim(), List.class);
            if (!(parsed instanceof List<?> rawRoutes)) {
                return workers;
            }
            for (Object rawRoute : rawRoutes) {
                if (rawRoute != null) {
                    workers.add(SupervisorWorkerType.fromRouteValue(String.valueOf(rawRoute)));
                }
            }
        }
        catch (Exception ignored) {
            // Worker 正常输出通常不是 JSON 数组，这里静默忽略非路由消息。
            return workers;
        }
        return workers;
    }

    /**
     * 把 Spring AI Message 转成统一 Message。
     *
     * @param springMessage Spring AI Message
     * @return 统一 Message
     */
    private static Message convertToFrameworkMessage(org.springframework.ai.chat.messages.Message springMessage) {
        // 这里生成的是“统一审计视图”，不是原封不动保留底层消息对象。
        // conversationId 使用固定占位值，是因为框架状态里的消息未必保留了项目侧会话标识。
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId("framework-supervisor-state")
                .role(resolveRole(springMessage))
                .name(springMessage.getMessageType().name())
                .content(resolveContent(springMessage))
                .build();
    }

    /**
     * 解析消息角色。
     *
     * @param springMessage Spring AI Message
     * @return 统一消息角色
     */
    private static MessageRole resolveRole(org.springframework.ai.chat.messages.Message springMessage) {
        if (springMessage instanceof SystemMessage) {
            return MessageRole.SYSTEM;
        }
        if (springMessage instanceof UserMessage) {
            return MessageRole.USER;
        }
        if (springMessage instanceof AssistantMessage) {
            return MessageRole.ASSISTANT;
        }
        if (springMessage instanceof ToolResponseMessage) {
            return MessageRole.TOOL;
        }
        return MessageRole.USER;
    }

    /**
     * 解析消息内容。
     *
     * @param springMessage Spring AI Message
     * @return 文本内容
     */
    private static String resolveContent(org.springframework.ai.chat.messages.Message springMessage) {
        if (springMessage instanceof ToolResponseMessage toolResponseMessage) {
            // ToolResponseMessage 本身不是单一字符串，这里转成文本是为了统一写入审计结果。
            return String.valueOf(toolResponseMessage.getResponses());
        }
        if (springMessage instanceof AbstractMessage abstractMessage) {
            return abstractMessage.getText();
        }
        return String.valueOf(springMessage);
    }

    /**
     * 根据 Worker 类型返回输出键。
     *
     * 这层映射让框架版状态可以被还原成和手写版一致的 StepRecord 结构。
     *
     * @param workerType Worker 类型
     * @return 输出键
     */
    private static String keyForWorker(SupervisorWorkerType workerType) {
        if (workerType == SupervisorWorkerType.WRITER) {
            return SupervisorStateKeys.WRITER_OUTPUT;
        }
        if (workerType == SupervisorWorkerType.TRANSLATOR) {
            return SupervisorStateKeys.TRANSLATOR_OUTPUT;
        }
        if (workerType == SupervisorWorkerType.REVIEWER) {
            return SupervisorStateKeys.REVIEWER_OUTPUT;
        }
        return "";
    }
}

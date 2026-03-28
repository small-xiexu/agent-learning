package com.xbk.agent.framework.engineering.framework.support;

import io.a2a.spec.Artifact;
import io.a2a.spec.EventKind;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A2A 响应提取器。
 *
 * 职责：从 A2A SendMessageResponse 中统一提取文本内容，
 * 避免解析逻辑散落在各个 Consumer 业务代码里。
 *
 * <p>A2A 响应的结构层次：
 * <pre>
 *   SendMessageResponse
 *     └── result (EventKind)
 *           ├── Task（大多数同步调用返回 Task）
 *           │    ├── status.state = COMPLETED
 *           │    └── artifacts[] → parts[] → TextPart.text
 *           └── Message（流式或消息回复场景）
 *                 └── parts[] → TextPart.text
 * </pre>
 *
 * @author xiexu
 */
public final class A2aResponseExtractor {

    private A2aResponseExtractor() {
    }

    /**
     * 从 A2A 响应中提取文本结果。
     *
     * <p>优先从 Task.artifacts 中取文本，其次从 Task.status.message 取，最后从 Message.parts 取。
     *
     * @param response A2A 发送响应
     * @return 提取到的文本，未提取到时返回空字符串
     */
    public static String extractText(SendMessageResponse response) {
        if (response == null || response.getResult() == null) {
            return "";
        }
        EventKind result = response.getResult();
        if (result instanceof Task) {
            return extractFromTask((Task) result);
        }
        if (result instanceof Message) {
            return extractFromMessage((Message) result);
        }
        return "";
    }

    /**
     * 验证响应是否成功完成（Task.status.state == COMPLETED）。
     *
     * @param response A2A 响应
     * @return true 表示成功完成
     */
    public static boolean isCompleted(SendMessageResponse response) {
        if (response == null || response.getResult() == null) {
            return false;
        }
        EventKind result = response.getResult();
        if (result instanceof Task) {
            Task task = (Task) result;
            return task.getStatus() != null && TaskState.COMPLETED == task.getStatus().state();
        }
        return false;
    }

    /**
     * 从 Task 中提取文本。
     *
     * @param task A2A Task
     * @return 文本内容
     */
    private static String extractFromTask(Task task) {
        // 优先从 artifacts 提取
        if (task.getArtifacts() != null && !task.getArtifacts().isEmpty()) {
            String fromArtifacts = task.getArtifacts().stream()
                    .map(A2aResponseExtractor::extractFromArtifact)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining("\n"));
            if (!fromArtifacts.isEmpty()) {
                return fromArtifacts;
            }
        }
        // 从 status.message 提取
        if (task.getStatus() != null && task.getStatus().message() != null) {
            return extractFromMessage(task.getStatus().message());
        }
        return "";
    }

    /**
     * 从 Artifact 中提取文本。
     *
     * @param artifact A2A Artifact
     * @return 文本内容
     */
    private static String extractFromArtifact(Artifact artifact) {
        if (artifact.parts() == null) {
            return "";
        }
        return artifact.parts().stream()
                .filter(part -> part instanceof TextPart)
                .map(part -> ((TextPart) part).getText())
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 从 Message 中提取文本。
     *
     * @param message A2A Message
     * @return 文本内容
     */
    private static String extractFromMessage(Message message) {
        List<Part<?>> parts = message.getParts();
        if (parts == null) {
            return "";
        }
        return parts.stream()
                .filter(part -> part instanceof TextPart)
                .map(part -> ((TextPart) part).getText())
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }
}

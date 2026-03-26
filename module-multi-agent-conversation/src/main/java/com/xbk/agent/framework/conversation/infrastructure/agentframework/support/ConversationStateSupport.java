package com.xbk.agent.framework.conversation.infrastructure.agentframework.support;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.conversation.domain.memory.ConversationTurn;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Conversation 状态支撑
 *
 * 职责：在 FlowAgent 状态中稳定保存 shared_messages 和 transcript，并在读取时恢复对象结构
 *
 * @author xiexu
 */
public final class ConversationStateSupport {

    private static final String TURN_NUMBER_KEY = "turnNumber";
    private static final String ROLE_TYPE_KEY = "roleType";
    private static final String CONTENT_KEY = "content";
    private static final String MESSAGE_ID_KEY = "messageId";
    private static final String CONVERSATION_ID_KEY = "conversationId";
    private static final String MESSAGE_ROLE_KEY = "messageRole";
    private static final String MESSAGE_NAME_KEY = "name";

    private ConversationStateSupport() {
    }

    /**
     * 把 transcript 转成可序列化状态结构。
     *
     * @param transcript transcript
     * @return 状态结构
     */
    public static List<Map<String, Object>> toStateTranscript(List<ConversationTurn> transcript) {
        // FlowAgent 状态本质上更适合保存 Map / List 这类可序列化结构，
        // 因此这里先把领域对象拍平成“只保留必要字段”的状态快照，
        // 让 transcript 可以稳定写进图状态、日志或后续节点上下文。
        List<Map<String, Object>> stateTranscript = new ArrayList<Map<String, Object>>();
        for (ConversationTurn turn : transcript) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(TURN_NUMBER_KEY, Integer.valueOf(turn.getTurnNumber()));
            item.put(ROLE_TYPE_KEY, turn.getRoleType().getStateValue());
            item.put(CONTENT_KEY, turn.getContent());
            stateTranscript.add(item);
        }
        return List.copyOf(stateTranscript);
    }

    /**
     * 从状态值恢复 transcript。
     *
     * @param stateValue 状态值
     * @return transcript
     */
    public static List<ConversationTurn> readTranscript(Object stateValue) {
        if (!(stateValue instanceof List<?>)) {
            return new ArrayList<ConversationTurn>();
        }
        // 读取阶段再把通用状态结构恢复成领域对象，
        // 这样上层协调器和结果对象仍然可以继续使用强类型 ConversationTurn。
        List<ConversationTurn> transcript = new ArrayList<ConversationTurn>();
        for (Object item : (List<?>) stateValue) {
            ConversationTurn turn = toTurn(item);
            if (turn != null) {
                transcript.add(turn);
            }
        }
        return transcript;
    }

    /**
     * 把共享消息转成可序列化状态结构。
     *
     * @param sharedMessages 共享消息
     * @return 状态结构
     */
    public static List<Map<String, Object>> toStateMessages(List<Message> sharedMessages) {
        // sharedMessages 不直接原样塞进状态，是为了避免框架状态里混入过多 Java 对象细节。
        // 这里保留 message 身份、角色和正文，足够支撑后续恢复和调试回放。
        List<Map<String, Object>> stateMessages = new ArrayList<Map<String, Object>>();
        for (Message message : sharedMessages) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(MESSAGE_ID_KEY, message.getMessageId());
            item.put(CONVERSATION_ID_KEY, message.getConversationId());
            item.put(MESSAGE_ROLE_KEY, message.getRole().name());
            item.put(CONTENT_KEY, message.getContent());
            item.put(MESSAGE_NAME_KEY, message.getName());
            stateMessages.add(item);
        }
        return List.copyOf(stateMessages);
    }

    /**
     * 从状态值恢复共享消息。
     *
     * @param stateValue 状态值
     * @return 共享消息
     */
    public static List<Message> readSharedMessages(Object stateValue) {
        if (!(stateValue instanceof List<?>)) {
            return new ArrayList<Message>();
        }
        // 这里和 transcript 一样，做的是“框架状态 -> 统一消息模型”的逆变换，
        // 保证框架版最终产出的 sharedMessages 能和手写版使用同一套 Message 类型对齐。
        List<Message> sharedMessages = new ArrayList<Message>();
        for (Object item : (List<?>) stateValue) {
            Message message = toMessage(item);
            if (message != null) {
                sharedMessages.add(message);
            }
        }
        return sharedMessages;
    }

    /**
     * 恢复单条 transcript。
     *
     * @param item 原始项
     * @return transcript 项
     */
    private static ConversationTurn toTurn(Object item) {
        if (item instanceof ConversationTurn) {
            return (ConversationTurn) item;
        }
        if (!(item instanceof Map<?, ?>)) {
            return null;
        }
        // 状态里保存的是字符串化 roleType，因此恢复时必须显式做一次“状态值 -> 枚举”的翻译。
        Map<?, ?> rawMap = (Map<?, ?>) item;
        ConversationRoleType roleType = ConversationRoleType.fromStateValue(String.valueOf(rawMap.get(ROLE_TYPE_KEY)));
        int turnNumber = resolveInteger(rawMap.get(TURN_NUMBER_KEY));
        String content = rawMap.get(CONTENT_KEY) == null ? "" : String.valueOf(rawMap.get(CONTENT_KEY));
        return new ConversationTurn(turnNumber, roleType, content);
    }

    /**
     * 恢复单条共享消息。
     *
     * @param item 原始项
     * @return 共享消息
     */
    private static Message toMessage(Object item) {
        if (item instanceof Message) {
            return (Message) item;
        }
        if (!(item instanceof Map<?, ?>)) {
            return null;
        }
        // 恢复 Message 时对缺失字段给默认值，是为了提高状态回放的容错性：
        // 即使框架侧只保留了部分字段，也不至于让最终结果构造直接失败。
        Map<?, ?> rawMap = (Map<?, ?>) item;
        return Message.builder()
                .messageId(resolveText(rawMap.get(MESSAGE_ID_KEY), "message-" + UUID.randomUUID()))
                .conversationId(resolveText(rawMap.get(CONVERSATION_ID_KEY), "conversation-" + UUID.randomUUID()))
                .role(resolveMessageRole(rawMap.get(MESSAGE_ROLE_KEY)))
                .content(resolveText(rawMap.get(CONTENT_KEY), ""))
                .name(resolveNullableText(rawMap.get(MESSAGE_NAME_KEY)))
                .build();
    }

    /**
     * 解析整数。
     *
     * @param value 原始值
     * @return 整数
     */
    private static int resolveInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException exception) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 解析消息角色。
     *
     * @param value 原始值
     * @return 消息角色
     */
    private static MessageRole resolveMessageRole(Object value) {
        if (value instanceof MessageRole) {
            return (MessageRole) value;
        }
        if (value instanceof String) {
            for (MessageRole role : MessageRole.values()) {
                if (role.name().equalsIgnoreCase((String) value)) {
                    return role;
                }
            }
        }
        return MessageRole.USER;
    }

    /**
     * 解析文本。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 文本
     */
    private static String resolveText(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    /**
     * 解析可空文本。
     *
     * @param value 原始值
     * @return 文本
     */
    private static String resolveNullableText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}

package com.xbk.agent.framework.roleplay.infrastructure.agentframework.support;

import com.xbk.agent.framework.roleplay.domain.memory.CamelDialogueTurn;
import com.xbk.agent.framework.roleplay.domain.role.CamelRoleType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CAMEL transcript 状态支撑
 *
 * 职责：在 FlowAgent 状态中以稳定结构保存 transcript，并在读取时兼容对象态与 Map 态
 *
 * @author xiexu
 */
public final class CamelTranscriptStateSupport {

    private static final String TURN_NUMBER_KEY = "turnNumber";
    private static final String ROLE_TYPE_KEY = "roleType";
    private static final String CONTENT_KEY = "content";

    private CamelTranscriptStateSupport() {
    }

    /**
     * 从状态值中恢复 transcript。
     *
     * @param stateValue 状态中的 transcript 值
     * @return 恢复后的 transcript
     */
    public static List<CamelDialogueTurn> readTranscript(Object stateValue) {
        if (!(stateValue instanceof List<?>)) {
            return new ArrayList<CamelDialogueTurn>();
        }
        List<?> rawList = (List<?>) stateValue;
        List<CamelDialogueTurn> transcript = new ArrayList<CamelDialogueTurn>();
        for (Object item : rawList) {
            CamelDialogueTurn turn = toDialogueTurn(item);
            if (turn != null) {
                transcript.add(turn);
            }
        }
        return transcript;
    }

    /**
     * 把 transcript 转成适合放入 Flow 状态的稳定结构。
     *
     * @param transcript transcript
     * @return 适合状态序列化的结构
     */
    public static List<Map<String, Object>> toStateTranscript(List<CamelDialogueTurn> transcript) {
        List<Map<String, Object>> stateTranscript = new ArrayList<Map<String, Object>>();
        for (CamelDialogueTurn turn : transcript) {
            stateTranscript.add(toStateTurn(turn));
        }
        return List.copyOf(stateTranscript);
    }

    /**
     * 把单条对象转换成状态结构。
     *
     * @param turn 对话轮次
     * @return 状态结构
     */
    private static Map<String, Object> toStateTurn(CamelDialogueTurn turn) {
        Map<String, Object> stateTurn = new LinkedHashMap<String, Object>();
        stateTurn.put(TURN_NUMBER_KEY, Integer.valueOf(turn.getTurnNumber()));
        stateTurn.put(ROLE_TYPE_KEY, turn.getRoleType().getStateValue());
        stateTurn.put(CONTENT_KEY, turn.getContent());
        return stateTurn;
    }

    /**
     * 把状态项恢复为对话轮次。
     *
     * @param item 状态项
     * @return 对话轮次，无法恢复时返回空
     */
    private static CamelDialogueTurn toDialogueTurn(Object item) {
        if (item instanceof CamelDialogueTurn) {
            return (CamelDialogueTurn) item;
        }
        if (!(item instanceof Map<?, ?>)) {
            return null;
        }
        Map<?, ?> rawMap = (Map<?, ?>) item;
        int turnNumber = resolveTurnNumber(rawMap.get(TURN_NUMBER_KEY));
        CamelRoleType roleType = resolveRoleType(rawMap.get(ROLE_TYPE_KEY));
        String content = rawMap.get(CONTENT_KEY) == null ? "" : String.valueOf(rawMap.get(CONTENT_KEY));
        if (roleType == null) {
            return null;
        }
        return new CamelDialogueTurn(turnNumber, roleType, content);
    }

    /**
     * 解析轮次编号。
     *
     * @param value 原始值
     * @return 轮次编号
     */
    private static int resolveTurnNumber(Object value) {
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
     * 解析角色类型。
     *
     * @param value 原始值
     * @return 角色类型
     */
    private static CamelRoleType resolveRoleType(Object value) {
        if (value instanceof CamelRoleType) {
            return (CamelRoleType) value;
        }
        if (!(value instanceof String)) {
            return null;
        }
        String text = ((String) value).trim();
        for (CamelRoleType roleType : CamelRoleType.values()) {
            if (roleType.name().equalsIgnoreCase(text)) {
                return roleType;
            }
            if (roleType.getStateValue().equalsIgnoreCase(text)) {
                return roleType;
            }
        }
        return null;
    }
}

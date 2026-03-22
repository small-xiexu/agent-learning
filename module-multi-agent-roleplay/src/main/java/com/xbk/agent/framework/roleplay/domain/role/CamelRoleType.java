package com.xbk.agent.framework.roleplay.domain.role;

/**
 * CAMEL 角色类型
 *
 * 职责：定义股票分析协作中的两个固定角色
 *
 * @author xiexu
 */
public enum CamelRoleType {

    /**
     * 交易员角色。
     */
    TRADER("交易员", "trader"),

    /**
     * 程序员角色。
     */
    PROGRAMMER("程序员", "programmer");

    private final String displayName;
    private final String stateValue;

    CamelRoleType(String displayName, String stateValue) {
        this.displayName = displayName;
        this.stateValue = stateValue;
    }

    /**
     * 返回展示名称。
     *
     * @return 展示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 返回状态值。
     *
     * @return 状态值
     */
    public String getStateValue() {
        return stateValue;
    }

    /**
     * 根据状态值解析角色类型。
     *
     * @param stateValue 状态值
     * @return 角色类型
     */
    public static CamelRoleType fromStateValue(String stateValue) {
        for (CamelRoleType value : values()) {
            if (value.stateValue.equals(stateValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unsupported role state value: " + stateValue);
    }
}

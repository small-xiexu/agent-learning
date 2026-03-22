package com.xbk.agent.framework.conversation.domain.role;

/**
 * 群聊角色类型
 *
 * 职责：定义 AutoGen RoundRobin 群聊中的三个参与角色
 *
 * @author xiexu
 */
public enum ConversationRoleType {

    /**
     * 产品经理。
     */
    PRODUCT_MANAGER("ProductManager", "product_manager"),

    /**
     * 工程师。
     */
    ENGINEER("Engineer", "engineer"),

    /**
     * 代码审查员。
     */
    CODE_REVIEWER("CodeReviewer", "code_reviewer");

    /**
     * 群聊中展示的角色名。
     */
    private final String displayName;

    /**
     * Flow 状态里使用的稳定值。
     */
    private final String stateValue;

    /**
     * 创建角色类型。
     *
     * @param displayName 展示名
     * @param stateValue 状态值
     */
    ConversationRoleType(String displayName, String stateValue) {
        this.displayName = displayName;
        this.stateValue = stateValue;
    }

    /**
     * 返回展示名。
     *
     * @return 展示名
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
     * 根据状态值恢复角色类型。
     *
     * @param value 状态值
     * @return 角色类型
     */
    public static ConversationRoleType fromStateValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PRODUCT_MANAGER;
        }
        String text = value.trim();
        for (ConversationRoleType roleType : values()) {
            if (roleType.stateValue.equalsIgnoreCase(text) || roleType.name().equalsIgnoreCase(text)) {
                return roleType;
            }
        }
        return PRODUCT_MANAGER;
    }
}

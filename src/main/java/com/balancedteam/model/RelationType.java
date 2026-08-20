package com.balancedteam.model;

/**
 * 团队间外交关系类型枚举
 */
public enum RelationType {
    ALLY("盟友"),
    ENEMY("敌对");

    private final String displayName;

    RelationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

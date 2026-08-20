package com.balancedteam.model;

/**
 * 团队关系状态枚举
 */
public enum RelationStatus {
    PENDING("申请中"),
    ACCEPTED("已确立");

    private final String displayName;

    RelationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

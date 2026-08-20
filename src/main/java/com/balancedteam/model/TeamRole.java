package com.balancedteam.model;

/**
 * 团队内成员职位与权限等级枚举
 * 数据库存储权限等级数字：3 为队长，2 为管理员，1 为普通队员
 */
public enum TeamRole {
    LEADER("队长", 3),
    OFFICER("管理员", 2),
    MEMBER("队员", 1);

    private final String displayName;
    private final int level;

    TeamRole(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 是否至少具备指定职位的权限等级
     */
    public boolean isAtLeast(TeamRole required) {
        if (required == null) return true;
        return this.level >= required.level;
    }

    /**
     * 是否有权解散队伍（仅队长 Level 3 具备，管理员 Level 2 无法解散）
     */
    public boolean canDisband() {
        return this == LEADER || this.level >= 3;
    }

    /**
     * 根据数据库存储的数字权限等级解析枚举
     *
     * @param level 权限等级（3: 队长, 2: 管理员, 1: 队员）
     * @return 对应的 TeamRole 枚举，默认返回 MEMBER
     */
    public static TeamRole fromLevel(int level) {
        for (TeamRole role : values()) {
            if (role.level == level) {
                return role;
            }
        }
        return MEMBER;
    }

    /**
     * 从数据库字段值（支持 Integer 或兼容历史 String）解析 TeamRole
     */
    public static TeamRole fromDatabase(Object value) {
        if (value == null) {
            return MEMBER;
        }
        if (value instanceof Number) {
            return fromLevel(((Number) value).intValue());
        }
        String str = value.toString().trim();
        try {
            int lvl = Integer.parseInt(str);
            return fromLevel(lvl);
        } catch (NumberFormatException ignored) {}

        try {
            return TeamRole.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        return MEMBER;
    }
}

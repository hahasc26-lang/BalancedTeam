package com.balancedteam.model;

import com.balancedteam.BalancedTeamPlugin;
import org.bukkit.command.CommandSender;

/**
 * 团队内成员职位与权限等级枚举
 * 数据库存储权限等级数字：3 为队长，2 为管理员，1 为普通队员
 */
public enum TeamRole {
    LEADER("Leader", 3),
    OFFICER("Officer", 2),
    MEMBER("Member", 1);

    private final String fallbackName;
    private final int level;

    TeamRole(String fallbackName, int level) {
        this.fallbackName = fallbackName;
        this.level = level;
    }

    public String getFallbackName() {
        return fallbackName;
    }

    /**
     * 获取职位展示名称（优先从当前插件配置的多语言系统获取，回退至安全英文名称）
     */
    public String getDisplayName() {
        BalancedTeamPlugin plugin = BalancedTeamPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager() != null) {
            return plugin.getConfigManager().getRoleDisplayName(this);
        }
        return fallbackName;
    }

    /**
     * 根据发送者客户端语言从语言配置文件中获取职位展示名称
     *
     * @param plugin 主插件实例
     * @param sender 消息接收者（可为 Player 或 ConsoleCommandSender）
     * @return 语言文件中定义的职位名称
     */
    public String getDisplayName(BalancedTeamPlugin plugin, CommandSender sender) {
        if (plugin != null && plugin.getConfigManager() != null) {
            return plugin.getConfigManager().getRoleDisplayName(sender, this);
        }
        return getDisplayName();
    }

    /**
     * 根据发送者客户端语言从语言配置文件中获取职位展示名称
     *
     * @param sender 消息接收者
     * @return 语言文件中定义的职位名称
     */
    public String getDisplayName(CommandSender sender) {
        return getDisplayName(BalancedTeamPlugin.getInstance(), sender);
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

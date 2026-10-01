package com.balancedteam.model;

import com.balancedteam.BalancedTeamPlugin;
import org.bukkit.command.CommandSender;

/**
 * 团队关系状态枚举
 */
public enum RelationStatus {
    PENDING("Pending"),
    ACCEPTED("Established");

    private final String fallbackName;

    RelationStatus(String fallbackName) {
        this.fallbackName = fallbackName;
    }

    public String getFallbackName() {
        return fallbackName;
    }

    /**
     * 获取关系状态展示名称（优先从当前插件配置的多语言系统获取，回退至安全英文名称）
     */
    public String getDisplayName() {
        BalancedTeamPlugin plugin = BalancedTeamPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager() != null) {
            return plugin.getConfigManager().getRelationStatusDisplayName(this);
        }
        return fallbackName;
    }

    /**
     * 根据发送者客户端语言从语言配置文件中获取关系状态展示名称
     *
     * @param plugin 主插件实例
     * @param sender 消息接收者（可为 Player 或 ConsoleCommandSender）
     * @return 语言文件中定义的关系状态名称
     */
    public String getDisplayName(BalancedTeamPlugin plugin, CommandSender sender) {
        if (plugin != null && plugin.getConfigManager() != null) {
            return plugin.getConfigManager().getRelationStatusDisplayName(sender, this);
        }
        return getDisplayName();
    }

    /**
     * 根据发送者客户端语言从语言配置文件中获取关系状态展示名称
     *
     * @param sender 消息接收者
     * @return 语言文件中定义的关系状态名称
     */
    public String getDisplayName(CommandSender sender) {
        return getDisplayName(BalancedTeamPlugin.getInstance(), sender);
    }
}

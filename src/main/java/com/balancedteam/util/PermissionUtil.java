package com.balancedteam.util;

import com.balancedteam.config.ConfigManager;
import com.balancedteam.model.Team;
import org.bukkit.entity.Player;

/**
 * 统一团队权限校验工具类
 */
public final class PermissionUtil {

    private PermissionUtil() {
    }

    /**
     * 判断玩家是否为该团队的队长
     */
    public static boolean isLeader(Player player, Team team) {
        // 防御性语法，防止空指针异常
        if (player == null || team == null)
            return false;
        return team.isLeader(player.getUniqueId());
    }

    /**
     * 判断玩家是否为该团队的副官（Officer）或队长（Leader）
     */
    public static boolean isOfficerOrLeader(Player player, Team team) {
        if (player == null || team == null)
            return false;
        return team.isOfficerOrLeader(player.getUniqueId());
    }

    /**
     * 校验玩家是否为队长，若不是则自动发送错误提示并播放错误音效
     *
     * @return true 若为队长；false 若不是
     */
    public static boolean checkLeader(Player player, Team team, ConfigManager config) {
        if (player == null)
            return false;
        if (team == null) {
            MessageUtil.sendMessage(player, config.getMessage("team_not_in_team"));
            SoundUtil.playError(player);
            return false;
        }
        if (!team.isLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(player, config.getMessage("team_not_leader"));
            SoundUtil.playError(player);
            return false;
        }
        return true;
    }

    /**
     * 校验玩家是否为副官或队长，若不是则自动发送错误提示并播放错误音效
     *
     * @return true 若为副官或队长；false 若不是
     */
    public static boolean checkOfficerOrLeader(Player player, Team team, ConfigManager config) {
        if (player == null)
            return false;
        if (team == null) {
            MessageUtil.sendMessage(player, config.getMessage("team_not_in_team"));
            SoundUtil.playError(player);
            return false;
        }
        if (!team.isOfficerOrLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(player, config.getMessage("team_not_officer_or_leader"));
            SoundUtil.playError(player);
            return false;
        }
        return true;
    }
}

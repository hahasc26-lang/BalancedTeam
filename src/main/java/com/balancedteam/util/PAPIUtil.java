package com.balancedteam.util;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.expansion.BalancedTeamExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * PlaceholderAPI 工具类
 * 负责占位符扩展生命周期注册以及双向变量安全解析
 */
public class PAPIUtil {

    private static BalancedTeamExpansion expansionInstance;

    /**
     * 检查服务器是否安装并启用了 PlaceholderAPI
     */
    public static boolean hasPAPI() {
        return Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    /**
     * 注册 BalancedTeam 占位符扩展到 PlaceholderAPI
     */
    public static boolean registerExpansion(BalancedTeamPlugin plugin) {
        if (!hasPAPI()) {
            return false;
        }
        try {
            if (expansionInstance == null) {
                expansionInstance = new BalancedTeamExpansion(plugin);
            }
            return expansionInstance.register();
        } catch (Throwable t) {
            PluginLogger.log(Level.WARNING, PluginLogger.LogKey.PAPI_REGISTER_ERROR, t, t.getMessage());
            return false;
        }
    }

    /**
     * 从 PlaceholderAPI 安全注销扩展
     */
    public static void unregisterExpansion() {
        if (expansionInstance != null && hasPAPI()) {
            try {
                expansionInstance.unregister();
            } catch (Throwable ignored) {}
            expansionInstance = null;
        }
    }

    /**
     * 解析单个字符串中的 PlaceholderAPI 占位符 (针对 OfflinePlayer)
     * 若未安装 PlaceholderAPI 或传入为空则原样返回
     */
    public static String setPlaceholders(OfflinePlayer player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (!hasPAPI()) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable t) {
            return text;
        }
    }

    /**
     * 解析单个字符串中的 PlaceholderAPI 占位符 (针对在线 Player)
     */
    public static String setPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (!hasPAPI()) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable t) {
            return text;
        }
    }

    /**
     * 解析多行字符串列表中的 PlaceholderAPI 占位符 (针对 OfflinePlayer)
     */
    public static List<String> setPlaceholders(OfflinePlayer player, List<String> list) {
        if (list == null || list.isEmpty()) {
            return list == null ? Collections.emptyList() : list;
        }
        if (!hasPAPI()) {
            return list;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, list);
        } catch (Throwable t) {
            return list;
        }
    }

    /**
     * 解析多行字符串列表中的 PlaceholderAPI 占位符 (针对在线 Player)
     */
    public static List<String> setPlaceholders(Player player, List<String> list) {
        if (list == null || list.isEmpty()) {
            return list == null ? Collections.emptyList() : list;
        }
        if (!hasPAPI()) {
            return list;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, list);
        } catch (Throwable t) {
            return list;
        }
    }
}

package com.balancedteam.listener;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.manager.ClientLanguageManager;
import com.balancedteam.model.Team;
import com.balancedteam.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家行为、登录与聊天监听器
 * 采用 Bukkit / Spigot 通用标准事件，兼容所有服务端
 */
public class PlayerListener implements Listener {

    private final BalancedTeamPlugin plugin;

    public PlayerListener(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ClientLanguageManager langMgr = plugin.getClientLanguageManager();
        if (langMgr != null) {
            String clientLocale = "unknown";
            try {
                clientLocale = player.getLocale();
            } catch (Throwable ignored) {}
            String effectiveCode = langMgr.getEffectiveLanguageCode(player);
            com.balancedteam.util.PluginLogger.info(
                    com.balancedteam.util.PluginLogger.LogKey.PLAYER_JOIN_LOCALE,
                    player.getName(),
                    clientLocale,
                    langMgr.getCanonicalCode(effectiveCode),
                    langMgr.getDisplayName(effectiveCode)
            );
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        // 1. 优先检查并消耗等待中的输入会话 (如 GUI 手动输入玩家名等)
        if (plugin.getChatInputManager().handleChat(player, message)) {
            event.setCancelled(true);
            return;
        }

        // 2. 团队聊天模式处理
        if (plugin.getChatManager().isInTeamChatMode(player.getUniqueId())) {
            // 全局聊天开关检测：关闭时自动退出模式并通知玩家
            if (!plugin.getConfigManager().isChatEnabled()) {
                plugin.getChatManager().removePlayer(player.getUniqueId());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "chat_disabled"));
                return;
            }
            Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
            if (team != null) {
                event.setCancelled(true);
                plugin.getChatManager().sendTeamChat(player, team, message);
            } else {
                // 如果已不在团队中，自动关闭团队聊天模式并通知
                plugin.getChatManager().removePlayer(player.getUniqueId());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_not_in_team"));
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "chat_team_toggle_off"));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getChatManager().removePlayer(event.getPlayer().getUniqueId());
        plugin.getChatInputManager().removePlayer(event.getPlayer().getUniqueId());
    }
}

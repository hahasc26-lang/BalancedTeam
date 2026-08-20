package com.balancedteam.listener;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.manager.LanguageManager;
import com.balancedteam.model.Team;
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
        LanguageManager langMgr = plugin.getLanguageManager();
        if (langMgr != null) {
            String clientLocale = "unknown";
            try {
                clientLocale = player.getLocale();
            } catch (Throwable ignored) {}
            String effectiveCode = langMgr.getEffectiveLanguageCode(player);
            plugin.getLogger().info("[Lang] 玩家 " + player.getName() + " 加入，客户端 Locale: " 
                    + clientLocale + "，匹配生效语言: " + langMgr.getCanonicalCode(effectiveCode) 
                    + " (" + langMgr.getDisplayName(effectiveCode) + ")");
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
            Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
            if (team != null) {
                event.setCancelled(true);
                plugin.getChatManager().sendTeamChat(player, team, message);
            } else {
                // 如果已不在团队中，自动关闭团队聊天模式
                plugin.getChatManager().toggleTeamChatMode(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getChatManager().removePlayer(event.getPlayer().getUniqueId());
        plugin.getChatInputManager().removePlayer(event.getPlayer().getUniqueId());
    }
}

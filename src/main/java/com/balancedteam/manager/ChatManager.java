package com.balancedteam.manager;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.model.Team;
import com.balancedteam.model.TeamMember;
import com.balancedteam.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 团队聊天与管理员监听管理器
 */
public class ChatManager {

    private final BalancedTeamPlugin plugin;

    // 开启监听模式的管理员 UUID 集合
    private final Set<UUID> spyPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // 开启团队聊天锁定模式的玩家 UUID 集合
    private final Set<UUID> teamChatModePlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ChatManager(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSpying(UUID uuid) {
        return spyPlayers.contains(uuid);
    }

    public boolean toggleSpy(UUID uuid) {
        if (spyPlayers.contains(uuid)) {
            spyPlayers.remove(uuid);
            return false;
        } else {
            spyPlayers.add(uuid);
            return true;
        }
    }

    public boolean isInTeamChatMode(UUID uuid) {
        return teamChatModePlayers.contains(uuid);
    }

    public boolean toggleTeamChatMode(UUID uuid) {
        if (teamChatModePlayers.contains(uuid)) {
            teamChatModePlayers.remove(uuid);
            return false;
        } else {
            teamChatModePlayers.add(uuid);
            return true;
        }
    }

    public void removePlayer(UUID uuid) {
        spyPlayers.remove(uuid);
        teamChatModePlayers.remove(uuid);
    }

    /**
     * 发送团队聊天消息并分发给队员和开启监听的管理员
     * 修复 PAPI 注入漏洞：确保先解析格式模板中的变量，最后再安全替换玩家聊天内容
     */
    public void sendTeamChat(Player sender, Team team, String message) {
        if (sender == null || team == null || message == null || message.trim().isEmpty()) {
            return;
        }
        // 全局聊天开关检测
        if (!plugin.getConfigManager().isChatEnabled()) {
            return;
        }

        TeamMember member = team.getMember(sender.getUniqueId());

        // 检查颜色代码权限：有权限则解析颜色，否则保留纯文本避免普通玩家滥用颜色/混淆代码
        String processedMessage = message;
        if (sender.hasPermission("balancedteam.chat.color")) {
            processedMessage = MessageUtil.color(processedMessage);
        }

        // 1. 发送给队内所有在线成员 (根据接收玩家客户端语言动态本地化职位 {ROLE})
        String format = plugin.getConfigManager().getChatFormat();
        for (UUID memberUuid : team.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                String roleName = member != null ? plugin.getConfigManager().getRoleDisplayName(p, member.getRole())
                        : plugin.getConfigManager().getRawMessage(p, "role.unknown");
                String prefixTemplate = format
                        .replace("{TEAM}", team.getName())
                        .replace("{ROLE}", roleName != null ? roleName : "")
                        .replace("{PLAYER}", sender.getName());
                String formattedPrefix = MessageUtil
                        .color(com.balancedteam.util.PAPIUtil.setPlaceholders(sender, prefixTemplate));
                String formattedMsg = formattedPrefix.replace("{MESSAGE}", processedMessage);
                MessageUtil.sendRawMessage(p, formattedMsg);
            }
        }

        // 2. 格式化管理员监听消息 (根据管理员客户端语言动态本地化职位 {ROLE})
        String spyFormat = plugin.getConfigManager().getSpyFormat();
        for (UUID adminUuid : spyPlayers) {
            if (!team.hasMember(adminUuid)) {
                Player admin = Bukkit.getPlayer(adminUuid);
                if (admin != null && admin.isOnline() && admin.hasPermission("balancedteam.admin.spy")) {
                    String adminRoleName = member != null ? plugin.getConfigManager().getRoleDisplayName(admin, member.getRole())
                            : plugin.getConfigManager().getRawMessage(admin, "role.unknown");
                    String rawSpyPrefix = spyFormat
                            .replace("{TEAM}", team.getName())
                            .replace("{ROLE}", adminRoleName != null ? adminRoleName : "")
                            .replace("{PLAYER}", sender.getName());
                    String spyFormattedPrefix = MessageUtil
                            .color(com.balancedteam.util.PAPIUtil.setPlaceholders(sender, rawSpyPrefix));
                    String spyFormattedMsg = spyFormattedPrefix.replace("{MESSAGE}", processedMessage);
                    MessageUtil.sendRawMessage(admin, spyFormattedMsg);
                }
            }
        }

        // 3. 终端监听 (如果开启)：使用服务端默认语言格式化职位名
        if (plugin.getConfigManager().isConsoleListenTeamChat()) {
            String consoleRoleName = member != null ? plugin.getConfigManager().getRoleDisplayName(member.getRole())
                    : plugin.getConfigManager().getRawMessage("role.unknown");
            String rawSpyPrefix = spyFormat
                    .replace("{TEAM}", team.getName())
                    .replace("{ROLE}", consoleRoleName != null ? consoleRoleName : "")
                    .replace("{PLAYER}", sender.getName());
            String consoleFormattedPrefix = MessageUtil
                    .color(com.balancedteam.util.PAPIUtil.setPlaceholders(sender, rawSpyPrefix));
            String consoleFormattedMsg = consoleFormattedPrefix.replace("{MESSAGE}", processedMessage);
            plugin.getLogger().info(consoleFormattedMsg);
        }
    }
}

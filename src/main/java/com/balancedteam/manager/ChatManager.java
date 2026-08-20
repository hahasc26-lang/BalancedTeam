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
     */
    public void sendTeamChat(Player sender, Team team, String message) {
        if (sender == null || team == null || message == null || message.trim().isEmpty()) {
            return;
        }

        TeamMember member = team.getMember(sender.getUniqueId());
        String roleName = member != null ? plugin.getConfigManager().getRoleDisplayName(member.getRole()) : plugin.getConfigManager().getRawMessage("role.unknown");

        // 格式化团队消息
        String format = plugin.getConfigManager().getChatFormat();
        String formattedMsg = MessageUtil.color(format
                .replace("{TEAM}", team.getName())
                .replace("{ROLE}", roleName)
                .replace("{PLAYER}", sender.getName())
                .replace("{MESSAGE}", message));

        // 发送给队内所有在线成员
        for (UUID memberUuid : team.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                MessageUtil.sendRawMessage(p, formattedMsg);
            }
        }

        // 格式化管理员监听消息
        String spyFormat = plugin.getConfigManager().getSpyFormat();
        String spyFormattedMsg = MessageUtil.color(spyFormat
                .replace("{TEAM}", team.getName())
                .replace("{ROLE}", roleName)
                .replace("{PLAYER}", sender.getName())
                .replace("{MESSAGE}", message));

        // 分发给所有在线且开启监听的非同队管理员
        for (UUID adminUuid : spyPlayers) {
            if (!team.hasMember(adminUuid)) {
                Player admin = Bukkit.getPlayer(adminUuid);
                if (admin != null && admin.isOnline() && admin.hasPermission("balancedteam.admin.spy")) {
                    MessageUtil.sendRawMessage(admin, spyFormattedMsg);
                }
            }
        }
    }
}

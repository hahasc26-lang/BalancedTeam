package com.balancedteam.command;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.model.Team;
import com.balancedteam.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 快捷团队聊天指令处理器 (/teammsg, /tc, /tm, /teamchat)
 */
public class TeamMsgCommand implements CommandExecutor, TabCompleter {

    private final BalancedTeamPlugin plugin;

    public TeamMsgCommand(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage("player_only"));
            return true;
        }

        Player player = (Player) sender;
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return true;
        }

        if (args.length > 0) {
            // 直接发送消息给团队
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            plugin.getChatManager().sendTeamChat(player, team, sb.toString().trim());
        } else {
            // 切换团队聊天锁定模式
            boolean enabled = plugin.getChatManager().toggleTeamChatMode(player.getUniqueId());
            String key = enabled ? "chat_team_toggle_on" : "chat_team_toggle_off";
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(key));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

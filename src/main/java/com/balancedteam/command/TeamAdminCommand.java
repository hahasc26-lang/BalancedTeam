package com.balancedteam.command;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.model.Team;
import com.balancedteam.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员指令处理器 (/teamadmin, /ta)
 */
public class TeamAdminCommand implements CommandExecutor, TabCompleter {

    private final BalancedTeamPlugin plugin;

    public TeamAdminCommand(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("balancedteam.admin")) {
            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spy":
                if (!(sender instanceof Player)) {
                    MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage("player_only"));
                    return true;
                }
                Player player = (Player) sender;
                boolean enabled = plugin.getChatManager().toggleSpy(player.getUniqueId());
                String key = enabled ? "spy_toggle_on" : "spy_toggle_off";
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(key));
                break;

            case "disband":
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage("admin_usage_disband"));
                    return true;
                }
                Team team = plugin.getTeamManager().getTeamByName(args[1]);
                if (team == null) {
                    Map<String, String> map = new HashMap<>();
                    map.put("TEAM", args[1]);
                    MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage("admin_team_not_found", map));
                    return true;
                }
                plugin.getTeamManager().disbandTeam(team).thenRun(() -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("TEAM", team.getName());
                    MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage("admin_force_disband_success", map));
                });
                break;

            case "reload":
                plugin.getConfigManager().load();
                MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage("reload_success"));
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpLines = plugin.getConfigManager().getMessageList("help.admin", Collections.emptyMap());
        for (String line : helpLines) {
            MessageUtil.sendMessage(sender, line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("balancedteam.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("spy", "disband", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && "disband".equalsIgnoreCase(args[0])) {
            return plugin.getTeamManager().getAllTeams().stream()
                    .map(t -> t.getName())
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}

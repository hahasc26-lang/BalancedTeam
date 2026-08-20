package com.balancedteam.command;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.gui.DisbandConfirmGui;
import com.balancedteam.gui.LeaveConfirmGui;
import com.balancedteam.gui.MemberManageGui;
import com.balancedteam.gui.TeamListGui;
import com.balancedteam.gui.TeamMenuGui;
import com.balancedteam.gui.TeamNotJoinedGui;
import com.balancedteam.gui.TeamSelectGui;
import com.balancedteam.model.Team;
import com.balancedteam.model.TeamMember;
import com.balancedteam.model.TeamRole;
import com.balancedteam.util.MessageUtil;
import com.balancedteam.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 玩家团队主指令处理器 (/team, /t)
 */
public class TeamCommand implements CommandExecutor, TabCompleter {

    private final BalancedTeamPlugin plugin;

    public TeamCommand(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage("player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
            if (team != null) {
                TeamMenuGui.open(plugin, player);
            } else {
                TeamNotJoinedGui.open(plugin, player);
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                sendHelp(player);
                break;
            case "list":
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }
                TeamListGui.open(plugin, player, page);
                break;
            case "menu":
            case "gui": {
                Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
                if (team != null) {
                    TeamMenuGui.open(plugin, player);
                } else {
                    TeamNotJoinedGui.open(plugin, player);
                }
                break;
            }
            case "members":
            case "member": {
                Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
                if (team != null) {
                    MemberManageGui.open(plugin, player, 1);
                } else {
                    TeamNotJoinedGui.open(plugin, player);
                }
                break;
            }
            case "create":
                handleCreate(player, args);
                break;
            case "disband":
                handleDisband(player);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "reject":
                handleReject(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "promote":
                handlePromote(player, args);
                break;
            case "demote":
                handleDemote(player, args);
                break;
            case "transfer":
                handleTransfer(player, args);
                break;
            case "chat":
            case "c":
            case "msg":
            case "message":
            case "teammsg":
            case "tc":
            case "tm":
                handleChat(player, args);
                break;
            case "ff":
            case "friendlyfire":
                handleFriendlyFire(player, args);
                break;
            case "ally":
                handleAlly(player, args);
                break;
            case "enemy":
                handleEnemy(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            default:
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("unknown_command"));
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        List<String> lines = plugin.getConfigManager().getMessageList("help.player", Collections.emptyMap());
        for (String line : lines) {
            MessageUtil.sendMessage(player, line);
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_create"));
            return;
        }

        if (plugin.getTeamManager().isPlayerInTeam(player.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_already_in_team"));
            return;
        }

        String name = args[1];
        if (!plugin.getTeamManager().isValidTeamName(name)) {
            Map<String, String> map = new HashMap<>();
            map.put("MIN", String.valueOf(plugin.getConfigManager().getNameMinLength()));
            map.put("MAX", String.valueOf(plugin.getConfigManager().getNameMaxLength()));
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_create_invalid_name", map));
            return;
        }

        if (plugin.getTeamManager().getTeamByName(name) != null) {
            Map<String, String> map = new HashMap<>();
            map.put("TEAM", name);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_create_already_exists", map));
            return;
        }

        plugin.getTeamManager().createTeam(player, name).thenAccept(team -> {
            if (team != null) {
                Map<String, String> map = new HashMap<>();
                map.put("TEAM", team.getName());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_create_success", map));
            } else {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("database_error"));
            }
        });
    }

    private void handleDisband(Player player) {
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }
        if (!team.isLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_leader"));
            return;
        }
        // 打开二次确认 GUI
        DisbandConfirmGui.open(plugin, player);
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_invite"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        TeamMember member = team.getMember(player.getUniqueId());
        if (member == null || !member.getRole().isAtLeast(TeamRole.OFFICER)) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_officer_or_leader"));
            return;
        }

        if (team.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
            Map<String, String> map = new HashMap<>();
            map.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_max_members", map));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            Map<String, String> map = new HashMap<>();
            map.put("PLAYER", args[1]);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("target_player_not_found", map));
            return;
        }

        if (plugin.getTeamManager().isPlayerInTeam(target.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_target_in_team"));
            return;
        }

        if (plugin.getInviteManager().hasValidInvite(target.getUniqueId(), team.getId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_already_sent"));
            return;
        }

        long timeout = plugin.getConfigManager().getInviteTimeout();
        plugin.getInviteManager().addInvite(target.getUniqueId(), team.getId(), player.getUniqueId(), timeout);

        Map<String, String> map = new HashMap<>();
        map.put("PLAYER", target.getName());
        map.put("TEAM", team.getName());
        map.put("TIMEOUT", String.valueOf(timeout));

        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_sent", map));
        MessageUtil.sendMessage(target, plugin.getConfigManager().getMessage("team_invite_received", map));
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_accept"));
            return;
        }

        if (plugin.getTeamManager().isPlayerInTeam(player.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_already_in_team"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team == null) {
            Map<String, String> map = new HashMap<>();
            map.put("TEAM", args[1]);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_found", map));
            return;
        }

        if (!plugin.getInviteManager().consumeInvite(player.getUniqueId(), team.getId())) {
            Map<String, String> map = new HashMap<>();
            map.put("TEAM", team.getName());
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_no_pending", map));
            return;
        }

        if (team.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
            Map<String, String> map = new HashMap<>();
            map.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_max_members", map));
            return;
        }

        plugin.getTeamManager().addMember(team, player.getUniqueId(), TeamRole.MEMBER).thenAccept(success -> {
            if (success) {
                Map<String, String> map = new HashMap<>();
                map.put("TEAM", team.getName());
                map.put("PLAYER", player.getName());

                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_join_success", map));

                for (UUID u : team.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())) {
                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("team_join_broadcast", map));
                    }
                }
            } else {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("database_error"));
            }
        });
    }

    private void handleReject(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_reject"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByName(args[1]);
        if (team != null) {
            plugin.getInviteManager().removeInvite(player.getUniqueId(), team.getId());
        }

        Map<String, String> map = new HashMap<>();
        map.put("TEAM", args[1]);
        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_rejected", map));
    }

    private void handleLeave(Player player) {
        LeaveConfirmGui.open(plugin, player);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_kick"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        TeamMember actor = team.getMember(player.getUniqueId());
        if (actor == null || !actor.getRole().isAtLeast(TeamRole.OFFICER)) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_officer_or_leader"));
            return;
        }

        TeamMember targetMember = team.getMemberByName(args[1]);
        if (targetMember == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_player_not_in_your_team"));
            return;
        }

        UUID targetUuid = targetMember.getUuid();
        if (targetUuid.equals(player.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_kick_self"));
            return;
        }

        if (targetMember.isLeader()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_kick_leader"));
            return;
        }

        if (!actor.canManage(targetMember)) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        plugin.getTeamManager().removeMember(team, targetUuid).thenAccept(success -> {
            if (success) {
                Map<String, String> map = new HashMap<>();
                map.put("PLAYER", args[1]);
                map.put("TEAM", team.getName());

                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_kick_success", map));

                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    MessageUtil.sendMessage(onlineTarget, plugin.getConfigManager().getMessage("team_kick_target_msg", map));
                }

                for (UUID u : team.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())) {
                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("team_kick_broadcast", map));
                    }
                }
            }
        });
    }

    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_promote"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        TeamMember actor = team.getMember(player.getUniqueId());
        if (actor == null || !actor.isLeader()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_leader"));
            return;
        }

        TeamMember targetMember = team.getMemberByName(args[1]);
        if (targetMember == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_player_not_in_your_team"));
            return;
        }

        UUID targetUuid = targetMember.getUuid();
        if (targetUuid.equals(player.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_member_cant_manage_self"));
            return;
        }

        if (targetMember.isOfficer()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_member_already_officer"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        plugin.getTeamManager().setMemberRole(team, targetUuid, TeamRole.OFFICER).thenAccept(success -> {
            if (success) {
                Map<String, String> map = new HashMap<>();
                map.put("PLAYER", args[1]);
                map.put("TEAM", team.getName());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_member_promote_success", map));

                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    MessageUtil.sendMessage(onlineTarget, plugin.getConfigManager().getMessage("team_member_promoted_msg", map));
                }

                for (UUID u : team.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId()) && (onlineTarget == null || !p.getUniqueId().equals(onlineTarget.getUniqueId()))) {
                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("team_member_promote_broadcast", map));
                    }
                }
            }
        });
    }

    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_demote"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        TeamMember actor = team.getMember(player.getUniqueId());
        if (actor == null || !actor.isLeader()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_leader"));
            return;
        }

        TeamMember targetMember = team.getMemberByName(args[1]);
        if (targetMember == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_player_not_in_your_team"));
            return;
        }

        UUID targetUuid = targetMember.getUuid();
        if (targetUuid.equals(player.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_member_cant_manage_self"));
            return;
        }

        if (targetMember.getRole() == TeamRole.MEMBER) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_member_already_member"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        plugin.getTeamManager().setMemberRole(team, targetUuid, TeamRole.MEMBER).thenAccept(success -> {
            if (success) {
                Map<String, String> map = new HashMap<>();
                map.put("PLAYER", args[1]);
                map.put("TEAM", team.getName());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_member_demote_success", map));

                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    MessageUtil.sendMessage(onlineTarget, plugin.getConfigManager().getMessage("team_member_demoted_msg", map));
                }

                for (UUID u : team.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId()) && (onlineTarget == null || !p.getUniqueId().equals(onlineTarget.getUniqueId()))) {
                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("team_member_demote_broadcast", map));
                    }
                }
            }
        });
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_transfer"));
            return;
        }

        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        TeamMember actor = team.getMember(player.getUniqueId());
        if (actor == null || !actor.isLeader()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_leader"));
            return;
        }

        TeamMember targetMember = team.getMemberByName(args[1]);
        if (targetMember == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_player_not_in_your_team"));
            return;
        }

        UUID targetUuid = targetMember.getUuid();
        if (targetUuid.equals(player.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_member_cant_manage_self"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        plugin.getTeamManager().transferLeader(team, targetUuid).thenAccept(success -> {
            if (success) {
                Map<String, String> map = new HashMap<>();
                map.put("PLAYER", args[1]);
                map.put("TEAM", team.getName());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_transfer_success", map));

                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    MessageUtil.sendMessage(onlineTarget, plugin.getConfigManager().getMessage("team_transfer_target_msg", map));
                }

                for (UUID u : team.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId()) && (onlineTarget == null || !p.getUniqueId().equals(onlineTarget.getUniqueId()))) {
                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("team_transfer_broadcast", map));
                    }
                }
            }
        });
    }

    private void handleChat(Player player, String[] args) {
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        if (args.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            plugin.getChatManager().sendTeamChat(player, team, sb.toString().trim());
        } else {
            boolean enabled = plugin.getChatManager().toggleTeamChatMode(player.getUniqueId());
            String key = enabled ? "chat_team_toggle_on" : "chat_team_toggle_off";
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(key));
        }
    }

    private void handleFriendlyFire(Player player, String[] args) {
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        TeamMember member = team.getMember(player.getUniqueId());
        boolean isLeader = (member != null && member.getRole() == TeamRole.LEADER);

        if (args.length < 2) {
            String status = plugin.getConfigManager().isFriendlyFireActive(team)
                    ? plugin.getConfigManager().getRawMessage("status.on")
                    : plugin.getConfigManager().getRawMessage("status.off");
            Map<String, String> map = new HashMap<>();
            map.put("STATUS", status);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_ff_status", map));
            return;
        }

        if (!plugin.getConfigManager().isAllowFriendlyFireToggle()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_ff_toggle_disabled"));
            return;
        }

        if (!isLeader) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_leader"));
            return;
        }

        long cd = plugin.getTeamManager().getFriendlyFireCooldownRemaining(player.getUniqueId());
        if (cd > 0) {
            Map<String, String> map = new HashMap<>();
            map.put("TIME", String.valueOf(cd));
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("cooldown", map));
            return;
        }

        boolean enable = "on".equalsIgnoreCase(args[1]) || "true".equalsIgnoreCase(args[1]) || "1".equals(args[1]);
        plugin.getTeamManager().setFriendlyFire(team, enable, player.getUniqueId()).thenAccept(success -> {
            if (success) {
                String key = enable ? "team_ff_toggle_on" : "team_ff_toggle_off";
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(key));
            }
        });
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_ally"));
            return;
        }

        Team myTeam = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (myTeam == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        TeamMember member = myTeam.getMember(player.getUniqueId());
        if (member == null || member.getRole() != TeamRole.LEADER) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_leader"));
            return;
        }

        String action = args[1].toLowerCase();
        if (args.length < 3) {
            if ("add".equals(action)) {
                TeamSelectGui.open(plugin, player, TeamSelectGui.SelectMode.ALLY, 1);
                return;
            }
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_specify_target"));
            return;
        }

        String targetTeamName = args[2];
        Team targetTeam = plugin.getTeamManager().getTeamByName(targetTeamName);
        if (targetTeam == null) {
            Map<String, String> map = new HashMap<>();
            map.put("TEAM", targetTeamName);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_found", map));
            return;
        }

        if (myTeam.getId() == targetTeam.getId()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_cant_ally_self"));
            return;
        }

        switch (action) {
            case "add": {
                if (plugin.getRelationManager().isAlly(myTeam.getId(), targetTeam.getId())) {
                    Map<String, String> map = new HashMap<>();
                    map.put("TEAM", targetTeam.getName());
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_already_allied", map));
                    return;
                }

                if (plugin.getRelationManager().getAllies(myTeam.getId()).size() >= plugin.getConfigManager().getMaxAllies()) {
                    Map<String, String> map = new HashMap<>();
                    map.put("MAX", String.valueOf(plugin.getConfigManager().getMaxAllies()));
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_max_reached", map));
                    return;
                }

                if (plugin.getRelationManager().getAllies(targetTeam.getId()).size() >= plugin.getConfigManager().getMaxAllies()) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_target_max_reached"));
                    return;
                }

                if (plugin.getRelationManager().hasPendingAllyRequest(myTeam.getId(), targetTeam.getId())) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_already_requested"));
                    return;
                }

                long timeout = plugin.getConfigManager().getAllyRequestTimeout();
                plugin.getRelationManager().sendAllyRequest(myTeam.getId(), targetTeam.getId(), timeout);

                Map<String, String> map = new HashMap<>();
                map.put("TEAM", targetTeam.getName());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_request_sent", map));

                Player targetLeader = Bukkit.getPlayer(targetTeam.getLeaderUuid());
                if (targetLeader != null && targetLeader.isOnline()) {
                    Map<String, String> reqMap = new HashMap<>();
                    reqMap.put("TEAM", myTeam.getName());
                    MessageUtil.sendMessage(targetLeader, plugin.getConfigManager().getMessage("ally_request_received", reqMap));
                }
                break;
            }
            case "accept": {
                if (!plugin.getRelationManager().hasPendingAllyRequest(targetTeam.getId(), myTeam.getId())) {
                    Map<String, String> map = new HashMap<>();
                    map.put("TEAM", targetTeam.getName());
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_no_pending_request", map));
                    return;
                }

                plugin.getRelationManager().acceptAllyRequest(targetTeam.getId(), myTeam.getId()).thenAccept(success -> {
                    if (success) {
                        Map<String, String> map = new HashMap<>();
                        map.put("TEAM", targetTeam.getName());
                        map.put("TEAM1", myTeam.getName());
                        map.put("TEAM2", targetTeam.getName());

                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_established", map));

                        Player targetLeader = Bukkit.getPlayer(targetTeam.getLeaderUuid());
                        if (targetLeader != null && targetLeader.isOnline()) {
                            Map<String, String> m2 = new HashMap<>();
                            m2.put("TEAM", myTeam.getName());
                            MessageUtil.sendMessage(targetLeader, plugin.getConfigManager().getMessage("ally_established", m2));
                        }
                    }
                });
                break;
            }
            case "remove": {
                if (!plugin.getRelationManager().isAlly(myTeam.getId(), targetTeam.getId())) {
                    Map<String, String> map = new HashMap<>();
                    map.put("TEAM", targetTeam.getName());
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_not_allied", map));
                    return;
                }

                plugin.getRelationManager().removeAlly(myTeam.getId(), targetTeam.getId()).thenAccept(success -> {
                    if (success) {
                        Map<String, String> map = new HashMap<>();
                        map.put("TEAM", targetTeam.getName());
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_remove_success", map));
                    }
                });
                break;
            }
            default:
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_unknown_sub"));
                break;
        }
    }

    private void handleEnemy(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("usage_enemy"));
            return;
        }

        Team myTeam = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (myTeam == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        TeamMember member = myTeam.getMember(player.getUniqueId());
        if (member == null || !member.getRole().isAtLeast(TeamRole.OFFICER)) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_officer_or_leader"));
            return;
        }

        String action = args[1].toLowerCase();
        if (args.length < 3) {
            if ("add".equals(action)) {
                TeamSelectGui.open(plugin, player, TeamSelectGui.SelectMode.ENEMY, 1);
                return;
            }
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_specify_target"));
            return;
        }

        String targetTeamName = args[2];
        Team targetTeam = plugin.getTeamManager().getTeamByName(targetTeamName);
        if (targetTeam == null) {
            Map<String, String> map = new HashMap<>();
            map.put("TEAM", targetTeamName);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_found", map));
            return;
        }

        if (myTeam.getId() == targetTeam.getId()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_add_self"));
            return;
        }

        if (action.equals("add")) {
            if (plugin.getRelationManager().isAlly(myTeam.getId(), targetTeam.getId())) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_cant_enemy_ally"));
                return;
            }

            if (plugin.getRelationManager().isEnemy(myTeam.getId(), targetTeam.getId())) {
                Map<String, String> map = new HashMap<>();
                map.put("TEAM", targetTeam.getName());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_already_enemy", map));
                return;
            }

            if (plugin.getRelationManager().getEnemies(myTeam.getId()).size() >= plugin.getConfigManager().getMaxEnemies()) {
                Map<String, String> map = new HashMap<>();
                map.put("MAX", String.valueOf(plugin.getConfigManager().getMaxEnemies()));
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_max_reached", map));
                return;
            }

            plugin.getRelationManager().addEnemy(myTeam.getId(), targetTeam.getId()).thenAccept(success -> {
                if (success) {
                    Map<String, String> map = new HashMap<>();
                    map.put("TEAM", targetTeam.getName());
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_add_success", map));
                }
            });
        } else if (action.equals("remove")) {
            if (!plugin.getRelationManager().isEnemy(myTeam.getId(), targetTeam.getId())) {
                Map<String, String> map = new HashMap<>();
                map.put("TEAM", targetTeam.getName());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_not_enemy", map));
                return;
            }

            plugin.getRelationManager().removeEnemy(myTeam.getId(), targetTeam.getId()).thenAccept(success -> {
                if (success) {
                    Map<String, String> map = new HashMap<>();
                    map.put("TEAM", targetTeam.getName());
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_remove_success", map));
                }
            });
        }
    }

    private void handleInfo(Player player, String[] args) {
        Team team;
        if (args.length > 1) {
            team = plugin.getTeamManager().getTeamByName(args[1]);
        } else {
            team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        }

        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(args.length > 1 ? "team_not_found" : "team_not_in_team"));
            return;
        }

        OfflinePlayer leader = Bukkit.getOfflinePlayer(team.getLeaderUuid());
        String leaderName = leader.getName() != null ? leader.getName() : "未知";

        List<Integer> allyIds = plugin.getRelationManager().getAllies(team.getId());
        List<Integer> enemyIds = plugin.getRelationManager().getEnemies(team.getId());

        String ffStatus = plugin.getConfigManager().isFriendlyFireActive(team)
                ? plugin.getConfigManager().getRawMessage("status.on")
                : plugin.getConfigManager().getRawMessage("status.off");

        Map<String, String> map = new HashMap<>();
        map.put("TEAM", team.getName());
        map.put("LEADER", leaderName);
        map.put("COUNT", String.valueOf(team.getMemberCount()));
        map.put("FF", ffStatus);
        map.put("ALLIES", String.valueOf(allyIds.size()));
        map.put("ENEMIES", String.valueOf(enemyIds.size()));
        map.put("DATE", TimeUtil.formatDate(team.getCreatedAt()));

        List<String> lines = plugin.getConfigManager().getMessageList("team_info", map);
        for (String line : lines) {
            MessageUtil.sendMessage(player, line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList("help", "list", "menu", "gui", "members", "create", "disband", "invite", "accept", "reject", "leave", "kick", "promote", "demote", "transfer", "chat", "msg", "ff", "ally", "enemy", "info"));
            return filter(list, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "invite":
                    return filter(Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).collect(Collectors.toList()), args[1]);
                case "kick":
                case "promote":
                case "demote":
                case "transfer":
                    if (sender instanceof Player) {
                        Team team = plugin.getTeamManager().getTeamByPlayer(((Player) sender).getUniqueId());
                        if (team != null) {
                            return filter(team.getMembers().keySet().stream()
                                    .map(id -> Bukkit.getOfflinePlayer(id))
                                    .map(op -> op.getName())
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList()), args[1]);
                        }
                    }
                    break;
                case "ff":
                    return filter(Arrays.asList("on", "off"), args[1]);
                case "ally":
                    return filter(Arrays.asList("add", "accept", "remove"), args[1]);
                case "enemy":
                    return filter(Arrays.asList("add", "remove"), args[1]);
                case "accept":
                case "reject":
                case "info":
                    return filter(plugin.getTeamManager().getAllTeams().stream().map(t -> t.getName()).collect(Collectors.toList()), args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if ("ally".equals(sub) || "enemy".equals(sub)) {
                return filter(plugin.getTeamManager().getAllTeams().stream().map(t -> t.getName()).collect(Collectors.toList()), args[2]);
            }
        }

        return list;
    }

    private List<String> filter(List<String> raw, String prefix) {
        if (prefix == null || prefix.isEmpty()) return raw;
        return raw.stream().filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}

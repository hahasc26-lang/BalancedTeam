package com.balancedteam.expansion;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.model.Team;
import com.balancedteam.model.TeamMember;
import com.balancedteam.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/**
 * BalancedTeam 插件 PlaceholderAPI 占位符扩展
 * 标识符: %balancedteam_<params>%
 */
public class BalancedTeamExpansion extends PlaceholderExpansion {

    private final BalancedTeamPlugin plugin;

    public BalancedTeamExpansion(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "balancedteam";
    }

    @Override
    public String getAuthor() {
        if (plugin.getDescription().getAuthors().isEmpty()) {
            return "haha";
        }
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        String lower = params.toLowerCase(Locale.ROOT);

        // =========================================================================
        // 1. 全局服务端团队统计与指定队伍查询占位符 (不强依赖当前玩家是否在线/加入团队)
        // =========================================================================

        // 全服团队总数
        if (lower.equals("total_teams")) {
            return String.valueOf(plugin.getTeamManager().getAllTeams().size());
        }

        // 全服加入团队的总玩家数
        if (lower.equals("total_members")) {
            int total = 0;
            for (Team t : plugin.getTeamManager().getAllTeams()) {
                if (t != null) {
                    total += t.getMemberCount();
                }
            }
            return String.valueOf(total);
        }

        // 指定团队信息查询: %balancedteam_team_<field>_<teamName>%
        if (lower.startsWith("team_")) {
            String sub = params.substring(5); // 去掉 "team_"

            // %balancedteam_team_exists_<teamName>%
            if (sub.toLowerCase(Locale.ROOT).startsWith("exists_")) {
                String targetTeamName = sub.substring(7);
                return plugin.getTeamManager().getTeamByName(targetTeamName) != null ? "true" : "false";
            }

            // %balancedteam_team_leader_<teamName>%
            if (sub.toLowerCase(Locale.ROOT).startsWith("leader_")) {
                String targetTeamName = sub.substring(7);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                if (team != null && team.getLeaderUuid() != null) {
                    return getPlayerNameByUuid(team.getLeaderUuid());
                }
                return "";
            }

            // %balancedteam_team_members_<teamName>% 或 %balancedteam_team_member_count_<teamName>%
            if (sub.toLowerCase(Locale.ROOT).startsWith("members_")) {
                String targetTeamName = sub.substring(8);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return team != null ? String.valueOf(team.getMemberCount()) : "0";
            }
            if (sub.toLowerCase(Locale.ROOT).startsWith("member_count_")) {
                String targetTeamName = sub.substring(13);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return team != null ? String.valueOf(team.getMemberCount()) : "0";
            }

            // %balancedteam_team_online_<teamName>% 或 %balancedteam_team_online_count_<teamName>%
            if (sub.toLowerCase(Locale.ROOT).startsWith("online_")) {
                String targetTeamName = sub.substring(7);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return team != null ? String.valueOf(getOnlineMemberCount(team)) : "0";
            }
            if (sub.toLowerCase(Locale.ROOT).startsWith("online_count_")) {
                String targetTeamName = sub.substring(13);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return team != null ? String.valueOf(getOnlineMemberCount(team)) : "0";
            }

            // %balancedteam_team_desc_<teamName>% 或 %balancedteam_team_description_<teamName>%
            if (sub.toLowerCase(Locale.ROOT).startsWith("desc_")) {
                String targetTeamName = sub.substring(5);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return (team != null && team.getDescription() != null) ? team.getDescription() : "";
            }
            if (sub.toLowerCase(Locale.ROOT).startsWith("description_")) {
                String targetTeamName = sub.substring(12);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return (team != null && team.getDescription() != null) ? team.getDescription() : "";
            }

            // %balancedteam_team_created_<teamName>% 或 %balancedteam_team_created_at_<teamName>%
            if (sub.toLowerCase(Locale.ROOT).startsWith("created_")) {
                String targetTeamName = sub.substring(8);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return (team != null && team.getCreatedAt() != null) ? TimeUtil.formatDate(team.getCreatedAt()) : "";
            }
            if (sub.toLowerCase(Locale.ROOT).startsWith("created_at_")) {
                String targetTeamName = sub.substring(11);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return (team != null && team.getCreatedAt() != null) ? TimeUtil.formatDate(team.getCreatedAt()) : "";
            }

            // %balancedteam_team_ff_<teamName>% 或 %balancedteam_team_friendly_fire_<teamName>%
            if (sub.toLowerCase(Locale.ROOT).startsWith("ff_")) {
                String targetTeamName = sub.substring(3);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return (team != null && team.isFriendlyFire()) ? "true" : "false";
            }
            if (sub.toLowerCase(Locale.ROOT).startsWith("friendly_fire_")) {
                String targetTeamName = sub.substring(14);
                Team team = plugin.getTeamManager().getTeamByName(targetTeamName);
                return (team != null && team.isFriendlyFire()) ? "true" : "false";
            }
        }

        // 如果玩家为空，后续基于玩家的占位符无法获取，返回空
        if (player == null) {
            return "";
        }

        Player onlinePlayer = player.getPlayer();
        UUID playerUuid = player.getUniqueId();
        Team team = plugin.getTeamManager().getTeamByPlayer(playerUuid);

        // =========================================================================
        // 2. 玩家所属团队状态及基本占位符
        // =========================================================================

        // 是否在团队中
        if (lower.equals("in_team") || lower.equals("has_team")) {
            return team != null ? "true" : "false";
        }

        // 如果玩家未加入团队，处理各种兜底值
        if (team == null) {
            switch (lower) {
                case "name":
                case "team_name":
                case "id":
                case "team_id":
                case "tag":
                case "leader":
                case "leader_name":
                case "leader_uuid":
                case "description":
                case "team_desc":
                case "team_description":
                case "created_at":
                case "team_created_at":
                case "joined_at":
                case "allies_list":
                case "enemies_list":
                    return "";

                case "role":
                case "member_role":
                case "role_name":
                    return plugin.getConfigManager().getRawMessage(onlinePlayer, "role.unknown");

                case "role_raw":
                    return "NONE";

                case "role_level":
                case "members":
                case "member_count":
                case "online":
                case "online_count":
                case "allies":
                case "ally_count":
                case "enemies":
                case "enemy_count":
                    return "0";

                case "is_leader":
                case "is_officer":
                case "friendly_fire":
                case "ff":
                    return "false";

                case "friendly_fire_formatted":
                case "ff_formatted":
                    return "关闭";

                case "max_members":
                    return String.valueOf(plugin.getConfigManager().getMaxMembers());

                case "max_allies":
                    return String.valueOf(plugin.getConfigManager().getMaxAllies());

                case "max_enemies":
                    return String.valueOf(plugin.getConfigManager().getMaxEnemies());

                default:
                    // 玩家无团队时的关系查询统一返回 NONE 或 false
                    if (lower.startsWith("relation_")) return "NONE";
                    if (lower.startsWith("is_ally_") || lower.startsWith("is_enemy_") || lower.startsWith("is_same_team_")) return "false";
                    return "";
            }
        }

        TeamMember member = team.getMember(playerUuid);

        // =========================================================================
        // 3. 玩家所在团队的详细信息
        // =========================================================================

        switch (lower) {
            case "name":
            case "team_name":
                return team.getName();

            case "id":
            case "team_id":
                return String.valueOf(team.getId());

            case "tag":
                return "[" + team.getName() + "]";

            case "leader":
            case "leader_name":
                return team.getLeaderUuid() != null ? getPlayerNameByUuid(team.getLeaderUuid()) : "";

            case "leader_uuid":
                return team.getLeaderUuid() != null ? team.getLeaderUuid().toString() : "";

            case "is_leader":
                return team.isLeader(playerUuid) ? "true" : "false";

            case "is_officer":
                return team.isOfficerOrLeader(playerUuid) ? "true" : "false";

            case "role":
            case "member_role":
            case "role_name":
                return member != null 
                        ? plugin.getConfigManager().getRoleDisplayName(onlinePlayer, member.getRole()) 
                        : plugin.getConfigManager().getRawMessage(onlinePlayer, "role.unknown");

            case "role_raw":
                return (member != null && member.getRole() != null) ? member.getRole().name() : "NONE";

            case "role_level":
                return String.valueOf((member != null && member.getRole() != null) ? member.getRole().getLevel() : 0);

            case "description":
            case "team_desc":
            case "team_description":
                return team.getDescription() != null ? team.getDescription() : "";

            case "friendly_fire":
            case "ff":
                return team.isFriendlyFire() ? "true" : "false";

            case "friendly_fire_formatted":
            case "ff_formatted":
                return team.isFriendlyFire() ? "开启" : "关闭";

            case "created_at":
            case "team_created_at":
                return team.getCreatedAt() != null ? TimeUtil.formatDate(team.getCreatedAt()) : "";

            case "joined_at":
                return (member != null && member.getJoinedAt() != null) ? TimeUtil.formatDate(member.getJoinedAt()) : "";

            case "members":
            case "member_count":
                return String.valueOf(team.getMemberCount());

            case "max_members":
                return String.valueOf(plugin.getConfigManager().getMaxMembers());

            case "online":
            case "online_count":
                return String.valueOf(getOnlineMemberCount(team));

            case "allies":
            case "ally_count":
                return String.valueOf(plugin.getRelationManager().getAllies(team.getId()).size());

            case "max_allies":
                return String.valueOf(plugin.getConfigManager().getMaxAllies());

            case "enemies":
            case "enemy_count":
                return String.valueOf(plugin.getRelationManager().getEnemies(team.getId()).size());

            case "max_enemies":
                return String.valueOf(plugin.getConfigManager().getMaxEnemies());

            case "allies_list": {
                java.util.List<Integer> allyIds = plugin.getRelationManager().getAllies(team.getId());
                java.util.List<String> names = new java.util.ArrayList<>();
                for (Integer allyId : allyIds) {
                    if (allyId != null) {
                        Team allyTeam = plugin.getTeamManager().getTeamById(allyId);
                        if (allyTeam != null) {
                            names.add(allyTeam.getName());
                        }
                    }
                }
                return String.join(", ", names);
            }

            case "enemies_list": {
                java.util.List<Integer> enemyIds = plugin.getRelationManager().getEnemies(team.getId());
                java.util.List<String> names = new java.util.ArrayList<>();
                for (Integer enemyId : enemyIds) {
                    if (enemyId != null) {
                        Team enemyTeam = plugin.getTeamManager().getTeamById(enemyId);
                        if (enemyTeam != null) {
                            names.add(enemyTeam.getName());
                        }
                    }
                }
                return String.join(", ", names);
            }
        }

        // =========================================================================
        // 4. 外交关系与玩家间关系判断占位符
        // =========================================================================

        // 与指定团队的关系: %balancedteam_relation_team_<targetTeamName>%
        if (lower.startsWith("relation_team_")) {
            String targetTeamName = params.substring("relation_team_".length());
            Team targetTeam = plugin.getTeamManager().getTeamByName(targetTeamName);
            if (targetTeam == null) return "NONE";
            if (targetTeam.getId() == team.getId()) return "SAME_TEAM";
            if (plugin.getRelationManager().isAlly(team.getId(), targetTeam.getId())) return "ALLY";
            if (plugin.getRelationManager().isEnemy(team.getId(), targetTeam.getId())) return "ENEMY";
            return "NONE";
        }

        // 与指定玩家的关系: %balancedteam_relation_<targetPlayerName>%
        if (lower.startsWith("relation_")) {
            String targetPlayerName = params.substring("relation_".length());
            UUID targetUuid = getUuidByPlayerName(targetPlayerName);
            if (targetUuid == null) return "NONE";
            Team targetTeam = plugin.getTeamManager().getTeamByPlayer(targetUuid);
            if (targetTeam == null) return "NONE";
            if (targetTeam.getId() == team.getId()) return "SAME_TEAM";
            if (plugin.getRelationManager().isAlly(team.getId(), targetTeam.getId())) return "ALLY";
            if (plugin.getRelationManager().isEnemy(team.getId(), targetTeam.getId())) return "ENEMY";
            return "NONE";
        }

        // %balancedteam_is_ally_<targetPlayerName>%
        if (lower.startsWith("is_ally_")) {
            String targetPlayerName = params.substring("is_ally_".length());
            UUID targetUuid = getUuidByPlayerName(targetPlayerName);
            if (targetUuid == null) return "false";
            Team targetTeam = plugin.getTeamManager().getTeamByPlayer(targetUuid);
            if (targetTeam == null) return "false";
            return plugin.getRelationManager().isAlly(team.getId(), targetTeam.getId()) ? "true" : "false";
        }

        // %balancedteam_is_enemy_<targetPlayerName>%
        if (lower.startsWith("is_enemy_")) {
            String targetPlayerName = params.substring("is_enemy_".length());
            UUID targetUuid = getUuidByPlayerName(targetPlayerName);
            if (targetUuid == null) return "false";
            Team targetTeam = plugin.getTeamManager().getTeamByPlayer(targetUuid);
            if (targetTeam == null) return "false";
            return plugin.getRelationManager().isEnemy(team.getId(), targetTeam.getId()) ? "true" : "false";
        }

        // %balancedteam_is_same_team_<targetPlayerName>%
        if (lower.startsWith("is_same_team_")) {
            String targetPlayerName = params.substring("is_same_team_".length());
            UUID targetUuid = getUuidByPlayerName(targetPlayerName);
            if (targetUuid == null) return "false";
            return team.hasMember(targetUuid) ? "true" : "false";
        }

        return null;
    }

    /**
     * 计算团队当前在线成员数量
     */
    private int getOnlineMemberCount(Team team) {
        if (team == null) return 0;
        int count = 0;
        for (UUID memberUuid : team.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(memberUuid);
            if (p != null && p.isOnline()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 安全根据 UUID 获取玩家名称
     */
    private String getPlayerNameByUuid(UUID uuid) {
        if (uuid == null) return "";
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        if (offline.getName() != null) {
            return offline.getName();
        }
        return uuid.toString();
    }

    /**
     * 根据玩家名查找 UUID（优先在线玩家匹配，其次离线团队成员缓存）
     */
    private UUID getUuidByPlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;
        Player online = Bukkit.getPlayerExact(playerName);
        if (online != null) {
            return online.getUniqueId();
        }
        // 遍历全服团队成员缓存以高效精准匹配已有团队记录的玩家
        for (Team t : plugin.getTeamManager().getAllTeams()) {
            if (t != null) {
                TeamMember m = t.getMemberByName(playerName);
                if (m != null) {
                    return m.getUuid();
                }
            }
        }
        return null;
    }
}

package com.balancedteam.manager;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.database.dao.MemberDao;
import com.balancedteam.database.dao.TeamDao;
import com.balancedteam.model.Team;
import com.balancedteam.model.TeamMember;
import com.balancedteam.model.TeamRole;
import com.balancedteam.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 团队核心业务与内存缓存管理器
 */
public class TeamManager {

    private final BalancedTeamPlugin plugin;
    private final TeamDao teamDao;
    private final MemberDao memberDao;

    // 内存双向索引缓存
    private final Map<Integer, Team> teamsById = new ConcurrentHashMap<>();
    private final Map<String, Team> teamsByName = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerTeamMap = new ConcurrentHashMap<>();

    // 平衡性冷却限制 (UUID -> ExpireMillis)
    private final Map<UUID, Long> friendlyFireCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> leaveTeamCooldowns = new ConcurrentHashMap<>();

    public TeamManager(BalancedTeamPlugin plugin, TeamDao teamDao, MemberDao memberDao) {
        this.plugin = plugin;
        this.teamDao = teamDao;
        this.memberDao = memberDao;
    }

    /**
     * 从数据库异步全量加载并构建内存双向索引
     */
    public CompletableFuture<Void> loadAllData() {
        return teamDao.loadAllTeams().thenCompose(teams -> {
            teamsById.clear();
            teamsByName.clear();
            playerTeamMap.clear();

            for (Team team : teams) {
                teamsById.put(team.getId(), team);
                teamsByName.put(team.getName().toLowerCase(), team);
            }

            return memberDao.loadAllMembers().thenAccept(members -> {
                for (TeamMember member : members) {
                    Team team = teamsById.get(member.getTeamId());
                    if (team != null) {
                        team.addMember(member);
                        playerTeamMap.put(member.getUuid(), team.getId());
                    }
                }
            });
        });
    }

    public Collection<Team> getAllTeams() {
        return teamsById.values();
    }

    public Team getTeamById(int id) {
        return teamsById.get(id);
    }

    public Team getTeamByName(String name) {
        if (name == null) return null;
        return teamsByName.get(name.toLowerCase());
    }

    public Team getTeamByPlayer(UUID uuid) {
        Integer teamId = playerTeamMap.get(uuid);
        if (teamId == null) return null;
        return teamsById.get(teamId);
    }

    public boolean isPlayerInTeam(UUID uuid) {
        return playerTeamMap.containsKey(uuid);
    }

    /**
     * 校验团队名称是否合法
     */
    public boolean isValidTeamName(String name) {
        if (name == null) return false;
        int min = plugin.getConfigManager().getNameMinLength();
        int max = plugin.getConfigManager().getNameMaxLength();
        if (name.length() < min || name.length() > max) return false;
        String regex = plugin.getConfigManager().getNameRegex();
        return Pattern.compile(regex).matcher(name).matches();
    }

    /**
     * 创建团队
     */
    public CompletableFuture<Team> createTeam(Player leader, String name) {
        if (isPlayerInTeam(leader.getUniqueId())) {
            return CompletableFuture.completedFuture(null);
        }
        if (getTeamByName(name) != null) {
            return CompletableFuture.completedFuture(null);
        }

        boolean defaultFf = plugin.getConfigManager().isDefaultFriendlyFire();
        Team team = new Team(name, leader.getUniqueId(), defaultFf, "");

        return plugin.getDatabaseManager().executeTransaction(conn -> {
            String pfx = plugin.getDatabaseManager().getTablePrefix();
            // 1. 创建团队
            String createTeamSql = "INSERT INTO `" + pfx + "teams` (`name`, `leader_uuid`, `friendly_fire`, `description`, `created_at`) VALUES (?, ?, ?, ?, ?)";
            int teamId;
            try (PreparedStatement ps = conn.prepareStatement(createTeamSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, team.getName());
                ps.setString(2, team.getLeaderUuid().toString());
                ps.setBoolean(3, team.isFriendlyFire());
                ps.setString(4, team.getDescription());
                ps.setTimestamp(5, team.getCreatedAt());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        teamId = rs.getInt(1);
                        team.setId(teamId);
                    } else {
                        throw new com.balancedteam.database.exception.DatabaseException("创建团队事务失败，未生成主键 ID");
                    }
                }
            }

            // 2. 插入队长成员记录
            TeamMember leaderMember = new TeamMember(
                    leader.getUniqueId(),
                    teamId,
                    TeamRole.LEADER,
                    new Timestamp(System.currentTimeMillis())
            );

            String createMemberSql;
            if (plugin.getDatabaseManager().isMySQL()) {
                createMemberSql = "INSERT INTO `" + pfx + "members` (`uuid`, `team_id`, `role`, `joined_at`) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `team_id` = VALUES(`team_id`), `role` = VALUES(`role`)";
            } else {
                createMemberSql = "INSERT OR REPLACE INTO `" + pfx + "members` (`uuid`, `team_id`, `role`, `joined_at`) VALUES (?, ?, ?, ?)";
            }

            try (PreparedStatement ps = conn.prepareStatement(createMemberSql)) {
                ps.setString(1, leaderMember.getUuid().toString());
                ps.setInt(2, leaderMember.getTeamId());
                ps.setInt(3, leaderMember.getRole().getLevel());
                ps.setTimestamp(4, leaderMember.getJoinedAt());
                ps.executeUpdate();
            }

            team.addMember(leaderMember);
            return team;
        }).thenApply(createdTeam -> {
            if (createdTeam != null) {
                teamsById.put(createdTeam.getId(), createdTeam);
                teamsByName.put(createdTeam.getName().toLowerCase(), createdTeam);
                playerTeamMap.put(leader.getUniqueId(), createdTeam.getId());
            }
            return createdTeam;
        });
    }

    /**
     * 解散团队（全量级联事务删除，保证原子性）
     */
    public CompletableFuture<Void> disbandTeam(Team team) {
        if (team == null) return CompletableFuture.completedFuture(null);

        // 通知队员并清理内存
        for (UUID uuid : team.getMembers().keySet()) {
            playerTeamMap.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                Map<String, String> map = new HashMap<>();
                map.put("TEAM", team.getName());
                MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("team_disband_broadcast", map));
            }
        }

        teamsById.remove(team.getId());
        teamsByName.remove(team.getName().toLowerCase());

        // 清理外交关系、入队邀请与入队申请内存
        plugin.getRelationManager().onTeamDisbanded(team.getId());
        plugin.getInviteManager().onTeamDisbanded(team.getId());
        plugin.getApplicationManager().onTeamDisbanded(team.getId());

        // 异步在同一个事务中级联删除所有关联表数据
        return plugin.getDatabaseManager().runTransaction(conn -> {
            String pfx = plugin.getDatabaseManager().getTablePrefix();
            try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM `" + pfx + "teams` WHERE `id` = ?");
                 PreparedStatement ps2 = conn.prepareStatement("DELETE FROM `" + pfx + "members` WHERE `team_id` = ?");
                 PreparedStatement ps3 = conn.prepareStatement("DELETE FROM `" + pfx + "relations` WHERE `team_id_1` = ? OR `team_id_2` = ?");
                 PreparedStatement ps4 = conn.prepareStatement("DELETE FROM `" + pfx + "invites` WHERE `team_id` = ?");
                 PreparedStatement ps5 = conn.prepareStatement("DELETE FROM `" + pfx + "ally_requests` WHERE `from_team_id` = ? OR `to_team_id` = ?");
                 PreparedStatement ps6 = conn.prepareStatement("DELETE FROM `" + pfx + "team_applications` WHERE `team_id` = ?")) {
                ps1.setInt(1, team.getId());
                ps1.executeUpdate();

                ps2.setInt(1, team.getId());
                ps2.executeUpdate();

                ps3.setInt(1, team.getId());
                ps3.setInt(2, team.getId());
                ps3.executeUpdate();

                ps4.setInt(1, team.getId());
                ps4.executeUpdate();

                ps5.setInt(1, team.getId());
                ps5.setInt(2, team.getId());
                ps5.executeUpdate();

                ps6.setInt(1, team.getId());
                ps6.executeUpdate();
            }
        });
    }

    /**
     * 添加成员
     */
    public CompletableFuture<Boolean> addMember(Team team, UUID playerUuid, TeamRole role) {
        if (team == null || isPlayerInTeam(playerUuid)) {
            return CompletableFuture.completedFuture(false);
        }

        TeamMember member = new TeamMember(
                playerUuid,
                team.getId(),
                role,
                new Timestamp(System.currentTimeMillis())
        );

        return memberDao.saveMember(member).thenApply(v -> {
            team.addMember(member);
            playerTeamMap.put(playerUuid, team.getId());
            // 清理该玩家加入队伍前在其他队伍的所有未决申请
            plugin.getApplicationManager().onPlayerJoinedTeam(playerUuid);
            return true;
        });
    }

    /**
     * 移除成员
     */
    public CompletableFuture<Boolean> removeMember(Team team, UUID playerUuid) {
        if (team == null || !team.hasMember(playerUuid)) {
            return CompletableFuture.completedFuture(false);
        }

        return memberDao.deleteMember(playerUuid).thenApply(v -> {
            team.removeMember(playerUuid);
            playerTeamMap.remove(playerUuid);

            // 记录退队冷却时间
            long cooldown = plugin.getConfigManager().getLeaveTeamCooldown();
            if (cooldown > 0) {
                leaveTeamCooldowns.put(playerUuid, System.currentTimeMillis() + (cooldown * 1000L));
            }
            return true;
        });
    }

    /**
     * 切换友伤状态
     */
    public CompletableFuture<Boolean> setFriendlyFire(Team team, boolean enabled, UUID actorUuid) {
        if (team == null) return CompletableFuture.completedFuture(false);

        // 检查友伤切换冷却
        if (actorUuid != null) {
            Long expire = friendlyFireCooldowns.get(actorUuid);
            if (expire != null && System.currentTimeMillis() < expire) {
                return CompletableFuture.completedFuture(false);
            }
        }

        team.setFriendlyFire(enabled);
        return teamDao.updateTeam(team).thenApply(v -> {
            if (actorUuid != null) {
                long cooldown = plugin.getConfigManager().getFriendlyFireCooldown();
                if (cooldown > 0) {
                    friendlyFireCooldowns.put(actorUuid, System.currentTimeMillis() + (cooldown * 1000L));
                }
            }
            return true;
        });
    }

    /**
     * 获取友伤切换剩余冷却时间 (秒)
     */
    public long getFriendlyFireCooldownRemaining(UUID uuid) {
        Long expire = friendlyFireCooldowns.get(uuid);
        if (expire == null) return 0;
        long diff = expire - System.currentTimeMillis();
        return diff > 0 ? (diff / 1000L) : 0;
    }

    /**
     * 设置成员职位（如普通队员与管理员之间的升降职）
     */
    public CompletableFuture<Boolean> setMemberRole(Team team, UUID targetUuid, TeamRole newRole) {
        if (team == null || !team.hasMember(targetUuid) || newRole == null) {
            return CompletableFuture.completedFuture(false);
        }

        TeamMember member = team.getMember(targetUuid);
        if (member == null) {
            return CompletableFuture.completedFuture(false);
        }

        member.setRole(newRole);
        return memberDao.saveMember(member).thenApply(v -> true);
    }

    /**
     * 转让队长职位
     * @param team 目标团队
     * @param newLeaderUuid 新队长 UUID
     */
    public CompletableFuture<Boolean> transferLeader(Team team, UUID newLeaderUuid) {
        if (team == null || !team.hasMember(newLeaderUuid)) {
            return CompletableFuture.completedFuture(false);
        }

        UUID oldLeaderUuid = team.getLeaderUuid();
        if (oldLeaderUuid == null || oldLeaderUuid.equals(newLeaderUuid)) {
            return CompletableFuture.completedFuture(false);
        }

        TeamMember oldLeaderMember = team.getMember(oldLeaderUuid);
        TeamMember newLeaderMember = team.getMember(newLeaderUuid);
        if (oldLeaderMember == null || newLeaderMember == null) {
            return CompletableFuture.completedFuture(false);
        }

        // 更新内存状态
        oldLeaderMember.setRole(TeamRole.OFFICER);
        newLeaderMember.setRole(TeamRole.LEADER);
        team.setLeaderUuid(newLeaderUuid);

        // 异步在同一个事务中原子性更新队伍队长与两名成员角色
        return plugin.getDatabaseManager().runTransaction(conn -> {
            String pfx = plugin.getDatabaseManager().getTablePrefix();
            try (PreparedStatement psTeam = conn.prepareStatement("UPDATE `" + pfx + "teams` SET `leader_uuid` = ? WHERE `id` = ?");
                 PreparedStatement psOld = conn.prepareStatement("UPDATE `" + pfx + "members` SET `role` = ? WHERE `uuid` = ?");
                 PreparedStatement psNew = conn.prepareStatement("UPDATE `" + pfx + "members` SET `role` = ? WHERE `uuid` = ?")) {
                psTeam.setString(1, newLeaderUuid.toString());
                psTeam.setInt(2, team.getId());
                psTeam.executeUpdate();

                psOld.setInt(1, TeamRole.OFFICER.getLevel());
                psOld.setString(2, oldLeaderUuid.toString());
                psOld.executeUpdate();

                psNew.setInt(1, TeamRole.LEADER.getLevel());
                psNew.setString(2, newLeaderUuid.toString());
                psNew.executeUpdate();
            }
        }).thenApply(v -> true);
    }

    /**
     * 获取退队剩余冷却时间 (秒)
     */
    public long getLeaveTeamCooldownRemaining(UUID uuid) {
        Long expire = leaveTeamCooldowns.get(uuid);
        if (expire == null) return 0;
        long diff = expire - System.currentTimeMillis();
        return diff > 0 ? (diff / 1000L) : 0;
    }
}

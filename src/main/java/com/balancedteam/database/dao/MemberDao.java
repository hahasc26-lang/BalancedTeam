package com.balancedteam.database.dao;

import com.balancedteam.database.DatabaseManager;
import com.balancedteam.database.exception.DatabaseException;
import com.balancedteam.model.TeamMember;
import com.balancedteam.model.TeamRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 团队成员数据访问对象
 * 数据库存储权限等级数字：3 为队长，2 为管理员，1 为队员
 */
public class MemberDao {

    private final DatabaseManager databaseManager;

    public MemberDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private String getTableName() {
        return databaseManager.getTablePrefix() + "members";
    }

    /**
     * 插入或更新成员信息（存储权限等级数字 1/2/3）
     */
    public CompletableFuture<Void> saveMember(TeamMember member) {
        return databaseManager.runAsync(() -> {
            String sql;
            if (databaseManager.isMySQL()) {
                sql = "INSERT INTO `" + getTableName() + "` (`uuid`, `team_id`, `role`, `joined_at`) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE `team_id` = VALUES(`team_id`), `role` = VALUES(`role`)";
            } else {
                sql = "INSERT OR REPLACE INTO `" + getTableName() + "` (`uuid`, `team_id`, `role`, `joined_at`) VALUES (?, ?, ?, ?)";
            }
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, member.getUuid().toString());
                ps.setInt(2, member.getTeamId());
                ps.setInt(3, member.getRole().getLevel());
                ps.setTimestamp(4, member.getJoinedAt());

                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("保存成员信息失败 (UUID: " + member.getUuid() + ")", e);
            }
        });
    }

    /**
     * 移除成员
     */
    public CompletableFuture<Void> deleteMember(UUID uuid) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `uuid` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除成员失败 (UUID: " + uuid + ")", e);
            }
        });
    }

    /**
     * 移除指定团队的所有成员
     */
    public CompletableFuture<Void> deleteMembersByTeamId(int teamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `team_id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, teamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除团队成员失败 (TeamId: " + teamId + ")", e);
            }
        });
    }

    /**
     * 按 UUID 查询指定成员
     */
    public CompletableFuture<Optional<TeamMember>> findMemberByUuid(UUID uuid) {
        return databaseManager.supplyAsync(() -> {
            String sql = "SELECT `uuid`, `team_id`, `role`, `joined_at` FROM `" + getTableName() + "` WHERE `uuid` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToMember(rs));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new DatabaseException("查询成员失败 (UUID: " + uuid + ")", e);
            }
        });
    }

    /**
     * 按 TeamId 查询该队伍所有成员
     */
    public CompletableFuture<List<TeamMember>> findMembersByTeamId(int teamId) {
        return databaseManager.supplyAsync(() -> {
            List<TeamMember> list = new ArrayList<>();
            String sql = "SELECT `uuid`, `team_id`, `role`, `joined_at` FROM `" + getTableName() + "` WHERE `team_id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToMember(rs));
                    }
                }
            } catch (SQLException e) {
                throw new DatabaseException("查询团队所有成员失败 (TeamId: " + teamId + ")", e);
            }
            return list;
        });
    }

    /**
     * 加载所有成员（解析数字等级 3:队长, 2:管理员, 1:队员）
     */
    public CompletableFuture<List<TeamMember>> loadAllMembers() {
        return databaseManager.supplyAsync(() -> {
            List<TeamMember> list = new ArrayList<>();
            String sql = "SELECT `uuid`, `team_id`, `role`, `joined_at` FROM `" + getTableName() + "`";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(mapResultSetToMember(rs));
                }
            } catch (SQLException e) {
                throw new DatabaseException("加载全部成员数据失败", e);
            }
            return list;
        });
    }

    private TeamMember mapResultSetToMember(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        int teamId = rs.getInt("team_id");
        TeamRole role = TeamRole.fromDatabase(rs.getObject("role"));
        Timestamp joinedAt = rs.getTimestamp("joined_at");

        return new TeamMember(uuid, teamId, role, joinedAt);
    }
}

package com.balancedteam.database.dao;

import com.balancedteam.database.DatabaseManager;
import com.balancedteam.database.exception.DatabaseException;
import com.balancedteam.model.Team;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 团队数据访问对象
 */
public class TeamDao {

    private final DatabaseManager databaseManager;

    public TeamDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private String getTableName() {
        return databaseManager.getTablePrefix() + "teams";
    }

    /**
     * 插入新团队并获取自增 ID
     */
    public CompletableFuture<Integer> createTeam(Team team) {
        return databaseManager.supplyAsync(() -> {
            String sql = "INSERT INTO `" + getTableName() + "` (`name`, `leader_uuid`, `friendly_fire`, `description`, `created_at`) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, team.getName());
                ps.setString(2, team.getLeaderUuid().toString());
                ps.setBoolean(3, team.isFriendlyFire());
                ps.setString(4, team.getDescription());
                ps.setTimestamp(5, team.getCreatedAt());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        team.setId(id);
                        return id;
                    }
                }
                throw new DatabaseException("创建团队失败，未生成主键 ID: " + team.getName());
            } catch (SQLException e) {
                throw new DatabaseException("创建团队数据库操作失败 (" + team.getName() + ")", e);
            }
        });
    }

    /**
     * 更新团队信息（队长、友伤、简介等）
     */
    public CompletableFuture<Void> updateTeam(Team team) {
        return databaseManager.runAsync(() -> {
            String sql = "UPDATE `" + getTableName() + "` SET `leader_uuid` = ?, `friendly_fire` = ?, `description` = ? WHERE `id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, team.getLeaderUuid().toString());
                ps.setBoolean(2, team.isFriendlyFire());
                ps.setString(3, team.getDescription());
                ps.setInt(4, team.getId());

                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("更新团队信息失败 (TeamId: " + team.getId() + ")", e);
            }
        });
    }

    /**
     * 删除团队
     */
    public CompletableFuture<Void> deleteTeam(int teamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, teamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除团队失败 (TeamId: " + teamId + ")", e);
            }
        });
    }

    /**
     * 按 ID 单条查询团队
     */
    public CompletableFuture<Optional<Team>> findTeamById(int teamId) {
        return databaseManager.supplyAsync(() -> {
            String sql = "SELECT `id`, `name`, `leader_uuid`, `friendly_fire`, `description`, `created_at` FROM `" + getTableName() + "` WHERE `id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToTeam(rs));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new DatabaseException("查询团队失败 (TeamId: " + teamId + ")", e);
            }
        });
    }

    /**
     * 按团队名称查询团队
     */
    public CompletableFuture<Optional<Team>> findTeamByName(String name) {
        return databaseManager.supplyAsync(() -> {
            String sql = "SELECT `id`, `name`, `leader_uuid`, `friendly_fire`, `description`, `created_at` FROM `" + getTableName() + "` WHERE `name` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToTeam(rs));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                throw new DatabaseException("按名称查询团队失败 (Name: " + name + ")", e);
            }
        });
    }

    /**
     * 加载所有团队（用于服务器启动时快速构建内存缓存）
     */
    public CompletableFuture<List<Team>> loadAllTeams() {
        return databaseManager.supplyAsync(() -> {
            List<Team> list = new ArrayList<>();
            String sql = "SELECT `id`, `name`, `leader_uuid`, `friendly_fire`, `description`, `created_at` FROM `" + getTableName() + "`";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(mapResultSetToTeam(rs));
                }
            } catch (SQLException e) {
                throw new DatabaseException("加载全部团队数据失败", e);
            }
            return list;
        });
    }

    private Team mapResultSetToTeam(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        UUID leaderUuid = UUID.fromString(rs.getString("leader_uuid"));
        boolean friendlyFire = rs.getBoolean("friendly_fire");
        String description = rs.getString("description");
        Timestamp createdAt = rs.getTimestamp("created_at");

        return new Team(id, name, leaderUuid, friendlyFire, description, createdAt);
    }
}

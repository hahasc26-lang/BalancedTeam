package com.balancedteam.database.dao;

import com.balancedteam.database.DatabaseManager;
import com.balancedteam.database.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 团队求和申请与战后保护数据访问对象
 */
public class TruceDao {

    private final DatabaseManager databaseManager;

    public TruceDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private String getRequestsTableName() {
        return databaseManager.getTablePrefix() + "truce_requests";
    }

    private String getProtectionsTableName() {
        return databaseManager.getTablePrefix() + "post_war_protections";
    }

    // =========================================================================
    // 1. 求和申请持久化 (Truce Requests)
    // =========================================================================

    public CompletableFuture<Void> saveTruceRequest(int fromTeamId, int toTeamId, long expireTime) {
        return databaseManager.runTransaction(conn -> {
            String deleteSql = "DELETE FROM `" + getRequestsTableName() + "` WHERE `from_team_id` = ? AND `to_team_id` = ?";
            String insertSql = "INSERT INTO `" + getRequestsTableName() + "` (`from_team_id`, `to_team_id`, `expire_time`) VALUES (?, ?, ?)";

            try (PreparedStatement dps = conn.prepareStatement(deleteSql)) {
                dps.setInt(1, fromTeamId);
                dps.setInt(2, toTeamId);
                dps.executeUpdate();
            }
            try (PreparedStatement ips = conn.prepareStatement(insertSql)) {
                ips.setInt(1, fromTeamId);
                ips.setInt(2, toTeamId);
                ips.setLong(3, expireTime);
                ips.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> deleteTruceRequest(int fromTeamId, int toTeamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getRequestsTableName() + "` WHERE `from_team_id` = ? AND `to_team_id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, fromTeamId);
                ps.setInt(2, toTeamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to delete truce request (From: " + fromTeamId + ", To: " + toTeamId + ")", e);
            }
        });
    }

    public CompletableFuture<Void> deleteTruceRequestsByTeam(int teamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getRequestsTableName() + "` WHERE `from_team_id` = ? OR `to_team_id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                ps.setInt(2, teamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to delete truce requests for team (TeamId: " + teamId + ")", e);
            }
        });
    }

    public CompletableFuture<Map<Integer, Map<Integer, Long>>> loadAllValidTruceRequests(long now) {
        return databaseManager.supplyAsync(() -> {
            Map<Integer, Map<Integer, Long>> result = new HashMap<>();
            String sql = "SELECT `from_team_id`, `to_team_id`, `expire_time` FROM `" + getRequestsTableName() + "` WHERE `expire_time` > ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, now);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int fromTeamId = rs.getInt("from_team_id");
                        int toTeamId = rs.getInt("to_team_id");
                        long expireTime = rs.getLong("expire_time");

                        result.computeIfAbsent(toTeamId, k -> new HashMap<>()).put(fromTeamId, expireTime);
                    }
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to load valid truce requests", e);
            }
            return result;
        });
    }

    public CompletableFuture<Integer> cleanExpiredTruceRequests(long now) {
        return databaseManager.supplyAsync(() -> {
            String sql = "DELETE FROM `" + getRequestsTableName() + "` WHERE `expire_time` <= ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, now);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to clean expired truce requests", e);
            }
        });
    }

    // =========================================================================
    // 2. 战后保护持久化 (Post-War Protections)
    // =========================================================================

    public CompletableFuture<Void> saveProtection(int teamId1, int teamId2, long expireTime) {
        int t1 = Math.min(teamId1, teamId2);
        int t2 = Math.max(teamId1, teamId2);
        return databaseManager.runTransaction(conn -> {
            String deleteSql = "DELETE FROM `" + getProtectionsTableName() + "` WHERE `team_id_1` = ? AND `team_id_2` = ?";
            String insertSql = "INSERT INTO `" + getProtectionsTableName() + "` (`team_id_1`, `team_id_2`, `expire_time`) VALUES (?, ?, ?)";

            try (PreparedStatement dps = conn.prepareStatement(deleteSql)) {
                dps.setInt(1, t1);
                dps.setInt(2, t2);
                dps.executeUpdate();
            }
            try (PreparedStatement ips = conn.prepareStatement(insertSql)) {
                ips.setInt(1, t1);
                ips.setInt(2, t2);
                ips.setLong(3, expireTime);
                ips.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> deleteProtection(int teamId1, int teamId2) {
        int t1 = Math.min(teamId1, teamId2);
        int t2 = Math.max(teamId1, teamId2);
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getProtectionsTableName() + "` WHERE `team_id_1` = ? AND `team_id_2` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, t1);
                ps.setInt(2, t2);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to delete post-war protection (" + t1 + " <-> " + t2 + ")", e);
            }
        });
    }

    public CompletableFuture<Void> deleteProtectionsByTeam(int teamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getProtectionsTableName() + "` WHERE `team_id_1` = ? OR `team_id_2` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                ps.setInt(2, teamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to delete post-war protections for team (" + teamId + ")", e);
            }
        });
    }

    public CompletableFuture<Map<String, Long>> loadAllValidProtections(long now) {
        return databaseManager.supplyAsync(() -> {
            Map<String, Long> result = new HashMap<>();
            String sql = "SELECT `team_id_1`, `team_id_2`, `expire_time` FROM `" + getProtectionsTableName() + "` WHERE `expire_time` > ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, now);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int t1 = rs.getInt("team_id_1");
                        int t2 = rs.getInt("team_id_2");
                        long expireTime = rs.getLong("expire_time");
                        String key = Math.min(t1, t2) + ":" + Math.max(t1, t2);
                        result.put(key, expireTime);
                    }
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to load valid post-war protections", e);
            }
            return result;
        });
    }

    public CompletableFuture<Integer> cleanExpiredProtections(long now) {
        return databaseManager.supplyAsync(() -> {
            String sql = "DELETE FROM `" + getProtectionsTableName() + "` WHERE `expire_time` <= ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, now);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to clean expired post-war protections", e);
            }
        });
    }
}

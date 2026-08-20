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
 * 团队同盟申请数据访问对象
 */
public class AllyRequestDao {

    private final DatabaseManager databaseManager;

    public AllyRequestDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private String getTableName() {
        return databaseManager.getTablePrefix() + "ally_requests";
    }

    /**
     * 保存或更新同盟申请（使用数据库事务确保原子性）
     */
    public CompletableFuture<Void> saveRequest(int fromTeamId, int toTeamId, long expireTime) {
        return databaseManager.runTransaction(conn -> {
            String deleteSql = "DELETE FROM `" + getTableName() + "` WHERE `from_team_id` = ? AND `to_team_id` = ?";
            String insertSql = "INSERT INTO `" + getTableName() + "` (`from_team_id`, `to_team_id`, `expire_time`) VALUES (?, ?, ?)";

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

    /**
     * 删除指定的同盟申请
     */
    public CompletableFuture<Void> deleteRequest(int fromTeamId, int toTeamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `from_team_id` = ? AND `to_team_id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, fromTeamId);
                ps.setInt(2, toTeamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除同盟申请失败 (From: " + fromTeamId + ", To: " + toTeamId + ")", e);
            }
        });
    }

    /**
     * 删除与指定团队关联的所有同盟申请 (无论是申请方还是接收方)
     */
    public CompletableFuture<Void> deleteRequestsByTeam(int teamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `from_team_id` = ? OR `to_team_id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                ps.setInt(2, teamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除团队关联同盟申请失败 (TeamId: " + teamId + ")", e);
            }
        });
    }

    /**
     * 加载所有未过期的同盟申请（单一职责：仅负责查询读取）
     * 返回结构：ToTeamId -> (FromTeamId -> ExpireTime)
     */
    public CompletableFuture<Map<Integer, Map<Integer, Long>>> loadAllValidRequests(long now) {
        return databaseManager.supplyAsync(() -> {
            Map<Integer, Map<Integer, Long>> result = new HashMap<>();
            String sql = "SELECT `from_team_id`, `to_team_id`, `expire_time` FROM `" + getTableName() + "` WHERE `expire_time` > ?";
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
                throw new DatabaseException("加载未过期同盟申请失败", e);
            }
            return result;
        });
    }

    /**
     * 清理所有已过期的同盟申请（独立清理方法）
     */
    public CompletableFuture<Integer> cleanExpiredRequests(long now) {
        return databaseManager.supplyAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `expire_time` <= ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, now);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("清理过期同盟申请失败", e);
            }
        });
    }
}

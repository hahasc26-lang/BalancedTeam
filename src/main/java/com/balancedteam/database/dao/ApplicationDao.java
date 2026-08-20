package com.balancedteam.database.dao;

import com.balancedteam.database.DatabaseManager;
import com.balancedteam.database.exception.DatabaseException;
import com.balancedteam.manager.ApplicationManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 玩家入队申请数据访问对象
 */
public class ApplicationDao {

    private final DatabaseManager databaseManager;

    public ApplicationDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private String getTableName() {
        return databaseManager.getTablePrefix() + "team_applications";
    }

    /**
     * 保存或更新入队申请
     */
    public CompletableFuture<Void> saveApplication(int teamId, UUID playerUuid, String playerName, long expireTime) {
        return databaseManager.runTransaction(conn -> {
            String deleteSql = "DELETE FROM `" + getTableName() + "` WHERE `team_id` = ? AND `player_uuid` = ?";
            String insertSql = "INSERT INTO `" + getTableName() + "` (`team_id`, `player_uuid`, `player_name`, `expire_time`) VALUES (?, ?, ?, ?)";

            try (PreparedStatement dps = conn.prepareStatement(deleteSql)) {
                dps.setInt(1, teamId);
                dps.setString(2, playerUuid.toString());
                dps.executeUpdate();
            }
            try (PreparedStatement ips = conn.prepareStatement(insertSql)) {
                ips.setInt(1, teamId);
                ips.setString(2, playerUuid.toString());
                ips.setString(3, playerName);
                ips.setLong(4, expireTime);
                ips.executeUpdate();
            }
        });
    }

    /**
     * 删除指定的入队申请
     */
    public CompletableFuture<Void> deleteApplication(int teamId, UUID playerUuid) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `team_id` = ? AND `player_uuid` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除入队申请失败 (TeamId: " + teamId + ", Player: " + playerUuid + ")", e);
            }
        });
    }

    /**
     * 删除指定队伍的所有入队申请
     */
    public CompletableFuture<Void> deleteApplicationsByTeam(int teamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `team_id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除团队所有入队申请失败 (TeamId: " + teamId + ")", e);
            }
        });
    }

    /**
     * 删除指定玩家发出的所有入队申请
     */
    public CompletableFuture<Void> deleteApplicationsByPlayer(UUID playerUuid) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `player_uuid` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除玩家所有入队申请失败 (Player: " + playerUuid + ")", e);
            }
        });
    }

    /**
     * 加载所有未过期的入队申请
     * 返回结构：Map<TeamId, Map<PlayerUUID, Application>>
     */
    public CompletableFuture<Map<Integer, Map<UUID, ApplicationManager.Application>>> loadAllValidApplications(long now) {
        return databaseManager.supplyAsync(() -> {
            Map<Integer, Map<UUID, ApplicationManager.Application>> result = new HashMap<>();
            String sql = "SELECT `team_id`, `player_uuid`, `player_name`, `expire_time` FROM `" + getTableName() + "` WHERE `expire_time` > ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, now);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int teamId = rs.getInt("team_id");
                        UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                        String playerName = rs.getString("player_name");
                        long expireTime = rs.getLong("expire_time");

                        ApplicationManager.Application app = new ApplicationManager.Application(teamId, playerUuid, playerName, expireTime, true);
                        result.computeIfAbsent(teamId, k -> new HashMap<>()).put(playerUuid, app);
                    }
                }
            } catch (SQLException e) {
                throw new DatabaseException("加载未过期入队申请失败", e);
            }
            return result;
        });
    }

    /**
     * 清理所有已过期的入队申请
     */
    public CompletableFuture<Integer> cleanExpiredApplications(long now) {
        return databaseManager.supplyAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `expire_time` <= ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, now);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("清理过期入队申请失败", e);
            }
        });
    }
}

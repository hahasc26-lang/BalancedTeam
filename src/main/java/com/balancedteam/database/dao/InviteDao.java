package com.balancedteam.database.dao;

import com.balancedteam.database.DatabaseManager;
import com.balancedteam.database.exception.DatabaseException;
import com.balancedteam.manager.InviteManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 玩家入队邀请数据访问对象
 */
public class InviteDao {

    private final DatabaseManager databaseManager;

    public InviteDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private String getTableName() {
        return databaseManager.getTablePrefix() + "invites";
    }

    /**
     * 保存或更新入队邀请（使用数据库事务确保原子性）
     */
    public CompletableFuture<Void> saveInvite(int teamId, UUID targetUuid, UUID inviterUuid, long expireTime) {
        return databaseManager.runTransaction(conn -> {
            String deleteSql = "DELETE FROM `" + getTableName() + "` WHERE `team_id` = ? AND `target_uuid` = ?";
            String insertSql = "INSERT INTO `" + getTableName() + "` (`team_id`, `target_uuid`, `inviter_uuid`, `expire_time`) VALUES (?, ?, ?, ?)";

            try (PreparedStatement dps = conn.prepareStatement(deleteSql)) {
                dps.setInt(1, teamId);
                dps.setString(2, targetUuid.toString());
                dps.executeUpdate();
            }
            try (PreparedStatement ips = conn.prepareStatement(insertSql)) {
                ips.setInt(1, teamId);
                ips.setString(2, targetUuid.toString());
                ips.setString(3, inviterUuid.toString());
                ips.setLong(4, expireTime);
                ips.executeUpdate();
            }
        });
    }

    /**
     * 删除指定的入队邀请
     */
    public CompletableFuture<Void> deleteInvite(UUID targetUuid, int teamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `team_id` = ? AND `target_uuid` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                ps.setString(2, targetUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除入队邀请失败 (TeamId: " + teamId + ", Target: " + targetUuid + ")", e);
            }
        });
    }

    /**
     * 删除指定队伍发出的所有入队邀请
     */
    public CompletableFuture<Void> deleteInvitesByTeam(int teamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `team_id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除团队所有入队邀请失败 (TeamId: " + teamId + ")", e);
            }
        });
    }

    /**
     * 加载所有未过期的入队邀请（单一职责：仅负责查询读取）
     */
    public CompletableFuture<Map<UUID, Map<Integer, InviteManager.Invite>>> loadAllValidInvites(long now) {
        return databaseManager.supplyAsync(() -> {
            Map<UUID, Map<Integer, InviteManager.Invite>> result = new HashMap<>();
            String sql = "SELECT `team_id`, `target_uuid`, `inviter_uuid`, `expire_time` FROM `" + getTableName() + "` WHERE `expire_time` > ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, now);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int teamId = rs.getInt("team_id");
                        UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                        UUID inviterUuid = UUID.fromString(rs.getString("inviter_uuid"));
                        long expireTime = rs.getLong("expire_time");

                        InviteManager.Invite invite = new InviteManager.Invite(teamId, inviterUuid, expireTime, true);
                        result.computeIfAbsent(targetUuid, k -> new HashMap<>()).put(teamId, invite);
                    }
                }
            } catch (SQLException e) {
                throw new DatabaseException("加载未过期入队邀请失败", e);
            }
            return result;
        });
    }

    /**
     * 清理所有已过期的入队邀请（独立清理方法）
     */
    public CompletableFuture<Integer> cleanExpiredInvites(long now) {
        return databaseManager.supplyAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `expire_time` <= ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, now);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("清理过期入队邀请失败", e);
            }
        });
    }
}

package com.balancedteam.database.dao;

import com.balancedteam.database.DatabaseManager;
import com.balancedteam.database.exception.DatabaseException;
import com.balancedteam.model.RelationStatus;
import com.balancedteam.model.RelationType;
import com.balancedteam.model.TeamRelation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 团队外交关系数据访问对象
 */
public class RelationDao {

    private final DatabaseManager databaseManager;

    public RelationDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private String getTableName() {
        return databaseManager.getTablePrefix() + "relations";
    }

    /**
     * 保存新外交关系并获取 ID
     */
    public CompletableFuture<Integer> createRelation(TeamRelation relation) {
        return databaseManager.supplyAsync(() -> {
            String sql = "INSERT INTO `" + getTableName() + "` (`team_id_1`, `team_id_2`, `relation_type`, `status`, `created_at`) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, relation.getTeamId1());
                ps.setInt(2, relation.getTeamId2());
                ps.setString(3, relation.getRelationType().name());
                ps.setString(4, relation.getStatus().name());
                ps.setTimestamp(5, relation.getCreatedAt());

                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        relation.setId(id);
                        return id;
                    }
                }
                throw new DatabaseException("创建外交关系失败，未生成主键 ID");
            } catch (SQLException e) {
                throw new DatabaseException("创建外交关系数据库操作失败 (" + relation.getTeamId1() + " <-> " + relation.getTeamId2() + ")", e);
            }
        });
    }

    /**
     * 更新关系状态 (如 PENDING -> ACCEPTED)
     */
    public CompletableFuture<Void> updateRelationStatus(int relationId, RelationStatus status) {
        return databaseManager.runAsync(() -> {
            String sql = "UPDATE `" + getTableName() + "` SET `status` = ? WHERE `id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, status.name());
                ps.setInt(2, relationId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("更新外交关系状态失败 (RelationId: " + relationId + ")", e);
            }
        });
    }

    /**
     * 删除单条关系
     */
    public CompletableFuture<Void> deleteRelation(int relationId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `id` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, relationId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除外交关系失败 (RelationId: " + relationId + ")", e);
            }
        });
    }

    /**
     * 删除与指定团队关联的所有关系
     */
    public CompletableFuture<Void> deleteRelationsByTeamId(int teamId) {
        return databaseManager.runAsync(() -> {
            String sql = "DELETE FROM `" + getTableName() + "` WHERE `team_id_1` = ? OR `team_id_2` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, teamId);
                ps.setInt(2, teamId);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("删除团队所有外交关系失败 (TeamId: " + teamId + ")", e);
            }
        });
    }

    /**
     * 查询指定团队的所有关系列表
     */
    public CompletableFuture<List<TeamRelation>> findRelationsByTeamId(int teamId) {
        return databaseManager.supplyAsync(() -> {
            List<TeamRelation> list = new ArrayList<>();
            String sql = "SELECT `id`, `team_id_1`, `team_id_2`, `relation_type`, `status`, `created_at` FROM `" + getTableName() + "` WHERE `team_id_1` = ? OR `team_id_2` = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, teamId);
                ps.setInt(2, teamId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToRelation(rs));
                    }
                }
            } catch (SQLException e) {
                throw new DatabaseException("查询团队外交关系失败 (TeamId: " + teamId + ")", e);
            }
            return list;
        });
    }

    /**
     * 加载所有外交关系
     */
    public CompletableFuture<List<TeamRelation>> loadAllRelations() {
        return databaseManager.supplyAsync(() -> {
            List<TeamRelation> list = new ArrayList<>();
            String sql = "SELECT `id`, `team_id_1`, `team_id_2`, `relation_type`, `status`, `created_at` FROM `" + getTableName() + "`";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(mapResultSetToRelation(rs));
                }
            } catch (SQLException e) {
                throw new DatabaseException("加载全部外交关系数据失败", e);
            }
            return list;
        });
    }

    private TeamRelation mapResultSetToRelation(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int teamId1 = rs.getInt("team_id_1");
        int teamId2 = rs.getInt("team_id_2");
        RelationType relationType = RelationType.valueOf(rs.getString("relation_type"));
        RelationStatus status = RelationStatus.valueOf(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");

        return new TeamRelation(id, teamId1, teamId2, relationType, status, createdAt);
    }
}

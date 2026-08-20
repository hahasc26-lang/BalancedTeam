package com.balancedteam.model;

import java.sql.Timestamp;

/**
 * 团队外交关系实体
 */
public class TeamRelation {
    private int id;
    private final int teamId1;
    private final int teamId2;
    private final RelationType relationType;
    private RelationStatus status;
    private final Timestamp createdAt;

    public TeamRelation(int id, int teamId1, int teamId2, RelationType relationType, RelationStatus status, Timestamp createdAt) {
        this.id = id;
        this.teamId1 = teamId1;
        this.teamId2 = teamId2;
        this.relationType = relationType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public TeamRelation(int teamId1, int teamId2, RelationType relationType, RelationStatus status, Timestamp createdAt) {
        this(0, teamId1, teamId2, relationType, status, createdAt);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTeamId1() {
        return teamId1;
    }

    public int getTeamId2() {
        return teamId2;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    public RelationStatus getStatus() {
        return status;
    }

    public void setStatus(RelationStatus status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * 判断给定队伍是否参与了此关系
     */
    public boolean involves(int teamId) {
        return teamId1 == teamId || teamId2 == teamId;
    }

    /**
     * 获取关系的另一方队伍 ID
     */
    public int getOtherTeamId(int teamId) {
        if (teamId1 == teamId) return teamId2;
        if (teamId2 == teamId) return teamId1;
        return 0;
    }
}

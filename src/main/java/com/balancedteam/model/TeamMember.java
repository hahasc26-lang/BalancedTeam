package com.balancedteam.model;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * 团队成员实体
 */
public class TeamMember {
    private final UUID uuid;
    private final int teamId;
    private TeamRole role;
    private final Timestamp joinedAt;

    public TeamMember(UUID uuid, int teamId, TeamRole role, Timestamp joinedAt) {
        this.uuid = uuid;
        this.teamId = teamId;
        this.role = role != null ? role : TeamRole.MEMBER;
        this.joinedAt = joinedAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getTeamId() {
        return teamId;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role != null ? role : TeamRole.MEMBER;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    /**
     * 是否为队长 (Level 3)
     */
    public boolean isLeader() {
        return role == TeamRole.LEADER;
    }

    /**
     * 是否为管理员 (Level 2)
     */
    public boolean isOfficer() {
        return role == TeamRole.OFFICER;
    }

    /**
     * 是否为管理员或更高职位 (Level >= 2)
     */
    public boolean isOfficerOrHigher() {
        return role != null && role.isAtLeast(TeamRole.OFFICER);
    }

    /**
     * 是否至少具备指定职位的权限等级
     */
    public boolean isAtLeast(TeamRole required) {
        return role != null && role.isAtLeast(required);
    }

    /**
     * 是否有权解散队伍（仅队长 Level 3 具备，管理员 Level 2 无法解散）
     */
    public boolean canDisband() {
        return role != null && role.canDisband();
    }

    /**
     * 是否可以管理目标成员（权限等级严格高于目标成员）
     */
    public boolean canManage(TeamMember target) {
        if (target == null) return true;
        if (this.role == null) return false;
        return this.role.getLevel() > target.getRole().getLevel();
    }
}

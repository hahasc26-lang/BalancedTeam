package com.balancedteam.model;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 团队实体类
 */
public class Team {
    private int id;
    private String name;
    private UUID leaderUuid;
    private boolean friendlyFire;
    private String description;
    private final Timestamp createdAt;

    // 内存成员映射: UUID -> TeamMember
    private final Map<UUID, TeamMember> members = new ConcurrentHashMap<>();

    public Team(int id, String name, UUID leaderUuid, boolean friendlyFire, String description, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.friendlyFire = friendlyFire;
        this.description = description == null ? "" : description;
        this.createdAt = createdAt;
    }

    public Team(String name, UUID leaderUuid, boolean friendlyFire, String description) {
        this(0, name, leaderUuid, friendlyFire, description, new Timestamp(System.currentTimeMillis()));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Map<UUID, TeamMember> getMembers() {
        return members;
    }

    public void addMember(TeamMember member) {
        members.put(member.getUuid(), member);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public TeamMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean isLeader(UUID uuid) {
        return leaderUuid != null && leaderUuid.equals(uuid);
    }

    public boolean isOfficerOrLeader(UUID uuid) {
        if (isLeader(uuid)) return true;
        TeamMember member = getMember(uuid);
        return member != null && member.isOfficerOrHigher();
    }

    public boolean isOfficer(UUID uuid) {
        TeamMember member = getMember(uuid);
        return member != null && member.isOfficer();
    }

    /**
     * 根据玩家名称在该团队的现有成员中查找成员
     * 优先匹配在线玩家，其次遍历本队已有成员的 UUID 匹配名称，完全避免主线程网络阻塞与废弃 API
     */
    public TeamMember getMemberByName(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;
        org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayerExact(playerName);
        if (online != null && hasMember(online.getUniqueId())) {
            return getMember(online.getUniqueId());
        }
        for (UUID uuid : members.keySet()) {
            org.bukkit.OfflinePlayer offline = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(playerName)) {
                return members.get(uuid);
            }
        }
        return null;
    }
}

package com.balancedteam.manager;

import com.balancedteam.database.dao.InviteDao;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家入队邀请管理器（内存+数据库双层持久化）
 */
public class InviteManager {

    public static class Invite {
        private final int teamId;
        private final UUID inviterUuid;
        private final long expireTimeMillis;

        public Invite(int teamId, UUID inviterUuid, long timeoutSeconds) {
            this.teamId = teamId;
            this.inviterUuid = inviterUuid;
            this.expireTimeMillis = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        }

        public Invite(int teamId, UUID inviterUuid, long expireTimeMillis, boolean isAbsolute) {
            this.teamId = teamId;
            this.inviterUuid = inviterUuid;
            this.expireTimeMillis = isAbsolute ? expireTimeMillis : System.currentTimeMillis() + expireTimeMillis;
        }

        public int getTeamId() {
            return teamId;
        }

        public UUID getInviterUuid() {
            return inviterUuid;
        }

        public long getExpireTimeMillis() {
            return expireTimeMillis;
        }

        public long getRemainingSeconds() {
            long rem = (expireTimeMillis - System.currentTimeMillis()) / 1000L;
            return Math.max(0, rem);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTimeMillis;
        }
    }

    private final InviteDao inviteDao;

    // TargetPlayer UUID -> (TeamId -> Invite)
    private final Map<UUID, Map<Integer, Invite>> pendingInvites = new ConcurrentHashMap<>();

    public InviteManager(InviteDao inviteDao) {
        this.inviteDao = inviteDao;
    }

    /**
     * 启动时全量预热内存数据并清理过期数据
     */
    public void init(Map<UUID, Map<Integer, Invite>> loadedInvites) {
        pendingInvites.clear();
        for (Map.Entry<UUID, Map<Integer, Invite>> entry : loadedInvites.entrySet()) {
            pendingInvites.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
        }
        cleanExpired(System.currentTimeMillis());
    }

    /**
     * 清理过期邀请
     */
    public void cleanExpired(long now) {
        if (inviteDao != null) {
            inviteDao.cleanExpiredInvites(now);
        }
    }

    /**
     * 添加邀请 (内存 + 数据库)
     */
    public void addInvite(UUID targetUuid, int teamId, UUID inviterUuid, long timeoutSeconds) {
        Invite invite = new Invite(teamId, inviterUuid, timeoutSeconds);
        pendingInvites.computeIfAbsent(targetUuid, k -> new ConcurrentHashMap<>())
                .put(teamId, invite);

        if (inviteDao != null) {
            inviteDao.saveInvite(teamId, targetUuid, inviterUuid, invite.getExpireTimeMillis());
        }
    }

    /**
     * 检查并消费（移除）有效邀请 (内存 + 数据库)
     */
    public boolean consumeInvite(UUID targetUuid, int teamId) {
        Map<Integer, Invite> teamMap = pendingInvites.get(targetUuid);
        if (teamMap == null) return false;

        Invite invite = teamMap.remove(teamId);
        if (teamMap.isEmpty()) {
            pendingInvites.remove(targetUuid);
        }

        if (invite != null) {
            if (inviteDao != null) {
                inviteDao.deleteInvite(targetUuid, teamId);
            }
            return !invite.isExpired();
        }

        return false;
    }

    /**
     * 判断是否已存在有效邀请
     */
    public boolean hasValidInvite(UUID targetUuid, int teamId) {
        Map<Integer, Invite> teamMap = pendingInvites.get(targetUuid);
        if (teamMap == null) return false;
        Invite invite = teamMap.get(teamId);
        if (invite == null) return false;
        if (invite.isExpired()) {
            teamMap.remove(teamId);
            if (teamMap.isEmpty()) {
                pendingInvites.remove(targetUuid);
            }
            if (inviteDao != null) {
                inviteDao.deleteInvite(targetUuid, teamId);
            }
            return false;
        }
        return true;
    }

    /**
     * 移除邀请 (内存 + 数据库)
     */
    public void removeInvite(UUID targetUuid, int teamId) {
        Map<Integer, Invite> teamMap = pendingInvites.get(targetUuid);
        if (teamMap != null) {
            teamMap.remove(teamId);
            if (teamMap.isEmpty()) {
                pendingInvites.remove(targetUuid);
            }
        }
        if (inviteDao != null) {
            inviteDao.deleteInvite(targetUuid, teamId);
        }
    }

    /**
     * 团队解散时清理所有相关邀请
     */
    public void onTeamDisbanded(int teamId) {
        for (Iterator<Map.Entry<UUID, Map<Integer, Invite>>> it = pendingInvites.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Map<Integer, Invite>> entry = it.next();
            entry.getValue().remove(teamId);
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
        if (inviteDao != null) {
            inviteDao.deleteInvitesByTeam(teamId);
        }
    }

    /**
     * 获取玩家收到的所有有效入队邀请
     */
    public List<Invite> getValidInvites(UUID targetUuid) {
        Map<Integer, Invite> teamMap = pendingInvites.get(targetUuid);
        if (teamMap == null || teamMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Invite> validList = new ArrayList<>();
        List<Integer> expiredIds = new ArrayList<>();

        for (Map.Entry<Integer, Invite> entry : teamMap.entrySet()) {
            Invite inv = entry.getValue();
            if (inv.isExpired()) {
                expiredIds.add(entry.getKey());
            } else {
                validList.add(inv);
            }
        }

        for (Integer expId : expiredIds) {
            teamMap.remove(expId);
            if (inviteDao != null) {
                inviteDao.deleteInvite(targetUuid, expId);
            }
        }
        if (teamMap.isEmpty()) {
            pendingInvites.remove(targetUuid);
        }

        return validList;
    }

    /**
     * 获取玩家收到的有效入队邀请数量
     */
    public int getValidInviteCount(UUID targetUuid) {
        return getValidInvites(targetUuid).size();
    }

    /**
     * 获取指定玩家针对特定团队的有效邀请详情
     */
    public Invite getInvite(UUID targetUuid, int teamId) {
        if (hasValidInvite(targetUuid, teamId)) {
            Map<Integer, Invite> teamMap = pendingInvites.get(targetUuid);
            if (teamMap != null) {
                return teamMap.get(teamId);
            }
        }
        return null;
    }
}

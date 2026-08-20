package com.balancedteam.manager;

import com.balancedteam.database.dao.ApplicationDao;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家入队申请管理器（内存+数据库双层持久化）
 */
public class ApplicationManager {

    public static class Application {
        private final int teamId;
        private final UUID playerUuid;
        private final String playerName;
        private final long expireTimeMillis;

        public Application(int teamId, UUID playerUuid, String playerName, long timeoutSeconds) {
            this.teamId = teamId;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.expireTimeMillis = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        }

        public Application(int teamId, UUID playerUuid, String playerName, long expireTimeMillis, boolean isAbsolute) {
            this.teamId = teamId;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.expireTimeMillis = isAbsolute ? expireTimeMillis : System.currentTimeMillis() + expireTimeMillis;
        }

        public int getTeamId() {
            return teamId;
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public String getPlayerName() {
            return playerName;
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

    private final ApplicationDao applicationDao;

    // TeamId -> (PlayerUUID -> Application)
    private final Map<Integer, Map<UUID, Application>> pendingApplications = new ConcurrentHashMap<>();

    public ApplicationManager(ApplicationDao applicationDao) {
        this.applicationDao = applicationDao;
    }

    /**
     * 启动时全量预热内存数据并清理过期数据
     */
    public void init(Map<Integer, Map<UUID, Application>> loaded) {
        pendingApplications.clear();
        for (Map.Entry<Integer, Map<UUID, Application>> entry : loaded.entrySet()) {
            pendingApplications.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
        }
        cleanExpired(System.currentTimeMillis());
    }

    /**
     * 清理过期入队申请
     */
    public void cleanExpired(long now) {
        if (applicationDao != null) {
            applicationDao.cleanExpiredApplications(now);
        }
    }

    /**
     * 添加申请 (内存 + 数据库)
     */
    public void addApplication(int teamId, UUID playerUuid, String playerName, long timeoutSeconds) {
        Application app = new Application(teamId, playerUuid, playerName, timeoutSeconds);
        pendingApplications.computeIfAbsent(teamId, k -> new ConcurrentHashMap<>())
                .put(playerUuid, app);

        if (applicationDao != null) {
            applicationDao.saveApplication(teamId, playerUuid, playerName, app.getExpireTimeMillis());
        }
    }

    /**
     * 检查并消费（移除）有效申请 (内存 + 数据库)
     */
    public boolean consumeApplication(int teamId, UUID playerUuid) {
        Map<UUID, Application> playerMap = pendingApplications.get(teamId);
        if (playerMap == null) return false;

        Application app = playerMap.remove(playerUuid);
        if (playerMap.isEmpty()) {
            pendingApplications.remove(teamId);
        }

        if (app != null) {
            if (applicationDao != null) {
                applicationDao.deleteApplication(teamId, playerUuid);
            }
            return !app.isExpired();
        }

        return false;
    }

    /**
     * 移除申请 (内存 + 数据库)
     */
    public void removeApplication(int teamId, UUID playerUuid) {
        Map<UUID, Application> playerMap = pendingApplications.get(teamId);
        if (playerMap != null) {
            playerMap.remove(playerUuid);
            if (playerMap.isEmpty()) {
                pendingApplications.remove(teamId);
            }
        }
        if (applicationDao != null) {
            applicationDao.deleteApplication(teamId, playerUuid);
        }
    }

    /**
     * 判断是否已存在有效入队申请
     */
    public boolean hasValidApplication(int teamId, UUID playerUuid) {
        Map<UUID, Application> playerMap = pendingApplications.get(teamId);
        if (playerMap == null) return false;
        Application app = playerMap.get(playerUuid);
        if (app == null) return false;
        if (app.isExpired()) {
            playerMap.remove(playerUuid);
            if (playerMap.isEmpty()) {
                pendingApplications.remove(teamId);
            }
            if (applicationDao != null) {
                applicationDao.deleteApplication(teamId, playerUuid);
            }
            return false;
        }
        return true;
    }

    /**
     * 团队解散时清理所有相关申请
     */
    public void onTeamDisbanded(int teamId) {
        pendingApplications.remove(teamId);
        if (applicationDao != null) {
            applicationDao.deleteApplicationsByTeam(teamId);
        }
    }

    /**
     * 当玩家加入队伍后，自动清理该玩家发出的所有其他入队申请
     */
    public void onPlayerJoinedTeam(UUID playerUuid) {
        for (Iterator<Map.Entry<Integer, Map<UUID, Application>>> it = pendingApplications.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Map<UUID, Application>> entry = it.next();
            entry.getValue().remove(playerUuid);
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
        if (applicationDao != null) {
            applicationDao.deleteApplicationsByPlayer(playerUuid);
        }
    }

    /**
     * 获取指定团队收到的所有有效入队申请
     */
    public List<Application> getValidApplications(int teamId) {
        Map<UUID, Application> playerMap = pendingApplications.get(teamId);
        if (playerMap == null || playerMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Application> validList = new ArrayList<>();
        List<UUID> expiredUuids = new ArrayList<>();

        for (Map.Entry<UUID, Application> entry : playerMap.entrySet()) {
            Application app = entry.getValue();
            if (app.isExpired()) {
                expiredUuids.add(entry.getKey());
            } else {
                validList.add(app);
            }
        }

        for (UUID expUuid : expiredUuids) {
            playerMap.remove(expUuid);
            if (applicationDao != null) {
                applicationDao.deleteApplication(teamId, expUuid);
            }
        }
        if (playerMap.isEmpty()) {
            pendingApplications.remove(teamId);
        }

        return validList;
    }

    /**
     * 获取指定团队收到的有效入队申请数量
     */
    public int getValidApplicationCount(int teamId) {
        return getValidApplications(teamId).size();
    }
}

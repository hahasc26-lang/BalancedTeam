package com.balancedteam.manager;

import com.balancedteam.database.dao.AllyRequestDao;
import com.balancedteam.database.dao.RelationDao;
import com.balancedteam.model.RelationStatus;
import com.balancedteam.model.RelationType;
import com.balancedteam.model.TeamRelation;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 团队外交（同盟/敌对/结盟申请）管理器（内存+数据库双层持久化）
 */
public class RelationManager {

    private final RelationDao relationDao;
    private final AllyRequestDao allyRequestDao;

    // 所有关系映射：Id -> TeamRelation
    private final Map<Integer, TeamRelation> relationsById = new ConcurrentHashMap<>();

    // 内存暂存盟友申请超时记录: TargetTeamId -> (RequesterTeamId -> ExpireTimestamp)
    private final Map<Integer, Map<Integer, Long>> pendingAllyRequests = new ConcurrentHashMap<>();

    public RelationManager(RelationDao relationDao, AllyRequestDao allyRequestDao) {
        this.relationDao = relationDao;
        this.allyRequestDao = allyRequestDao;
    }

    public void init(List<TeamRelation> loadedRelations) {
        relationsById.clear();
        for (TeamRelation rel : loadedRelations) {
            relationsById.put(rel.getId(), rel);
        }
    }

    public void initRequests(Map<Integer, Map<Integer, Long>> loadedRequests) {
        pendingAllyRequests.clear();
        for (Map.Entry<Integer, Map<Integer, Long>> entry : loadedRequests.entrySet()) {
            pendingAllyRequests.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
        }
        cleanExpired(System.currentTimeMillis());
    }

    /**
     * 清理过期同盟申请
     */
    public void cleanExpired(long now) {
        if (allyRequestDao != null) {
            allyRequestDao.cleanExpiredRequests(now);
        }
    }

    /**
     * 发送结盟请求（内存暂存并持久化到数据库）
     */
    public void sendAllyRequest(int fromTeamId, int toTeamId, long timeoutSeconds) {
        long expireTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        pendingAllyRequests.computeIfAbsent(toTeamId, k -> new ConcurrentHashMap<>())
                .put(fromTeamId, expireTime);

        if (allyRequestDao != null) {
            allyRequestDao.saveRequest(fromTeamId, toTeamId, expireTime);
        }
    }

    /**
     * 检查是否存在有效的结盟申请
     */
    public boolean hasPendingAllyRequest(int fromTeamId, int toTeamId) {
        Map<Integer, Long> map = pendingAllyRequests.get(toTeamId);
        if (map == null) return false;
        Long expire = map.get(fromTeamId);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            map.remove(fromTeamId);
            if (map.isEmpty()) {
                pendingAllyRequests.remove(toTeamId);
            }
            if (allyRequestDao != null) {
                allyRequestDao.deleteRequest(fromTeamId, toTeamId);
            }
            return false;
        }
        return true;
    }

    /**
     * 消费（接受）结盟申请并持久化
     */
    public CompletableFuture<Boolean> acceptAllyRequest(int requesterTeamId, int acceptingTeamId) {
        Map<Integer, Long> map = pendingAllyRequests.get(acceptingTeamId);
        if (map != null) {
            map.remove(requesterTeamId);
            if (map.isEmpty()) {
                pendingAllyRequests.remove(acceptingTeamId);
            }
        }
        if (allyRequestDao != null) {
            allyRequestDao.deleteRequest(requesterTeamId, acceptingTeamId);
        }

        TeamRelation relation = new TeamRelation(
                requesterTeamId,
                acceptingTeamId,
                RelationType.ALLY,
                RelationStatus.ACCEPTED,
                new Timestamp(System.currentTimeMillis())
        );

        return relationDao.createRelation(relation).thenApply(id -> {
            if (id > 0) {
                relationsById.put(id, relation);
                return true;
            }
            return false;
        });
    }

    /**
     * 解除同盟关系
     */
    public CompletableFuture<Boolean> removeAlly(int teamId1, int teamId2) {
        Optional<TeamRelation> opt = relationsById.values().stream()
                .filter(r -> r.getRelationType() == RelationType.ALLY && r.involves(teamId1) && r.involves(teamId2))
                .findFirst();

        if (opt.isPresent()) {
            TeamRelation rel = opt.get();
            return relationDao.deleteRelation(rel.getId()).thenApply(v -> {
                relationsById.remove(rel.getId());
                return true;
            });
        }
        return CompletableFuture.completedFuture(false);
    }

    /**
     * 添加敌对关系 (单向/双方)
     */
    public CompletableFuture<Boolean> addEnemy(int teamId1, int teamId2) {
        if (isEnemy(teamId1, teamId2)) {
            return CompletableFuture.completedFuture(false);
        }

        TeamRelation relation = new TeamRelation(
                teamId1,
                teamId2,
                RelationType.ENEMY,
                RelationStatus.ACCEPTED,
                new Timestamp(System.currentTimeMillis())
        );

        return relationDao.createRelation(relation).thenApply(id -> {
            if (id > 0) {
                relationsById.put(id, relation);
                return true;
            }
            return false;
        });
    }

    /**
     * 移除敌对关系
     */
    public CompletableFuture<Boolean> removeEnemy(int teamId1, int teamId2) {
        Optional<TeamRelation> opt = relationsById.values().stream()
                .filter(r -> r.getRelationType() == RelationType.ENEMY && r.involves(teamId1) && r.involves(teamId2))
                .findFirst();

        if (opt.isPresent()) {
            TeamRelation rel = opt.get();
            return relationDao.deleteRelation(rel.getId()).thenApply(v -> {
                relationsById.remove(rel.getId());
                return true;
            });
        }
        return CompletableFuture.completedFuture(false);
    }

    /**
     * 当团队解散时清理所有外交关系与结盟申请
     */
    public void onTeamDisbanded(int teamId) {
        relationsById.values().removeIf(r -> r.involves(teamId));
        pendingAllyRequests.remove(teamId);
        for (Map<Integer, Long> map : pendingAllyRequests.values()) {
            map.remove(teamId);
        }
        relationDao.deleteRelationsByTeamId(teamId);
        if (allyRequestDao != null) {
            allyRequestDao.deleteRequestsByTeam(teamId);
        }
    }

    /**
     * 判断两队是否为盟友
     */
    public boolean isAlly(int teamId1, int teamId2) {
        if (teamId1 <= 0 || teamId2 <= 0 || teamId1 == teamId2) return false;
        return relationsById.values().stream()
                .anyMatch(r -> r.getRelationType() == RelationType.ALLY
                        && r.getStatus() == RelationStatus.ACCEPTED
                        && r.involves(teamId1)
                        && r.involves(teamId2));
    }

    /**
     * 判断两队是否为敌对
     */
    public boolean isEnemy(int teamId1, int teamId2) {
        if (teamId1 <= 0 || teamId2 <= 0 || teamId1 == teamId2) return false;
        return relationsById.values().stream()
                .anyMatch(r -> r.getRelationType() == RelationType.ENEMY && r.involves(teamId1) && r.involves(teamId2));
    }

    /**
     * 获取指定团队的所有盟友团队 ID 列表
     */
    public List<Integer> getAllies(int teamId) {
        return relationsById.values().stream()
                .filter(r -> r.getRelationType() == RelationType.ALLY && r.getStatus() == RelationStatus.ACCEPTED && r.involves(teamId))
                .map(r -> r.getOtherTeamId(teamId))
                .collect(Collectors.toList());
    }

    /**
     * 获取指定团队的所有敌对团队 ID 列表
     */
    public List<Integer> getEnemies(int teamId) {
        return relationsById.values().stream()
                .filter(r -> r.getRelationType() == RelationType.ENEMY && r.involves(teamId))
                .map(r -> r.getOtherTeamId(teamId))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有向指定队伍发送的有效（未过期）盟友申请方队伍 ID 列表
     * 用于通知中心 GUI 展示待处理申请
     */
    public List<Integer> getPendingRequestsTo(int targetTeamId) {
        Map<Integer, Long> map = pendingAllyRequests.get(targetTeamId);
        if (map == null || map.isEmpty()) return Collections.emptyList();
        long now = System.currentTimeMillis();
        // 同时清理过期项并同步数据库
        List<Integer> expiredRequesterIds = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : map.entrySet()) {
            if (entry.getValue() <= now) {
                expiredRequesterIds.add(entry.getKey());
            }
        }
        for (Integer expId : expiredRequesterIds) {
            map.remove(expId);
            if (allyRequestDao != null) {
                allyRequestDao.deleteRequest(expId, targetTeamId);
            }
        }
        if (map.isEmpty()) {
            pendingAllyRequests.remove(targetTeamId);
        }
        return new ArrayList<>(map.keySet());
    }
}

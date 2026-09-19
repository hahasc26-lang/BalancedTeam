package com.balancedteam.manager;

import com.balancedteam.database.dao.AllyRequestDao;
import com.balancedteam.database.dao.RelationDao;
import com.balancedteam.database.dao.TruceDao;
import com.balancedteam.model.RelationStatus;
import com.balancedteam.model.RelationType;
import com.balancedteam.model.TeamRelation;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 团队外交（同盟/敌对/结盟申请/停战求和/战后保护）管理器（内存+数据库双层持久化）
 */
public class RelationManager {

    private final com.balancedteam.BalancedTeamPlugin plugin;
    private final RelationDao relationDao;
    private final AllyRequestDao allyRequestDao;
    private final TruceDao truceDao;

    // 所有关系映射：Id -> TeamRelation
    private final Map<Integer, TeamRelation> relationsById = new ConcurrentHashMap<>();

    // 内存暂存盟友申请超时记录: TargetTeamId -> (RequesterTeamId -> ExpireTimestamp)
    private final Map<Integer, Map<Integer, Long>> pendingAllyRequests = new ConcurrentHashMap<>();

    // 内存暂存求和申请超时记录: TargetTeamId -> (RequesterTeamId -> ExpireTimestamp)
    private final Map<Integer, Map<Integer, Long>> pendingTruceRequests = new ConcurrentHashMap<>();

    // 战后保护记录映射: "minTeamId:maxTeamId" -> ExpireTimestamp
    private final Map<String, Long> postWarProtections = new ConcurrentHashMap<>();

    public RelationManager(com.balancedteam.BalancedTeamPlugin plugin, RelationDao relationDao, AllyRequestDao allyRequestDao, TruceDao truceDao) {
        this.plugin = plugin;
        this.relationDao = relationDao;
        this.allyRequestDao = allyRequestDao;
        this.truceDao = truceDao;
    }

    public RelationManager(com.balancedteam.BalancedTeamPlugin plugin, RelationDao relationDao, AllyRequestDao allyRequestDao) {
        this(plugin, relationDao, allyRequestDao, null);
    }

    public RelationManager(RelationDao relationDao, AllyRequestDao allyRequestDao) {
        this(com.balancedteam.BalancedTeamPlugin.getInstance(), relationDao, allyRequestDao, null);
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

    public void initTruceRequests(Map<Integer, Map<Integer, Long>> loadedRequests) {
        pendingTruceRequests.clear();
        for (Map.Entry<Integer, Map<Integer, Long>> entry : loadedRequests.entrySet()) {
            pendingTruceRequests.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
        }
        cleanExpiredTruceRequests(System.currentTimeMillis());
    }

    public void initProtections(Map<String, Long> loadedProtections) {
        postWarProtections.clear();
        postWarProtections.putAll(loadedProtections);
        cleanExpiredProtections(System.currentTimeMillis());
    }

    /**
     * 清理过期同盟申请
     */
    public void cleanExpired(long now) {
        if (allyRequestDao != null) {
            allyRequestDao.cleanExpiredRequests(now);
        }
    }

    public void cleanExpiredTruceRequests(long now) {
        if (truceDao != null) {
            truceDao.cleanExpiredTruceRequests(now);
        }
    }

    public void cleanExpiredProtections(long now) {
        if (truceDao != null) {
            truceDao.cleanExpiredProtections(now);
        }
    }

    /**
     * 发送结盟请求（具备同盟上限、已有同盟/宿敌关系互斥检查）
     */
    public void sendAllyRequest(int fromTeamId, int toTeamId, long timeoutSeconds) {
        if (plugin != null && plugin.getConfigManager() != null) {
            int maxAllies = plugin.getConfigManager().getMaxAllies();
            if (getAllies(fromTeamId).size() >= maxAllies || getAllies(toTeamId).size() >= maxAllies) {
                return;
            }
        }
        if (isAlly(fromTeamId, toTeamId) || isEnemy(fromTeamId, toTeamId)) {
            return;
        }

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
     * 消费（接受）结盟申请并持久化（增加双向同盟上限与互斥防御校验）
     */
    public CompletableFuture<Boolean> acceptAllyRequest(int requesterTeamId, int acceptingTeamId) {
        if (plugin != null && plugin.getConfigManager() != null) {
            int maxAllies = plugin.getConfigManager().getMaxAllies();
            if (getAllies(requesterTeamId).size() >= maxAllies || getAllies(acceptingTeamId).size() >= maxAllies) {
                return CompletableFuture.completedFuture(false);
            }
        }
        if (isAlly(requesterTeamId, acceptingTeamId) || isEnemy(requesterTeamId, acceptingTeamId)) {
            return CompletableFuture.completedFuture(false);
        }

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
     * 拒绝结盟申请（从内存与数据库中删除，不建立关系）
     */
    public void denyAllyRequest(int requesterTeamId, int acceptingTeamId) {
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
     * 添加敌对关系 (单向/双方，具备宿敌上限与同盟互斥校验，战后保护期内禁止宣战)
     */
    public CompletableFuture<Boolean> addEnemy(int teamId1, int teamId2) {
        if (isDeclaredEnemy(teamId1, teamId2) || isAlly(teamId1, teamId2) || isUnderPostWarProtection(teamId1, teamId2)) {
            return CompletableFuture.completedFuture(false);
        }

        if (plugin != null && plugin.getConfigManager() != null) {
            int maxEnemies = plugin.getConfigManager().getMaxEnemies();
            // 宣战上限仅限制宣战发起方(teamId1)标记的敌对数量，不应因目标队伍(teamId2)仇家过多而阻止宣战
            if (getDeclaredEnemies(teamId1).size() >= maxEnemies) {
                return CompletableFuture.completedFuture(false);
            }
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
     * 移除敌对关系 (仅允许宣战发起方撤销自己标记的敌对关系)
     */
    public CompletableFuture<Boolean> removeEnemy(int initiatorTeamId, int targetTeamId) {
        Optional<TeamRelation> opt = relationsById.values().stream()
                .filter(r -> r.getRelationType() == RelationType.ENEMY
                        && r.getTeamId1() == initiatorTeamId
                        && r.getTeamId2() == targetTeamId)
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
     * 当团队解散时清理所有外交关系与结盟申请、求和申请及战后保护
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

        // 清理该队伍的求和申请
        pendingTruceRequests.remove(teamId);
        for (Map<Integer, Long> map : pendingTruceRequests.values()) {
            map.remove(teamId);
        }
        if (truceDao != null) {
            truceDao.deleteTruceRequestsByTeam(teamId);
            truceDao.deleteProtectionsByTeam(teamId);
        }

        // 清理内存中的战后保护
        String idStr = String.valueOf(teamId);
        postWarProtections.keySet().removeIf(key -> key.startsWith(idStr + ":") || key.endsWith(":" + idStr));
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
     * 判断某队伍是否主动将另一队伍标记为敌对（单向宣战发起方检查）
     */
    public boolean isDeclaredEnemy(int initiatorTeamId, int targetTeamId) {
        if (initiatorTeamId <= 0 || targetTeamId <= 0 || initiatorTeamId == targetTeamId) return false;
        return relationsById.values().stream()
                .anyMatch(r -> r.getRelationType() == RelationType.ENEMY
                        && r.getTeamId1() == initiatorTeamId
                        && r.getTeamId2() == targetTeamId);
    }

    /**
     * 获取指定团队主动宣战标记的所有敌对团队 ID 列表
     */
    public List<Integer> getDeclaredEnemies(int teamId) {
        return relationsById.values().stream()
                .filter(r -> r.getRelationType() == RelationType.ENEMY && r.getTeamId1() == teamId)
                .map(TeamRelation::getTeamId2)
                .collect(Collectors.toList());
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

    // =========================================================================
    // 战后保护机制 (Post-War Protection)
    // =========================================================================

    private String getProtectionKey(int teamId1, int teamId2) {
        return Math.min(teamId1, teamId2) + ":" + Math.max(teamId1, teamId2);
    }

    /**
     * 开启两队之间的战后保护
     */
    public void startPostWarProtection(int teamId1, int teamId2, long durationSeconds) {
        if (teamId1 <= 0 || teamId2 <= 0 || teamId1 == teamId2 || durationSeconds <= 0) return;
        long expireTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        postWarProtections.put(getProtectionKey(teamId1, teamId2), expireTime);
        if (truceDao != null) {
            truceDao.saveProtection(teamId1, teamId2, expireTime);
        }
    }

    /**
     * 判断两队当前是否处于战后保护期
     */
    public boolean isUnderPostWarProtection(int teamId1, int teamId2) {
        if (teamId1 <= 0 || teamId2 <= 0 || teamId1 == teamId2) return false;
        String key = getProtectionKey(teamId1, teamId2);
        Long expireTime = postWarProtections.get(key);
        if (expireTime == null) return false;
        long now = System.currentTimeMillis();
        if (expireTime <= now) {
            postWarProtections.remove(key);
            if (truceDao != null) {
                truceDao.deleteProtection(teamId1, teamId2);
            }
            return false;
        }
        return true;
    }

    /**
     * 获取两队之间战后保护剩余秒数（若无保护或已过期返回 0）
     */
    public long getPostWarProtectionRemainingSeconds(int teamId1, int teamId2) {
        if (teamId1 <= 0 || teamId2 <= 0 || teamId1 == teamId2) return 0;
        String key = getProtectionKey(teamId1, teamId2);
        Long expireTime = postWarProtections.get(key);
        if (expireTime == null) return 0;
        long now = System.currentTimeMillis();
        if (expireTime <= now) {
            postWarProtections.remove(key);
            if (truceDao != null) {
                truceDao.deleteProtection(teamId1, teamId2);
            }
            return 0;
        }
        return (expireTime - now + 999L) / 1000L;
    }

    /**
     * 手动清除战后保护
     */
    public void deletePostWarProtection(int teamId1, int teamId2) {
        postWarProtections.remove(getProtectionKey(teamId1, teamId2));
        if (truceDao != null) {
            truceDao.deleteProtection(teamId1, teamId2);
        }
    }

    // =========================================================================
    // 求和机制 (Truce Request & Peace Agreement)
    // =========================================================================

    /**
     * 检查是否已有生效中的求和申请
     */
    public boolean hasPendingTruceRequest(int fromTeamId, int toTeamId) {
        Map<Integer, Long> map = pendingTruceRequests.get(toTeamId);
        if (map == null) return false;
        Long expireTime = map.get(fromTeamId);
        if (expireTime == null) return false;
        if (expireTime <= System.currentTimeMillis()) {
            map.remove(fromTeamId);
            if (truceDao != null) {
                truceDao.deleteTruceRequest(fromTeamId, toTeamId);
            }
            return false;
        }
        return true;
    }

    /**
     * 获取求和申请剩余秒数
     */
    public long getTruceRequestRemainingSeconds(int fromTeamId, int toTeamId) {
        Map<Integer, Long> map = pendingTruceRequests.get(toTeamId);
        if (map == null) return 0;
        Long expireTime = map.get(fromTeamId);
        if (expireTime == null) return 0;
        long now = System.currentTimeMillis();
        if (expireTime <= now) {
            map.remove(fromTeamId);
            if (truceDao != null) {
                truceDao.deleteTruceRequest(fromTeamId, toTeamId);
            }
            return 0;
        }
        return (expireTime - now + 999L) / 1000L;
    }

    /**
     * 发送求和申请（双方必须当前为敌对状态，且未有生效中的求和申请）
     */
    public CompletableFuture<Boolean> sendTruceRequest(int fromTeamId, int toTeamId) {
        if (!isEnemy(fromTeamId, toTeamId)) {
            return CompletableFuture.completedFuture(false);
        }
        if (hasPendingTruceRequest(fromTeamId, toTeamId)) {
            return CompletableFuture.completedFuture(false);
        }

        int timeout = (plugin != null && plugin.getConfigManager() != null)
                ? plugin.getConfigManager().getTruceRequestTimeout() : 300;
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);

        pendingTruceRequests.computeIfAbsent(toTeamId, k -> new ConcurrentHashMap<>()).put(fromTeamId, expireTime);

        if (truceDao != null) {
            return truceDao.saveTruceRequest(fromTeamId, toTeamId, expireTime).thenApply(v -> true);
        }
        return CompletableFuture.completedFuture(true);
    }

    /**
     * 撤销求和申请
     */
    public void cancelTruceRequest(int fromTeamId, int toTeamId) {
        Map<Integer, Long> map = pendingTruceRequests.get(toTeamId);
        if (map != null) {
            map.remove(fromTeamId);
            if (map.isEmpty()) {
                pendingTruceRequests.remove(toTeamId);
            }
        }
        if (truceDao != null) {
            truceDao.deleteTruceRequest(fromTeamId, toTeamId);
        }
    }

    /**
     * 拒绝求和申请
     */
    public void denyTruceRequest(int requesterTeamId, int acceptingTeamId) {
        cancelTruceRequest(requesterTeamId, acceptingTeamId);
    }

    /**
     * 接受求和申请（双方达成停战协议）：
     * 1. 清理双方所有的求和申请
     * 2. 删除两队之间的全部敌对关系（无论单向还是互设）
     * 3. 自动开启战后保护机制
     */
    public CompletableFuture<Boolean> acceptTruceRequest(int requesterTeamId, int acceptingTeamId) {
        cancelTruceRequest(requesterTeamId, acceptingTeamId);
        cancelTruceRequest(acceptingTeamId, requesterTeamId);

        return removeAllEnemyRelations(requesterTeamId, acceptingTeamId).thenApply(success -> {
            if (success) {
                int protectionSeconds = (plugin != null && plugin.getConfigManager() != null)
                        ? plugin.getConfigManager().getPostWarProtectionSeconds() : 1800;
                if (protectionSeconds > 0) {
                    startPostWarProtection(requesterTeamId, acceptingTeamId, protectionSeconds);
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 删除两队之间的全部敌对关系（不论是 A->B 还是 B->A）
     */
    public CompletableFuture<Boolean> removeAllEnemyRelations(int teamId1, int teamId2) {
        List<TeamRelation> toDelete = relationsById.values().stream()
                .filter(r -> r.getRelationType() == RelationType.ENEMY && r.involves(teamId1) && r.involves(teamId2))
                .collect(Collectors.toList());

        if (toDelete.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (TeamRelation rel : toDelete) {
            futures.add(relationDao.deleteRelation(rel.getId()).thenRun(() -> {
                relationsById.remove(rel.getId());
            }));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> true);
    }

    /**
     * 获取所有向指定队伍发送有效求和申请的团队 ID 列表
     */
    public List<Integer> getPendingTruceRequestsTo(int targetTeamId) {
        Map<Integer, Long> map = pendingTruceRequests.get(targetTeamId);
        if (map == null || map.isEmpty()) return Collections.emptyList();
        long now = System.currentTimeMillis();
        List<Integer> expiredIds = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : map.entrySet()) {
            if (entry.getValue() <= now) {
                expiredIds.add(entry.getKey());
            }
        }
        for (Integer expId : expiredIds) {
            map.remove(expId);
            if (truceDao != null) {
                truceDao.deleteTruceRequest(expId, targetTeamId);
            }
        }
        if (map.isEmpty()) {
            pendingTruceRequests.remove(targetTeamId);
        }
        return new ArrayList<>(map.keySet());
    }

    /**
     * 获取指定队伍对外发送的所有有效求和申请的目标团队 ID 列表
     */
    public List<Integer> getPendingTruceRequestsFrom(int fromTeamId) {
        List<Integer> targetIds = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Map<Integer, Long>> entry : pendingTruceRequests.entrySet()) {
            int toTeamId = entry.getKey();
            Map<Integer, Long> map = entry.getValue();
            Long exp = map.get(fromTeamId);
            if (exp != null) {
                if (exp > now) {
                    targetIds.add(toTeamId);
                } else {
                    map.remove(fromTeamId);
                    if (truceDao != null) {
                        truceDao.deleteTruceRequest(fromTeamId, toTeamId);
                    }
                }
            }
        }
        return targetIds;
    }

    /**
     * 获取指定团队参与的所有有效战后保护（返回: 对方团队 ID -> 剩余秒数）
     */
    public Map<Integer, Long> getActivePostWarProtectionsFor(int teamId) {
        Map<Integer, Long> result = new HashMap<>();
        long now = System.currentTimeMillis();
        List<String> expiredKeys = new ArrayList<>();
        for (Map.Entry<String, Long> entry : postWarProtections.entrySet()) {
            String key = entry.getKey();
            Long exp = entry.getValue();
            if (exp <= now) {
                expiredKeys.add(key);
                continue;
            }
            String[] parts = key.split(":");
            if (parts.length == 2) {
                try {
                    int t1 = Integer.parseInt(parts[0]);
                    int t2 = Integer.parseInt(parts[1]);
                    if (t1 == teamId) {
                        result.put(t2, (exp - now + 999L) / 1000L);
                    } else if (t2 == teamId) {
                        result.put(t1, (exp - now + 999L) / 1000L);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        for (String expKey : expiredKeys) {
            postWarProtections.remove(expKey);
            String[] parts = expKey.split(":");
            if (parts.length == 2 && truceDao != null) {
                try {
                    truceDao.deleteProtection(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }
}

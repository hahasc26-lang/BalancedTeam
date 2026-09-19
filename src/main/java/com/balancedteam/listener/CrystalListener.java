package com.balancedteam.listener;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.model.Team;
import com.balancedteam.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 末影水晶监听器 (针对 Anarchy / 阵营服 Crystal PvP 深度优化)
 * 职责：
 * 1. 监听末影水晶放置行为 (EntityPlaceEvent 与 PlayerInteractEvent 双重保障)
 * 2. 监听末影水晶攻击/引爆行为 (EntityDamageByEntityEvent)，支持近战击打、弹射物穿透及水晶连锁爆炸
 * 3. 关联爆炸伤害源与实际责任玩家，保障队伍友伤与同盟保护机制覆盖末影水晶
 * 4. 自动管理水晶缓存生命周期，防止内存泄漏
 */
public class CrystalListener implements Listener {

    private final BalancedTeamPlugin plugin;

    // 水晶放置者记录 (CrystalUUID -> Record)
    private final Map<UUID, CrystalRecord> crystalPlacers = new ConcurrentHashMap<>();

    // 水晶引爆者记录 (CrystalUUID -> Record)
    private final Map<UUID, CrystalRecord> crystalDetonators = new ConcurrentHashMap<>();

    // 交互放置预记录 (World:X:Y:Z -> PendingRecord)，用于兼容无法触发 EntityPlaceEvent 的环境
    private final Map<String, PendingRecord> pendingPlacements = new ConcurrentHashMap<>();

    // 提示消息冷却 (PlayerUUID -> LastWarnTimeMillis)
    private final Map<UUID, Long> warnCooldowns = new ConcurrentHashMap<>();

    // 定期清理定时任务
    private BukkitTask cleanupTask;

    public CrystalListener(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    /**
     * 1. 监听末影水晶放置事件 (Paper / Spigot 标准实体放置事件)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (event.getEntity() instanceof EnderCrystal && event.getPlayer() != null) {
            recordPlacer(event.getEntity().getUniqueId(), event.getPlayer().getUniqueId());
        }
    }

    /**
     * 2. 玩家右键交互预监听 (作为 EntityPlaceEvent 在部分服务端衍生版失效时的兜底保障)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Material blockType = event.getClickedBlock().getType();
        if (blockType != Material.OBSIDIAN && blockType != Material.BEDROCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.END_CRYSTAL) {
            Location blockLoc = event.getClickedBlock().getLocation();
            String key = formatLocationKey(blockLoc.getWorld().getName(), blockLoc.getBlockX(), blockLoc.getBlockY() + 1, blockLoc.getBlockZ());
            pendingPlacements.put(key, new PendingRecord(event.getPlayer().getUniqueId(), System.currentTimeMillis()));
        }
    }

    /**
     * 3. 实体生成监听：匹配右键预放置记录与生成的水晶实体
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal)) {
            return;
        }

        EnderCrystal crystal = (EnderCrystal) event.getEntity();
        if (crystalPlacers.containsKey(crystal.getUniqueId())) {
            return;
        }

        Location loc = crystal.getLocation();
        String key = formatLocationKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        PendingRecord pending = pendingPlacements.remove(key);

        // 如果精确坐标未匹配，尝试在当前方块上下浮动 1 格匹配
        if (pending == null) {
            String lowerKey = formatLocationKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
            pending = pendingPlacements.remove(lowerKey);
        }

        if (pending != null && (System.currentTimeMillis() - pending.getTimestamp() < 3000L)) {
            recordPlacer(crystal.getUniqueId(), pending.getPlayerId());
        }
    }

    /**
     * 4. 监听末影水晶受损 / 被引爆事件 (近战、弓箭、三叉戟、TNT、雪球或连锁水晶爆炸)
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal)) {
            return;
        }

        EnderCrystal crystal = (EnderCrystal) event.getEntity();
        Entity damager = event.getDamager();
        Player attacker = resolveAttacker(damager);

        if (attacker != null) {
            // 可选：防拆同队/同盟水晶判定 (balance.prevent_friendly_crystal_break，默认 false，仅在友伤保护机制启用时生效)
            if (plugin.getConfigManager().isFriendlyFireProtectionEnabled() && plugin.getConfigManager().isPreventFriendlyCrystalBreak()) {
                UUID placerId = getPlacer(crystal.getUniqueId());
                if (placerId != null && !placerId.equals(attacker.getUniqueId())) {
                    Team attackerTeam = plugin.getTeamManager().getTeamByPlayer(attacker.getUniqueId());
                    Team placerTeam = plugin.getTeamManager().getTeamByPlayer(placerId);
                    if (attackerTeam != null && placerTeam != null) {
                        // 1. 同队
                        if (attackerTeam.getId() == placerTeam.getId() && !plugin.getConfigManager().isFriendlyFireActive(attackerTeam)) {
                            event.setCancelled(true);
                            Player placer = Bukkit.getPlayer(placerId);
                            String placerName = placer != null ? placer.getName() : "Teammate";
                            sendWarning(attacker, "team_crystal_protected", placerName);
                            return;
                        }
                        // 2. 同盟
                        if (attackerTeam.getId() != placerTeam.getId() && plugin.getRelationManager().isAlly(attackerTeam.getId(), placerTeam.getId())) {
                            if (!plugin.getConfigManager().isAllyFriendlyFireAllowed()) {
                                event.setCancelled(true);
                                Player placer = Bukkit.getPlayer(placerId);
                                String placerName = placer != null ? placer.getName() : "Ally";
                                sendWarning(attacker, "ally_crystal_protected", placerName);
                                return;
                            }
                        }
                    }
                }
            }

            // 仅在未被取消时记录该水晶的引爆者玩家
            if (!event.isCancelled()) {
                recordDetonator(crystal.getUniqueId(), attacker.getUniqueId());
            }
        } else if (damager instanceof EnderCrystal) {
            // 水晶连锁爆炸传递：水晶 A 炸爆了水晶 B，水晶 B 的实际责任人传递为水晶 A 的责任人
            UUID sourceDetonator = getDetonator(damager.getUniqueId());
            if (sourceDetonator != null) {
                recordDetonator(crystal.getUniqueId(), sourceDetonator);
            } else {
                UUID sourcePlacer = getPlacer(damager.getUniqueId());
                if (sourcePlacer != null) {
                    recordDetonator(crystal.getUniqueId(), sourcePlacer);
                }
            }
        }
    }

    /**
     * 监控外部保护插件（如 WorldGuard、Residence、主城保护）对水晶破坏事件的取消，
     * 若事件被取消则撤销引爆者记录，杜绝虚假幽灵引爆者 (Phantom Detonator) 漏洞。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCrystalDamageCancelledMonitor(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof EnderCrystal && event.isCancelled()) {
            crystalDetonators.remove(event.getEntity().getUniqueId());
        }
    }

    /**
     * 5. 水晶准备爆炸或发生爆炸，延迟清理缓存
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof EnderCrystal) {
            scheduleRemoval(event.getEntity().getUniqueId(), 100L); // 5秒后清理
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof EnderCrystal) {
            scheduleRemoval(event.getEntity().getUniqueId(), 100L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderCrystal) {
            scheduleRemoval(event.getEntity().getUniqueId(), 100L);
        }
    }

    /**
     * 解析末影水晶的实际引爆人/责任玩家
     * 判定顺序：
     * 1. 优先读取最后一次直接引爆该水晶的玩家 (Detonator)
     * 2. 若无直接引爆者记录，回退至放置该水晶的玩家 (Placer)
     *
     * @param crystal 末影水晶实体
     * @return 责任玩家 (在线) 或 null
     */
    public Player getResponsiblePlayer(EnderCrystal crystal) {
        if (crystal == null) {
            return null;
        }
        UUID crystalId = crystal.getUniqueId();

        // 1. 引爆者
        CrystalRecord detRecord = crystalDetonators.get(crystalId);
        if (detRecord != null) {
            Player player = Bukkit.getPlayer(detRecord.getPlayerId());
            if (player != null && player.isOnline()) {
                return player;
            }
        }

        // 2. 放置者
        CrystalRecord placeRecord = crystalPlacers.get(crystalId);
        if (placeRecord != null) {
            Player player = Bukkit.getPlayer(placeRecord.getPlayerId());
            if (player != null && player.isOnline()) {
                return player;
            }
        }

        return null;
    }

    /**
     * 获取末影水晶的责任玩家 UUID (即便玩家离线也能获取到 UUID 用于队伍数据比对)
     */
    public UUID getResponsiblePlayerUUID(UUID crystalId) {
        if (crystalId == null) {
            return null;
        }
        CrystalRecord detRecord = crystalDetonators.get(crystalId);
        if (detRecord != null) {
            return detRecord.getPlayerId();
        }
        CrystalRecord placeRecord = crystalPlacers.get(crystalId);
        if (placeRecord != null) {
            return placeRecord.getPlayerId();
        }
        return null;
    }

    public UUID getResponsiblePlayerUUID(EnderCrystal crystal) {
        return crystal != null ? getResponsiblePlayerUUID(crystal.getUniqueId()) : null;
    }

    public UUID getPlacer(UUID crystalId) {
        CrystalRecord record = crystalPlacers.get(crystalId);
        return record != null ? record.getPlayerId() : null;
    }

    public UUID getDetonator(UUID crystalId) {
        CrystalRecord record = crystalDetonators.get(crystalId);
        return record != null ? record.getPlayerId() : null;
    }

    public void recordPlacer(UUID crystalId, UUID playerId) {
        crystalPlacers.put(crystalId, new CrystalRecord(playerId, System.currentTimeMillis()));
    }

    public void recordDetonator(UUID crystalId, UUID playerId) {
        crystalDetonators.put(crystalId, new CrystalRecord(playerId, System.currentTimeMillis()));
    }

    /**
     * 解析攻击源所对应的玩家
     */
    public Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        if (damager instanceof TNTPrimed) {
            Entity source = ((TNTPrimed) damager).getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        return null;
    }

    private void scheduleRemoval(UUID crystalId, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            crystalPlacers.remove(crystalId);
            crystalDetonators.remove(crystalId);
        }, delayTicks);
    }

    private void startCleanupTask() {
        // 每 60 秒定期清理超过 30 分钟未爆炸的历史放置记录与超过 10 秒的预放置记录，防止内存泄漏
        this.cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();

            // 清理过期预放置记录 (5秒有效)
            pendingPlacements.entrySet().removeIf(entry -> now - entry.getValue().getTimestamp() > 5000L);

            // 清理过期引爆记录 (30秒后即便未被正常销毁也自动清除)
            crystalDetonators.entrySet().removeIf(entry -> now - entry.getValue().getTimestamp() > 30000L);

            // 清理长期未被引爆的水晶放置记录 (30分钟未被引爆且世界中不再有效)
            if (crystalPlacers.size() > 2000) {
                crystalPlacers.entrySet().removeIf(entry -> now - entry.getValue().getTimestamp() > 1800000L);
            }
        }, 1200L, 1200L);
    }

    public void clear() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        crystalPlacers.clear();
        crystalDetonators.clear();
        pendingPlacements.clear();
        warnCooldowns.clear();
    }

    private void sendWarning(Player player, String messageKey, String targetName) {
        long now = System.currentTimeMillis();
        Long last = warnCooldowns.get(player.getUniqueId());
        if (last == null || (now - last) > 1500L) {
            warnCooldowns.put(player.getUniqueId(), now);
            Map<String, String> map = new HashMap<>();
            map.put("PLAYER", targetName);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, messageKey, map));
        }
    }

    private String formatLocationKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    /**
     * 水晶记录实体
     */
    public static class CrystalRecord {
        private final UUID playerId;
        private final long timestamp;

        public CrystalRecord(UUID playerId, long timestamp) {
            this.playerId = playerId;
            this.timestamp = timestamp;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 预放置记录
     */
    private static class PendingRecord {
        private final UUID playerId;
        private final long timestamp;

        public PendingRecord(UUID playerId, long timestamp) {
            this.playerId = playerId;
            this.timestamp = timestamp;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}

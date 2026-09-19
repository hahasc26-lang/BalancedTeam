package com.balancedteam.listener;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.model.Team;
import com.balancedteam.util.MessageUtil;
import com.balancedteam.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 友伤与同盟伤害阻断监听器 (针对无规则高频战斗深度优化，支持近战、远程、药水、滞留云、末影水晶、TNT、引雷闪电及驯服宠物)
 */
public class DamageListener implements Listener {

    private final BalancedTeamPlugin plugin;
    // 消息提示冷却，防止高频连击刷屏 (PlayerUUID -> LastWarnTimeMillis)
    private final Map<UUID, Long> warnCooldowns = new ConcurrentHashMap<>();

    public DamageListener(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // 全局友伤保护开关检测：若服务端管理员全局禁用了友伤保护机制，则插件不进行任何伤害拦截
        if (!plugin.getConfigManager().isFriendlyFireProtectionEnabled()) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Entity damager = event.getDamager();
        boolean isCrystalDamage = damager instanceof EnderCrystal;

        UUID attackerUuid = getAttackerUuid(damager);
        if (attackerUuid == null || attackerUuid.equals(victim.getUniqueId())) {
            return;
        }

        Team victimTeam = plugin.getTeamManager().getTeamByPlayer(victim.getUniqueId());
        if (victimTeam == null) {
            return;
        }

        Team attackerTeam = plugin.getTeamManager().getTeamByPlayer(attackerUuid);
        if (attackerTeam == null) {
            return;
        }

        Player attackerPlayer = Bukkit.getPlayer(attackerUuid);

        // 1. 同队友伤判定
        if (victimTeam.getId() == attackerTeam.getId()) {
            if (!plugin.getConfigManager().isFriendlyFireActive(victimTeam)) {
                event.setCancelled(true);
                if (attackerPlayer != null && attackerPlayer.isOnline()) {
                    String msgKey = isCrystalDamage ? "team_crystal_ff_protected" : "team_ff_protected";
                    sendDamageWarning(attackerPlayer, msgKey, victim.getName());
                }
                return;
            }
        }

        // 2. 同盟队伍伤害保护判定
        if (victimTeam.getId() != attackerTeam.getId()) {
            if (plugin.getRelationManager().isAlly(victimTeam.getId(), attackerTeam.getId())) {
                if (!plugin.getConfigManager().isAllyFriendlyFireAllowed()) {
                    event.setCancelled(true);
                    if (attackerPlayer != null && attackerPlayer.isOnline()) {
                        String msgKey = isCrystalDamage ? "ally_crystal_ff_protected" : "ally_ff_protected";
                        sendDamageWarning(attackerPlayer, msgKey, victim.getName());
                    }
                    return;
                }
            }

            // 3. 战后保护机制判定（停战两队之间禁止互相伤害）
            long remaining = plugin.getRelationManager().getPostWarProtectionRemainingSeconds(victimTeam.getId(), attackerTeam.getId());
            if (remaining > 0) {
                event.setCancelled(true);
                if (attackerPlayer != null && attackerPlayer.isOnline()) {
                    Map<String, String> map = new HashMap<>();
                    map.put("PLAYER", victim.getName());
                    map.put("TEAM", victimTeam.getName());
                    map.put("TIME", TimeUtil.formatDuration(attackerPlayer, remaining));
                    sendDamageWarning(attackerPlayer, "team_post_war_protected", map);
                }
                return;
            }
        }
    }

    /**
     * 喷溅药水友伤拦截 (防止使用剧毒、迟缓、虚弱、伤害等负面药水恶意伤害队友或盟友)
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!plugin.getConfigManager().isFriendlyFireProtectionEnabled()) {
            return;
        }

        ProjectileSource shooter = event.getPotion().getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }

        Player attacker = (Player) shooter;
        Team attackerTeam = plugin.getTeamManager().getTeamByPlayer(attacker.getUniqueId());
        if (attackerTeam == null) {
            return;
        }

        // 检查药水是否包含有害/负面效果
        boolean hasHarmful = false;
        for (PotionEffect effect : event.getPotion().getEffects()) {
            if (isHarmfulEffect(effect.getType())) {
                hasHarmful = true;
                break;
            }
        }
        if (!hasHarmful) {
            return;
        }

        for (LivingEntity entity : event.getAffectedEntities()) {
            if (!(entity instanceof Player)) {
                continue;
            }
            Player victim = (Player) entity;
            if (victim.getUniqueId().equals(attacker.getUniqueId())) {
                continue;
            }

            Team victimTeam = plugin.getTeamManager().getTeamByPlayer(victim.getUniqueId());
            if (victimTeam == null) {
                continue;
            }

            if (victimTeam.getId() == attackerTeam.getId()) {
                if (!plugin.getConfigManager().isFriendlyFireActive(victimTeam)) {
                    event.setIntensity(victim, 0.0);
                    sendDamageWarning(attacker, "team_ff_protected", victim.getName());
                }
            } else if (plugin.getRelationManager().isAlly(victimTeam.getId(), attackerTeam.getId())) {
                if (!plugin.getConfigManager().isAllyFriendlyFireAllowed()) {
                    event.setIntensity(victim, 0.0);
                    sendDamageWarning(attacker, "ally_ff_protected", victim.getName());
                }
            } else if (plugin.getRelationManager().isUnderPostWarProtection(victimTeam.getId(), attackerTeam.getId())) {
                event.setIntensity(victim, 0.0);
                long remaining = plugin.getRelationManager().getPostWarProtectionRemainingSeconds(victimTeam.getId(), attackerTeam.getId());
                Map<String, String> map = new HashMap<>();
                map.put("PLAYER", victim.getName());
                map.put("TEAM", victimTeam.getName());
                map.put("TIME", TimeUtil.formatDuration(attacker, remaining));
                sendDamageWarning(attacker, "team_post_war_protected", map);
            }
        }
    }

    /**
     * 滞留药水云友伤拦截
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {
        if (!plugin.getConfigManager().isFriendlyFireProtectionEnabled()) {
            return;
        }

        ProjectileSource source = event.getEntity().getSource();
        if (!(source instanceof Player)) {
            return;
        }

        Player attacker = (Player) source;
        Team attackerTeam = plugin.getTeamManager().getTeamByPlayer(attacker.getUniqueId());
        if (attackerTeam == null) {
            return;
        }

        boolean hasHarmful = false;
        for (PotionEffect effect : event.getEntity().getCustomEffects()) {
            if (isHarmfulEffect(effect.getType())) {
                hasHarmful = true;
                break;
            }
        }
        if (!hasHarmful) {
            try {
                if (event.getEntity().getBasePotionData() != null) {
                    org.bukkit.potion.PotionType pt = event.getEntity().getBasePotionData().getType();
                    if (pt != null) {
                        for (PotionEffect pe : pt.getPotionEffects()) {
                            if (isHarmfulEffect(pe.getType())) {
                                hasHarmful = true;
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (!hasHarmful) {
            return;
        }

        Iterator<LivingEntity> iterator = event.getAffectedEntities().iterator();
        while (iterator.hasNext()) {
            LivingEntity entity = iterator.next();
            if (!(entity instanceof Player)) {
                continue;
            }
            Player victim = (Player) entity;
            if (victim.getUniqueId().equals(attacker.getUniqueId())) {
                continue;
            }

            Team victimTeam = plugin.getTeamManager().getTeamByPlayer(victim.getUniqueId());
            if (victimTeam == null) {
                continue;
            }

            if (victimTeam.getId() == attackerTeam.getId()) {
                if (!plugin.getConfigManager().isFriendlyFireActive(victimTeam)) {
                    iterator.remove();
                    sendDamageWarning(attacker, "team_ff_protected", victim.getName());
                }
            } else if (plugin.getRelationManager().isAlly(victimTeam.getId(), attackerTeam.getId())) {
                if (!plugin.getConfigManager().isAllyFriendlyFireAllowed()) {
                    iterator.remove();
                    sendDamageWarning(attacker, "ally_ff_protected", victim.getName());
                }
            } else if (plugin.getRelationManager().isUnderPostWarProtection(victimTeam.getId(), attackerTeam.getId())) {
                iterator.remove();
                long remaining = plugin.getRelationManager().getPostWarProtectionRemainingSeconds(victimTeam.getId(), attackerTeam.getId());
                Map<String, String> map = new HashMap<>();
                map.put("PLAYER", victim.getName());
                map.put("TEAM", victimTeam.getName());
                map.put("TIME", TimeUtil.formatDuration(attacker, remaining));
                sendDamageWarning(attacker, "team_post_war_protected", map);
            }
        }
    }

    /**
     * 判断药水效果是否为有害减益效果
     */
    public static boolean isHarmfulEffect(PotionEffectType type) {
        if (type == null) return false;
        String name = type.getName();
        return name.equals("HARM") || name.equals("POISON") || name.equals("WITHER")
                || name.equals("SLOW") || name.equals("WEAKNESS") || name.equals("BLINDNESS")
                || name.equals("CONFUSION") || name.equals("HUNGER") || name.equals("LEVITATION")
                || name.equals("UNLUCK") || name.equals("DARKNESS") || name.equals("BAD_OMEN")
                || name.equals("SLOW_DIGGING");
    }

    /**
     * 解析实际攻击者玩家 UUID（支持近战、弹射物、药水、滞留云、末影水晶、TNT、引雷闪电及驯服宠物）
     * 即便引爆者或攻击者在伤害计算瞬间离线或死亡，也能准确返回 UUID 供队伍校验
     */
    public UUID getAttackerUuid(Entity damager) {
        if (damager == null) {
            return null;
        }
        if (damager instanceof Player) {
            return damager.getUniqueId();
        }
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return ((Player) shooter).getUniqueId();
            }
        }
        if (damager instanceof ThrownPotion) {
            ProjectileSource shooter = ((ThrownPotion) damager).getShooter();
            if (shooter instanceof Player) {
                return ((Player) shooter).getUniqueId();
            }
        }
        if (damager instanceof AreaEffectCloud) {
            ProjectileSource shooter = ((AreaEffectCloud) damager).getSource();
            if (shooter instanceof Player) {
                return ((Player) shooter).getUniqueId();
            }
        }
        if (damager instanceof EnderCrystal) {
            if (plugin.getCrystalListener() != null) {
                return plugin.getCrystalListener().getResponsiblePlayerUUID((EnderCrystal) damager);
            }
        }
        if (damager instanceof TNTPrimed) {
            Entity source = ((TNTPrimed) damager).getSource();
            if (source instanceof Player) {
                return source.getUniqueId();
            }
        }
        if (damager instanceof LightningStrike) {
            LightningStrike lightning = (LightningStrike) damager;
            Entity causingEntity = lightning.getCausingEntity();
            if (causingEntity instanceof Player) {
                return causingEntity.getUniqueId();
            }
        }
        if (damager instanceof Tameable) {
            AnimalTamer owner = ((Tameable) damager).getOwner();
            if (owner != null) {
                return owner.getUniqueId();
            }
        }
        return null;
    }

    /**
     * 兼容性保留：解析实际攻击者玩家（在线）
     */
    public Player getAttackerPlayer(Entity damager) {
        UUID uuid = getAttackerUuid(damager);
        return uuid != null ? Bukkit.getPlayer(uuid) : null;
    }

    private void sendDamageWarning(Player attacker, String messageKey, String victimName) {
        Map<String, String> map = new HashMap<>();
        map.put("PLAYER", victimName);
        sendDamageWarning(attacker, messageKey, map);
    }

    private void sendDamageWarning(Player attacker, String messageKey, Map<String, String> placeholders) {
        long now = System.currentTimeMillis();
        Long last = warnCooldowns.get(attacker.getUniqueId());
        if (last == null || (now - last) > 1500L) {
            warnCooldowns.put(attacker.getUniqueId(), now);
            String msg = plugin.getConfigManager().getMessage(attacker, messageKey, placeholders);
            // 缺失消息回退机制
            if (msg.contains("Missing message") && messageKey.endsWith("_crystal_ff_protected")) {
                String fallbackKey = messageKey.contains("team") ? "team_ff_protected" : "ally_ff_protected";
                msg = plugin.getConfigManager().getMessage(attacker, fallbackKey, placeholders);
            }
            MessageUtil.sendMessage(attacker, msg);
        }
    }
}

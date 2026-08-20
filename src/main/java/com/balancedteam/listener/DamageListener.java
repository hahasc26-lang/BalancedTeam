package com.balancedteam.listener;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.model.Team;
import com.balancedteam.util.MessageUtil;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 友伤与同盟伤害阻断监听器 (针对无规则高频战斗深度优化)
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

        Player victim = (Player) event.getEntity();
        Player attacker = getAttackerPlayer(event.getDamager());

        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        Team victimTeam = plugin.getTeamManager().getTeamByPlayer(victim.getUniqueId());
        if (victimTeam == null) {
            return;
        }

        Team attackerTeam = plugin.getTeamManager().getTeamByPlayer(attacker.getUniqueId());
        if (attackerTeam == null) {
            return;
        }

        // 1. 同队友伤判定
        if (victimTeam.getId() == attackerTeam.getId()) {
            if (!plugin.getConfigManager().isFriendlyFireActive(victimTeam)) {
                event.setCancelled(true);
                sendDamageWarning(attacker, "team_ff_protected", victim.getName());
                return;
            }
        }

        // 2. 同盟队伍伤害保护判定
        if (victimTeam.getId() != attackerTeam.getId()) {
            if (plugin.getRelationManager().isAlly(victimTeam.getId(), attackerTeam.getId())) {
                if (!plugin.getConfigManager().isAllyFriendlyFireAllowed()) {
                    event.setCancelled(true);
                    sendDamageWarning(attacker, "ally_ff_protected", victim.getName());
                }
            }
        }
    }

    /**
     * 解析实际攻击者玩家（支持近战、弹射物、投掷药水、滞留药水云等）
     */
    private Player getAttackerPlayer(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        if (damager instanceof ThrownPotion) {
            ProjectileSource shooter = ((ThrownPotion) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        if (damager instanceof AreaEffectCloud) {
            ProjectileSource shooter = ((AreaEffectCloud) damager).getSource();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        return null;
    }

    private void sendDamageWarning(Player attacker, String messageKey, String victimName) {
        long now = System.currentTimeMillis();
        Long last = warnCooldowns.get(attacker.getUniqueId());
        if (last == null || (now - last) > 1500L) {
            warnCooldowns.put(attacker.getUniqueId(), now);
            Map<String, String> map = new HashMap<>();
            map.put("PLAYER", victimName);
            MessageUtil.sendMessage(attacker, plugin.getConfigManager().getMessage(messageKey, map));
        }
    }
}

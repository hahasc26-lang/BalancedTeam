package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.config.GuiConfigKeys;
import com.balancedteam.gui.util.ItemBuilder;
import com.balancedteam.gui.util.PagedGuiHelper;
import com.balancedteam.model.Team;
import com.balancedteam.model.TeamMember;
import com.balancedteam.model.TeamRole;
import com.balancedteam.util.MessageUtil;
import com.balancedteam.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 退出团队二次确认 GUI
 * 玩家执行 /team leave 或点击菜单中的离开团队按钮时弹出此界面
 */
public class LeaveConfirmGui {

    /**
     * 打开退出团队确认界面。
     * 会先验证玩家是否在队伍中、是否为队长、是否在冷却中。
     *
     * @param plugin 插件实例
     * @param player 执行者
     */
    public static void open(BalancedTeamPlugin plugin, Player player) {
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        TeamMember member = team.getMember(player.getUniqueId());
        if (member != null && member.getRole() == TeamRole.LEADER) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_leave_leader_cant_leave"));
            SoundUtil.playError(player);
            return;
        }

        long cd = plugin.getTeamManager().getLeaveTeamCooldownRemaining(player.getUniqueId());
        if (cd > 0) {
            Map<String, String> map = new HashMap<>();
            map.put("TIME", String.valueOf(cd));
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("cooldown", map));
            SoundUtil.playError(player);
            return;
        }

        // 构建 GUI
        String title = plugin.getConfigManager().getRawMessage(GuiConfigKeys.LEAVE_CONFIRM_TITLE);
        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, MessageUtil.color(title));
        holder.setInventory(inv);

        // 背景装饰：灰色玻璃面板
        PagedGuiHelper.fillAll(inv, ItemBuilder.grayGlass());

        // ── 确认退出按钮（槽 11，一次性点击处理器，防止连击）──
        String confirmName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.LEAVE_CONFIRM_NAME);
        List<String> confirmLore = plugin.getConfigManager().getMessageList(
                GuiConfigKeys.LEAVE_CONFIRM_LORE, Collections.emptyMap());

        ItemStack confirmItem = new ItemBuilder(Material.OAK_DOOR)
                .name(confirmName)
                .lore(confirmLore)
                .build();
        inv.setItem(11, confirmItem);

        holder.setOneTimeClickHandler(11, e -> {
            player.closeInventory();

            // 再次校验（防止 GUI 开着期间发生变化）
            Team currentTeam = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
            if (currentTeam == null) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
                return;
            }

            TeamMember currentMember = currentTeam.getMember(player.getUniqueId());
            if (currentMember != null && currentMember.getRole() == TeamRole.LEADER) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_leave_leader_cant_leave"));
                SoundUtil.playError(player);
                return;
            }

            long currentCd = plugin.getTeamManager().getLeaveTeamCooldownRemaining(player.getUniqueId());
            if (currentCd > 0) {
                Map<String, String> map = new HashMap<>();
                map.put("TIME", String.valueOf(currentCd));
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("cooldown", map));
                SoundUtil.playError(player);
                return;
            }

            // 执行离开队伍
            plugin.getTeamManager().removeMember(currentTeam, player.getUniqueId()).thenAccept(success -> {
                if (success) {
                    SoundUtil.playSuccess(player);
                    Map<String, String> map = new HashMap<>();
                    map.put("TEAM", currentTeam.getName());
                    map.put("PLAYER", player.getName());

                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_leave_success", map));

                    for (UUID u : currentTeam.getMembers().keySet()) {
                        Player p = Bukkit.getPlayer(u);
                        if (p != null && p.isOnline()) {
                            MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("team_leave_broadcast", map));
                        }
                    }
                } else {
                    SoundUtil.playError(player);
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("database_error"));
                }
            });
        });

        // ── 取消按钮（槽 15）──
        String cancelName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.LEAVE_CANCEL_NAME);
        List<String> cancelLore = plugin.getConfigManager().getMessageList(
                GuiConfigKeys.LEAVE_CANCEL_LORE, Collections.emptyMap());

        ItemStack cancelItem = new ItemBuilder(Material.LIME_CONCRETE)
                .name(cancelName)
                .lore(cancelLore)
                .build();
        inv.setItem(15, cancelItem);

        holder.setClickHandler(15, e -> {
            SoundUtil.playClick(player);
            TeamMenuGui.open(plugin, player);
        });

        player.openInventory(inv);
    }
}

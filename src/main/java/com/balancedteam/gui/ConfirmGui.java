package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.config.GuiConfigKeys;
import com.balancedteam.gui.util.ItemBuilder;
import com.balancedteam.gui.util.PagedGuiHelper;
import com.balancedteam.model.Team;
import com.balancedteam.model.TeamMember;
import com.balancedteam.model.TeamRole;
import com.balancedteam.util.MessageUtil;
import com.balancedteam.util.PermissionUtil;
import com.balancedteam.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 通用二次确认 GUI，支持「解散团队」、「退出团队」、「踢出成员」、「转让队长」、「提升管理员」与「降职队员」六种模式。
 * 通过 {@link Mode} 枚举区分，避免重复代码。
 */
public class ConfirmGui {

    /**
     * 确认模式枚举
     */
    public enum Mode {
        /** 解散团队 —— 仅队长可操作 */
        DISBAND,
        /** 退出团队 —— 非队长成员可操作 */
        LEAVE,
        /** 踢出成员 —— 队长/管理员可操作 */
        KICK,
        /** 转让团队 —— 仅队长可操作 */
        TRANSFER,
        /** 提升为管理员 —— 仅队长可操作 */
        PROMOTE,
        /** 降级为普通队员 —— 仅队长可操作 */
        DEMOTE
    }

    /**
     * 打开指定模式的二次确认界面（解散或退出）。
     *
     * @param plugin 插件实例
     * @param player 执行者
     * @param mode   操作模式（{@link Mode#DISBAND} 或 {@link Mode#LEAVE}）
     */
    public static void open(BalancedTeamPlugin plugin, Player player, Mode mode) {
        open(plugin, player, mode, null);
    }

    /**
     * 打开指定模式的二次确认界面。
     * 会在打开前进行前置校验，不满足条件时直接发送提示并返回。
     *
     * @param plugin     插件实例
     * @param player     执行者
     * @param mode       操作模式
     * @param targetUuid 目标玩家 UUID（仅针对指定玩家操作的模式需要，其他模式传 null）
     */
    public static void open(BalancedTeamPlugin plugin, Player player, Mode mode, UUID targetUuid) {
        // ── 前置校验 ──
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_not_in_team"));
            return;
        }

        TeamMember member = team.getMember(player.getUniqueId());
        if (member == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_not_in_team"));
            return;
        }

        String targetName = "";

        if (mode == Mode.DISBAND) {
            // 解散：必须是队长
            if (!PermissionUtil.checkLeader(player, team, plugin.getConfigManager())) {
                return;
            }
        } else if (mode == Mode.LEAVE) {
            // 退出：必须不是队长、且不在冷却中
            if (member.getRole() == TeamRole.LEADER) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_leave_leader_cant_leave"));
                SoundUtil.playError(player);
                return;
            }
            long cd = plugin.getTeamManager().getLeaveTeamCooldownRemaining(player.getUniqueId());
            if (cd > 0) {
                Map<String, String> map = new HashMap<>();
                map.put("TIME", String.valueOf(cd));
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "cooldown", map));
                SoundUtil.playError(player);
                return;
            }
        } else if (mode == Mode.KICK) {
            // 踢出：targetUuid 校验
            if (targetUuid == null) {
                return;
            }
            if (targetUuid.equals(player.getUniqueId())) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_kick_self"));
                SoundUtil.playError(player);
                return;
            }
            if (!member.getRole().isAtLeast(TeamRole.OFFICER)) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_not_officer_or_leader"));
                SoundUtil.playError(player);
                return;
            }
            TeamMember targetMember = team.getMember(targetUuid);
            if (targetMember == null) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_player_not_in_your_team"));
                SoundUtil.playError(player);
                return;
            }
            if (!member.canManage(targetMember)) {
                if (targetMember.isLeader()) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_kick_leader"));
                } else {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "no_permission"));
                }
                SoundUtil.playError(player);
                return;
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
            targetName = op.getName() != null ? op.getName() : "未知";
        } else if (mode == Mode.TRANSFER) {
            // 转让：必须是队长
            if (!PermissionUtil.checkLeader(player, team, plugin.getConfigManager())) {
                return;
            }
            if (targetUuid == null) {
                return;
            }
            if (targetUuid.equals(player.getUniqueId())) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_cant_manage_self"));
                SoundUtil.playError(player);
                return;
            }
            TeamMember targetMember = team.getMember(targetUuid);
            if (targetMember == null) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_player_not_in_your_team"));
                SoundUtil.playError(player);
                return;
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
            targetName = op.getName() != null ? op.getName() : "未知";
        } else if (mode == Mode.PROMOTE) {
            // 提升：必须是队长
            if (!PermissionUtil.checkLeader(player, team, plugin.getConfigManager())) {
                return;
            }
            if (targetUuid == null) {
                return;
            }
            if (targetUuid.equals(player.getUniqueId())) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_cant_manage_self"));
                SoundUtil.playError(player);
                return;
            }
            TeamMember targetMember = team.getMember(targetUuid);
            if (targetMember == null) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_player_not_in_your_team"));
                SoundUtil.playError(player);
                return;
            }
            if (targetMember.isOfficer()) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_already_officer"));
                SoundUtil.playError(player);
                return;
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
            targetName = op.getName() != null ? op.getName() : "未知";
        } else if (mode == Mode.DEMOTE) {
            // 降职：必须是队长
            if (!PermissionUtil.checkLeader(player, team, plugin.getConfigManager())) {
                return;
            }
            if (targetUuid == null) {
                return;
            }
            if (targetUuid.equals(player.getUniqueId())) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_cant_manage_self"));
                SoundUtil.playError(player);
                return;
            }
            TeamMember targetMember = team.getMember(targetUuid);
            if (targetMember == null) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_player_not_in_your_team"));
                SoundUtil.playError(player);
                return;
            }
            if (targetMember.getRole() == TeamRole.MEMBER) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_already_member"));
                SoundUtil.playError(player);
                return;
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
            targetName = op.getName() != null ? op.getName() : "未知";
        }

        // ── 占位符准备 ──
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("TEAM", team.getName());
        placeholders.put("PLAYER", targetName);

        // ── 构建 GUI ──
        String titleKey;
        if (mode == Mode.DISBAND) {
            titleKey = GuiConfigKeys.DISBAND_CONFIRM_TITLE;
        } else if (mode == Mode.LEAVE) {
            titleKey = GuiConfigKeys.LEAVE_CONFIRM_TITLE;
        } else if (mode == Mode.KICK) {
            titleKey = GuiConfigKeys.KICK_CONFIRM_TITLE;
        } else if (mode == Mode.TRANSFER) {
            titleKey = GuiConfigKeys.TRANSFER_CONFIRM_TITLE;
        } else if (mode == Mode.PROMOTE) {
            titleKey = GuiConfigKeys.PROMOTE_CONFIRM_TITLE;
        } else {
            titleKey = GuiConfigKeys.DEMOTE_CONFIRM_TITLE;
        }
        String title = plugin.getConfigManager().getRawMessage(player, titleKey, placeholders);

        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, MessageUtil.color(title));
        holder.setInventory(inv);

        // 背景颜色配置
        if (mode == Mode.DISBAND || mode == Mode.KICK) {
            PagedGuiHelper.fillAll(inv, ItemBuilder.redGlass());
        } else if (mode == Mode.TRANSFER) {
            PagedGuiHelper.fillAll(inv, ItemBuilder.yellowGlass());
        } else if (mode == Mode.PROMOTE) {
            PagedGuiHelper.fillAll(inv, ItemBuilder.filler(Material.LIME_STAINED_GLASS_PANE));
        } else {
            PagedGuiHelper.fillAll(inv, ItemBuilder.grayGlass());
        }

        // ── 确认按钮（槽 11，一次性点击，防止连击）──
        String confirmNameKey;
        String confirmLoreKey;
        Material confirmMaterial;

        if (mode == Mode.DISBAND) {
            confirmNameKey = GuiConfigKeys.DISBAND_CONFIRM_NAME;
            confirmLoreKey = GuiConfigKeys.DISBAND_CONFIRM_LORE;
            confirmMaterial = Material.TNT;
        } else if (mode == Mode.LEAVE) {
            confirmNameKey = GuiConfigKeys.LEAVE_CONFIRM_NAME;
            confirmLoreKey = GuiConfigKeys.LEAVE_CONFIRM_LORE;
            confirmMaterial = Material.OAK_DOOR;
        } else if (mode == Mode.KICK) {
            confirmNameKey = GuiConfigKeys.KICK_CONFIRM_NAME;
            confirmLoreKey = GuiConfigKeys.KICK_CONFIRM_LORE;
            confirmMaterial = Material.IRON_DOOR;
        } else if (mode == Mode.TRANSFER) {
            confirmNameKey = GuiConfigKeys.TRANSFER_CONFIRM_NAME;
            confirmLoreKey = GuiConfigKeys.TRANSFER_CONFIRM_LORE;
            confirmMaterial = Material.GOLDEN_HELMET;
        } else if (mode == Mode.PROMOTE) {
            confirmNameKey = GuiConfigKeys.PROMOTE_CONFIRM_NAME;
            confirmLoreKey = GuiConfigKeys.PROMOTE_CONFIRM_LORE;
            confirmMaterial = Material.GOLDEN_CHESTPLATE;
        } else {
            confirmNameKey = GuiConfigKeys.DEMOTE_CONFIRM_NAME;
            confirmLoreKey = GuiConfigKeys.DEMOTE_CONFIRM_LORE;
            confirmMaterial = Material.IRON_CHESTPLATE;
        }

        String confirmName = plugin.getConfigManager().getRawMessage(player, confirmNameKey, placeholders);
        List<String> confirmLore = plugin.getConfigManager().getMessageList(
                player, confirmLoreKey, placeholders);

        ItemStack confirmItem = new ItemBuilder(confirmMaterial)
                .name(confirmName)
                .lore(confirmLore)
                .build();
        inv.setItem(11, confirmItem);

        holder.setOneTimeClickHandler(11, e -> {
            player.closeInventory();

            // 再次校验（防止 GUI 开着期间状态发生变化）
            Team currentTeam = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
            if (currentTeam == null) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_not_in_team"));
                SoundUtil.playError(player);
                return;
            }

            if (mode == Mode.DISBAND) {
                if (!PermissionUtil.checkLeader(player, currentTeam, plugin.getConfigManager())) {
                    return;
                }
                // 执行解散
                plugin.getTeamManager().disbandTeam(currentTeam).thenRun(() -> {
                    SoundUtil.playWarning(player);
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_disband_success"));
                });

            } else if (mode == Mode.LEAVE) {
                TeamMember currentMember = currentTeam.getMember(player.getUniqueId());
                if (currentMember != null && currentMember.getRole() == TeamRole.LEADER) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_leave_leader_cant_leave"));
                    SoundUtil.playError(player);
                    return;
                }
                long currentCd = plugin.getTeamManager().getLeaveTeamCooldownRemaining(player.getUniqueId());
                if (currentCd > 0) {
                    Map<String, String> map = new HashMap<>();
                    map.put("TIME", String.valueOf(currentCd));
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "cooldown", map));
                    SoundUtil.playError(player);
                    return;
                }
                // 执行退出
                plugin.getTeamManager().removeMember(currentTeam, player.getUniqueId()).thenAccept(success -> {
                    if (success) {
                        SoundUtil.playSuccess(player);
                        Map<String, String> map = new HashMap<>();
                        map.put("TEAM", currentTeam.getName());
                        map.put("PLAYER", player.getName());
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_leave_success", map));
                        for (UUID u : currentTeam.getMembers().keySet()) {
                            Player p = Bukkit.getPlayer(u);
                            if (p != null && p.isOnline()) {
                                MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "team_leave_broadcast", map));
                            }
                        }
                    } else {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "database_error"));
                    }
                });
            } else if (mode == Mode.KICK) {
                if (targetUuid == null) {
                    return;
                }
                TeamMember currentActor = currentTeam.getMember(player.getUniqueId());
                TeamMember currentTarget = currentTeam.getMember(targetUuid);

                if (currentActor == null || !currentActor.getRole().isAtLeast(TeamRole.OFFICER)) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_not_officer_or_leader"));
                    SoundUtil.playError(player);
                    return;
                }
                if (currentTarget == null) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_player_not_in_your_team"));
                    SoundUtil.playError(player);
                    return;
                }
                if (!currentActor.canManage(currentTarget)) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "no_permission"));
                    SoundUtil.playError(player);
                    return;
                }

                OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
                String kickedPlayerName = op.getName() != null ? op.getName() : "未知";

                // 执行踢出
                plugin.getTeamManager().removeMember(currentTeam, targetUuid).thenAccept(success -> {
                    if (success) {
                        SoundUtil.playSuccess(player);
                        Map<String, String> map = new HashMap<>();
                        map.put("TEAM", currentTeam.getName());
                        map.put("PLAYER", kickedPlayerName);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_kick_success", map));

                        Player targetOnline = op.getPlayer();
                        if (targetOnline != null && targetOnline.isOnline()) {
                            MessageUtil.sendMessage(targetOnline, plugin.getConfigManager().getMessage(targetOnline, "team_kick_target_msg", map));
                        }

                        for (UUID u : currentTeam.getMembers().keySet()) {
                            Player p = Bukkit.getPlayer(u);
                            if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())) {
                                MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "team_kick_broadcast", map));
                            }
                        }
                    } else {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "database_error"));
                    }
                });
            } else if (mode == Mode.TRANSFER) {
                if (targetUuid == null) {
                    return;
                }
                if (!PermissionUtil.checkLeader(player, currentTeam, plugin.getConfigManager())) {
                    return;
                }
                TeamMember currentTarget = currentTeam.getMember(targetUuid);
                if (currentTarget == null) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_player_not_in_your_team"));
                    SoundUtil.playError(player);
                    return;
                }

                OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
                String targetPlayerName = op.getName() != null ? op.getName() : "未知";

                // 执行转让
                plugin.getTeamManager().transferLeader(currentTeam, targetUuid).thenAccept(success -> {
                    if (success) {
                        SoundUtil.playSuccess(player);
                        Map<String, String> map = new HashMap<>();
                        map.put("TEAM", currentTeam.getName());
                        map.put("PLAYER", targetPlayerName);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_transfer_success", map));

                        Player targetOnline = op.getPlayer();
                        if (targetOnline != null && targetOnline.isOnline()) {
                            MessageUtil.sendMessage(targetOnline, plugin.getConfigManager().getMessage(targetOnline, "team_transfer_target_msg", map));
                            SoundUtil.playDing(targetOnline);
                        }

                        for (UUID u : currentTeam.getMembers().keySet()) {
                            Player p = Bukkit.getPlayer(u);
                            if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())
                                    && (targetOnline == null || !p.getUniqueId().equals(targetOnline.getUniqueId()))) {
                                MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "team_transfer_broadcast", map));
                            }
                        }
                    } else {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "database_error"));
                    }
                });
            } else if (mode == Mode.PROMOTE) {
                if (targetUuid == null) {
                    return;
                }
                if (!PermissionUtil.checkLeader(player, currentTeam, plugin.getConfigManager())) {
                    return;
                }
                TeamMember currentTarget = currentTeam.getMember(targetUuid);
                if (currentTarget == null) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_player_not_in_your_team"));
                    SoundUtil.playError(player);
                    return;
                }

                OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
                String targetPlayerName = op.getName() != null ? op.getName() : "未知";

                // 执行提升
                plugin.getTeamManager().setMemberRole(currentTeam, targetUuid, TeamRole.OFFICER).thenAccept(success -> {
                    if (success) {
                        SoundUtil.playSuccess(player);
                        Map<String, String> map = new HashMap<>();
                        map.put("TEAM", currentTeam.getName());
                        map.put("PLAYER", targetPlayerName);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_promote_success", map));

                        Player targetOnline = op.getPlayer();
                        if (targetOnline != null && targetOnline.isOnline()) {
                            MessageUtil.sendMessage(targetOnline, plugin.getConfigManager().getMessage(targetOnline, "team_member_promoted_msg", map));
                            SoundUtil.playDing(targetOnline);
                        }

                        for (UUID u : currentTeam.getMembers().keySet()) {
                            Player p = Bukkit.getPlayer(u);
                            if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())
                                    && (targetOnline == null || !p.getUniqueId().equals(targetOnline.getUniqueId()))) {
                                MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "team_member_promote_broadcast", map));
                            }
                        }
                    } else {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "database_error"));
                    }
                });
            } else if (mode == Mode.DEMOTE) {
                if (targetUuid == null) {
                    return;
                }
                if (!PermissionUtil.checkLeader(player, currentTeam, plugin.getConfigManager())) {
                    return;
                }
                TeamMember currentTarget = currentTeam.getMember(targetUuid);
                if (currentTarget == null) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_player_not_in_your_team"));
                    SoundUtil.playError(player);
                    return;
                }

                OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
                String targetPlayerName = op.getName() != null ? op.getName() : "未知";

                // 执行降职
                plugin.getTeamManager().setMemberRole(currentTeam, targetUuid, TeamRole.MEMBER).thenAccept(success -> {
                    if (success) {
                        SoundUtil.playSuccess(player);
                        Map<String, String> map = new HashMap<>();
                        map.put("TEAM", currentTeam.getName());
                        map.put("PLAYER", targetPlayerName);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_demote_success", map));

                        Player targetOnline = op.getPlayer();
                        if (targetOnline != null && targetOnline.isOnline()) {
                            MessageUtil.sendMessage(targetOnline, plugin.getConfigManager().getMessage(targetOnline, "team_member_demoted_msg", map));
                        }

                        for (UUID u : currentTeam.getMembers().keySet()) {
                            Player p = Bukkit.getPlayer(u);
                            if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())
                                    && (targetOnline == null || !p.getUniqueId().equals(targetOnline.getUniqueId()))) {
                                MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "team_member_demote_broadcast", map));
                            }
                        }
                    } else {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "database_error"));
                    }
                });
            }
        });

        // ── 取消按钮（槽 15）──
        String cancelNameKey = (mode == Mode.DISBAND || mode == Mode.LEAVE)
                ? GuiConfigKeys.CONFIRM_CANCEL_TO_MENU_NAME
                : GuiConfigKeys.CONFIRM_CANCEL_TO_MEMBERS_NAME;
        String cancelLoreKey = (mode == Mode.DISBAND || mode == Mode.LEAVE)
                ? GuiConfigKeys.CONFIRM_CANCEL_TO_MENU_LORE
                : GuiConfigKeys.CONFIRM_CANCEL_TO_MEMBERS_LORE;

        String cancelName = plugin.getConfigManager().getRawMessage(player, cancelNameKey);
        if (cancelName.contains("Missing message")) {
            cancelName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.CONFIRM_CANCEL_NAME);
        }

        List<String> cancelLore = plugin.getConfigManager().getMessageList(
                player, cancelLoreKey, Collections.emptyMap());
        if (cancelLore == null || cancelLore.isEmpty()) {
            cancelLore = plugin.getConfigManager().getMessageList(
                    player, GuiConfigKeys.CONFIRM_CANCEL_LORE, Collections.emptyMap());
        }

        ItemStack cancelItem = new ItemBuilder(Material.LIME_CONCRETE)
                .name(cancelName)
                .lore(cancelLore)
                .build();
        inv.setItem(15, cancelItem);

        holder.setClickHandler(15, e -> {
            SoundUtil.playClick(player);
            if (mode == Mode.DISBAND || mode == Mode.LEAVE) {
                TeamMenuGui.open(plugin, player);
            } else {
                MemberManageGui.open(plugin, player, 1);
            }
        });

        player.openInventory(inv);
    }
}

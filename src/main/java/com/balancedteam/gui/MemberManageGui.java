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
import com.balancedteam.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 团队成员管理 GUI
 * - 直观展示全队所有成员头像、职位、在线状态与入队时间
 * - 队长左键点击：普通队员与管理员之间一键升降职
 * - 队长/管理员右键点击：权限范围内一键踢出成员
 * - 队长 Shift+左键点击：一键转让团队队长职位
 * - 支持分页与快速跳转邀请新成员
 */
public class MemberManageGui {

    private static final int PAGE_SIZE = 45;

    public static void open(BalancedTeamPlugin plugin, Player player, int page) {
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            TeamNotJoinedGui.open(plugin, player);
            return;
        }

        TeamMember actorMember = team.getMember(player.getUniqueId());
        boolean actorIsLeader = (actorMember != null && actorMember.isLeader());
        boolean actorIsOfficer = (actorMember != null && actorMember.isOfficer());

        // 排序成员：队长 > 管理员 > 普通队员，同职位按加入时间升序
        List<TeamMember> sortedMembers = team.getMembers().values().stream()
                .sorted((m1, m2) -> {
                    int lvl1 = m1.getRole().getLevel();
                    int lvl2 = m2.getRole().getLevel();
                    if (lvl1 != lvl2) {
                        return Integer.compare(lvl2, lvl1);
                    }
                    return m1.getJoinedAt().compareTo(m2.getJoinedAt());
                })
                .collect(Collectors.toList());

        int totalPages = PagedGuiHelper.calculateTotalPages(sortedMembers.size(), PAGE_SIZE);
        int currentPage = PagedGuiHelper.clampPage(page, totalPages);

        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("PAGE", String.valueOf(currentPage));
        titleMap.put("TOTAL_PAGE", String.valueOf(totalPages));
        titleMap.put("TEAM", team.getName());
        String title = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TITLE, titleMap);

        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inv);
        holder.setRefreshAction(p -> open(plugin, p, currentPage));

        // 填充成员列表项 (槽位 0-44)
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, sortedMembers.size());

        String onlineStatus = plugin.getConfigManager().getRawMessage(player, "status.online");
        String offlineStatus = plugin.getConfigManager().getRawMessage(player, "status.offline");

        for (int i = startIndex; i < endIndex; i++) {
            TeamMember targetMember = sortedMembers.get(i);
            int slot = i - startIndex;

            OfflinePlayer op = Bukkit.getOfflinePlayer(targetMember.getUuid());
            String targetName = op.getName() != null ? op.getName() : "未知";
            String status = op.isOnline() ? onlineStatus : offlineStatus;
            String roleDisplayName = plugin.getConfigManager().getRoleDisplayName(player, targetMember.getRole());
            String joinDate = TimeUtil.formatDate(targetMember.getJoinedAt());

            String roleIcon;
            if (targetMember.isLeader()) {
                roleIcon = "&c👑";
            } else if (targetMember.isOfficer()) {
                roleIcon = "&6🛡";
            } else {
                roleIcon = "&7👤";
            }

            Map<String, String> itemMap = new HashMap<>();
            itemMap.put("PLAYER", targetName);
            itemMap.put("ROLE", roleDisplayName);
            itemMap.put("ROLE_ICON", roleIcon);
            itemMap.put("STATUS", status);
            itemMap.put("DATE", joinDate);
            itemMap.put("TEAM", team.getName());

            String itemName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_ITEM_NAME, itemMap);
            List<String> itemLore = new ArrayList<>(plugin.getConfigManager().getMessageList(player, GuiConfigKeys.MEMBER_MANAGE_ITEM_LORE, itemMap));

            boolean isSelf = targetMember.getUuid().equals(player.getUniqueId());

            itemLore.add("");
            if (isSelf) {
                itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_SELF));
            } else if (actorIsLeader) {
                if (targetMember.isOfficer()) {
                    itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_DEMOTE));
                    itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_KICK));
                    itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_TRANSFER));
                } else {
                    itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_PROMOTE));
                    itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_KICK));
                    itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_TRANSFER));
                }
            } else if (actorIsOfficer) {
                if (targetMember.getRole() == TeamRole.MEMBER) {
                    itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_KICK));
                } else {
                    itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_NO_PERM));
                }
            } else {
                itemLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_TIP_VIEW_ONLY));
            }

            ItemStack memberItem = new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(targetMember.getUuid())
                    .name(itemName)
                    .lore(itemLore)
                    .build();

            holder.setClickHandler(slot, e -> {
                ClickType clickType = e.getClick();

                if (isSelf) {
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_cant_manage_self"));
                    SoundUtil.playError(player);
                    return;
                }

                // 1. Shift+左键：队长转让团队
                if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                    if (!actorIsLeader) {
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_not_leader"));
                        SoundUtil.playError(player);
                        return;
                    }

                    plugin.getTeamManager().transferLeader(team, targetMember.getUuid()).thenAccept(success -> {
                        if (success) {
                            Map<String, String> map = new HashMap<>(itemMap);
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_transfer_success", map));
                            SoundUtil.playSuccess(player);

                            Player targetOnline = op.getPlayer();
                            if (targetOnline != null && targetOnline.isOnline()) {
                                MessageUtil.sendMessage(targetOnline, plugin.getConfigManager().getMessage(targetOnline, "team_transfer_target_msg", map));
                                SoundUtil.playDing(targetOnline);
                            }

                            for (UUID u : team.getMembers().keySet()) {
                                Player p = Bukkit.getPlayer(u);
                                if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId()) && (targetOnline == null || !p.getUniqueId().equals(targetOnline.getUniqueId()))) {
                                    MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "team_transfer_broadcast", map));
                                }
                            }

                            holder.refresh(player);
                        }
                    });
                    return;
                }

                // 2. 右键点击：踢出团队
                if (clickType.isRightClick()) {
                    if (actorMember == null || !actorMember.canManage(targetMember)) {
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "no_permission"));
                        SoundUtil.playError(player);
                        return;
                    }

                    plugin.getTeamManager().removeMember(team, targetMember.getUuid()).thenAccept(success -> {
                        if (success) {
                            Map<String, String> map = new HashMap<>(itemMap);
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_kick_success", map));
                            SoundUtil.playSuccess(player);

                            Player targetOnline = op.getPlayer();
                            if (targetOnline != null && targetOnline.isOnline()) {
                                MessageUtil.sendMessage(targetOnline, plugin.getConfigManager().getMessage(targetOnline, "team_kick_target_msg", map));
                            }

                            for (UUID u : team.getMembers().keySet()) {
                                Player p = Bukkit.getPlayer(u);
                                if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())) {
                                    MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "team_kick_broadcast", map));
                                }
                            }

                            holder.refresh(player);
                        }
                    });
                    return;
                }

                // 3. 左键点击：升降职
                if (clickType.isLeftClick()) {
                    if (!actorIsLeader) {
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_not_leader"));
                        SoundUtil.playError(player);
                        return;
                    }

                    if (targetMember.getRole() == TeamRole.MEMBER) {
                        // 提升为管理员
                        plugin.getTeamManager().setMemberRole(team, targetMember.getUuid(), TeamRole.OFFICER).thenAccept(success -> {
                            if (success) {
                                Map<String, String> map = new HashMap<>(itemMap);
                                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_promote_success", map));
                                SoundUtil.playSuccess(player);

                                Player targetOnline = op.getPlayer();
                                if (targetOnline != null && targetOnline.isOnline()) {
                                    MessageUtil.sendMessage(targetOnline, plugin.getConfigManager().getMessage(targetOnline, "team_member_promoted_msg", map));
                                    SoundUtil.playDing(targetOnline);
                                }

                                for (UUID u : team.getMembers().keySet()) {
                                    Player p = Bukkit.getPlayer(u);
                                    if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId()) && (targetOnline == null || !p.getUniqueId().equals(targetOnline.getUniqueId()))) {
                                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "team_member_promote_broadcast", map));
                                    }
                                }

                                holder.refresh(player);
                            }
                        });
                    } else if (targetMember.isOfficer()) {
                        // 降级为普通队员
                        plugin.getTeamManager().setMemberRole(team, targetMember.getUuid(), TeamRole.MEMBER).thenAccept(success -> {
                            if (success) {
                                Map<String, String> map = new HashMap<>(itemMap);
                                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_member_demote_success", map));
                                SoundUtil.playSuccess(player);

                                Player targetOnline = op.getPlayer();
                                if (targetOnline != null && targetOnline.isOnline()) {
                                    MessageUtil.sendMessage(targetOnline, plugin.getConfigManager().getMessage(targetOnline, "team_member_demoted_msg", map));
                                }

                                for (UUID u : team.getMembers().keySet()) {
                                    Player p = Bukkit.getPlayer(u);
                                    if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId()) && (targetOnline == null || !p.getUniqueId().equals(targetOnline.getUniqueId()))) {
                                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "team_member_demote_broadcast", map));
                                    }
                                }

                                holder.refresh(player);
                            }
                        });
                    }
                }
            });

            inv.setItem(slot, memberItem);
        }

        // ---- 底部控制栏 (槽位 45-53) ----
        PagedGuiHelper.fillRange(inv, 45, 53, ItemBuilder.grayGlass());

        // 上一页 (槽位 45)
        if (currentPage > 1) {
            Map<String, String> prevMap = new HashMap<>();
            prevMap.put("PAGE", String.valueOf(currentPage - 1));
            String prevName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_PREV_PAGE, prevMap);
            PagedGuiHelper.setupPrevButton(holder, inv, 45, player, currentPage, prevName, () -> open(plugin, player, currentPage - 1));
        }

        // 快速跳转邀请成员 (槽位 47)
        if (actorIsLeader || actorIsOfficer) {
            String inviteBtnName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_INVITE_BUTTON_NAME);
            List<String> inviteBtnLore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.MEMBER_MANAGE_INVITE_BUTTON_LORE, Collections.emptyMap());
            ItemStack inviteBtnItem = new ItemBuilder(Material.WRITABLE_BOOK)
                    .name(inviteBtnName)
                    .lore(inviteBtnLore)
                    .build();
            inv.setItem(47, inviteBtnItem);
            holder.setClickHandler(47, e -> {
                SoundUtil.playClick(player);
                PlayerSelectGui.open(plugin, player, 1);
            });
        }

        // 返回按钮 (槽位 49)
        String backName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_BACK_BUTTON);
        PagedGuiHelper.setupBackButton(holder, inv, 49, player, backName, () -> TeamMenuGui.open(plugin, player));

        // 下一页 (槽位 53)
        if (currentPage < totalPages) {
            Map<String, String> nextMap = new HashMap<>();
            nextMap.put("PAGE", String.valueOf(currentPage + 1));
            String nextName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.MEMBER_MANAGE_NEXT_PAGE, nextMap);
            PagedGuiHelper.setupNextButton(holder, inv, 53, player, currentPage, totalPages, nextName, () -> open(plugin, player, currentPage + 1));
        }

        player.openInventory(inv);
    }
}

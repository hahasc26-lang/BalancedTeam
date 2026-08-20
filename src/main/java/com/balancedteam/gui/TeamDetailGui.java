package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.config.GuiConfigKeys;
import com.balancedteam.gui.util.ItemBuilder;
import com.balancedteam.gui.util.PagedGuiHelper;
import com.balancedteam.model.Team;
import com.balancedteam.util.MessageUtil;
import com.balancedteam.util.SoundUtil;
import com.balancedteam.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 团队详情查看 GUI
 */
public class TeamDetailGui {

    public static void open(BalancedTeamPlugin plugin, Player player, Team team, int returnPage) {
        if (team == null) return;

        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("TEAM", team.getName());
        String title = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_TITLE, titleMap);

        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 36, MessageUtil.color(title));
        holder.setInventory(inv);

        // 黑色边框玻璃填充
        PagedGuiHelper.fillAll(inv, ItemBuilder.blackGlass());

        // 1. 队长信息卡 (槽位 11)
        OfflinePlayer leader = Bukkit.getOfflinePlayer(team.getLeaderUuid());
        String leaderName = leader.getName() != null ? leader.getName() : "未知";
        String leaderStatus = leader.isOnline() ? plugin.getConfigManager().getRawMessage("status.online") : plugin.getConfigManager().getRawMessage("status.offline");
        String ffStatus = plugin.getConfigManager().isFriendlyFireActive(team)
                ? plugin.getConfigManager().getRawMessage("status.on")
                : plugin.getConfigManager().getRawMessage("status.off");

        Map<String, String> leaderMap = new HashMap<>();
        leaderMap.put("LEADER", leaderName);
        leaderMap.put("STATUS", leaderStatus);
        leaderMap.put("DATE", TimeUtil.formatDate(team.getCreatedAt()));
        leaderMap.put("FF", ffStatus);

        String leaderItemName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_LEADER_ITEM_NAME, leaderMap);
        List<String> leaderLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.DETAIL_LEADER_ITEM_LORE, leaderMap);

        ItemStack leaderItem = new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(team.getLeaderUuid())
                .name(leaderItemName)
                .lore(leaderLore)
                .build();
        inv.setItem(11, leaderItem);

        // 2. 成员列表卡 (槽位 13)
        String onlineIcon = plugin.getConfigManager().getRawMessage("status.member_online");
        String offlineIcon = plugin.getConfigManager().getRawMessage("status.member_offline");

        List<String> memberLore = team.getMembers().values().stream()
                .map(m -> {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(m.getUuid());
                    String name = op.getName() != null ? op.getName() : "未知";
                    String status = op.isOnline() ? onlineIcon : offlineIcon;
                    String roleName = plugin.getConfigManager().getRoleDisplayName(m.getRole());
                    return status + " &7[" + roleName + "&7] &f" + name;
                })
                .collect(Collectors.toList());

        Map<String, String> memberMap = new HashMap<>();
        memberMap.put("COUNT", String.valueOf(team.getMemberCount()));
        String membersItemName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_MEMBERS_ITEM_NAME, memberMap);

        ItemStack membersItem = new ItemBuilder(Material.PLAYER_HEAD)
                .name(membersItemName)
                .lore(memberLore)
                .build();
        inv.setItem(13, membersItem);
        if (team.hasMember(player.getUniqueId())) {
            holder.setClickHandler(13, e -> {
                SoundUtil.playClick(player);
                MemberManageGui.open(plugin, player, 1);
            });
        }

        // 3. 外交关系卡 (槽位 15)
        List<Integer> allyIds = plugin.getRelationManager().getAllies(team.getId());
        List<Integer> enemyIds = plugin.getRelationManager().getEnemies(team.getId());

        String allyPrefix = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_RELATION_ALLY_PREFIX);
        String enemyPrefix = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_RELATION_ENEMY_PREFIX);

        List<String> allyNames = allyIds.stream()
                .map(id -> {
                    Team t = plugin.getTeamManager().getTeamById(id);
                    return t != null ? allyPrefix + t.getName() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (allyNames.isEmpty()) allyNames.add(plugin.getConfigManager().getRawMessage("status.none_ally"));

        List<String> enemyNames = enemyIds.stream()
                .map(id -> {
                    Team t = plugin.getTeamManager().getTeamById(id);
                    return t != null ? enemyPrefix + t.getName() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (enemyNames.isEmpty()) enemyNames.add(plugin.getConfigManager().getRawMessage("status.none_enemy"));

        Map<String, String> relMap = new HashMap<>();
        relMap.put("COUNT", String.valueOf(allyIds.size()));
        String alliesHeader = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_RELATION_ALLIES_HEADER, relMap);

        relMap.put("COUNT", String.valueOf(enemyIds.size()));
        String enemiesHeader = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_RELATION_ENEMIES_HEADER, relMap);

        List<String> relationLore = new ArrayList<>();
        relationLore.add(alliesHeader);
        relationLore.addAll(allyNames);
        relationLore.add("");
        relationLore.add(enemiesHeader);
        relationLore.addAll(enemyNames);

        String relationItemName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_RELATION_ITEM_NAME);
        ItemStack relationItem = new ItemBuilder(Material.SHIELD)
                .name(relationItemName)
                .lore(relationLore)
                .build();
        inv.setItem(15, relationItem);

        // 4. 未入队玩家展示【申请加入队伍】按钮 (槽位 22)
        if (!plugin.getTeamManager().isPlayerInTeam(player.getUniqueId())) {
            int maxMembers = plugin.getConfigManager().getMaxMembers();
            boolean isFull = team.getMemberCount() >= maxMembers;
            boolean alreadyApplied = plugin.getApplicationManager().hasValidApplication(team.getId(), player.getUniqueId());

            Map<String, String> appMap = new HashMap<>();
            appMap.put("TEAM", team.getName());
            appMap.put("MAX", String.valueOf(maxMembers));
            appMap.put("COUNT", String.valueOf(team.getMemberCount()));

            if (alreadyApplied) {
                String appliedName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_APPLIED_ITEM_NAME, appMap);
                List<String> appliedLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.DETAIL_APPLIED_ITEM_LORE, appMap);
                ItemStack appliedItem = new ItemBuilder(Material.CLOCK)
                        .name(appliedName)
                        .lore(appliedLore)
                        .build();
                inv.setItem(22, appliedItem);
                holder.setClickHandler(22, e -> {
                    SoundUtil.playDing(player);
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_apply_already_sent", appMap));
                });
            } else if (isFull) {
                String fullName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_FULL_ITEM_NAME, appMap);
                List<String> fullLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.DETAIL_FULL_ITEM_LORE, appMap);
                ItemStack fullItem = new ItemBuilder(Material.BARRIER)
                        .name(fullName)
                        .lore(fullLore)
                        .build();
                inv.setItem(22, fullItem);
                holder.setClickHandler(22, e -> {
                    SoundUtil.playError(player);
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_max_members", appMap));
                });
            } else {
                String applyName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_APPLY_BUTTON_NAME, appMap);
                List<String> applyLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.DETAIL_APPLY_BUTTON_LORE, appMap);
                ItemStack applyItem = new ItemBuilder(Material.EMERALD)
                        .name(applyName)
                        .lore(applyLore)
                        .build();
                inv.setItem(22, applyItem);
                holder.setClickHandler(22, e -> {
                    if (plugin.getTeamManager().isPlayerInTeam(player.getUniqueId())) {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_already_in_team"));
                        open(plugin, player, team, returnPage);
                        return;
                    }
                    if (team.getMemberCount() >= maxMembers) {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_max_members", appMap));
                        open(plugin, player, team, returnPage);
                        return;
                    }
                    if (plugin.getApplicationManager().hasValidApplication(team.getId(), player.getUniqueId())) {
                        SoundUtil.playDing(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_apply_already_sent", appMap));
                        open(plugin, player, team, returnPage);
                        return;
                    }

                    int timeout = plugin.getConfigManager().getInviteTimeout();
                    plugin.getApplicationManager().addApplication(team.getId(), player.getUniqueId(), player.getName(), timeout);

                    SoundUtil.playSuccess(player);
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_apply_sent", appMap));

                    // 通知目标团队在线队长与管理员
                    Map<String, String> notifyMap = new HashMap<>();
                    notifyMap.put("PLAYER", player.getName());
                    notifyMap.put("TEAM", team.getName());
                    for (com.balancedteam.model.TeamMember m : team.getMembers().values()) {
                        if (m.getRole() == com.balancedteam.model.TeamRole.LEADER || m.getRole() == com.balancedteam.model.TeamRole.OFFICER) {
                            Player leaderOrOfficer = Bukkit.getPlayer(m.getUuid());
                            if (leaderOrOfficer != null && leaderOrOfficer.isOnline()) {
                                MessageUtil.sendMessage(leaderOrOfficer, plugin.getConfigManager().getMessage("team_apply_received", notifyMap));
                            }
                        }
                    }

                    open(plugin, player, team, returnPage);
                });
            }
        }

        // 5. 返回按钮 (槽位 31)
        String backButtonName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DETAIL_BACK_BUTTON);
        PagedGuiHelper.setupBackButton(holder, inv, 31, player, backButtonName, () -> {
            TeamListGui.open(plugin, player, returnPage);
        });

        player.openInventory(inv);
    }
}

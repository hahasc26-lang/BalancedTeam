package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.config.GuiConfigKeys;
import com.balancedteam.gui.util.ItemBuilder;
import com.balancedteam.gui.util.PagedGuiHelper;
import com.balancedteam.model.Team;
import com.balancedteam.model.TeamMember;
import com.balancedteam.util.MessageUtil;
import com.balancedteam.util.PermissionUtil;
import com.balancedteam.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 个人团队控制面板 GUI (/team menu 或 /team gui)
 */
public class TeamMenuGui {

    public static void open(BalancedTeamPlugin plugin, Player player) {
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            TeamNotJoinedGui.open(plugin, player);
            return;
        }

        TeamMember myMember = team.getMember(player.getUniqueId());
        boolean isLeader = (myMember != null && myMember.isLeader());
        boolean isOfficerOrLeader = (myMember != null && (myMember.isLeader() || myMember.getRole() == com.balancedteam.model.TeamRole.OFFICER));
        String roleName = myMember != null ? plugin.getConfigManager().getRoleDisplayName(myMember.getRole())
                : plugin.getConfigManager().getRawMessage("role.unknown");

        String title = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_TITLE);
        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 45, MessageUtil.color(title));
        holder.setInventory(inv);
        holder.setRefreshAction(p -> open(plugin, p));

        // 背景填充
        PagedGuiHelper.fillAll(inv, ItemBuilder.grayGlass());

        // 1. 团队基本信息 (槽位 10)
        String ffStatus = plugin.getConfigManager().isFriendlyFireActive(team)
                ? plugin.getConfigManager().getRawMessage("status.on")
                : plugin.getConfigManager().getRawMessage("status.off");
        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("TEAM", team.getName());
        infoMap.put("ROLE", roleName);
        infoMap.put("COUNT", String.valueOf(team.getMemberCount()));
        infoMap.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
        infoMap.put("FF", ffStatus);

        String infoName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_INFO_ITEM_NAME, infoMap);
        List<String> infoLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.MENU_INFO_ITEM_LORE, infoMap);

        ItemStack infoItem = new ItemBuilder(Material.BEACON)
                .name(infoName)
                .lore(infoLore)
                .build();
        inv.setItem(10, infoItem);
        holder.setClickHandler(10, e -> {
            SoundUtil.playClick(player);
            TeamDetailGui.open(plugin, player, team, 1);
        });

        // 2. 团队成员管理 (槽位 12)
        Map<String, String> memberBtnMap = new HashMap<>();
        memberBtnMap.put("COUNT", String.valueOf(team.getMemberCount()));
        memberBtnMap.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
        String memberBtnName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_MEMBERS_BUTTON_NAME, memberBtnMap);
        List<String> memberBtnLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.MENU_MEMBERS_BUTTON_LORE, memberBtnMap);
        ItemStack memberBtnItem = new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(player.getUniqueId())
                .name(memberBtnName)
                .lore(memberBtnLore)
                .build();
        inv.setItem(12, memberBtnItem);
        holder.setClickHandler(12, e -> {
            SoundUtil.playClick(player);
            MemberManageGui.open(plugin, player, 1);
        });

        // 3. 邀请新成员 (槽位 14)
        String inviteName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_INVITE_ITEM_NAME);
        List<String> inviteLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.MENU_INVITE_ITEM_LORE,
                Collections.emptyMap());

        ItemStack inviteItem = new ItemBuilder(Material.WRITABLE_BOOK)
                .name(inviteName)
                .lore(inviteLore)
                .build();
        inv.setItem(14, inviteItem);
        holder.setClickHandler(14, e -> {
            if (!PermissionUtil.checkOfficerOrLeader(player, team, plugin.getConfigManager())) {
                return;
            }

            if (team.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
                Map<String, String> map = new HashMap<>();
                map.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_max_members", map));
                SoundUtil.playError(player);
                return;
            }

            SoundUtil.playClick(player);
            PlayerSelectGui.open(plugin, player, 1);
        });

        // 4. 友伤开关 (槽位 16)
        boolean ffActive = plugin.getConfigManager().isFriendlyFireActive(team);
        Material ffMaterial = ffActive ? Material.NETHERITE_SWORD : Material.SHIELD;
        String ffStatusText = ffActive
                ? plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_FF_STATUS_ON)
                : plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_FF_STATUS_OFF);

        String ffTip;
        if (!plugin.getConfigManager().isAllowFriendlyFireToggle()) {
            ffTip = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_FF_DISABLED_TIP);
        } else if (isLeader) {
            ffTip = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_FF_LEADER_TIP);
        } else {
            ffTip = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_FF_NON_LEADER_TIP);
        }

        List<String> ffLore = new ArrayList<>();
        ffLore.add("&7当前状态: " + ffStatusText);
        ffLore.add("");
        ffLore.add(ffTip);

        String ffName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_FF_ITEM_NAME);
        ItemStack ffItem = new ItemBuilder(ffMaterial)
                .name(ffName)
                .lore(ffLore)
                .build();
        inv.setItem(16, ffItem);
        holder.setClickHandler(16, e -> {
            if (!plugin.getConfigManager().isAllowFriendlyFireToggle()) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_ff_toggle_disabled"));
                SoundUtil.playError(player);
                return;
            }
            if (!PermissionUtil.checkLeader(player, team, plugin.getConfigManager())) {
                return;
            }
            long cd = plugin.getTeamManager().getFriendlyFireCooldownRemaining(player.getUniqueId());
            if (cd > 0) {
                Map<String, String> map = new HashMap<>();
                map.put("TIME", String.valueOf(cd));
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("cooldown", map));
                SoundUtil.playError(player);
                return;
            }
            boolean newState = !team.isFriendlyFire();
            plugin.getTeamManager().setFriendlyFire(team, newState, player.getUniqueId()).thenAccept(success -> {
                if (success) {
                    String msgKey = newState ? "team_ff_toggle_on" : "team_ff_toggle_off";
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(msgKey));
                    SoundUtil.playSuccess(player);
                    holder.refresh(player);
                }
            });
        });

        // 4. 盟友管理 (槽位 20)
        int allyCount = plugin.getRelationManager().getAllies(team.getId()).size();
        int pendingAllyCount = plugin.getRelationManager().getPendingRequestsTo(team.getId()).size();
        Map<String, String> allyMap = new HashMap<>();
        allyMap.put("COUNT", String.valueOf(allyCount));
        allyMap.put("MAX", String.valueOf(plugin.getConfigManager().getMaxAllies()));
        allyMap.put("PENDING", String.valueOf(pendingAllyCount));
        String allyBtnName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_ALLY_BUTTON_NAME, allyMap);
        List<String> allyBtnLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.MENU_ALLY_BUTTON_LORE, allyMap);
        ItemStack allyBtnItem = new ItemBuilder(Material.LIME_BANNER)
                .name(allyBtnName)
                .lore(allyBtnLore)
                .build();
        inv.setItem(20, allyBtnItem);
        holder.setClickHandler(20, e -> {
            SoundUtil.playClick(player);
            AllyManageGui.open(plugin, player, 1);
        });

        // 5. 通知中心 (槽位 22)
        int pendingInviteCount = plugin.getInviteManager().getValidInviteCount(player.getUniqueId());
        int pendingAppCount = isOfficerOrLeader ? plugin.getApplicationManager().getValidApplicationCount(team.getId()) : 0;
        int totalPending = (isLeader ? pendingAllyCount : 0) + pendingInviteCount + pendingAppCount;
        Map<String, String> notifMap = new HashMap<>();
        notifMap.put("PENDING", String.valueOf(totalPending));
        Material notifMaterial = totalPending > 0 ? Material.BELL : Material.PAPER;
        String notifName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_NOTIFICATION_BUTTON_NAME, notifMap);
        List<String> notifLore = plugin.getConfigManager().getMessageList(
                totalPending > 0 ? GuiConfigKeys.MENU_NOTIFICATION_BUTTON_LORE_PENDING : GuiConfigKeys.MENU_NOTIFICATION_BUTTON_LORE,
                notifMap);
        ItemStack notifItem = new ItemBuilder(notifMaterial)
                .name(notifName)
                .lore(notifLore)
                .build();
        inv.setItem(22, notifItem);
        holder.setClickHandler(22, e -> {
            SoundUtil.playClick(player);
            NotificationGui.open(plugin, player, 1);
        });

        // 6. 敌对管理 (槽位 24)
        int enemyCount = plugin.getRelationManager().getEnemies(team.getId()).size();
        Map<String, String> enemyMap = new HashMap<>();
        enemyMap.put("COUNT", String.valueOf(enemyCount));
        enemyMap.put("MAX", String.valueOf(plugin.getConfigManager().getMaxEnemies()));
        String enemyBtnName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_ENEMY_BUTTON_NAME, enemyMap);
        List<String> enemyBtnLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.MENU_ENEMY_BUTTON_LORE, enemyMap);
        ItemStack enemyBtnItem = new ItemBuilder(Material.RED_BANNER)
                .name(enemyBtnName)
                .lore(enemyBtnLore)
                .build();
        inv.setItem(24, enemyBtnItem);
        holder.setClickHandler(24, e -> {
            SoundUtil.playClick(player);
            EnemyManageGui.open(plugin, player, 1);
        });

        // 7. 退出/解散团队 (槽位 31)
        if (isLeader) {
            String disbandName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_DISBAND_ITEM_NAME);
            List<String> disbandLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.MENU_DISBAND_ITEM_LORE,
                    Collections.emptyMap());

            ItemStack disbandItem = new ItemBuilder(Material.TNT)
                    .name(disbandName)
                    .lore(disbandLore)
                    .build();
            inv.setItem(31, disbandItem);
            holder.setClickHandler(31, e -> {
                SoundUtil.playWarning(player);
                DisbandConfirmGui.open(plugin, player);
            });
        } else {
            String leaveName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.MENU_LEAVE_ITEM_NAME);
            List<String> leaveLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.MENU_LEAVE_ITEM_LORE,
                    Collections.emptyMap());

            ItemStack leaveItem = new ItemBuilder(Material.OAK_DOOR)
                    .name(leaveName)
                    .lore(leaveLore)
                    .build();
            inv.setItem(31, leaveItem);
            holder.setClickHandler(31, e -> {
                SoundUtil.playWarning(player);
                LeaveConfirmGui.open(plugin, player);
            });
        }

        player.openInventory(inv);
    }
}

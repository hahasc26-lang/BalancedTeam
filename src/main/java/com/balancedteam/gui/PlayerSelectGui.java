package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.config.GuiConfigKeys;
import com.balancedteam.gui.util.ItemBuilder;
import com.balancedteam.gui.util.PagedGuiHelper;
import com.balancedteam.manager.InviteManager;
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
import java.util.stream.Collectors;

/**
 * 在线玩家选择与一键邀请 GUI
 * - 直观展示全服在线玩家列表与状态（未加队/已有队伍/本队成员/已发送邀请）
 * - 支持鼠标左键一键发送入队邀请
 * - 支持点击唤起聊天框手动输入模式
 */
public class PlayerSelectGui {

    private static final int PAGE_SIZE = 45;

    public static void open(BalancedTeamPlugin plugin, Player player, int page) {
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (!PermissionUtil.checkOfficerOrLeader(player, team, plugin.getConfigManager())) {
            return;
        }

        // 获取全服所有除自己外的在线玩家
        List<Player> targetPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))
                .collect(Collectors.toList());

        int totalPages = PagedGuiHelper.calculateTotalPages(targetPlayers.size(), PAGE_SIZE);
        int currentPage = PagedGuiHelper.clampPage(page, totalPages);

        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("PAGE", String.valueOf(currentPage));
        titleMap.put("TOTAL_PAGE", String.valueOf(totalPages));
        String title = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_TITLE, titleMap);

        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inv);
        holder.setRefreshAction(p -> open(plugin, p, currentPage));

        // 填充玩家列表条目 (槽位 0-44)
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, targetPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            Player target = targetPlayers.get(i);
            int slot = i - startIndex;

            boolean inMyTeam = team.hasMember(target.getUniqueId());
            boolean inOtherTeam = !inMyTeam && plugin.getTeamManager().isPlayerInTeam(target.getUniqueId());
            boolean alreadyInvited = !inMyTeam && plugin.getInviteManager().hasValidInvite(target.getUniqueId(), team.getId());

            Map<String, String> itemMap = new HashMap<>();
            itemMap.put("PLAYER", target.getName());

            ItemStack item;
            if (inMyTeam) {
                // 本队成员
                TeamMember member = team.getMember(target.getUniqueId());
                String roleName = member != null ? plugin.getConfigManager().getRoleDisplayName(player, member.getRole())
                        : plugin.getConfigManager().getRawMessage(player, "role.unknown");
                itemMap.put("ROLE", roleName);

                String name = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_ITEM_IN_MY_TEAM_NAME, itemMap);
                List<String> lore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.PLAYER_SELECT_ITEM_IN_MY_TEAM_LORE, itemMap);

                item = new ItemBuilder(Material.PLAYER_HEAD)
                        .skullOwner(target.getUniqueId())
                        .name(name)
                        .lore(lore)
                        .build();

                holder.setClickHandler(slot, e -> {
                    SoundUtil.playError(player);
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_target_in_your_team"));
                });
            } else if (alreadyInvited) {
                // 已发送邀请
                InviteManager.Invite invite = plugin.getInviteManager().getInvite(target.getUniqueId(), team.getId());
                long remaining = invite != null ? invite.getRemainingSeconds() : 0;
                itemMap.put("REMAINING", String.valueOf(remaining));

                String name = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_ITEM_ALREADY_INVITED_NAME, itemMap);
                List<String> lore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.PLAYER_SELECT_ITEM_ALREADY_INVITED_LORE, itemMap);

                item = new ItemBuilder(Material.PLAYER_HEAD)
                        .skullOwner(target.getUniqueId())
                        .name(name)
                        .lore(lore)
                        .build();

                holder.setClickHandler(slot, e -> {
                    SoundUtil.playError(player);
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_already_sent"));
                });
            } else if (inOtherTeam) {
                // 已加入其他队伍
                Team otherTeam = plugin.getTeamManager().getTeamByPlayer(target.getUniqueId());
                String otherTeamName = otherTeam != null ? otherTeam.getName() : "未知队伍";
                itemMap.put("TEAM", otherTeamName);

                String name = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_ITEM_IN_TEAM_NAME, itemMap);
                List<String> lore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.PLAYER_SELECT_ITEM_IN_TEAM_LORE, itemMap);

                item = new ItemBuilder(Material.PLAYER_HEAD)
                        .skullOwner(target.getUniqueId())
                        .name(name)
                        .lore(lore)
                        .build();

                holder.setClickHandler(slot, e -> {
                    SoundUtil.playError(player);
                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_target_in_team"));
                });
            } else {
                // 可邀请玩家
                String name = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_ITEM_AVAILABLE_NAME, itemMap);
                List<String> lore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.PLAYER_SELECT_ITEM_AVAILABLE_LORE, itemMap);

                item = new ItemBuilder(Material.PLAYER_HEAD)
                        .skullOwner(target.getUniqueId())
                        .name(name)
                        .lore(lore)
                        .build();

                // 点击一键发送邀请
                holder.setClickHandler(slot, e -> {
                    if (team.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
                        Map<String, String> map = new HashMap<>();
                        map.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_max_members", map));
                        SoundUtil.playError(player);
                        return;
                    }

                    if (!target.isOnline()) {
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "target_player_not_found", itemMap));
                        SoundUtil.playError(player);
                        holder.refresh(player);
                        return;
                    }

                    long timeout = plugin.getConfigManager().getInviteTimeout();
                    plugin.getInviteManager().addInvite(target.getUniqueId(), team.getId(), player.getUniqueId(), timeout);

                    Map<String, String> sendMap = new HashMap<>();
                    sendMap.put("PLAYER", target.getName());
                    sendMap.put("TEAM", team.getName());
                    sendMap.put("TIMEOUT", String.valueOf(timeout));

                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_sent", sendMap));
                    MessageUtil.sendMessage(target, plugin.getConfigManager().getMessage(target, "team_invite_received", sendMap));
                    SoundUtil.playSuccess(player);
                    SoundUtil.playDing(target);

                    // 刷新当前界面
                    holder.refresh(player);
                });
            }

            inv.setItem(slot, item);
        }

        // 若当前无任何其他在线玩家，显示占位提示 (槽位 22)
        if (targetPlayers.isEmpty()) {
            String emptyName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_NO_PLAYERS_NAME);
            List<String> emptyLore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.PLAYER_SELECT_NO_PLAYERS_LORE, Collections.emptyMap());
            PagedGuiHelper.setupEmptyPlaceholder(inv, 22, Material.PAPER, emptyName, emptyLore);
        }

        // ---- 底部控制栏 (槽位 45-53) ----
        PagedGuiHelper.fillRange(inv, 45, 53, ItemBuilder.grayGlass());

        // 上一页 (槽位 45)
        if (currentPage > 1) {
            Map<String, String> prevMap = new HashMap<>();
            prevMap.put("PAGE", String.valueOf(currentPage - 1));
            String prevName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_PREV_PAGE, prevMap);
            PagedGuiHelper.setupPrevButton(holder, inv, 45, player, currentPage, prevName, () -> open(plugin, player, currentPage - 1));
        }

        // 手动输入玩家名按钮 (槽位 48)
        String manualName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_MANUAL_INPUT_NAME);
        List<String> manualLore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.PLAYER_SELECT_MANUAL_INPUT_LORE, Collections.emptyMap());
        ItemStack manualItem = new ItemBuilder(Material.SPYGLASS).name(manualName).lore(manualLore).build();
        inv.setItem(48, manualItem);
        holder.setClickHandler(48, e -> {
            SoundUtil.playClick(player);
            player.closeInventory();
            String prompt = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_CHAT_PROMPT);
            plugin.getChatInputManager().requestInput(player, prompt, targetName -> {
                executeInviteByName(plugin, player, team, targetName);
            }, () -> TeamMenuGui.open(plugin, player));
        });

        // 返回按钮 (槽位 49)
        String backName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_BACK_BUTTON);
        PagedGuiHelper.setupBackButton(holder, inv, 49, player, backName, () -> TeamMenuGui.open(plugin, player));

        // 下一页 (槽位 53)
        if (currentPage < totalPages) {
            Map<String, String> nextMap = new HashMap<>();
            nextMap.put("PAGE", String.valueOf(currentPage + 1));
            String nextName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.PLAYER_SELECT_NEXT_PAGE, nextMap);
            PagedGuiHelper.setupNextButton(holder, inv, 53, player, currentPage, totalPages, nextName, () -> open(plugin, player, currentPage + 1));
        }

        player.openInventory(inv);
    }

    /**
     * 统一按玩家名执行邀请逻辑
     */
    public static void executeInviteByName(BalancedTeamPlugin plugin, Player player, Team team, String targetName) {
        if (targetName == null || targetName.trim().isEmpty()) {
            return;
        }

        String cleanName = targetName.trim();
        Player target = Bukkit.getPlayer(cleanName);
        if (target == null || !target.isOnline()) {
            Map<String, String> map = new HashMap<>();
            map.put("PLAYER", cleanName);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "target_player_not_found", map));
            SoundUtil.playError(player);
            return;
        }

        if (team.hasMember(target.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_target_in_your_team"));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getTeamManager().isPlayerInTeam(target.getUniqueId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_target_in_team"));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getInviteManager().hasValidInvite(target.getUniqueId(), team.getId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_already_sent"));
            SoundUtil.playError(player);
            return;
        }

        if (team.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
            Map<String, String> map = new HashMap<>();
            map.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_max_members", map));
            SoundUtil.playError(player);
            return;
        }

        long timeout = plugin.getConfigManager().getInviteTimeout();
        plugin.getInviteManager().addInvite(target.getUniqueId(), team.getId(), player.getUniqueId(), timeout);

        Map<String, String> map = new HashMap<>();
        map.put("PLAYER", target.getName());
        map.put("TEAM", team.getName());
        map.put("TIMEOUT", String.valueOf(timeout));

        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_invite_sent", map));
        MessageUtil.sendMessage(target, plugin.getConfigManager().getMessage(target, "team_invite_received", map));
        SoundUtil.playSuccess(player);
        SoundUtil.playDing(target);
    }
}

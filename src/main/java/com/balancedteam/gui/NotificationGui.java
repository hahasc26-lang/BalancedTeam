package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.config.GuiConfigKeys;
import com.balancedteam.gui.util.ItemBuilder;
import com.balancedteam.gui.util.PagedGuiHelper;
import com.balancedteam.manager.ApplicationManager;
import com.balancedteam.manager.InviteManager;
import com.balancedteam.model.Team;
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

import java.util.*;

/**
 * 消息与通知中心 GUI
 * - 展示玩家收到的个人入队邀请（左键接受 / 右键拒绝）
 * - 展示向本队发出的待处理盟友申请（队长可左键接受）
 * - 展示未入队玩家向本队提交的入队申请（队长与管理员可审理：左键接受 / 右键拒绝）
 * - 无通知时显示「暂无通知」占位符
 * - 底部「返回」按钮：有队伍返回 TeamMenuGui，未加入队伍返回 TeamNotJoinedGui
 */
public class NotificationGui {

    private static final int REQUEST_SLOTS = 36;

    private enum NotificationType {
        TEAM_INVITE,
        ALLY_REQUEST,
        JOIN_APPLICATION
    }

    private static class NotificationEntry {
        final NotificationType type;
        final Object data;

        NotificationEntry(NotificationType type, Object data) {
            this.type = type;
            this.data = data;
        }
    }

    public static void open(BalancedTeamPlugin plugin, Player player, int page) {
        Team myTeam = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        boolean isLeader = PermissionUtil.isLeader(player, myTeam);
        boolean isOfficerOrLeader = PermissionUtil.isOfficerOrLeader(player, myTeam);

        // 1. 收集待处理的入队邀请
        List<InviteManager.Invite> invites = plugin.getInviteManager().getValidInvites(player.getUniqueId());

        // 2. 收集待处理的盟友申请 (仅队长可见)
        List<Integer> allyRequesterIds = (myTeam != null && isLeader)
                ? plugin.getRelationManager().getPendingRequestsTo(myTeam.getId())
                : Collections.emptyList();

        // 3. 收集待处理的入队申请 (队长与管理员可见)
        List<ApplicationManager.Application> applications = (myTeam != null && isOfficerOrLeader)
                ? plugin.getApplicationManager().getValidApplications(myTeam.getId())
                : Collections.emptyList();

        List<NotificationEntry> entries = new ArrayList<>();
        for (InviteManager.Invite invite : invites) {
            entries.add(new NotificationEntry(NotificationType.TEAM_INVITE, invite));
        }
        for (Integer requesterId : allyRequesterIds) {
            entries.add(new NotificationEntry(NotificationType.ALLY_REQUEST, requesterId));
        }
        for (ApplicationManager.Application app : applications) {
            entries.add(new NotificationEntry(NotificationType.JOIN_APPLICATION, app));
        }

        int totalItems = entries.size();
        int totalPages = PagedGuiHelper.calculateTotalPages(totalItems, REQUEST_SLOTS);
        int currentPage = PagedGuiHelper.clampPage(page, totalPages);

        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("COUNT", String.valueOf(totalItems));
        String title = plugin.getConfigManager().getRawMessage(GuiConfigKeys.NOTIFICATION_TITLE, titleMap);

        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 45, MessageUtil.color(title));
        holder.setInventory(inv);
        holder.setRefreshAction(p -> open(plugin, p, currentPage));

        // 背景黄色玻璃填充
        PagedGuiHelper.fillAll(inv, ItemBuilder.yellowGlass());

        // 填充通知条目 (槽位 0-35)
        int startIndex = (currentPage - 1) * REQUEST_SLOTS;
        int endIndex = Math.min(startIndex + REQUEST_SLOTS, totalItems);

        for (int i = startIndex; i < endIndex; i++) {
            NotificationEntry entry = entries.get(i);
            int slot = i - startIndex;

            if (entry.type == NotificationType.TEAM_INVITE) {
                InviteManager.Invite invite = (InviteManager.Invite) entry.data;
                Team targetTeam = plugin.getTeamManager().getTeamById(invite.getTeamId());
                if (targetTeam == null) continue;

                OfflinePlayer inviter = Bukkit.getOfflinePlayer(invite.getInviterUuid());
                String inviterName = inviter.getName() != null ? inviter.getName() : "未知玩家";

                Map<String, String> itemMap = new HashMap<>();
                itemMap.put("TEAM", targetTeam.getName());
                itemMap.put("INVITER", inviterName);
                itemMap.put("COUNT", String.valueOf(targetTeam.getMemberCount()));
                itemMap.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
                itemMap.put("REMAINING", String.valueOf(invite.getRemainingSeconds()));

                String itemName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.NOTIFICATION_INVITE_ITEM_NAME, itemMap);
                List<String> itemLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.NOTIFICATION_INVITE_ITEM_LORE, itemMap);

                ItemStack item = new ItemBuilder(Material.PLAYER_HEAD)
                        .skullOwner(invite.getInviterUuid())
                        .name(itemName)
                        .lore(itemLore)
                        .build();
                inv.setItem(slot, item);

                // 点击处理 (左键接受 / 右键拒绝)
                holder.setClickHandler(slot, e -> {
                    if (e.isRightClick()) {
                        // 右键拒绝
                        plugin.getInviteManager().removeInvite(player.getUniqueId(), targetTeam.getId());
                        SoundUtil.playDing(player);
                        Map<String, String> rejectMap = new HashMap<>();
                        rejectMap.put("TEAM", targetTeam.getName());
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_rejected", rejectMap));
                        holder.refresh(player);
                    } else {
                        // 左键接受
                        if (plugin.getTeamManager().isPlayerInTeam(player.getUniqueId())) {
                            SoundUtil.playError(player);
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_already_in_team"));
                            holder.refresh(player);
                            return;
                        }

                        if (!plugin.getInviteManager().consumeInvite(player.getUniqueId(), targetTeam.getId())) {
                            SoundUtil.playError(player);
                            Map<String, String> noMap = new HashMap<>();
                            noMap.put("TEAM", targetTeam.getName());
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_no_pending", noMap));
                            holder.refresh(player);
                            return;
                        }

                        if (targetTeam.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
                            SoundUtil.playError(player);
                            Map<String, String> maxMap = new HashMap<>();
                            maxMap.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_max_members", maxMap));
                            return;
                        }

                        plugin.getTeamManager().addMember(targetTeam, player.getUniqueId(), TeamRole.MEMBER).thenAccept(success -> {
                            if (success) {
                                SoundUtil.playSuccess(player);
                                Map<String, String> successMap = new HashMap<>();
                                successMap.put("TEAM", targetTeam.getName());
                                successMap.put("PLAYER", player.getName());

                                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_join_success", successMap));

                                for (UUID u : targetTeam.getMembers().keySet()) {
                                    Player p = Bukkit.getPlayer(u);
                                    if (p != null && p.isOnline() && !p.getUniqueId().equals(player.getUniqueId())) {
                                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("team_join_broadcast", successMap));
                                    }
                                }

                                Bukkit.getScheduler().runTask(plugin, () -> TeamMenuGui.open(plugin, player));
                            } else {
                                SoundUtil.playError(player);
                                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("database_error"));
                            }
                        });
                    }
                });

            } else if (entry.type == NotificationType.ALLY_REQUEST) {
                int requesterId = (Integer) entry.data;
                Team requesterTeam = plugin.getTeamManager().getTeamById(requesterId);
                if (requesterTeam == null || myTeam == null) continue;

                Map<String, String> itemMap = new HashMap<>();
                itemMap.put("TEAM", requesterTeam.getName());
                itemMap.put("COUNT", String.valueOf(requesterTeam.getMemberCount()));

                String itemName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.NOTIFICATION_REQUEST_ITEM_NAME, itemMap);
                List<String> itemLore = isLeader
                        ? plugin.getConfigManager().getMessageList(GuiConfigKeys.NOTIFICATION_REQUEST_ITEM_LORE_LEADER, itemMap)
                        : plugin.getConfigManager().getMessageList(GuiConfigKeys.NOTIFICATION_REQUEST_ITEM_LORE, itemMap);

                ItemStack item = new ItemBuilder(Material.PLAYER_HEAD)
                        .skullOwner(requesterTeam.getLeaderUuid())
                        .name(itemName)
                        .lore(itemLore)
                        .build();
                inv.setItem(slot, item);

                // 队长点击处理申请（左键接受 / 右键拒绝）
                if (isLeader) {
                    final Team currentMyTeam = myTeam;
                    final Team finalRequesterTeam = requesterTeam;
                    holder.setClickHandler(slot, e -> {
                        if (e.isRightClick()) {
                            // 右键拒绝
                            plugin.getRelationManager().denyAllyRequest(finalRequesterTeam.getId(), currentMyTeam.getId());
                            SoundUtil.playDing(player);
                            Map<String, String> rejectMap = new HashMap<>();
                            rejectMap.put("TEAM", finalRequesterTeam.getName());
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_request_denied", rejectMap));
                            // 通知申请方队长（如在线）
                            Player requesterLeader = Bukkit.getPlayer(finalRequesterTeam.getLeaderUuid());
                            if (requesterLeader != null && requesterLeader.isOnline()) {
                                Map<String, String> notifyMap = new HashMap<>();
                                notifyMap.put("TEAM", currentMyTeam.getName());
                                MessageUtil.sendMessage(requesterLeader, plugin.getConfigManager().getMessage("ally_request_denied_notify", notifyMap));
                            }
                            holder.refresh(player);
                        } else {
                            // 左键接受
                            int currentAllyCount = plugin.getRelationManager().getAllies(currentMyTeam.getId()).size();
                            int maxAllies = plugin.getConfigManager().getMaxAllies();
                            if (currentAllyCount >= maxAllies) {
                                SoundUtil.playError(player);
                                Map<String, String> msgMap = new HashMap<>();
                                msgMap.put("MAX", String.valueOf(maxAllies));
                                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_max_reached", msgMap));
                                return;
                            }
                            int requesterAllyCount = plugin.getRelationManager().getAllies(finalRequesterTeam.getId()).size();
                            if (requesterAllyCount >= maxAllies) {
                                SoundUtil.playError(player);
                                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_target_max_reached"));
                                return;
                            }

                            plugin.getRelationManager().acceptAllyRequest(finalRequesterTeam.getId(), currentMyTeam.getId()).thenAccept(success -> {
                                if (success) {
                                    SoundUtil.playSuccess(player);
                                    // 通知本队在线成员
                                    Map<String, String> msgMap = new HashMap<>();
                                    msgMap.put("TEAM", finalRequesterTeam.getName());
                                    for (UUID uuid : currentMyTeam.getMembers().keySet()) {
                                        Player p = Bukkit.getPlayer(uuid);
                                        if (p != null && p.isOnline()) {
                                            MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("ally_established", msgMap));
                                        }
                                    }
                                    // 通知申请方在线成员
                                    for (UUID uuid : finalRequesterTeam.getMembers().keySet()) {
                                        Player p = Bukkit.getPlayer(uuid);
                                        if (p != null && p.isOnline()) {
                                            Map<String, String> bMap = new HashMap<>();
                                            bMap.put("TEAM", currentMyTeam.getName());
                                            MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("ally_established", bMap));
                                        }
                                    }
                                    // 全服广播
                                    Map<String, String> broadcastMap = new HashMap<>();
                                    broadcastMap.put("TEAM1", finalRequesterTeam.getName());
                                    broadcastMap.put("TEAM2", currentMyTeam.getName());
                                    for (Player online : Bukkit.getOnlinePlayers()) {
                                        MessageUtil.sendMessage(online, plugin.getConfigManager().getMessage("ally_established_broadcast", broadcastMap));
                                    }
                                    holder.refresh(player);
                                }
                            });
                        }
                    });
                }
            } else if (entry.type == NotificationType.JOIN_APPLICATION) {
                ApplicationManager.Application app = (ApplicationManager.Application) entry.data;
                if (myTeam == null) continue;

                Map<String, String> itemMap = new HashMap<>();
                itemMap.put("PLAYER", app.getPlayerName());
                itemMap.put("COUNT", String.valueOf(myTeam.getMemberCount()));
                itemMap.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
                itemMap.put("REMAINING", String.valueOf(app.getRemainingSeconds()));

                String itemName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.NOTIFICATION_APPLICATION_ITEM_NAME, itemMap);
                List<String> itemLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.NOTIFICATION_APPLICATION_ITEM_LORE, itemMap);

                ItemStack item = new ItemBuilder(Material.PLAYER_HEAD)
                        .skullOwner(app.getPlayerUuid())
                        .name(itemName)
                        .lore(itemLore)
                        .build();
                inv.setItem(slot, item);

                // 队长与管理员点击审理入队申请 (左键接受 / 右键拒绝)
                final Team currentTeam = myTeam;
                holder.setClickHandler(slot, e -> {
                    if (e.isRightClick()) {
                        // 右键拒绝
                        plugin.getApplicationManager().removeApplication(currentTeam.getId(), app.getPlayerUuid());
                        SoundUtil.playDing(player);

                        Map<String, String> rejectMap = new HashMap<>();
                        rejectMap.put("PLAYER", app.getPlayerName());
                        rejectMap.put("TEAM", currentTeam.getName());
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_apply_rejected", rejectMap));

                        Player applicant = Bukkit.getPlayer(app.getPlayerUuid());
                        if (applicant != null && applicant.isOnline()) {
                            MessageUtil.sendMessage(applicant, plugin.getConfigManager().getMessage("team_apply_rejected_notify", rejectMap));
                        }
                        holder.refresh(player);
                    } else {
                        // 左键接受
                        if (plugin.getTeamManager().isPlayerInTeam(app.getPlayerUuid())) {
                            SoundUtil.playError(player);
                            plugin.getApplicationManager().removeApplication(currentTeam.getId(), app.getPlayerUuid());
                            Map<String, String> inTeamMap = new HashMap<>();
                            inTeamMap.put("PLAYER", app.getPlayerName());
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_apply_target_in_team", inTeamMap));
                            holder.refresh(player);
                            return;
                        }

                        if (currentTeam.getMemberCount() >= plugin.getConfigManager().getMaxMembers()) {
                            SoundUtil.playError(player);
                            Map<String, String> maxMap = new HashMap<>();
                            maxMap.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_invite_max_members", maxMap));
                            return;
                        }

                        if (!plugin.getApplicationManager().consumeApplication(currentTeam.getId(), app.getPlayerUuid())) {
                            SoundUtil.playError(player);
                            holder.refresh(player);
                            return;
                        }

                        plugin.getTeamManager().addMember(currentTeam, app.getPlayerUuid(), TeamRole.MEMBER).thenAccept(success -> {
                            if (success) {
                                SoundUtil.playSuccess(player);
                                Map<String, String> successMap = new HashMap<>();
                                successMap.put("TEAM", currentTeam.getName());
                                successMap.put("PLAYER", app.getPlayerName());

                                // 通知全队
                                for (UUID u : currentTeam.getMembers().keySet()) {
                                    Player p = Bukkit.getPlayer(u);
                                    if (p != null && p.isOnline()) {
                                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("team_join_broadcast", successMap));
                                    }
                                }

                                // 通知申请者
                                Player applicant = Bukkit.getPlayer(app.getPlayerUuid());
                                if (applicant != null && applicant.isOnline()) {
                                    SoundUtil.playSuccess(applicant);
                                    MessageUtil.sendMessage(applicant, plugin.getConfigManager().getMessage("team_apply_accepted_notify", successMap));
                                }

                                holder.refresh(player);
                            } else {
                                SoundUtil.playError(player);
                                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("database_error"));
                            }
                        });
                    }
                });
            }
        }

        // 若无通知，显示占位提示（槽位 16）
        if (entries.isEmpty()) {
            String emptyName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.NOTIFICATION_NO_REQUEST_NAME);
            List<String> emptyLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.NOTIFICATION_NO_REQUEST_LORE, Collections.emptyMap());
            PagedGuiHelper.setupEmptyPlaceholder(inv, 16, Material.PAPER, emptyName, emptyLore);
        }

        // ---- 底部控制栏（槽位 36-44）----

        // 上一页（槽位 36）
        if (currentPage > 1) {
            Map<String, String> prevMap = new HashMap<>();
            prevMap.put("PAGE", String.valueOf(currentPage - 1));
            String prevName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.NOTIFICATION_PREV_PAGE, prevMap);
            PagedGuiHelper.setupPrevButton(holder, inv, 36, player, currentPage, prevName, () -> open(plugin, player, currentPage - 1));
        }

        // 返回按钮（槽位 40）
        String backName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.NOTIFICATION_BACK_BUTTON);
        final Team returnCheckTeam = myTeam;
        PagedGuiHelper.setupBackButton(holder, inv, 40, player, backName, () -> {
            if (returnCheckTeam != null) {
                TeamMenuGui.open(plugin, player);
            } else {
                TeamNotJoinedGui.open(plugin, player);
            }
        });

        // 下一页（槽位 44）
        if (currentPage < totalPages) {
            Map<String, String> nextMap = new HashMap<>();
            nextMap.put("PAGE", String.valueOf(currentPage + 1));
            String nextName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.NOTIFICATION_NEXT_PAGE, nextMap);
            PagedGuiHelper.setupNextButton(holder, inv, 44, player, currentPage, totalPages, nextName, () -> open(plugin, player, currentPage + 1));
        }

        player.openInventory(inv);
    }
}

package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.config.GuiConfigKeys;
import com.balancedteam.gui.util.ItemBuilder;
import com.balancedteam.gui.util.PagedGuiHelper;
import com.balancedteam.model.Team;
import com.balancedteam.util.MessageUtil;
import com.balancedteam.util.PermissionUtil;
import com.balancedteam.util.SoundUtil;
import com.balancedteam.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 敌对管理 GUI
 * - 展示当前敌对列表，队长可点击移除敌对标记
 * - 底部「宣战」按钮提示玩家使用命令
 * - 底部「返回」按钮回到 TeamMenuGui
 */
public class EnemyManageGui {

    // 每页最多展示的敌对队伍数（上方 3 行）
    private static final int ENEMY_SLOTS = 27;

    public static void open(BalancedTeamPlugin plugin, Player player, int page) {
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "team_not_in_team"));
            SoundUtil.playError(player);
            return;
        }

        boolean isOfficerOrLeader = PermissionUtil.isOfficerOrLeader(player, team);

        // 获取敌对 ID 列表并分页
        List<Integer> enemyIds = plugin.getRelationManager().getEnemies(team.getId());
        int totalPages = PagedGuiHelper.calculateTotalPages(enemyIds.size(), ENEMY_SLOTS);
        int currentPage = PagedGuiHelper.clampPage(page, totalPages);

        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("PAGE", String.valueOf(currentPage));
        titleMap.put("TOTAL_PAGE", String.valueOf(totalPages));
        String title = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_TITLE, titleMap);

        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inv);
        holder.setRefreshAction(p -> open(plugin, p, currentPage));

        // 红色背景玻璃填充
        PagedGuiHelper.fillAll(inv, ItemBuilder.redGlass());

        // 填充敌对条目（槽位 0-26）
        int startIndex = (currentPage - 1) * ENEMY_SLOTS;
        int endIndex = Math.min(startIndex + ENEMY_SLOTS, enemyIds.size());

        for (int i = startIndex; i < endIndex; i++) {
            int enemyId = enemyIds.get(i);
            Team enemyTeam = plugin.getTeamManager().getTeamById(enemyId);
            if (enemyTeam == null) continue;

            int slot = i - startIndex;

            Map<String, String> itemMap = new HashMap<>();
            itemMap.put("TEAM", enemyTeam.getName());
            itemMap.put("COUNT", String.valueOf(enemyTeam.getMemberCount()));

            boolean declaredByUs = plugin.getRelationManager().isDeclaredEnemy(team.getId(), enemyTeam.getId());
            boolean hasIncomingTruce = plugin.getRelationManager().hasPendingTruceRequest(enemyTeam.getId(), team.getId());
            boolean hasOutgoingTruce = plugin.getRelationManager().hasPendingTruceRequest(team.getId(), enemyTeam.getId());

            String itemName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_ITEM_NAME, itemMap);
            List<String> itemLore = new ArrayList<>(plugin.getConfigManager().getMessageList(player, GuiConfigKeys.ENEMY_MANAGE_ITEM_LORE, itemMap));

            if (hasIncomingTruce) {
                long rem = plugin.getRelationManager().getTruceRequestRemainingSeconds(enemyTeam.getId(), team.getId());
                itemLore.add("");
                itemLore.add(MessageUtil.color("&f🕊 &a对方已向我方发送停战求和申请！"));
                itemLore.add(MessageUtil.color("&7剩余考虑时间: &e" + rem + " &7秒"));
                if (isOfficerOrLeader) {
                    itemLore.add(MessageUtil.color("&a▶ 左键点击: 同意停战 (开启战后保护)"));
                    itemLore.add(MessageUtil.color("&c▶ 右键点击: 拒绝求和"));
                }
            } else if (hasOutgoingTruce) {
                long rem = plugin.getRelationManager().getTruceRequestRemainingSeconds(team.getId(), enemyTeam.getId());
                itemLore.add("");
                itemLore.add(MessageUtil.color("&e⌛ 我方已发起停战求和 (等待对方同意)"));
                itemLore.add(MessageUtil.color("&7有效时间剩余: &e" + rem + " &7秒"));
                if (isOfficerOrLeader) {
                    itemLore.add(MessageUtil.color("&c▶ 点击撤销求和申请"));
                }
            } else if (declaredByUs) {
                itemLore.add("");
                itemLore.add(MessageUtil.color("&c⚔ 双方处于交战状态 (需双方同意方可解除)"));
                if (isOfficerOrLeader) {
                    itemLore.add(MessageUtil.color("&f▶ 点击发起停战求和申请"));
                }
            } else {
                itemLore.add("");
                itemLore.add(MessageUtil.color("&c⚠ 对方单方面向我方宣战"));
                if (isOfficerOrLeader) {
                    itemLore.add(MessageUtil.color("&e▶ 左键点击: 向对方宣战 (迎战)"));
                    itemLore.add(MessageUtil.color("&f▶ 右键点击: 发起停战求和申请"));
                }
            }

            ItemStack item = new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(enemyTeam.getLeaderUuid())
                    .name(itemName)
                    .lore(itemLore)
                    .build();
            inv.setItem(slot, item);

            // 管理员/队长点击交互
            if (isOfficerOrLeader) {
                final Team finalEnemyTeam = enemyTeam;
                final boolean wasDeclaredByUs = declaredByUs;
                final boolean finalHasIncomingTruce = hasIncomingTruce;
                final boolean finalHasOutgoingTruce = hasOutgoingTruce;
                holder.setClickHandler(slot, e -> {
                    if (finalHasIncomingTruce) {
                        if (e.isRightClick()) {
                            // 拒绝求和
                            plugin.getRelationManager().denyTruceRequest(finalEnemyTeam.getId(), team.getId());
                            SoundUtil.playDing(player);
                            Map<String, String> map = new HashMap<>();
                            map.put("TEAM", finalEnemyTeam.getName());
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "truce_denied", map));

                            Map<String, String> notifyMap = new HashMap<>();
                            notifyMap.put("TEAM", team.getName());
                            for (UUID u : finalEnemyTeam.getMembers().keySet()) {
                                Player p = Bukkit.getPlayer(u);
                                if (p != null && p.isOnline()) {
                                    MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "truce_denied_notify", notifyMap));
                                }
                            }
                            holder.refresh(player);
                        } else {
                            // 同意求和
                            int protectionSeconds = plugin.getConfigManager().getPostWarProtectionSeconds();
                            plugin.getRelationManager().acceptTruceRequest(finalEnemyTeam.getId(), team.getId()).thenAccept(success -> {
                                if (success) {
                                    SoundUtil.playSuccess(player);
                                    Map<String, String> bcMap = new HashMap<>();
                                    bcMap.put("TEAM1", finalEnemyTeam.getName());
                                    bcMap.put("TEAM2", team.getName());

                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "truce_established_broadcast", bcMap));
                                    }

                                    Map<String, String> toEnemyMap = new HashMap<>();
                                    toEnemyMap.put("TEAM", team.getName());
                                    for (UUID u : finalEnemyTeam.getMembers().keySet()) {
                                        Player p = Bukkit.getPlayer(u);
                                        if (p != null && p.isOnline()) {
                                            SoundUtil.playSuccess(p);
                                            MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "truce_established", toEnemyMap));
                                            if (protectionSeconds > 0) {
                                                Map<String, String> protMap = new HashMap<>();
                                                protMap.put("TIME", TimeUtil.formatDuration(p, protectionSeconds));
                                                MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "truce_protection_started", protMap));
                                            }
                                        }
                                    }

                                    Map<String, String> toOurMap = new HashMap<>();
                                    toOurMap.put("TEAM", finalEnemyTeam.getName());
                                    for (UUID u : team.getMembers().keySet()) {
                                        Player p = Bukkit.getPlayer(u);
                                        if (p != null && p.isOnline()) {
                                            SoundUtil.playSuccess(p);
                                            MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "truce_established", toOurMap));
                                            if (protectionSeconds > 0) {
                                                Map<String, String> protMap = new HashMap<>();
                                                protMap.put("TIME", TimeUtil.formatDuration(p, protectionSeconds));
                                                MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "truce_protection_started", protMap));
                                            }
                                        }
                                    }

                                    holder.refresh(player);
                                } else {
                                    SoundUtil.playError(player);
                                    MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "database_error"));
                                }
                            });
                        }
                    } else if (finalHasOutgoingTruce) {
                        // 撤销求和申请
                        plugin.getRelationManager().cancelTruceRequest(team.getId(), finalEnemyTeam.getId());
                        SoundUtil.playDing(player);
                        Map<String, String> cancelMap = new HashMap<>();
                        cancelMap.put("TEAM", finalEnemyTeam.getName());
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "truce_cancel_success", cancelMap));
                        holder.refresh(player);
                    } else if (wasDeclaredByUs || e.isRightClick()) {
                        // 发起求和申请
                        plugin.getRelationManager().sendTruceRequest(team.getId(), finalEnemyTeam.getId()).thenAccept(success -> {
                            if (success) {
                                SoundUtil.playSuccess(player);
                                Map<String, String> map = new HashMap<>();
                                map.put("TEAM", finalEnemyTeam.getName());
                                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "truce_request_sent", map));

                                Map<String, String> notifyMap = new HashMap<>();
                                notifyMap.put("TEAM", team.getName());
                                for (UUID u : finalEnemyTeam.getMembers().keySet()) {
                                    Player p = Bukkit.getPlayer(u);
                                    if (p != null && p.isOnline()) {
                                        SoundUtil.playDing(p);
                                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage(p, "truce_request_received", notifyMap));
                                    }
                                }
                                holder.refresh(player);
                            } else {
                                SoundUtil.playError(player);
                                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "database_error"));
                            }
                        });
                    } else {
                        // 迎战宣战
                        TeamSelectGui.executeEnemyAdd(plugin, player, team, finalEnemyTeam);
                        holder.refresh(player);
                    }
                });
            }
        }

        // 若无敌对队伍，显示占位提示 (槽位 13)
        if (enemyIds.isEmpty()) {
            String emptyName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_NO_ENEMY_NAME);
            List<String> emptyLore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.ENEMY_MANAGE_NO_ENEMY_LORE, Collections.emptyMap());
            PagedGuiHelper.setupEmptyPlaceholder(inv, 13, Material.BARRIER, emptyName, emptyLore);
        }

        // ---- 底部控制栏（槽位 27-53）----

        // 上一页（槽位 27）
        if (currentPage > 1) {
            Map<String, String> prevMap = new HashMap<>();
            prevMap.put("PAGE", String.valueOf(currentPage - 1));
            String prevName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_PREV_PAGE, prevMap);
            PagedGuiHelper.setupPrevButton(holder, inv, 27, player, currentPage, prevName, () -> open(plugin, player, currentPage - 1));
        }

        // 宣战按钮（槽位 31），非管理员/队长灰色不可点
        boolean canDeclare = PermissionUtil.isOfficerOrLeader(player, team);
        String addName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_ADD_BUTTON_NAME);
        List<String> addLore = canDeclare
                ? plugin.getConfigManager().getMessageList(player, GuiConfigKeys.ENEMY_MANAGE_ADD_BUTTON_LORE_LEADER, Collections.emptyMap())
                : plugin.getConfigManager().getMessageList(player, GuiConfigKeys.ENEMY_MANAGE_ADD_BUTTON_LORE, Collections.emptyMap());
        Material addMaterial = canDeclare ? Material.REDSTONE : Material.GRAY_DYE;
        ItemStack addItem = new ItemBuilder(addMaterial).name(addName).lore(addLore).build();
        inv.setItem(31, addItem);
        if (canDeclare) {
            holder.setClickHandler(31, e -> {
                SoundUtil.playClick(player);
                TeamSelectGui.open(plugin, player, TeamSelectGui.SelectMode.ENEMY, 1);
            });
        }

        // 返回按钮（槽位 49）
        String backName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_BACK_BUTTON);
        PagedGuiHelper.setupBackButton(holder, inv, 49, player, backName, () -> TeamMenuGui.open(plugin, player));

        // 战后保护状态指示（槽位 40）
        Map<Integer, Long> activeProtections = plugin.getRelationManager().getActivePostWarProtectionsFor(team.getId());
        if (!activeProtections.isEmpty()) {
            Map<String, String> countMap = new HashMap<>();
            countMap.put("COUNT", String.valueOf(activeProtections.size()));
            String badgeName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_PROTECTION_NAME, countMap);

            List<String> protLore = new ArrayList<>(plugin.getConfigManager().getMessageList(player, GuiConfigKeys.ENEMY_MANAGE_PROTECTION_HEADER, countMap));
            for (Map.Entry<Integer, Long> entry : activeProtections.entrySet()) {
                Team otherTeam = plugin.getTeamManager().getTeamById(entry.getKey());
                String otherName = (otherTeam != null) ? otherTeam.getName() : ("#" + entry.getKey());
                Map<String, String> itemMap = new HashMap<>();
                itemMap.put("TEAM", otherName);
                itemMap.put("TIME", TimeUtil.formatDuration(player, entry.getValue()));
                protLore.add(plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_PROTECTION_ITEM, itemMap));
            }
            protLore.addAll(plugin.getConfigManager().getMessageList(player, GuiConfigKeys.ENEMY_MANAGE_PROTECTION_FOOTER, countMap));

            ItemStack protItem = new ItemBuilder(Material.SHIELD)
                    .name(badgeName)
                    .lore(protLore)
                    .build();
            inv.setItem(40, protItem);
        }

        // 下一页（槽位 35）
        if (currentPage < totalPages) {
            Map<String, String> nextMap = new HashMap<>();
            nextMap.put("PAGE", String.valueOf(currentPage + 1));
            String nextName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_NEXT_PAGE, nextMap);
            PagedGuiHelper.setupNextButton(holder, inv, 35, player, currentPage, totalPages, nextName, () -> open(plugin, player, currentPage + 1));
        }

        player.openInventory(inv);
    }
}

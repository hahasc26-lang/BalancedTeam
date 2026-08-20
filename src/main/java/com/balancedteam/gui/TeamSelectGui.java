package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.config.GuiConfigKeys;
import com.balancedteam.gui.util.ItemBuilder;
import com.balancedteam.gui.util.PagedGuiHelper;
import com.balancedteam.model.Team;
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
import java.util.stream.Collectors;

/**
 * 目标团队快速选择与交互 GUI
 * - 适用于【发送同盟申请】和【向目标团队宣战】的快速可视化选择
 * - 展示全服其他团队的队长、人数、同盟与敌对状态
 * - 支持鼠标左键一键提交结盟申请/宣战，并支持聊天框手动输入模式
 */
public class TeamSelectGui {

    private static final int PAGE_SIZE = 45;

    public enum SelectMode {
        ALLY,   // 同盟申请选择模式
        ENEMY   // 宣战选择模式
    }

    public static void open(BalancedTeamPlugin plugin, Player player, SelectMode mode, int page) {
        Team myTeam = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (myTeam == null) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_in_team"));
            return;
        }

        // 权限校验：结盟申请必须为队长，宣战必须为管理员或队长
        if (mode == SelectMode.ALLY) {
            if (!PermissionUtil.checkLeader(player, myTeam, plugin.getConfigManager())) {
                return;
            }
        } else {
            if (!PermissionUtil.checkOfficerOrLeader(player, myTeam, plugin.getConfigManager())) {
                return;
            }
        }

        // 获取全服所有除自己队伍以外的团队，按名称字典序排序
        List<Team> targetTeams = plugin.getTeamManager().getAllTeams().stream()
                .filter(t -> t.getId() != myTeam.getId())
                .sorted((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()))
                .collect(Collectors.toList());

        int totalPages = PagedGuiHelper.calculateTotalPages(targetTeams.size(), PAGE_SIZE);
        int currentPage = PagedGuiHelper.clampPage(page, totalPages);

        Map<String, String> titleMap = new HashMap<>();
        titleMap.put("PAGE", String.valueOf(currentPage));
        titleMap.put("TOTAL_PAGE", String.valueOf(totalPages));
        String titleKey = (mode == SelectMode.ALLY) ? GuiConfigKeys.TEAM_SELECT_TITLE_ALLY : GuiConfigKeys.TEAM_SELECT_TITLE_ENEMY;
        String title = plugin.getConfigManager().getRawMessage(titleKey, titleMap);

        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inv);
        holder.setRefreshAction(p -> open(plugin, p, mode, currentPage));

        // 填充目标团队条目 (槽位 0-44)
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, targetTeams.size());

        for (int i = startIndex; i < endIndex; i++) {
            Team targetTeam = targetTeams.get(i);
            int slot = i - startIndex;

            boolean isAlly = plugin.getRelationManager().isAlly(myTeam.getId(), targetTeam.getId());
            boolean isEnemy = plugin.getRelationManager().isEnemy(myTeam.getId(), targetTeam.getId());
            boolean hasPendingRequest = plugin.getRelationManager().hasPendingAllyRequest(myTeam.getId(), targetTeam.getId());

            OfflinePlayer leader = Bukkit.getOfflinePlayer(targetTeam.getLeaderUuid());
            String leaderName = leader.getName() != null ? leader.getName() : "未知";

            Map<String, String> itemMap = new HashMap<>();
            itemMap.put("TEAM", targetTeam.getName());
            itemMap.put("LEADER", leaderName);
            itemMap.put("COUNT", String.valueOf(targetTeam.getMemberCount()));
            itemMap.put("MAX", String.valueOf(plugin.getConfigManager().getMaxMembers()));

            ItemStack item;
            if (mode == SelectMode.ALLY) {
                // ==================== 同盟申请模式 ====================
                if (isAlly) {
                    // 已是盟友
                    String name = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_ITEM_IS_ALLY_NAME, itemMap);
                    List<String> lore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_ITEM_IS_ALLY_LORE, itemMap);
                    item = new ItemBuilder(Material.EMERALD_BLOCK).name(name).lore(lore).build();
                    holder.setClickHandler(slot, e -> {
                        SoundUtil.playError(player);
                        Map<String, String> m = new HashMap<>();
                        m.put("TEAM", targetTeam.getName());
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_already_allied", m));
                    });
                } else if (isEnemy) {
                    // 是敌对队伍，不可结盟
                    String name = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_ITEM_IS_ENEMY_NAME, itemMap);
                    List<String> lore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_ITEM_IS_ENEMY_LORE, itemMap);
                    item = new ItemBuilder(Material.REDSTONE_BLOCK).name(name).lore(lore).build();
                    holder.setClickHandler(slot, e -> {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_cant_enemy_ally"));
                    });
                } else if (hasPendingRequest) {
                    // 已发送结盟申请待审理中
                    String name = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_ITEM_PENDING_NAME, itemMap);
                    List<String> lore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_ITEM_PENDING_LORE, itemMap);
                    item = new ItemBuilder(Material.CLOCK).name(name).lore(lore).build();
                    holder.setClickHandler(slot, e -> {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_already_requested"));
                    });
                } else if (plugin.getRelationManager().getAllies(myTeam.getId()).size() >= plugin.getConfigManager().getMaxAllies()) {
                    // 本队盟友已达上限
                    Map<String, String> maxMap = new HashMap<>(itemMap);
                    maxMap.put("MAX_ALLY", String.valueOf(plugin.getConfigManager().getMaxAllies()));
                    String name = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_ITEM_MAX_REACHED_NAME, maxMap);
                    List<String> lore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_ITEM_MAX_REACHED_LORE, maxMap);
                    item = new ItemBuilder(Material.BARRIER).name(name).lore(lore).build();
                    holder.setClickHandler(slot, e -> {
                        SoundUtil.playError(player);
                        Map<String, String> m = new HashMap<>();
                        m.put("MAX", String.valueOf(plugin.getConfigManager().getMaxAllies()));
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_max_reached", m));
                    });
                } else {
                    // 可发送同盟申请
                    String name = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_ITEM_AVAILABLE_ALLY_NAME, itemMap);
                    List<String> lore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_ITEM_AVAILABLE_ALLY_LORE, itemMap);
                    item = new ItemBuilder(Material.PLAYER_HEAD)
                            .skullOwner(targetTeam.getLeaderUuid())
                            .name(name)
                            .lore(lore)
                            .build();
                    holder.setClickHandler(slot, e -> {
                        executeAllyRequest(plugin, player, myTeam, targetTeam);
                        holder.refresh(player);
                    });
                }
            } else {
                // ==================== 宣战模式 ====================
                if (isEnemy) {
                    // 已经是敌对团队
                    String name = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_ITEM_IS_ENEMY_NAME, itemMap);
                    List<String> lore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_ITEM_IS_ENEMY_LORE, itemMap);
                    item = new ItemBuilder(Material.REDSTONE_BLOCK).name(name).lore(lore).build();
                    holder.setClickHandler(slot, e -> {
                        SoundUtil.playError(player);
                        Map<String, String> m = new HashMap<>();
                        m.put("TEAM", targetTeam.getName());
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_already_enemy", m));
                    });
                } else if (isAlly) {
                    // 是盟友团队，不可直接宣战
                    String name = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_ITEM_IS_ALLY_NAME, itemMap);
                    List<String> lore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_ITEM_IS_ALLY_LORE, itemMap);
                    item = new ItemBuilder(Material.EMERALD_BLOCK).name(name).lore(lore).build();
                    holder.setClickHandler(slot, e -> {
                        SoundUtil.playError(player);
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_cant_enemy_ally"));
                    });
                } else if (plugin.getRelationManager().getEnemies(myTeam.getId()).size() >= plugin.getConfigManager().getMaxEnemies()) {
                    // 敌对数量已达上限
                    Map<String, String> maxMap = new HashMap<>(itemMap);
                    maxMap.put("MAX_ENEMY", String.valueOf(plugin.getConfigManager().getMaxEnemies()));
                    String name = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_ITEM_MAX_REACHED_NAME, maxMap);
                    List<String> lore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_ITEM_MAX_REACHED_LORE, maxMap);
                    item = new ItemBuilder(Material.BARRIER).name(name).lore(lore).build();
                    holder.setClickHandler(slot, e -> {
                        SoundUtil.playError(player);
                        Map<String, String> m = new HashMap<>();
                        m.put("MAX", String.valueOf(plugin.getConfigManager().getMaxEnemies()));
                        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_max_reached", m));
                    });
                } else {
                    // 可宣战
                    String name = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_ITEM_AVAILABLE_ENEMY_NAME, itemMap);
                    List<String> lore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_ITEM_AVAILABLE_ENEMY_LORE, itemMap);
                    item = new ItemBuilder(Material.PLAYER_HEAD)
                            .skullOwner(targetTeam.getLeaderUuid())
                            .name(name)
                            .lore(lore)
                            .build();
                    holder.setClickHandler(slot, e -> {
                        executeEnemyAdd(plugin, player, myTeam, targetTeam);
                        holder.refresh(player);
                    });
                }
            }

            inv.setItem(slot, item);
        }

        // 若全服无其他团队，显示占位提示 (槽位 22)
        if (targetTeams.isEmpty()) {
            String emptyName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_NO_TEAMS_NAME);
            List<String> emptyLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_NO_TEAMS_LORE, Collections.emptyMap());
            PagedGuiHelper.setupEmptyPlaceholder(inv, 22, Material.PAPER, emptyName, emptyLore);
        }

        // ---- 底部控制栏 (槽位 45-53) ----
        PagedGuiHelper.fillRange(inv, 45, 53, ItemBuilder.grayGlass());

        // 上一页 (槽位 45)
        if (currentPage > 1) {
            Map<String, String> prevMap = new HashMap<>();
            prevMap.put("PAGE", String.valueOf(currentPage - 1));
            String prevName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_PREV_PAGE, prevMap);
            PagedGuiHelper.setupPrevButton(holder, inv, 45, player, currentPage, prevName, () -> open(plugin, player, mode, currentPage - 1));
        }

        // 手动输入团队名按钮 (槽位 48)
        String manualName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_MANUAL_INPUT_NAME);
        List<String> manualLore = plugin.getConfigManager().getMessageList(GuiConfigKeys.TEAM_SELECT_MANUAL_INPUT_LORE, Collections.emptyMap());
        ItemStack manualItem = new ItemBuilder(Material.SPYGLASS).name(manualName).lore(manualLore).build();
        inv.setItem(48, manualItem);
        holder.setClickHandler(48, e -> {
            SoundUtil.playClick(player);
            player.closeInventory();
            String promptKey = (mode == SelectMode.ALLY) ? GuiConfigKeys.TEAM_SELECT_CHAT_PROMPT_ALLY : GuiConfigKeys.TEAM_SELECT_CHAT_PROMPT_ENEMY;
            String prompt = plugin.getConfigManager().getRawMessage(promptKey);
            plugin.getChatInputManager().requestInput(player, prompt, targetTeamName -> {
                if (mode == SelectMode.ALLY) {
                    executeAllyRequestByName(plugin, player, myTeam, targetTeamName);
                } else {
                    executeEnemyAddByName(plugin, player, myTeam, targetTeamName);
                }
            }, () -> {
                if (mode == SelectMode.ALLY) {
                    AllyManageGui.open(plugin, player, 1);
                } else {
                    EnemyManageGui.open(plugin, player, 1);
                }
            });
        });

        // 返回按钮 (槽位 49)
        String backName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_BACK_BUTTON);
        PagedGuiHelper.setupBackButton(holder, inv, 49, player, backName, () -> {
            if (mode == SelectMode.ALLY) {
                AllyManageGui.open(plugin, player, 1);
            } else {
                EnemyManageGui.open(plugin, player, 1);
            }
        });

        // 下一页 (槽位 53)
        if (currentPage < totalPages) {
            Map<String, String> nextMap = new HashMap<>();
            nextMap.put("PAGE", String.valueOf(currentPage + 1));
            String nextName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.TEAM_SELECT_NEXT_PAGE, nextMap);
            PagedGuiHelper.setupNextButton(holder, inv, 53, player, currentPage, totalPages, nextName, () -> open(plugin, player, mode, currentPage + 1));
        }

        player.openInventory(inv);
    }

    /**
     * 执行发送同盟申请逻辑
     */
    public static void executeAllyRequest(BalancedTeamPlugin plugin, Player player, Team myTeam, Team targetTeam) {
        if (targetTeam == null) return;

        if (myTeam.getId() == targetTeam.getId()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_cant_ally_self"));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getRelationManager().isAlly(myTeam.getId(), targetTeam.getId())) {
            Map<String, String> map = new HashMap<>();
            map.put("TEAM", targetTeam.getName());
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_already_allied", map));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getRelationManager().isEnemy(myTeam.getId(), targetTeam.getId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_cant_enemy_ally"));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getRelationManager().getAllies(myTeam.getId()).size() >= plugin.getConfigManager().getMaxAllies()) {
            Map<String, String> map = new HashMap<>();
            map.put("MAX", String.valueOf(plugin.getConfigManager().getMaxAllies()));
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_max_reached", map));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getRelationManager().getAllies(targetTeam.getId()).size() >= plugin.getConfigManager().getMaxAllies()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_target_max_reached"));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getRelationManager().hasPendingAllyRequest(myTeam.getId(), targetTeam.getId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_already_requested"));
            SoundUtil.playError(player);
            return;
        }

        long timeout = plugin.getConfigManager().getAllyRequestTimeout();
        plugin.getRelationManager().sendAllyRequest(myTeam.getId(), targetTeam.getId(), timeout);

        Map<String, String> map = new HashMap<>();
        map.put("TEAM", targetTeam.getName());
        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("ally_request_sent", map));
        SoundUtil.playSuccess(player);

        Player targetLeader = Bukkit.getPlayer(targetTeam.getLeaderUuid());
        if (targetLeader != null && targetLeader.isOnline()) {
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("TEAM", myTeam.getName());
            MessageUtil.sendMessage(targetLeader, plugin.getConfigManager().getMessage("ally_request_received", reqMap));
            SoundUtil.playDing(targetLeader);
        }
    }

    /**
     * 按名称执行发送同盟申请
     */
    public static void executeAllyRequestByName(BalancedTeamPlugin plugin, Player player, Team myTeam, String targetTeamName) {
        if (targetTeamName == null || targetTeamName.trim().isEmpty()) return;
        String cleanName = targetTeamName.trim();
        Team targetTeam = plugin.getTeamManager().getTeamByName(cleanName);
        if (targetTeam == null) {
            Map<String, String> map = new HashMap<>();
            map.put("TEAM", cleanName);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_found", map));
            SoundUtil.playError(player);
            return;
        }
        executeAllyRequest(plugin, player, myTeam, targetTeam);
    }

    /**
     * 执行宣战逻辑
     */
    public static void executeEnemyAdd(BalancedTeamPlugin plugin, Player player, Team myTeam, Team targetTeam) {
        if (targetTeam == null) return;

        if (myTeam.getId() == targetTeam.getId()) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_add_self"));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getRelationManager().isAlly(myTeam.getId(), targetTeam.getId())) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_cant_enemy_ally"));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getRelationManager().isEnemy(myTeam.getId(), targetTeam.getId())) {
            Map<String, String> map = new HashMap<>();
            map.put("TEAM", targetTeam.getName());
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_already_enemy", map));
            SoundUtil.playError(player);
            return;
        }

        if (plugin.getRelationManager().getEnemies(myTeam.getId()).size() >= plugin.getConfigManager().getMaxEnemies()) {
            Map<String, String> map = new HashMap<>();
            map.put("MAX", String.valueOf(plugin.getConfigManager().getMaxEnemies()));
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_max_reached", map));
            SoundUtil.playError(player);
            return;
        }

        plugin.getRelationManager().addEnemy(myTeam.getId(), targetTeam.getId()).thenAccept(success -> {
            if (success) {
                SoundUtil.playSuccess(player);
                Map<String, String> map = new HashMap<>();
                map.put("TEAM", targetTeam.getName());
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("enemy_add_success", map));

                // 向目标团队在线成员广播宣战提示
                for (UUID u : targetTeam.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null && p.isOnline()) {
                        SoundUtil.playWarning(p);
                        Map<String, String> enemyNotify = new HashMap<>();
                        enemyNotify.put("TEAM", myTeam.getName());
                        MessageUtil.sendMessage(p, plugin.getConfigManager().getMessage("enemy_declared_notify", enemyNotify));
                    }
                }
            } else {
                SoundUtil.playError(player);
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("database_error"));
            }
        });
    }

    /**
     * 按名称执行宣战
     */
    public static void executeEnemyAddByName(BalancedTeamPlugin plugin, Player player, Team myTeam, String targetTeamName) {
        if (targetTeamName == null || targetTeamName.trim().isEmpty()) return;
        String cleanName = targetTeamName.trim();
        Team targetTeam = plugin.getTeamManager().getTeamByName(cleanName);
        if (targetTeam == null) {
            Map<String, String> map = new HashMap<>();
            map.put("TEAM", cleanName);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_not_found", map));
            SoundUtil.playError(player);
            return;
        }
        executeEnemyAdd(plugin, player, myTeam, targetTeam);
    }
}

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

        boolean isLeader = PermissionUtil.isLeader(player, team);

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

            String itemName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.ENEMY_MANAGE_ITEM_NAME, itemMap);
            List<String> itemLore = isLeader
                    ? plugin.getConfigManager().getMessageList(player, GuiConfigKeys.ENEMY_MANAGE_ITEM_LORE_LEADER, itemMap)
                    : plugin.getConfigManager().getMessageList(player, GuiConfigKeys.ENEMY_MANAGE_ITEM_LORE, itemMap);

            ItemStack item = new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(enemyTeam.getLeaderUuid())
                    .name(itemName)
                    .lore(itemLore)
                    .build();
            inv.setItem(slot, item);

            // 队长点击可移除敌对标记
            if (isLeader) {
                final Team finalEnemyTeam = enemyTeam;
                holder.setClickHandler(slot, e -> {
                    plugin.getRelationManager().removeEnemy(team.getId(), finalEnemyTeam.getId()).thenAccept(success -> {
                        if (success) {
                            SoundUtil.playSuccess(player);
                            Map<String, String> msgMap = new HashMap<>();
                            msgMap.put("TEAM", finalEnemyTeam.getName());
                            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "enemy_remove_success", msgMap));
                            holder.refresh(player);
                        }
                    });
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

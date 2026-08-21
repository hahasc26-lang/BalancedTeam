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

/**
 * 团队列表分页 GUI (/team list)
 */
public class TeamListGui {

    private static final int PAGE_SIZE = 45; // 定义每页显示的队伍数量（对应背包的 0-44 号槽位）

    public static void open(BalancedTeamPlugin plugin, Player player, int page) {
        List<Team> allTeams = new ArrayList<>(plugin.getTeamManager().getAllTeams());
        allTeams.sort(Comparator.<Team>comparingInt(t -> t.getMemberCount()).reversed());

        int totalPages = PagedGuiHelper.calculateTotalPages(allTeams.size(), PAGE_SIZE);
        int currentPage = PagedGuiHelper.clampPage(page, totalPages);

        Map<String, String> titlePlaceholders = new HashMap<>();
        titlePlaceholders.put("PAGE", String.valueOf(currentPage));
        titlePlaceholders.put("TOTAL_PAGE", String.valueOf(totalPages));
        String title = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.LIST_TITLE, titlePlaceholders);

        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, MessageUtil.color(title));
        holder.setInventory(inv);
        holder.setRefreshAction(p -> open(plugin, p, currentPage));

        // 填充队伍条目 (槽位 0-44)
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, allTeams.size());

        for (int i = startIndex; i < endIndex; i++) {
            Team team = allTeams.get(i);
            int slot = i - startIndex;

            OfflinePlayer leader = Bukkit.getOfflinePlayer(team.getLeaderUuid());
            String leaderName = leader.getName() != null ? leader.getName() : "未知";

            int onlineCount = 0;
            for (UUID u : team.getMembers().keySet()) {
                Player p = Bukkit.getPlayer(u);
                if (p != null && p.isOnline()) {
                    onlineCount++;
                }
            }

            int allyCount = plugin.getRelationManager().getAllies(team.getId()).size();
            int enemyCount = plugin.getRelationManager().getEnemies(team.getId()).size();
            String ffStatus = plugin.getConfigManager().isFriendlyFireActive(team)
                    ? plugin.getConfigManager().getRawMessage(player, "status.on")
                    : plugin.getConfigManager().getRawMessage(player, "status.off");

            Map<String, String> itemMap = new HashMap<>();
            itemMap.put("TEAM", team.getName());
            itemMap.put("LEADER", leaderName);
            itemMap.put("ONLINE", String.valueOf(onlineCount));
            itemMap.put("TOTAL", String.valueOf(team.getMemberCount()));
            itemMap.put("FF", ffStatus);
            itemMap.put("ALLIES", String.valueOf(allyCount));
            itemMap.put("ENEMIES", String.valueOf(enemyCount));
            itemMap.put("DATE", TimeUtil.formatDate(team.getCreatedAt()));

            String itemName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.LIST_TEAM_ITEM_NAME,
                    itemMap);
            List<String> itemLore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.LIST_TEAM_ITEM_LORE,
                    itemMap);

            ItemStack item = new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(team.getLeaderUuid())
                    .name(itemName)
                    .lore(itemLore)
                    .build();

            inv.setItem(slot, item);
            holder.setClickHandler(slot, e -> {
                SoundUtil.playClick(player);
                TeamDetailGui.open(plugin, player, team, currentPage);
            });
        }

        // 若服务器暂无团队，显示占位符 (槽位 22)
        if (allTeams.isEmpty()) {
            PagedGuiHelper.setupEmptyPlaceholder(inv, 22, Material.PAPER,
                    "&7(暂无已创建的团队)",
                    Collections.singletonList("&7输入 /team create <团队名称> 创建全服第一个团队！"));
        }

        // 底部控制栏 (槽位 45-53) 背景填充
        PagedGuiHelper.fillRange(inv, 45, 53, ItemBuilder.grayGlass());

        // 上一页 (槽位 45)
        if (currentPage > 1) {
            Map<String, String> prevMap = new HashMap<>();
            prevMap.put("PAGE", String.valueOf(currentPage - 1));
            String prevName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.LIST_PREV_PAGE, prevMap);
            PagedGuiHelper.setupPrevButton(holder, inv, 45, player, currentPage, prevName,
                    () -> open(plugin, player, currentPage - 1));
        }

        // 当前页统计信息 (槽位 49)
        Map<String, String> sumMap = new HashMap<>();
        sumMap.put("TOTAL_TEAMS", String.valueOf(allTeams.size()));
        sumMap.put("PAGE", String.valueOf(currentPage));
        sumMap.put("TOTAL_PAGES", String.valueOf(totalPages));
        String sumName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.LIST_SUMMARY_ITEM_NAME, sumMap);
        List<String> sumLore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.LIST_SUMMARY_ITEM_LORE,
                sumMap);

        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name(sumName)
                .lore(sumLore)
                .build();
        inv.setItem(49, infoItem);

        // 下一页 (槽位 53)
        if (currentPage < totalPages) {
            Map<String, String> nextMap = new HashMap<>();
            nextMap.put("PAGE", String.valueOf(currentPage + 1));
            String nextName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.LIST_NEXT_PAGE, nextMap);
            PagedGuiHelper.setupNextButton(holder, inv, 53, player, currentPage, totalPages, nextName,
                    () -> open(plugin, player, currentPage + 1));
        }

        player.openInventory(inv);
    }
}

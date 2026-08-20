package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.config.GuiConfigKeys;
import com.balancedteam.gui.util.ItemBuilder;
import com.balancedteam.gui.util.PagedGuiHelper;
import com.balancedteam.util.MessageUtil;
import com.balancedteam.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家未加入队伍时的团队主菜单 GUI (/team 或 /team menu)
 * 提供功能：
 * 1. 打开服务器队伍列表 (TeamListGui)
 * 2. 打开消息/通知页面 (NotificationGui)
 * 3. 创建团队指引
 */
public class TeamNotJoinedGui {

    public static void open(BalancedTeamPlugin plugin, Player player) {
        String title = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.NOT_JOINED_TITLE);
        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, MessageUtil.color(title));
        holder.setInventory(inv);

        // 背景填充
        PagedGuiHelper.fillAll(inv, ItemBuilder.grayGlass());

        // 1. 打开服务器队伍列表 (槽位 11)
        String listName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.NOT_JOINED_LIST_BUTTON_NAME);
        List<String> listLore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.NOT_JOINED_LIST_BUTTON_LORE, Collections.emptyMap());
        ItemStack listItem = new ItemBuilder(Material.COMPASS)
                .name(listName)
                .lore(listLore)
                .build();
        inv.setItem(11, listItem);
        holder.setClickHandler(11, e -> {
            SoundUtil.playClick(player);
            TeamListGui.open(plugin, player, 1);
        });

        // 2. 创建队伍指引 (槽位 13)
        String createName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.NOT_JOINED_CREATE_INFO_NAME);
        List<String> createLore = plugin.getConfigManager().getMessageList(player, GuiConfigKeys.NOT_JOINED_CREATE_INFO_LORE, Collections.emptyMap());
        ItemStack createItem = new ItemBuilder(Material.NETHER_STAR)
                .name(createName)
                .lore(createLore)
                .build();
        inv.setItem(13, createItem);
        holder.setClickHandler(13, e -> SoundUtil.playDing(player));

        // 3. 打开消息/通知中心 (槽位 15)
        int inviteCount = plugin.getInviteManager().getValidInviteCount(player.getUniqueId());
        Map<String, String> countMap = new HashMap<>();
        countMap.put("COUNT", String.valueOf(inviteCount));

        Material notifMaterial = inviteCount > 0 ? Material.BELL : Material.PAPER;
        String notifName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.NOT_JOINED_NOTIFICATION_BUTTON_NAME, countMap);
        List<String> notifLore = plugin.getConfigManager().getMessageList(
                player,
                inviteCount > 0 ? GuiConfigKeys.NOT_JOINED_NOTIFICATION_BUTTON_LORE_PENDING : GuiConfigKeys.NOT_JOINED_NOTIFICATION_BUTTON_LORE,
                countMap
        );

        ItemStack notifItem = new ItemBuilder(notifMaterial)
                .name(notifName)
                .lore(notifLore)
                .build();
        inv.setItem(15, notifItem);
        holder.setClickHandler(15, e -> {
            SoundUtil.playClick(player);
            NotificationGui.open(plugin, player, 1);
        });

        // 4. 关闭按钮 (槽位 22)
        String closeName = plugin.getConfigManager().getRawMessage(player, GuiConfigKeys.NOT_JOINED_CLOSE_BUTTON);
        ItemStack closeItem = new ItemBuilder(Material.BARRIER).name(closeName).build();
        inv.setItem(22, closeItem);
        holder.setClickHandler(22, e -> {
            SoundUtil.playClick(player);
            player.closeInventory();
        });

        player.openInventory(inv);
    }
}

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

import java.util.Collections;
import java.util.List;

/**
 * 解散团队二次确认 GUI
 * 玩家执行 /team disband 或点击菜单中的解散按钮时弹出此界面
 */
public class DisbandConfirmGui {

    /**
     * 打开解散确认界面。
     * 会先验证玩家是否为队长，若不满足条件则直接发送提示并返回。
     *
     * @param plugin 插件实例
     * @param player 执行者（必须是队长）
     */
    public static void open(BalancedTeamPlugin plugin, Player player) {
        Team team = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
        if (!PermissionUtil.checkLeader(player, team, plugin.getConfigManager())) {
            return;
        }

        // 构建 GUI
        String title = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DISBAND_CONFIRM_TITLE);
        GuiHolder holder = new GuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, MessageUtil.color(title));
        holder.setInventory(inv);

        // 背景装饰：红色玻璃面板
        PagedGuiHelper.fillAll(inv, ItemBuilder.redGlass());

        // ── 确认解散按钮（槽 11，一次性点击处理器，防止连击）──
        String confirmName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DISBAND_CONFIRM_NAME);
        List<String> confirmLore = plugin.getConfigManager().getMessageList(
                GuiConfigKeys.DISBAND_CONFIRM_LORE, Collections.emptyMap());

        ItemStack confirmItem = new ItemBuilder(Material.TNT)
                .name(confirmName)
                .lore(confirmLore)
                .build();
        inv.setItem(11, confirmItem);

        holder.setOneTimeClickHandler(11, e -> {
            player.closeInventory();

            // 再次校验（防止 GUI 开着期间角色发生变化）
            Team currentTeam = plugin.getTeamManager().getTeamByPlayer(player.getUniqueId());
            if (!PermissionUtil.checkLeader(player, currentTeam, plugin.getConfigManager())) {
                return;
            }

            // 执行解散
            plugin.getTeamManager().disbandTeam(currentTeam).thenRun(() -> {
                SoundUtil.playWarning(player);
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("team_disband_success"));
            });
        });

        // ── 取消按钮（槽 15）──
        String cancelName = plugin.getConfigManager().getRawMessage(GuiConfigKeys.DISBAND_CANCEL_NAME);
        List<String> cancelLore = plugin.getConfigManager().getMessageList(
                GuiConfigKeys.DISBAND_CANCEL_LORE, Collections.emptyMap());

        ItemStack cancelItem = new ItemBuilder(Material.LIME_CONCRETE)
                .name(cancelName)
                .lore(cancelLore)
                .build();
        inv.setItem(15, cancelItem);

        holder.setClickHandler(15, e -> {
            SoundUtil.playClick(player);
            TeamMenuGui.open(plugin, player);
        });

        player.openInventory(inv);
    }
}

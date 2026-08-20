package com.balancedteam.listener;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.gui.GuiHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * GUI 点击分发与防刷物品/防拖拽监听器
 */
public class GuiListener implements Listener {

    private final BalancedTeamPlugin plugin;

    public GuiListener(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    public BalancedTeamPlugin getPlugin() {
        return plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
            GuiHolder holder = (GuiHolder) event.getInventory().getHolder();
            holder.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }
}

package com.balancedteam.gui;

import com.balancedteam.BalancedTeamPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 自定义 GUI 持有者基类（事件绑定、安全性防护、防抖与内聚刷新）
 */
public class GuiHolder implements InventoryHolder {

    private Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();

    // 防抖：记录上一次点击时间戳（单位：毫秒）
    private long lastClickTime = 0L;
    private static final long DEBOUNCE_MILLIS = 200L;

    // 内聚界面刷新动作
    private Consumer<Player> refreshAction;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setClickHandler(int slot, Consumer<InventoryClickEvent> handler) {
        clickHandlers.put(slot, handler);
    }

    /**
     * 注册一次性点击处理器（触发后立即注销，防止多次点击异步请求）
     */
    public void setOneTimeClickHandler(int slot, Consumer<InventoryClickEvent> handler) {
        clickHandlers.put(slot, event -> {
            removeClickHandler(slot);
            handler.accept(event);
        });
    }

    public void removeClickHandler(int slot) {
        clickHandlers.remove(slot);
    }

    public void clearClickHandlers() {
        clickHandlers.clear();
    }

    public void setRefreshAction(Consumer<Player> refreshAction) {
        this.refreshAction = refreshAction;
    }

    /**
     * 触发内聚界面刷新。
     * 自动判断当前线程环境：若在异步数据库回调线程中，自动调度回主线程执行重绘。
     *
     * @param player 刷新界面的目标玩家
     */
    public void refresh(Player player) {
        if (refreshAction == null || player == null || !player.isOnline()) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            refreshAction.accept(player);
        } else {
            Bukkit.getScheduler().runTask(BalancedTeamPlugin.getInstance(), () -> {
                if (player.isOnline()) {
                    refreshAction.accept(player);
                }
            });
        }
    }

    /**
     * 处理点击事件（包含槽位边界校验、容器归属校验、防抖过滤）
     */
    public void handleClick(InventoryClickEvent event) {
        // 1. 确保点击的是当前 GUI 顶部容器，而非玩家自己的背包区域或外部空白区
        if (event.getClickedInventory() == null || event.getClickedInventory() != inventory) {
            return;
        }

        // 2. 槽位范围有效性检查
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || (inventory != null && rawSlot >= inventory.getSize())) {
            return;
        }

        // 3. 点击防抖检查（防止高频连击或鼠标宏刷操作）
        long now = System.currentTimeMillis();
        if (now - lastClickTime < DEBOUNCE_MILLIS) {
            return;
        }
        lastClickTime = now;

        // 4. 执行对应槽位的点击处理器
        Consumer<InventoryClickEvent> handler = clickHandlers.get(rawSlot);
        if (handler != null) {
            handler.accept(event);
        }
    }
}

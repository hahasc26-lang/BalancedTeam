package com.balancedteam.gui.util;

import com.balancedteam.gui.GuiHolder;
import com.balancedteam.util.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 通用分页计算与控制栏组件构建辅助工具类
 */
public final class PagedGuiHelper {

    private PagedGuiHelper() {}

    /**
     * 计算总页数（至少为 1 页）
     */
    public static int calculateTotalPages(int totalItems, int pageSize) {
        if (totalItems <= 0 || pageSize <= 0) return 1;
        return (int) Math.ceil((double) totalItems / pageSize);
    }

    /**
     * 校验并纠正当前请求的页码范围
     */
    public static int clampPage(int requestedPage, int totalPages) {
        if (requestedPage < 1) return 1;
        if (requestedPage > totalPages) return totalPages;
        return requestedPage;
    }

    /**
     * 填充指定槽位范围的背景物品
     */
    public static void fillRange(Inventory inv, int startSlot, int endSlot, ItemStack item) {
        for (int i = startSlot; i <= endSlot && i < inv.getSize(); i++) {
            inv.setItem(i, item);
        }
    }

    /**
     * 填充整个 Inventory 的背景物品
     */
    public static void fillAll(Inventory inv, ItemStack item) {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, item);
        }
    }

    /**
     * 构建上一页按钮（仅在 currentPage > 1 时显示并绑定点击事件）
     */
    public static void setupPrevButton(GuiHolder holder, Inventory inv, int slot, Player player,
                                       int currentPage, String name, Runnable onPrev) {
        if (currentPage > 1) {
            ItemStack item = new ItemBuilder(Material.ARROW).name(name).build();
            inv.setItem(slot, item);
            holder.setClickHandler(slot, e -> {
                SoundUtil.playPage(player);
                if (onPrev != null) onPrev.run();
            });
        }
    }

    /**
     * 构建下一页按钮（仅在 currentPage < totalPages 时显示并绑定点击事件）
     */
    public static void setupNextButton(GuiHolder holder, Inventory inv, int slot, Player player,
                                       int currentPage, int totalPages, String name, Runnable onNext) {
        if (currentPage < totalPages) {
            ItemStack item = new ItemBuilder(Material.ARROW).name(name).build();
            inv.setItem(slot, item);
            holder.setClickHandler(slot, e -> {
                SoundUtil.playPage(player);
                if (onNext != null) onNext.run();
            });
        }
    }

    /**
     * 构建返回按钮
     */
    public static void setupBackButton(GuiHolder holder, Inventory inv, int slot, Player player,
                                       String name, Runnable onBack) {
        ItemStack item = new ItemBuilder(Material.BARRIER).name(name).build();
        inv.setItem(slot, item);
        holder.setClickHandler(slot, e -> {
            SoundUtil.playClick(player);
            if (onBack != null) onBack.run();
        });
    }

    /**
     * 构建空内容占位符
     */
    public static void setupEmptyPlaceholder(Inventory inv, int slot, Material material, String name, List<String> lore) {
        ItemStack item = ItemBuilder.placeholder(material, name, lore);
        inv.setItem(slot, item);
    }
}

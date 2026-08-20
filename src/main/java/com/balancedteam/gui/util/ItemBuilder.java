package com.balancedteam.gui.util;

import com.balancedteam.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * GUI 物品构建工具
 * 100% 兼容 Bukkit, Spigot, Paper 及所有衍生服务端
 * 包含常用边框玻璃与占位物品的预制静态工厂方法
 */
public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.meta = this.itemStack.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null && name != null) {
            meta.setDisplayName(MessageUtil.color(name));
        }
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(List<String> lore) {
        if (meta != null && lore != null) {
            meta.setLore(MessageUtil.colorList(lore));
        }
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        if (meta != null && line != null) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            } else {
                lore = new ArrayList<>(lore);
            }
            lore.add(MessageUtil.color(line));
            meta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder skullOwner(UUID uuid) {
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            skullMeta.setOwningPlayer(player);
        }
        return this;
    }

    public ItemBuilder hideFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    // =========================================================================
    // 常用静态工厂方法与预制玻璃板
    // =========================================================================

    /**
     * 创建纯色空白填充玻璃板
     */
    public static ItemStack filler(Material glassMaterial) {
        return new ItemBuilder(glassMaterial).name(" ").build();
    }

    /**
     * 灰色填充玻璃板
     */
    public static ItemStack grayGlass() {
        return filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    /**
     * 红色填充玻璃板
     */
    public static ItemStack redGlass() {
        return filler(Material.RED_STAINED_GLASS_PANE);
    }

    /**
     * 黄色填充玻璃板
     */
    public static ItemStack yellowGlass() {
        return filler(Material.YELLOW_STAINED_GLASS_PANE);
    }

    /**
     * 黑色填充玻璃板
     */
    public static ItemStack blackGlass() {
        return filler(Material.BLACK_STAINED_GLASS_PANE);
    }

    /**
     * 快速构建占位提示物品（如无内容时的 Barrier / Paper）
     */
    public static ItemStack placeholder(Material material, String name, List<String> lore) {
        return new ItemBuilder(material).name(name).lore(lore).build();
    }
}

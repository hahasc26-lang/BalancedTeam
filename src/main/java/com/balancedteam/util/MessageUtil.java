package com.balancedteam.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息与颜色处理工具类
 * 100% 兼容 Bukkit, Spigot, Paper 及所有衍生服务端
 * 支持 & 传统颜色代码 与 &#RRGGBB / <#RRGGBB> 16进制 RGB 颜色
 */
public class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern BRACKET_HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    /**
     * 转换包含传统代码 & 和 16 进制颜色的字符串
     */
    public static String color(String message) {
        if (message == null) return "";

        // 替换 &#RRGGBB 格式
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            try {
                matcher.appendReplacement(sb, ChatColor.of("#" + hex).toString());
            } catch (Throwable ignored) {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        message = sb.toString();

        // 替换 <#RRGGBB> 格式
        Matcher bracketMatcher = BRACKET_HEX_PATTERN.matcher(message);
        sb = new StringBuilder();
        while (bracketMatcher.find()) {
            String hex = bracketMatcher.group(1);
            try {
                bracketMatcher.appendReplacement(sb, ChatColor.of("#" + hex).toString());
            } catch (Throwable ignored) {
                bracketMatcher.appendReplacement(sb, "");
            }
        }
        bracketMatcher.appendTail(sb);
        message = sb.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * 将字符串列表进行颜色代码渲染
     */
    public static List<String> colorList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(list.size());
        for (String s : list) {
            result.add(color(s));
        }
        return result;
    }

    /**
     * 向发送者发送带颜色格式的消息
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(color(message));
        }
    }

    /**
     * 向玩家发送原始消息
     */
    public static void sendRawMessage(Player player, String message) {
        if (player != null && player.isOnline() && message != null && !message.isEmpty()) {
            player.sendMessage(color(message));
        }
    }
}

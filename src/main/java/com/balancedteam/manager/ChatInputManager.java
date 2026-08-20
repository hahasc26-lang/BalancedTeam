package com.balancedteam.manager;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.util.MessageUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 聊天栏智能输入捕获管理器
 * 用于在 GUI 需要玩家输入文本（如邀请玩家名、搜索等）时安全、平滑且 100% 兼容地捕获玩家在聊天栏的下一条输入
 */
public class ChatInputManager {

    private final BalancedTeamPlugin plugin;
    private final Map<UUID, InputSession> activeSessions = new ConcurrentHashMap<>();

    public ChatInputManager(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    public static class InputSession {
        private final Consumer<String> onInput;
        private final Runnable onCancel;
        private BukkitTask timeoutTask;

        public InputSession(Consumer<String> onInput, Runnable onCancel) {
            this.onInput = onInput;
            this.onCancel = onCancel;
        }
    }

    /**
     * 唤起玩家输入等待会话
     *
     * @param player        目标玩家
     * @param promptMessage 提示给玩家的信息
     * @param onInput       输入成功回调
     * @param onCancel      取消或超时回调
     */
    public void requestInput(Player player, String promptMessage, Consumer<String> onInput, Runnable onCancel) {
        UUID uuid = player.getUniqueId();
        cancel(player, false);

        InputSession session = new InputSession(onInput, onCancel);

        // 30 秒超时自动取消
        session.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            InputSession removed = activeSessions.remove(uuid);
            if (removed != null) {
                MessageUtil.sendMessage(player, "&7[BalancedTeam] 输入已超时取消。");
                if (removed.onCancel != null) {
                    removed.onCancel.run();
                }
            }
        }, 20L * 30);

        activeSessions.put(uuid, session);

        // 发送提示文本与可点击的快速填入建议
        MessageUtil.sendMessage(player, promptMessage);

        String suggestText = plugin.getConfigManager().getRawMessage("gui.player_select.chat_prompt_suggest");
        if (suggestText != null && !suggestText.isEmpty()) {
            try {
                TextComponent suggestComp = new TextComponent(MessageUtil.color(suggestText));
                suggestComp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/team invite "));
                suggestComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(MessageUtil.color("&7点击自动在聊天栏填入 /team invite "))));
                player.spigot().sendMessage(suggestComp);
            } catch (Throwable fallback) {
                // 兼容纯 Bukkit 环境 (无 spigot chat component 支持时退回普通提示)
                player.sendMessage(MessageUtil.color(suggestText));
            }
        }
    }

    /**
     * 处理玩家聊天事件中的输入
     *
     * @param player  发消息的玩家
     * @param message 消息内容
     * @return true 表示已被输入会话捕获并消耗
     */
    public boolean handleChat(Player player, String message) {
        UUID uuid = player.getUniqueId();
        InputSession session = activeSessions.remove(uuid);
        if (session == null) {
            return false;
        }

        if (session.timeoutTask != null) {
            session.timeoutTask.cancel();
        }

        String input = message.trim();
        if ("cancel".equalsIgnoreCase(input) || "取消".equals(input) || "exit".equalsIgnoreCase(input)) {
            String cancelMsg = plugin.getConfigManager().getRawMessage("gui.player_select.chat_cancel");
            MessageUtil.sendMessage(player, cancelMsg != null ? cancelMsg : "&7已取消输入。");
            if (session.onCancel != null) {
                Bukkit.getScheduler().runTask(plugin, session.onCancel);
            }
            return true;
        }

        if (session.onInput != null) {
            Bukkit.getScheduler().runTask(plugin, () -> session.onInput.accept(input));
        }

        return true;
    }

    /**
     * 取消玩家的输入会话
     */
    public void cancel(Player player, boolean notify) {
        UUID uuid = player.getUniqueId();
        InputSession session = activeSessions.remove(uuid);
        if (session != null) {
            if (session.timeoutTask != null) {
                session.timeoutTask.cancel();
            }
            if (notify) {
                MessageUtil.sendMessage(player, "&7已取消输入。");
            }
            if (session.onCancel != null) {
                Bukkit.getScheduler().runTask(plugin, session.onCancel);
            }
        }
    }

    public void removePlayer(UUID uuid) {
        InputSession session = activeSessions.remove(uuid);
        if (session != null && session.timeoutTask != null) {
            session.timeoutTask.cancel();
        }
    }
}

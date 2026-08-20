package com.balancedteam.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * 玩家交互与 GUI 操作即时音效反馈工具类
 */
public final class SoundUtil {

    private SoundUtil() {
    }

    /**
     * 播放常规按钮点击音效
     */
    public static void playClick(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
    }

    /**
     * 播放翻页音效
     */
    public static void playPage(Player player) {
        play(player, Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.2f);
    }

    /**
     * 播放操作成功音效（如成功结盟、成功加队、切换成功等）
     */
    public static void playSuccess(Player player) {
        play(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
    }

    /**
     * 播放轻量级确认/选中音效
     */
    public static void playDing(Player player) {
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.3f);
    }

    /**
     * 播放操作失败 / 权限不足 / 拒绝音效
     */
    public static void playError(Player player) {
        play(player, Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }

    /**
     * 播放危险/警告音效（如解散确认、宣战等）
     */
    public static void playWarning(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
    }

    /**
     * 统一的音效播放方法
     * 负责空值校验和在线状态检查，所有公开方法都通过它来播放音效
     *
     * @param player 目标玩家
     * @param sound  音效类型
     * @param volume 音量
     * @param pitch  音调
     */
    private static void play(Player player, Sound sound, float volume, float pitch) {
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
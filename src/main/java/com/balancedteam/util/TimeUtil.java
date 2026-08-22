package com.balancedteam.util;

import com.balancedteam.BalancedTeamPlugin;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间格式化工具类
 */
public class TimeUtil {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static volatile String currentPattern = DEFAULT_PATTERN;

    private static final String DEFAULT_UNIT_DAY = "d";
    private static final String DEFAULT_UNIT_HOUR = "h";
    private static final String DEFAULT_UNIT_MINUTE = "min";
    private static final String DEFAULT_UNIT_SECOND = "sec";
    private static final String DEFAULT_TEXT_UNKNOWN = "Unknown";

    private static volatile String unitDay = DEFAULT_UNIT_DAY;
    private static volatile String unitHour = DEFAULT_UNIT_HOUR;
    private static volatile String unitMinute = DEFAULT_UNIT_MINUTE;
    private static volatile String unitSecond = DEFAULT_UNIT_SECOND;
    private static volatile String textUnknown = DEFAULT_TEXT_UNKNOWN;

    /**
     * 设置自定义时间格式
     * @param pattern SimpleDateFormat 格式模板
     */
    public static void setDateFormat(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            currentPattern = DEFAULT_PATTERN;
            return;
        }
        try {
            new SimpleDateFormat(pattern);
            currentPattern = pattern;
        } catch (IllegalArgumentException e) {
            currentPattern = DEFAULT_PATTERN;
        }
    }

    /**
     * 设置全局默认的时间单位表达
     */
    public static void setTimeUnits(String day, String hour, String minute, String second, String unknown) {
        unitDay = (day != null && !day.trim().isEmpty()) ? day : DEFAULT_UNIT_DAY;
        unitHour = (hour != null && !hour.trim().isEmpty()) ? hour : DEFAULT_UNIT_HOUR;
        unitMinute = (minute != null && !minute.trim().isEmpty()) ? minute : DEFAULT_UNIT_MINUTE;
        unitSecond = (second != null && !second.trim().isEmpty()) ? second : DEFAULT_UNIT_SECOND;
        textUnknown = (unknown != null && !unknown.trim().isEmpty()) ? unknown : DEFAULT_TEXT_UNKNOWN;
    }

    public static String formatDate(Date date) {
        if (date == null) return textUnknown;
        try {
            return new SimpleDateFormat(currentPattern).format(date);
        } catch (Exception e) {
            return new SimpleDateFormat(DEFAULT_PATTERN).format(date);
        }
    }

    public static String formatDate(CommandSender sender, Date date) {
        if (date == null) {
            BalancedTeamPlugin plugin = BalancedTeamPlugin.getInstance();
            if (plugin != null && plugin.getConfigManager() != null) {
                return plugin.getConfigManager().getRawMessage(sender, "time_unit.unknown");
            }
            return textUnknown;
        }
        return formatDate(date);
    }

    /**
     * 使用全局配置的语言单位格式化持续时间 (默认为 min / sec)
     * @param seconds 总秒数
     * @return 格式化后的字符串
     */
    public static String formatDuration(long seconds) {
        return formatDuration(seconds, unitDay, unitHour, unitMinute, unitSecond);
    }

    /**
     * 适配指定发送者客户端语言的持续时间格式化
     * @param sender 消息接收者/玩家
     * @param seconds 总秒数
     * @return 格式化后的字符串
     */
    public static String formatDuration(CommandSender sender, long seconds) {
        BalancedTeamPlugin plugin = BalancedTeamPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager() != null) {
            String d = plugin.getConfigManager().getRawMessage(sender, "time_unit.day");
            String h = plugin.getConfigManager().getRawMessage(sender, "time_unit.hour");
            String m = plugin.getConfigManager().getRawMessage(sender, "time_unit.minute");
            String s = plugin.getConfigManager().getRawMessage(sender, "time_unit.second");

            String resolvedD = (d != null && !d.contains("Missing message")) ? d : unitDay;
            String resolvedH = (h != null && !h.contains("Missing message")) ? h : unitHour;
            String resolvedM = (m != null && !m.contains("Missing message")) ? m : unitMinute;
            String resolvedS = (s != null && !s.contains("Missing message")) ? s : unitSecond;

            return formatDuration(seconds, resolvedD, resolvedH, resolvedM, resolvedS);
        }
        return formatDuration(seconds);
    }

    /**
     * 指定各个时间单位的持续时间格式化
     * @param seconds 秒数
     * @param d 天单位
     * @param h 时单位
     * @param m 分单位
     * @param s 秒单位
     * @return 格式化后的字符串
     */
    public static String formatDuration(long seconds, String d, String h, String m, String s) {
        if (seconds < 0) seconds = 0;
        if (seconds < 60) {
            return seconds + s;
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        if (days > 0) {
            return days + d + hours + h + minutes + m + remainingSeconds + s;
        }
        if (hours > 0) {
            return hours + h + minutes + m + remainingSeconds + s;
        }
        return minutes + m + remainingSeconds + s;
    }
}

package com.balancedteam.util;

import com.balancedteam.BalancedTeamPlugin;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 时间格式化工具类
 */
public class TimeUtil {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static volatile String currentPattern = DEFAULT_PATTERN;
    private static volatile TimeZone currentTimeZone = TimeZone.getDefault();

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
     * 设置自定义时间格式与时区
     * @param pattern SimpleDateFormat 格式模板
     * @param timezoneId 时区ID (如 "default", "GMT+8", "Asia/Shanghai", "UTC")
     */
    public static void setDateFormat(String pattern, String timezoneId) {
        setDateFormat(pattern);
        if (timezoneId == null || timezoneId.trim().isEmpty() || "default".equalsIgnoreCase(timezoneId.trim())) {
            currentTimeZone = TimeZone.getDefault();
        } else {
            try {
                currentTimeZone = TimeZone.getTimeZone(timezoneId.trim());
            } catch (Exception e) {
                currentTimeZone = TimeZone.getDefault();
            }
        }
    }

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
     * 获取当前生效的时区
     */
    public static TimeZone getCurrentTimeZone() {
        return currentTimeZone;
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
            SimpleDateFormat sdf = new SimpleDateFormat(currentPattern);
            if (currentTimeZone != null) {
                sdf.setTimeZone(currentTimeZone);
            }
            return sdf.format(date);
        } catch (Exception e) {
            SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_PATTERN);
            if (currentTimeZone != null) {
                sdf.setTimeZone(currentTimeZone);
            }
            return sdf.format(date);
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
     * 优化非零单位组合，避免出现 "1小时0分0秒"、"30分0秒" 等臃肿显示
     * @param seconds 秒数
     * @param d 天单位
     * @param h 时单位
     * @param m 分单位
     * @param s 秒单位
     * @return 格式化后的字符串
     */
    public static String formatDuration(long seconds, String d, String h, String m, String s) {
        if (seconds <= 0) return "0" + s;
        if (seconds < 60) {
            return seconds + s;
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        // 判断是否为英文等拉丁字符单位 (如 d, h, min, sec)，若为拉丁字符则在单位间增加空格更易阅读
        boolean needSpace = d != null && !d.isEmpty() && d.charAt(0) < 128;
        String sep = needSpace ? " " : "";

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(d);
            if (hours > 0) sb.append(sep).append(hours).append(h);
            if (minutes > 0) sb.append(sep).append(minutes).append(m);
            if (remainingSeconds > 0) sb.append(sep).append(remainingSeconds).append(s);
            return sb.toString().trim();
        }
        if (hours > 0) {
            sb.append(hours).append(h);
            if (minutes > 0) sb.append(sep).append(minutes).append(m);
            if (remainingSeconds > 0) sb.append(sep).append(remainingSeconds).append(s);
            return sb.toString().trim();
        }
        sb.append(minutes).append(m);
        if (remainingSeconds > 0) {
            sb.append(sep).append(remainingSeconds).append(s);
        }
        return sb.toString().trim();
    }
}

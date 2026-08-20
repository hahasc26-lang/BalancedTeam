package com.balancedteam.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间格式化工具类
 */
public class TimeUtil {
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static volatile String currentPattern = DEFAULT_PATTERN;

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

    public static String formatDate(Date date) {
        if (date == null) return "未知";
        try {
            return new SimpleDateFormat(currentPattern).format(date);
        } catch (Exception e) {
            return new SimpleDateFormat(DEFAULT_PATTERN).format(date);
        }
    }

    public static String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        }
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "分" + remainingSeconds + "秒";
    }
}

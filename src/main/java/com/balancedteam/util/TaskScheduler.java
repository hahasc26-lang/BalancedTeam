package com.balancedteam.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * 跨平台任务调度适配器 (完美向下兼容 Spigot/Paper 并原生支持 Folia 多线程区域调度)
 */
public final class TaskScheduler {

    private static Boolean isFolia;

    private TaskScheduler() {}

    /**
     * 自动检测当前服务端环境是否为 Folia 多线程架构
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    /**
     * 运行异步任务
     */
    public static void runAsync(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    /**
     * 在指定实体 (如 Player、EnderCrystal) 所在的线程区域内同步运行任务
     */
    public static void runEntityTask(Plugin plugin, Entity entity, Runnable runnable) {
        if (entity == null) {
            runGlobalTask(plugin, runnable);
            return;
        }
        if (isFolia()) {
            entity.getScheduler().run(plugin, task -> runnable.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * 延迟指定刻数 (Ticks) 在实体所在线程区域内运行任务
     */
    public static TaskWrapper runEntityTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        if (entity == null) {
            return runGlobalTaskLater(plugin, runnable, delayTicks);
        }
        if (isFolia()) {
            var scheduledTask = entity.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delayTicks);
            return scheduledTask::cancel;
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return bukkitTask::cancel;
        }
    }

    /**
     * 运行全局主区域同步任务 (无特定实体关联，如全局 GUI 或广播)
     */
    public static void runGlobalTask(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * 延迟指定刻数在全局主区域运行任务
     */
    public static TaskWrapper runGlobalTaskLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (isFolia()) {
            var scheduledTask = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), delayTicks);
            return scheduledTask::cancel;
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return bukkitTask::cancel;
        }
    }

    /**
     * 以固定时间间隔异步循环运行任务 (用于后台缓存清理或日志落盘)
     */
    public static TaskWrapper runTimerAsync(Plugin plugin, Runnable runnable, long initialDelaySeconds, long periodSeconds) {
        if (isFolia()) {
            var scheduledTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin, task -> runnable.run(), initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
            return scheduledTask::cancel;
        } else {
            long delayTicks = initialDelaySeconds * 20L;
            long periodTicks = periodSeconds * 20L;
            var bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
            return bukkitTask::cancel;
        }
    }

    /**
     * 任务取消包装器函数接口
     */
    @FunctionalInterface
    public interface TaskWrapper {
        void cancel();
    }
}

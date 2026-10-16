package com.balancedteam.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 跨平台任务调度适配器 (完美向下兼容 Spigot/Paper 并原生支持 Folia 多线程区域调度)
 * 注意：Folia 专属 API 通过反射调用，确保本文件在 Spigot API 环境下也能正常编译。
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
            try {
                // Folia: Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run())
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                asyncScheduler.getClass()
                        .getMethod("runNow", Plugin.class, java.util.function.Consumer.class)
                        .invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run());
            } catch (Exception e) {
                // 反射失败时回退至标准 BukkitScheduler
                Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            }
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
            try {
                // Folia: entity.getScheduler().run(plugin, task -> runnable.run(), null)
                Object entityScheduler = Entity.class.getMethod("getScheduler").invoke(entity);
                entityScheduler.getClass()
                        .getMethod("run", Plugin.class, java.util.function.Consumer.class, Runnable.class)
                        .invoke(entityScheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), null);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
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
            try {
                // Folia: entity.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delayTicks)
                Object entityScheduler = Entity.class.getMethod("getScheduler").invoke(entity);
                Object scheduledTask = entityScheduler.getClass()
                        .getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class)
                        .invoke(entityScheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), null, delayTicks);
                if (scheduledTask != null) {
                    Method cancelMethod = scheduledTask.getClass().getMethod("cancel");
                    return () -> {
                        try { cancelMethod.invoke(scheduledTask); } catch (Exception ignored) {}
                    };
                }
            } catch (Exception e) {
                // 反射失败回退
            }
            var bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return bukkitTask::cancel;
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
            try {
                // Folia: Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run())
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                globalScheduler.getClass()
                        .getMethod("run", Plugin.class, java.util.function.Consumer.class)
                        .invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run());
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * 延迟指定刻数在全局主区域运行任务
     */
    public static TaskWrapper runGlobalTaskLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (isFolia()) {
            try {
                // Folia: Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), delayTicks)
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Object scheduledTask = globalScheduler.getClass()
                        .getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, long.class)
                        .invoke(globalScheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(), delayTicks);
                if (scheduledTask != null) {
                    Method cancelMethod = scheduledTask.getClass().getMethod("cancel");
                    return () -> {
                        try { cancelMethod.invoke(scheduledTask); } catch (Exception ignored) {}
                    };
                }
            } catch (Exception e) {
                // 反射失败回退
            }
            var bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return bukkitTask::cancel;
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
            try {
                // Folia: Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> runnable.run(), initialDelaySeconds, periodSeconds, TimeUnit.SECONDS)
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Object scheduledTask = asyncScheduler.getClass()
                        .getMethod("runAtFixedRate", Plugin.class, java.util.function.Consumer.class, long.class, long.class, TimeUnit.class)
                        .invoke(asyncScheduler, plugin, (java.util.function.Consumer<Object>) task -> runnable.run(),
                                initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
                if (scheduledTask != null) {
                    Method cancelMethod = scheduledTask.getClass().getMethod("cancel");
                    return () -> {
                        try { cancelMethod.invoke(scheduledTask); } catch (Exception ignored) {}
                    };
                }
            } catch (Exception e) {
                // 反射失败回退
            }
            long delayTicks = initialDelaySeconds * 20L;
            long periodTicks = periodSeconds * 20L;
            var bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
            return bukkitTask::cancel;
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

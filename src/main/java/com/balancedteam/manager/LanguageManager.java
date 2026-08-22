package com.balancedteam.manager;

import com.balancedteam.BalancedTeamPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 集中多语言管理器
 * 支持全量内存缓存、客户端语言自动检测 (Player.getLocale())、模糊匹配与回退机制、
 * 以及玩家自主切换与偏好持久化。
 */
public class LanguageManager {

    public static final String DEFAULT_LANGUAGE = "zh_CN";
    public static final String PREF_AUTO = "auto";
    private static final String[] BUILTIN_LANGUAGES = {"zh_CN", "zh_TW", "en_US"};

    private final BalancedTeamPlugin plugin;

    // 内存语言文件缓存：normalized key (如 "zh_cn", "en_us") -> YamlConfiguration
    private final Map<String, YamlConfiguration> languageConfigs = new ConcurrentHashMap<>();
    // normalized key -> 标准原始大小写代码 (如 "zh_cn" -> "zh_CN")
    private final Map<String, String> canonicalCodes = new ConcurrentHashMap<>();
    // normalized key -> 语言友好展示名称 (如 "zh_cn" -> "简体中文")
    private final Map<String, String> displayNames = new ConcurrentHashMap<>();

    // 玩家语言偏好缓存：UUID -> 规范化语言代码 或 "auto"
    private final Map<UUID, String> playerPreferences = new ConcurrentHashMap<>();
    private File userPrefFile;
    private YamlConfiguration userPrefConfig;

    private String serverDefaultLanguage = DEFAULT_LANGUAGE;

    public LanguageManager(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载/重载所有语言配置与玩家偏好
     */
    public synchronized void load() {
        languageConfigs.clear();
        canonicalCodes.clear();
        displayNames.clear();

        // 1. 释放内置语言文件
        saveDefaultLanguageFiles();

        // 2. 扫描 plugins/BalancedTeam/lang/ 目录下的所有 .yml 文件
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        File[] files = langDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                loadLanguageFile(file);
            }
        }

        // 3. 确定服务端配置的默认语言
        String configuredLang = plugin.getConfig().getString("language", DEFAULT_LANGUAGE);
        this.serverDefaultLanguage = resolveLanguageCode(configuredLang);

        com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.LANG_INIT_SUCCESS, 
                languageConfigs.size(), getCanonicalCode(serverDefaultLanguage));

        // 4. 加载玩家语言偏好持久化数据
        loadUserPreferences();
    }

    /**
     * 释放内置语言文件
     */
    private void saveDefaultLanguageFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        for (String lang : BUILTIN_LANGUAGES) {
            String resourcePath = "lang/" + lang + ".yml";
            File targetFile = new File(plugin.getDataFolder(), resourcePath);
            if (!targetFile.exists()) {
                try {
                    plugin.saveResource(resourcePath, false);
                } catch (Exception e) {
                    com.balancedteam.util.PluginLogger.warning(com.balancedteam.util.PluginLogger.LogKey.LANG_RELEASE_FAIL, resourcePath);
                }
            }
        }
    }

    /**
     * 加载单个语言文件并进行防漏项自动补全
     */
    private void loadLanguageFile(File file) {
        String fileName = file.getName();
        String code = fileName.substring(0, fileName.length() - 4); // 去掉 .yml
        String normalized = normalizeCode(code);

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            // 补全缺失项
            InputStream defStream = plugin.getResource("lang/" + code + ".yml");
            if (defStream == null) {
                defStream = plugin.getResource("lang/" + DEFAULT_LANGUAGE + ".yml");
            }
            if (defStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
                config.setDefaults(defConfig);
                config.options().copyDefaults(true);
                try {
                    config.save(file);
                } catch (Exception e) {
                    com.balancedteam.util.PluginLogger.log(Level.WARNING, com.balancedteam.util.PluginLogger.LogKey.LANG_SAVE_COMPLETION_FAIL, e, fileName);
                }
            }

            // 读取友好展示名称
            String dispName = config.getString("language_name");
            if (dispName == null || dispName.trim().isEmpty()) {
                dispName = code;
            }

            languageConfigs.put(normalized, config);
            canonicalCodes.put(normalized, code);
            displayNames.put(normalized, dispName);
        } catch (Exception e) {
            com.balancedteam.util.PluginLogger.log(Level.SEVERE, com.balancedteam.util.PluginLogger.LogKey.LANG_LOAD_FAIL, e, fileName);
        }
    }

    /**
     * 加载玩家语言偏好持久化数据
     */
    private void loadUserPreferences() {
        playerPreferences.clear();
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.userPrefFile = new File(dataDir, "user_languages.yml");
        if (!userPrefFile.exists()) {
            try {
                userPrefFile.createNewFile();
            } catch (Exception ignored) {}
        }
        this.userPrefConfig = YamlConfiguration.loadConfiguration(userPrefFile);
        for (String key : userPrefConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String val = userPrefConfig.getString(key);
                if (val != null && !val.trim().isEmpty()) {
                    playerPreferences.put(uuid, val.trim());
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    /**
     * 保存玩家语言偏好到持久化文件
     */
    public synchronized void saveUserPreferences() {
        if (userPrefConfig == null || userPrefFile == null) return;
        try {
            for (Map.Entry<UUID, String> entry : playerPreferences.entrySet()) {
                userPrefConfig.set(entry.getKey().toString(), entry.getValue());
            }
            userPrefConfig.save(userPrefFile);
        } catch (Exception e) {
            com.balancedteam.util.PluginLogger.log(Level.WARNING, com.balancedteam.util.PluginLogger.LogKey.LANG_SAVE_PREF_FAIL, e);
        }
    }

    /**
     * 标准化代码字符串 (支持相对路径解析、小写、替换横杠为下划线、去除 .yml)
     */
    public static String normalizeCode(String code) {
        if (code == null) return "";
        String s = code.trim().replace('\\', '/');
        if (s.contains("/")) {
            s = s.substring(s.lastIndexOf('/') + 1);
        }
        s = s.toLowerCase().replace('-', '_');
        if (s.endsWith(".yml")) {
            s = s.substring(0, s.length() - 4);
        }
        return s;
    }

    /**
     * 智能语言解析匹配算法
     * 匹配逻辑：精确匹配 -> 前缀模糊匹配 -> 服务端默认语言 -> 终极兜底语言
     * 
     * @param rawCode 客户端 locale 或语言代码 (如 "zh_CN", "zh_TW", "zh_HK", "en_GB")
     * @return 解析匹配后规范化的已加载语言键 (例如 "zh_cn")
     */
    public String resolveLanguageCode(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return serverDefaultLanguage != null ? serverDefaultLanguage : normalizeCode(DEFAULT_LANGUAGE);
        }

        String normalized = normalizeCode(rawCode);

        // 1. 精确匹配
        if (languageConfigs.containsKey(normalized)) {
            return normalized;
        }

        // 2. 前缀模糊匹配 (如 zh_hk -> 寻找 zh_cn / zh_tw; en_gb -> 寻找 en_us)
        if (normalized.contains("_")) {
            String prefix = normalized.split("_")[0];
            // 优先检查常见前缀匹配
            if (prefix.equals("zh")) {
                if (normalized.contains("tw") || normalized.contains("hk") || normalized.contains("mo")) {
                    if (languageConfigs.containsKey("zh_tw")) return "zh_tw";
                }
                if (languageConfigs.containsKey("zh_cn")) return "zh_cn";
            }
            // 通用前缀匹配
            for (String loadedKey : languageConfigs.keySet()) {
                if (loadedKey.startsWith(prefix + "_") || loadedKey.equals(prefix)) {
                    return loadedKey;
                }
            }
        } else {
            // 如果传入仅为前缀 (如 "zh" 或 "en")
            for (String loadedKey : languageConfigs.keySet()) {
                if (loadedKey.startsWith(normalized + "_") || loadedKey.equals(normalized)) {
                    return loadedKey;
                }
            }
        }

        // 3. 回退到服务端默认语言
        if (serverDefaultLanguage != null && languageConfigs.containsKey(serverDefaultLanguage)) {
            return serverDefaultLanguage;
        }

        // 4. 终极兜底 (返回任意已加载语言或 zh_cn)
        if (languageConfigs.containsKey("zh_cn")) {
            return "zh_cn";
        }
        if (languageConfigs.containsKey("en_us")) {
            return "en_us";
        }
        if (!languageConfigs.isEmpty()) {
            return languageConfigs.keySet().iterator().next();
        }

        return normalizeCode(DEFAULT_LANGUAGE);
    }

    /**
     * 获取指定 CommandSender / 玩家当前实际生效的语言代码 (normalized key)
     */
    public String getEffectiveLanguageCode(CommandSender sender) {
        if (sender == null || !(sender instanceof Player)) {
            return serverDefaultLanguage;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // 检查玩家手动偏好
        String pref = playerPreferences.get(uuid);
        if (pref != null && !pref.equalsIgnoreCase(PREF_AUTO)) {
            return resolveLanguageCode(pref);
        }

        // 自动检测模式：读取客户端 locale
        String clientLocale = null;
        try {
            clientLocale = player.getLocale();
        } catch (Throwable ignored) {}

        return resolveLanguageCode(clientLocale);
    }

    /**
     * 获取玩家当前是否处于自动检测模式
     */
    public boolean isAutoMode(UUID uuid) {
        String pref = playerPreferences.get(uuid);
        return pref == null || pref.equalsIgnoreCase(PREF_AUTO);
    }

    /**
     * 获取指定 CommandSender 的语言配置对象
     */
    public YamlConfiguration getConfiguration(CommandSender sender) {
        String code = getEffectiveLanguageCode(sender);
        YamlConfiguration config = languageConfigs.get(code);
        if (config == null) {
            config = languageConfigs.get(serverDefaultLanguage);
        }
        if (config == null && !languageConfigs.isEmpty()) {
            config = languageConfigs.values().iterator().next();
        }
        return config;
    }

    /**
     * 获取全局/服务端默认语言配置对象
     */
    public YamlConfiguration getDefaultConfiguration() {
        YamlConfiguration config = languageConfigs.get(serverDefaultLanguage);
        if (config == null && !languageConfigs.isEmpty()) {
            config = languageConfigs.values().iterator().next();
        }
        return config;
    }

    /**
     * 获取玩家手动设置的偏好 (如 "zh_CN", "en_US" 或 "auto")
     */
    public String getPlayerPreference(UUID uuid) {
        return playerPreferences.getOrDefault(uuid, PREF_AUTO);
    }

    /**
     * 设置玩家的语言偏好 (传入 "auto" 或具体语言代码)
     */
    public void setPlayerPreference(UUID uuid, String rawCode) {
        if (rawCode == null || rawCode.equalsIgnoreCase(PREF_AUTO)) {
            playerPreferences.put(uuid, PREF_AUTO);
        } else {
            String resolved = resolveLanguageCode(rawCode);
            playerPreferences.put(uuid, resolved);
        }
        // 异步保存偏好
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveUserPreferences);
    }

    /**
     * 获取规范大小写的语言代码 (如 "zh_CN")
     */
    public String getCanonicalCode(String normalizedCode) {
        return canonicalCodes.getOrDefault(normalizedCode, normalizedCode);
    }

    /**
     * 获取语言展示名称 (如 "简体中文")
     */
    public String getDisplayName(String normalizedCode) {
        return displayNames.getOrDefault(normalizedCode, getCanonicalCode(normalizedCode));
    }

    /**
     * 获取所有已载入的语言信息列表
     * @return List of Map containing "code", "canonical", "name"
     */
    public List<Map<String, String>> getAvailableLanguages() {
        List<Map<String, String>> list = new ArrayList<>();
        for (String normalized : languageConfigs.keySet()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", normalized);
            item.put("canonical", getCanonicalCode(normalized));
            item.put("name", getDisplayName(normalized));
            list.add(item);
        }
        return list;
    }

    /**
     * 获取服务端默认语言规范代码 (如 "zh_CN")
     */
    public String getServerDefaultCanonical() {
        return getCanonicalCode(serverDefaultLanguage);
    }
}

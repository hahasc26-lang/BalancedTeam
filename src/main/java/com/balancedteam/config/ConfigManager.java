package com.balancedteam.config;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.model.TeamRole;
import com.balancedteam.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * 配置文件与集中多语言文本管理器
 * 支持从 lang/ 文件夹动态加载多语言文件，内置支持 zh_CN, zh_TW, en_US
 */
public class ConfigManager {

    public static final String DEFAULT_LANGUAGE = "zh_CN";
    private static final String[] BUILTIN_LANGUAGES = {"zh_CN", "zh_TW", "en_US"};

    private final BalancedTeamPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File langFile;
    private String currentLanguage = DEFAULT_LANGUAGE;

    public ConfigManager(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // 1. 保存并加载 config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // 2. 确保 lang 文件夹存在并释放所有内置语言文件
        saveDefaultLanguageFiles();

        // 3. 读取 config.yml 中的 language 配置
        String langSetting = config.getString("language", DEFAULT_LANGUAGE);
        if (langSetting == null || langSetting.trim().isEmpty()) {
            langSetting = DEFAULT_LANGUAGE;
        }
        // 容错：若用户填写了 .yml 后缀则自动截除
        if (langSetting.toLowerCase().endsWith(".yml")) {
            langSetting = langSetting.substring(0, langSetting.length() - 4);
        }
        this.currentLanguage = langSetting;

        // 4. 加载目标语言文件 (lang/{language}.yml)
        File langDir = new File(plugin.getDataFolder(), "lang");
        this.langFile = new File(langDir, currentLanguage + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("[Lang] 未找到语言文件: lang/" + currentLanguage + ".yml，正在回退至默认语言 " + DEFAULT_LANGUAGE + "...");
            this.langFile = new File(langDir, DEFAULT_LANGUAGE + ".yml");
            this.currentLanguage = DEFAULT_LANGUAGE;
            if (!langFile.exists()) {
                plugin.saveResource("lang/" + DEFAULT_LANGUAGE + ".yml", false);
            }
        }

        this.messages = YamlConfiguration.loadConfiguration(langFile);

        // 5. 合并默认消息配置（防漏项与自动同步新键）
        InputStream defMessageStream = plugin.getResource("lang/" + currentLanguage + ".yml");
        if (defMessageStream == null) {
            // 若为用户自定义语言，尝试以默认语言作为缺省补全流
            defMessageStream = plugin.getResource("lang/" + DEFAULT_LANGUAGE + ".yml");
        }

        if (defMessageStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defMessageStream, StandardCharsets.UTF_8));
            this.messages.setDefaults(defConfig);
            this.messages.options().copyDefaults(true);
            try {
                this.messages.save(langFile);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Lang] 保存补全语言文件失败: " + langFile.getName(), e);
            }
        }

        plugin.getLogger().info("[Lang] 已成功加载语言包: " + currentLanguage + " (" + langFile.getName() + ")");

        // 6. 同步时间格式到 TimeUtil
        com.balancedteam.util.TimeUtil.setDateFormat(getDateFormat());
    }

    /**
     * 释放所有内置支持的多语言文件到 plugins/BalancedTeam/lang/
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
                    plugin.getLogger().warning("[Lang] 释放内置语言文件失败: " + resourcePath);
                }
            }
        }
    }

    /**
     * 获取当前加载的语言代号 (例如 zh_CN, en_US)
     */
    public String getLanguage() {
        return currentLanguage;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    /**
     * 获取带有前缀的消息
     */
    public String getMessage(String key) {
        return getMessage(key, Collections.emptyMap());
    }

    /**
     * 获取带有前缀并替换占位符的消息
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String prefix = messages.getString("prefix");
        if (prefix == null && messages.getDefaults() != null) {
            prefix = messages.getDefaults().getString("prefix", "&8[&bBalancedTeam&8] &r");
        }
        if (prefix == null) {
            prefix = "&8[&bBalancedTeam&8] &r";
        }

        String msg = messages.getString(key);
        if (msg == null && messages.getDefaults() != null) {
            msg = messages.getDefaults().getString(key);
        }
        if (msg == null) {
            msg = "&c[Missing message: " + key + "]";
        }

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return MessageUtil.color(prefix + msg);
    }

    /**
     * 获取不带前缀的单行消息
     */
    public String getRawMessage(String key) {
        return getRawMessage(key, Collections.emptyMap());
    }

    /**
     * 获取不带前缀并替换占位符的单行消息
     */
    public String getRawMessage(String key, Map<String, String> placeholders) {
        String msg = messages.getString(key);
        if (msg == null && messages.getDefaults() != null) {
            msg = messages.getDefaults().getString(key);
        }
        if (msg == null) {
            msg = "&c[Missing message: " + key + "]";
        }

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return MessageUtil.color(msg);
    }

    /**
     * 获取字符串列表并逐行替换占位符与颜色代码（常用于多行帮助、GUI Lore）
     */
    public List<String> getMessageList(String key, Map<String, String> placeholders) {
        List<String> list = messages.getStringList(key);
        if ((list == null || list.isEmpty()) && messages.getDefaults() != null) {
            list = messages.getDefaults().getStringList(key);
        }
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String line : list) {
            if (placeholders != null) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    line = line.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
            result.add(MessageUtil.color(line));
        }
        return result;
    }

    /**
     * 获取职位多语言展示名称
     */
    public String getRoleDisplayName(TeamRole role) {
        if (role == null) return getRawMessage("role.unknown");
        switch (role) {
            case LEADER:
                return getRawMessage("role.leader");
            case OFFICER:
                return getRawMessage("role.officer");
            case MEMBER:
                return getRawMessage("role.member");
            default:
                return getRawMessage("role.unknown");
        }
    }

    // 快捷配置项获取
    public int getMaxMembers() {
        return config.getInt("balance.max_members", 10);
    }

    public int getMaxAllies() {
        return config.getInt("balance.max_allies", 3);
    }

    public int getMaxEnemies() {
        return config.getInt("balance.max_enemies", 10);
    }

    public boolean isDefaultFriendlyFire() {
        return config.getBoolean("balance.friendly_fire_default", false);
    }

    public boolean isAllyFriendlyFireAllowed() {
        return config.getBoolean("balance.ally_friendly_fire", false);
    }

    public int getFriendlyFireCooldown() {
        return config.getInt("balance.friendly_fire_cooldown_seconds", 30);
    }

    public int getLeaveTeamCooldown() {
        return config.getInt("balance.leave_team_cooldown_seconds", 60);
    }

    public int getNameMinLength() {
        return config.getInt("balance.name_min_length", 2);
    }

    public int getNameMaxLength() {
        return config.getInt("balance.name_max_length", 16);
    }

    public String getNameRegex() {
        return config.getString("balance.name_regex", "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$");
    }

    public int getInviteTimeout() {
        return config.getInt("balance.invite_timeout_seconds", 3600);
    }

    public int getAllyRequestTimeout() {
        return config.getInt("balance.ally_request_timeout_seconds", 60);
    }

    public String getDateFormat() {
        return config.getString("date_format", "yyyy-MM-dd HH:mm:ss");
    }

    public boolean isAllowFriendlyFireToggle() {
        return config.getBoolean("balance.allow_friendly_fire_toggle", true);
    }

    /**
     * 判断指定队伍的友伤是否实际处于开启状态
     * 如果管理员禁止切换友伤，则队伍成员间伤害强制开启 (返回 true)
     */
    public boolean isFriendlyFireActive(com.balancedteam.model.Team team) {
        if (!isAllowFriendlyFireToggle()) {
            return true;
        }
        return team != null && team.isFriendlyFire();
    }

    public String getChatFormat() {
        return config.getString("chat.format", "&8[&b团队&8] &7[{ROLE}&7] &f{PLAYER}&7: &b{MESSAGE}");
    }

    public String getSpyFormat() {
        return config.getString("chat.spy_format", "&8[&c监听&8] &7[&e{TEAM}&7] &7[{ROLE}&7] &f{PLAYER}&7: &f{MESSAGE}");
    }
}

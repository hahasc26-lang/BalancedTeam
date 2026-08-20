package com.balancedteam.config;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.model.TeamRole;
import com.balancedteam.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 配置文件管理器
 * 代理并集成 LanguageManager 集中多语言系统，支持根据 CommandSender 动态适配客户端语言。
 */
public class ConfigManager {

    private final BalancedTeamPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        // 1. 保存并加载 config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // 2. 加载多语言管理器
        if (plugin.getLanguageManager() != null) {
            plugin.getLanguageManager().load();
        }

        // 3. 同步时间格式到 TimeUtil
        com.balancedteam.util.TimeUtil.setDateFormat(getDateFormat());
    }

    /**
     * 获取服务端当前配置的默认语言代号 (例如 zh_CN, en_US)
     */
    public String getLanguage() {
        if (plugin.getLanguageManager() != null) {
            return plugin.getLanguageManager().getServerDefaultCanonical();
        }
        return "zh_CN";
    }

    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * 获取服务端默认语言的配置对象
     */
    public FileConfiguration getMessages() {
        if (plugin.getLanguageManager() != null) {
            return plugin.getLanguageManager().getDefaultConfiguration();
        }
        return new YamlConfiguration();
    }

    /**
     * 获取指定发送者对应语言的配置对象
     */
    public FileConfiguration getMessages(CommandSender sender) {
        if (plugin.getLanguageManager() != null) {
            return plugin.getLanguageManager().getConfiguration(sender);
        }
        return getMessages();
    }

    // =========================================================================
    // 带前缀消息获取 (支持多语言适配)
    // =========================================================================

    public String getMessage(CommandSender sender, String key) {
        return getMessage(sender, key, Collections.emptyMap());
    }

    public String getMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        FileConfiguration langConfig = getMessages(sender);

        String prefix = langConfig.getString("prefix");
        if (prefix == null && langConfig.getDefaults() != null) {
            prefix = langConfig.getDefaults().getString("prefix", "&8[&bBalancedTeam&8] &r");
        }
        if (prefix == null) {
            prefix = "&8[&bBalancedTeam&8] &r";
        }

        String msg = langConfig.getString(key);
        if (msg == null && langConfig.getDefaults() != null) {
            msg = langConfig.getDefaults().getString(key);
        }
        // 如果当前语言文件无此键，从默认语言尝试读取
        if (msg == null && plugin.getLanguageManager() != null) {
            FileConfiguration defConfig = plugin.getLanguageManager().getDefaultConfiguration();
            if (defConfig != null && defConfig != langConfig) {
                msg = defConfig.getString(key);
            }
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
     * 全局默认语言带有前缀的消息 (兼容旧代码)
     */
    public String getMessage(String key) {
        return getMessage((CommandSender) null, key, Collections.emptyMap());
    }

    /**
     * 全局默认语言带有前缀并替换占位符的消息 (兼容旧代码)
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        return getMessage((CommandSender) null, key, placeholders);
    }

    // =========================================================================
    // 不带前缀单行消息获取 (支持多语言适配)
    // =========================================================================

    public String getRawMessage(CommandSender sender, String key) {
        return getRawMessage(sender, key, Collections.emptyMap());
    }

    public String getRawMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        FileConfiguration langConfig = getMessages(sender);

        String msg = langConfig.getString(key);
        if (msg == null && langConfig.getDefaults() != null) {
            msg = langConfig.getDefaults().getString(key);
        }
        if (msg == null && plugin.getLanguageManager() != null) {
            FileConfiguration defConfig = plugin.getLanguageManager().getDefaultConfiguration();
            if (defConfig != null && defConfig != langConfig) {
                msg = defConfig.getString(key);
            }
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

    public String getRawMessage(String key) {
        return getRawMessage((CommandSender) null, key, Collections.emptyMap());
    }

    public String getRawMessage(String key, Map<String, String> placeholders) {
        return getRawMessage((CommandSender) null, key, placeholders);
    }

    // =========================================================================
    // 多行消息与 GUI Lore 列表获取 (支持多语言适配)
    // =========================================================================

    public List<String> getMessageList(CommandSender sender, String key, Map<String, String> placeholders) {
        FileConfiguration langConfig = getMessages(sender);

        List<String> list = langConfig.getStringList(key);
        if ((list == null || list.isEmpty()) && langConfig.getDefaults() != null) {
            list = langConfig.getDefaults().getStringList(key);
        }
        if ((list == null || list.isEmpty()) && plugin.getLanguageManager() != null) {
            FileConfiguration defConfig = plugin.getLanguageManager().getDefaultConfiguration();
            if (defConfig != null && defConfig != langConfig) {
                list = defConfig.getStringList(key);
            }
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

    public List<String> getMessageList(String key, Map<String, String> placeholders) {
        return getMessageList((CommandSender) null, key, placeholders);
    }

    // =========================================================================
    // 职位名称多语言展示
    // =========================================================================

    public String getRoleDisplayName(CommandSender sender, TeamRole role) {
        if (role == null) return getRawMessage(sender, "role.unknown");
        switch (role) {
            case LEADER:
                return getRawMessage(sender, "role.leader");
            case OFFICER:
                return getRawMessage(sender, "role.officer");
            case MEMBER:
                return getRawMessage(sender, "role.member");
            default:
                return getRawMessage(sender, "role.unknown");
        }
    }

    public String getRoleDisplayName(TeamRole role) {
        return getRoleDisplayName((CommandSender) null, role);
    }

    // =========================================================================
    // 快捷配置项获取
    // =========================================================================

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
        return config.getInt("balance.ally_request_timeout_seconds", 3600);
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

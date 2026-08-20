package com.balancedteam.command;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.manager.LanguageManager;
import com.balancedteam.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语言设置独立指令处理器 (/teamlang, /tlang, /btlang)
 */
public class TeamLangCommand implements CommandExecutor, TabCompleter {

    private final BalancedTeamPlugin plugin;

    public TeamLangCommand(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return handleLangCommand(plugin, sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return getTabCompletions(plugin, sender, args);
    }

    /**
     * 统一处理语言指令逻辑 (供 /teamlang 与 /team lang 共享)
     */
    public static boolean handleLangCommand(BalancedTeamPlugin plugin, CommandSender sender, String[] args) {
        LanguageManager langMgr = plugin.getLanguageManager();
        if (langMgr == null) {
            MessageUtil.sendMessage(sender, "&c语言管理器尚未初始化！");
            return true;
        }

        if (args.length == 0) {
            sendLangStatus(plugin, sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                sendLangHelp(plugin, sender);
                break;

            case "list":
                sendLangList(plugin, sender);
                break;

            case "auto":
                if (!(sender instanceof Player)) {
                    MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "player_only"));
                    return true;
                }
                Player pAuto = (Player) sender;
                langMgr.setPlayerPreference(pAuto.getUniqueId(), LanguageManager.PREF_AUTO);
                String activeCode = langMgr.getEffectiveLanguageCode(pAuto);
                String clientLocale = "unknown";
                try {
                    clientLocale = pAuto.getLocale();
                } catch (Throwable ignored) {}
                Map<String, String> autoMap = new HashMap<>();
                autoMap.put("LOCALE", clientLocale);
                autoMap.put("CODE", langMgr.getCanonicalCode(activeCode));
                autoMap.put("NAME", langMgr.getDisplayName(activeCode));
                MessageUtil.sendMessage(pAuto, plugin.getConfigManager().getMessage(pAuto, "lang_set_auto_success", autoMap));
                break;

            case "reload":
                if (!sender.hasPermission("balancedteam.admin")) {
                    MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "no_permission"));
                    return true;
                }
                langMgr.load();
                Map<String, String> reloadMap = new HashMap<>();
                reloadMap.put("COUNT", String.valueOf(langMgr.getAvailableLanguages().size()));
                MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_reload_success", reloadMap));
                break;

            case "set":
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, "&c使用方法: /teamlang set <语言代码|auto>");
                    return true;
                }
                setLanguage(plugin, sender, args[1]);
                break;

            default:
                // 直接输入语言代码 (如 /teamlang zh_CN 或 /teamlang en_US)
                setLanguage(plugin, sender, args[0]);
                break;
        }

        return true;
    }

    private static void setLanguage(BalancedTeamPlugin plugin, CommandSender sender, String rawCode) {
        LanguageManager langMgr = plugin.getLanguageManager();
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "player_only"));
            return;
        }

        Player player = (Player) sender;

        if (rawCode.equalsIgnoreCase("auto")) {
            langMgr.setPlayerPreference(player.getUniqueId(), LanguageManager.PREF_AUTO);
            String activeCode = langMgr.getEffectiveLanguageCode(player);
            String clientLocale = "unknown";
            try {
                clientLocale = player.getLocale();
            } catch (Throwable ignored) {}
            Map<String, String> autoMap = new HashMap<>();
            autoMap.put("LOCALE", clientLocale);
            autoMap.put("CODE", langMgr.getCanonicalCode(activeCode));
            autoMap.put("NAME", langMgr.getDisplayName(activeCode));
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "lang_set_auto_success", autoMap));
            return;
        }

        String resolved = langMgr.resolveLanguageCode(rawCode);
        boolean exists = langMgr.getAvailableLanguages().stream().anyMatch(m -> m.get("code").equalsIgnoreCase(resolved));
        if (!exists) {
            Map<String, String> map = new HashMap<>();
            map.put("CODE", rawCode);
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "lang_not_found", map));
            return;
        }

        langMgr.setPlayerPreference(player.getUniqueId(), resolved);

        Map<String, String> map = new HashMap<>();
        map.put("CODE", langMgr.getCanonicalCode(resolved));
        map.put("NAME", langMgr.getDisplayName(resolved));
        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage(player, "lang_set_success", map));
    }

    private static void sendLangStatus(BalancedTeamPlugin plugin, CommandSender sender) {
        LanguageManager langMgr = plugin.getLanguageManager();
        String activeCode = langMgr.getEffectiveLanguageCode(sender);
        String clientLocale = "Console";
        String mode = plugin.getConfigManager().getRawMessage(sender, "lang_mode_auto");

        if (sender instanceof Player) {
            Player p = (Player) sender;
            try {
                clientLocale = p.getLocale();
            } catch (Throwable ignored) {}
            if (!langMgr.isAutoMode(p.getUniqueId())) {
                mode = plugin.getConfigManager().getRawMessage(sender, "lang_mode_manual");
            }
        }

        Map<String, String> map = new HashMap<>();
        map.put("MODE", mode);
        map.put("LANG", langMgr.getCanonicalCode(activeCode));
        map.put("LANG_NAME", langMgr.getDisplayName(activeCode));
        map.put("LOCALE", clientLocale);

        MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_current_status", map));
        sendLangHelp(plugin, sender);
    }

    private static void sendLangList(BalancedTeamPlugin plugin, CommandSender sender) {
        LanguageManager langMgr = plugin.getLanguageManager();
        String activeCode = langMgr.getEffectiveLanguageCode(sender);

        MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_list_header"));
        String activeTag = plugin.getConfigManager().getRawMessage(sender, "lang_list_active_tag");

        for (Map<String, String> item : langMgr.getAvailableLanguages()) {
            String code = item.get("code");
            String canonical = item.get("canonical");
            String name = item.get("name");

            boolean isActive = code.equalsIgnoreCase(activeCode);
            Map<String, String> itemMap = new HashMap<>();
            itemMap.put("CODE", canonical);
            itemMap.put("NAME", name);
            itemMap.put("ACTIVE", isActive ? activeTag : "");

            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_list_item", itemMap));
        }
    }

    private static void sendLangHelp(BalancedTeamPlugin plugin, CommandSender sender) {
        List<String> list = plugin.getConfigManager().getMessageList(sender, "lang_help", Collections.emptyMap());
        for (String line : list) {
            sender.sendMessage(line);
        }
    }

    /**
     * Tab 补全处理
     */
    public static List<String> getTabCompletions(BalancedTeamPlugin plugin, CommandSender sender, String[] args) {
        LanguageManager langMgr = plugin.getLanguageManager();
        if (langMgr == null) return Collections.emptyList();

        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("list", "auto", "help"));
            if (sender.hasPermission("balancedteam.admin")) {
                list.add("reload");
            }
            for (Map<String, String> item : langMgr.getAvailableLanguages()) {
                list.add(item.get("canonical"));
            }
            String prefix = args[0].toLowerCase();
            return list.stream().filter(s -> s.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            List<String> list = new ArrayList<>(Collections.singletonList("auto"));
            for (Map<String, String> item : langMgr.getAvailableLanguages()) {
                list.add(item.get("canonical"));
            }
            String prefix = args[1].toLowerCase();
            return list.stream().filter(s -> s.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}

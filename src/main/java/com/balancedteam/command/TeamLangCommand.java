package com.balancedteam.command;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.manager.ClientLanguageManager;
import com.balancedteam.manager.ServerLanguageManager;
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
        ClientLanguageManager langMgr = plugin.getClientLanguageManager();
        ServerLanguageManager serverLangMgr = plugin.getServerLanguageManager();
        if (langMgr == null && serverLangMgr == null) {
            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_not_initialized"));
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
                    // 控制台设置 auto：根据系统 Locale 自动检测
                    setConsoleLanguageAuto(plugin, sender);
                    return true;
                }
                if (langMgr == null) {
                    MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_not_initialized"));
                    return true;
                }
                Player pAuto = (Player) sender;
                langMgr.setPlayerPreference(pAuto.getUniqueId(), ClientLanguageManager.PREF_AUTO);
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
                if (plugin.getServerLanguageManager() != null) {
                    plugin.getServerLanguageManager().load();
                }
                if (langMgr != null) {
                    langMgr.load();
                }
                int count = serverLangMgr != null ? serverLangMgr.getSupportedLanguages().size()
                        : (langMgr != null ? langMgr.getAvailableLanguages().size() : 0);
                Map<String, String> reloadMap = new HashMap<>();
                reloadMap.put("COUNT", String.valueOf(count));
                MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_reload_success", reloadMap));
                break;

            case "set":
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_usage_set"));
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
        if (!(sender instanceof Player)) {
            // 控制台执行：直接切换控制台/服务端语言
            setConsoleLanguage(plugin, sender, rawCode);
            return;
        }

        ClientLanguageManager langMgr = plugin.getClientLanguageManager();
        if (langMgr == null) {
            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_not_initialized"));
            return;
        }

        Player player = (Player) sender;

        if (rawCode.equalsIgnoreCase("auto")) {
            langMgr.setPlayerPreference(player.getUniqueId(), ClientLanguageManager.PREF_AUTO);
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

    /**
     * 控制台切换语言：直接切换控制台/服务端语言并保存至 config.yml
     */
    private static void setConsoleLanguage(BalancedTeamPlugin plugin, CommandSender sender, String rawCode) {
        if (rawCode.equalsIgnoreCase("auto")) {
            setConsoleLanguageAuto(plugin, sender);
            return;
        }

        ServerLanguageManager serverLangMgr = plugin.getServerLanguageManager();
        if (serverLangMgr == null) {
            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_not_initialized"));
            return;
        }

        if (!serverLangMgr.isSupported(rawCode)) {
            Map<String, String> map = new HashMap<>();
            map.put("CODE", rawCode);
            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_not_found", map));
            return;
        }

        // 更新服务端/控制台语言并持久化至 config.yml
        serverLangMgr.setServerLanguage(rawCode, true);
        plugin.getConfigManager().syncTimeUnits();

        String activeCanonical = serverLangMgr.getServerLanguageCanonical();
        String displayName = serverLangMgr.getDisplayName(activeCanonical);

        Map<String, String> map = new HashMap<>();
        map.put("CODE", activeCanonical);
        map.put("NAME", displayName);

        String successMsg = plugin.getConfigManager().getMessage(sender, "lang_set_console_success", map);
        if (successMsg == null || successMsg.contains("Missing message")) {
            successMsg = plugin.getConfigManager().getMessage(sender, "lang_set_success", map);
        }
        MessageUtil.sendMessage(sender, successMsg);
    }

    /**
     * 控制台切换为根据服务端 JVM / 操作系统环境 Locale 自动检测
     */
    private static void setConsoleLanguageAuto(BalancedTeamPlugin plugin, CommandSender sender) {
        ServerLanguageManager serverLangMgr = plugin.getServerLanguageManager();
        if (serverLangMgr == null) {
            MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_not_initialized"));
            return;
        }

        String sysLocale = Locale.getDefault().toString();
        String resolved = serverLangMgr.resolveServerLanguage(sysLocale);
        serverLangMgr.setServerLanguage(resolved, true);
        plugin.getConfigManager().syncTimeUnits();

        String activeCanonical = serverLangMgr.getServerLanguageCanonical();
        String displayName = serverLangMgr.getDisplayName(activeCanonical);

        Map<String, String> autoMap = new HashMap<>();
        autoMap.put("LOCALE", sysLocale);
        autoMap.put("CODE", activeCanonical);
        autoMap.put("NAME", displayName);

        String successMsg = plugin.getConfigManager().getMessage(sender, "lang_set_console_auto_success", autoMap);
        if (successMsg == null || successMsg.contains("Missing message")) {
            successMsg = plugin.getConfigManager().getMessage(sender, "lang_set_auto_success", autoMap);
        }
        MessageUtil.sendMessage(sender, successMsg);
    }

    private static void sendLangStatus(BalancedTeamPlugin plugin, CommandSender sender) {
        ClientLanguageManager langMgr = plugin.getClientLanguageManager();
        ServerLanguageManager serverLangMgr = plugin.getServerLanguageManager();
        String activeCode = langMgr != null ? langMgr.getEffectiveLanguageCode(sender) : "en_us";
        String clientLocale;
        String mode;

        if (sender instanceof Player) {
            Player p = (Player) sender;
            clientLocale = "unknown";
            try {
                clientLocale = p.getLocale();
            } catch (Throwable ignored) {}
            if (langMgr != null && !langMgr.isAutoMode(p.getUniqueId())) {
                mode = plugin.getConfigManager().getRawMessage(sender, "lang_mode_manual");
            } else {
                mode = plugin.getConfigManager().getRawMessage(sender, "lang_mode_auto");
            }
        } else {
            mode = plugin.getConfigManager().getRawMessage(sender, "lang_mode_console");
            if (mode == null || mode.contains("Missing message")) {
                mode = plugin.getConfigManager().getRawMessage(sender, "lang_mode_manual");
            }
            clientLocale = "Server (" + Locale.getDefault().toString() + ")";
        }

        String canonicalCode = serverLangMgr != null ? serverLangMgr.getCanonicalCode(activeCode)
                : (langMgr != null ? langMgr.getCanonicalCode(activeCode) : activeCode);
        String dispName = serverLangMgr != null ? serverLangMgr.getDisplayName(activeCode)
                : (langMgr != null ? langMgr.getDisplayName(activeCode) : canonicalCode);

        Map<String, String> map = new HashMap<>();
        map.put("MODE", mode);
        map.put("LANG", canonicalCode);
        map.put("LANG_NAME", dispName);
        map.put("LOCALE", clientLocale);

        MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_current_status", map));
        sendLangHelp(plugin, sender);
    }

    private static void sendLangList(BalancedTeamPlugin plugin, CommandSender sender) {
        ClientLanguageManager langMgr = plugin.getClientLanguageManager();
        ServerLanguageManager serverLangMgr = plugin.getServerLanguageManager();
        String activeCode = langMgr != null ? langMgr.getEffectiveLanguageCode(sender) : "en_us";

        MessageUtil.sendMessage(sender, plugin.getConfigManager().getMessage(sender, "lang_list_header"));
        String activeTag = plugin.getConfigManager().getRawMessage(sender, "lang_list_active_tag");

        List<Map<String, String>> langs = serverLangMgr != null
                ? serverLangMgr.getSupportedLanguages()
                : (langMgr != null ? langMgr.getAvailableLanguages() : Collections.emptyList());

        for (Map<String, String> item : langs) {
            String code = item.get("code");
            String canonical = item.get("canonical");
            String name = item.get("name");

            boolean isActive = code != null && code.equalsIgnoreCase(activeCode);
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
            MessageUtil.sendMessage(sender, line);
        }
    }

    /**
     * Tab 补全处理
     */
    public static List<String> getTabCompletions(BalancedTeamPlugin plugin, CommandSender sender, String[] args) {
        ClientLanguageManager langMgr = plugin.getClientLanguageManager();
        ServerLanguageManager serverLangMgr = plugin.getServerLanguageManager();
        if (langMgr == null && serverLangMgr == null) return Collections.emptyList();

        Set<String> allLangs = new LinkedHashSet<>();
        if (serverLangMgr != null) {
            allLangs.addAll(ServerLanguageManager.BUILTIN_SERVER_LANGUAGES);
        }
        if (langMgr != null) {
            for (Map<String, String> item : langMgr.getAvailableLanguages()) {
                allLangs.add(item.get("canonical"));
            }
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("list", "auto", "help"));
            if (sender.hasPermission("balancedteam.admin")) {
                list.add("reload");
            }
            list.addAll(allLangs);
            String prefix = args[0].toLowerCase();
            return list.stream().filter(s -> s.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            List<String> list = new ArrayList<>(Collections.singletonList("auto"));
            list.addAll(allLangs);
            String prefix = args[1].toLowerCase();
            return list.stream().filter(s -> s.toLowerCase().startsWith(prefix)).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}


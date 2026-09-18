package com.balancedteam.manager;

import com.balancedteam.BalancedTeamPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端语言管理器
 * 专门负责管理服务端层面的语言环境、控制台输出日志语言配置以及系统消息语言。
 * 对应 config.yml 中的 server_messages_language 设置项。
 */
public class ServerLanguageManager {

    public static final String DEFAULT_SERVER_LANGUAGE = "en_US";
    public static final List<String> BUILTIN_SERVER_LANGUAGES = Collections.unmodifiableList(
            Arrays.asList("zh_CN", "zh_TW", "en_US", "ja_JP", "ru_RU", "de_DE", "es_ES")
    );

    private final BalancedTeamPlugin plugin;

    // 规范化代码 (例如 "en_us")
    private volatile String serverLanguage = normalizeCode(DEFAULT_SERVER_LANGUAGE);
    // 标准大小写代码 (例如 "en_US")
    private volatile String serverLanguageCanonical = DEFAULT_SERVER_LANGUAGE;

    // 规范化代码 -> 规范化大小写 (如 "zh_cn" -> "zh_CN")
    private final Map<String, String> canonicalCodes = new ConcurrentHashMap<>();

    public ServerLanguageManager(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
        initBuiltinCodes();
    }

    private void initBuiltinCodes() {
        for (String lang : BUILTIN_SERVER_LANGUAGES) {
            canonicalCodes.put(normalizeCode(lang), lang);
        }
    }

    /**
     * 加载/重载服务端语言设置
     */
    public synchronized void load() {
        initBuiltinCodes();

        // 优先读取 server_messages_language，若未配置则回退到 language
        String configuredLang = null;
        if (plugin.getConfig() != null) {
            configuredLang = plugin.getConfig().getString("server_messages_language");
            if (configuredLang == null || configuredLang.trim().isEmpty()) {
                configuredLang = plugin.getConfig().getString("language");
            }
        }

        if (configuredLang == null || configuredLang.trim().isEmpty()) {
            configuredLang = DEFAULT_SERVER_LANGUAGE;
        }

        String normalized = normalizeCode(configuredLang);
        this.serverLanguage = resolveServerLanguage(normalized);
        this.serverLanguageCanonical = canonicalCodes.getOrDefault(this.serverLanguage, configuredLang.trim());
    }

    /**
     * 解析服务端语言代码 (精确匹配 -> 前缀模糊匹配 -> 兜底默认语言)
     */
    public String resolveServerLanguage(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return normalizeCode(DEFAULT_SERVER_LANGUAGE);
        }

        String norm = normalizeCode(rawCode);

        // 1. 精确匹配
        if (canonicalCodes.containsKey(norm)) {
            return norm;
        }

        // 2. 前缀模糊匹配
        if (norm.startsWith("zh")) {
            if (norm.contains("tw") || norm.contains("hk") || norm.contains("mo")) {
                return "zh_tw";
            }
            return "zh_cn";
        }
        if (norm.startsWith("en")) {
            return "en_us";
        }
        if (norm.startsWith("ja")) {
            return "ja_jp";
        }
        if (norm.startsWith("ru")) {
            return "ru_ru";
        }
        if (norm.startsWith("de")) {
            return "de_de";
        }
        if (norm.startsWith("es")) {
            return "es_es";
        }

        // 3. 回退到服务端默认语言
        return normalizeCode(DEFAULT_SERVER_LANGUAGE);
    }

    /**
     * 标准化语言代码字符串 (支持相对路径、文件后缀、横杠转换与小写规范)
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
     * 获取规范化小写的服务端当前语言代码 (如 "en_us", "zh_cn")
     */
    public String getServerLanguage() {
        return serverLanguage;
    }

    /**
     * 获取规范格式大小写的服务端当前语言代码 (如 "en_US", "zh_CN")
     */
    public String getServerLanguageCanonical() {
        return serverLanguageCanonical;
    }

    /**
     * 注册或更新一个规范大小写的语言代码映射
     */
    public void registerCanonicalCode(String normalized, String canonical) {
        canonicalCodes.put(normalized, canonical);
    }

    /**
     * 检查给定的语言是否为支持的服务端语言
     */
    public boolean isSupported(String code) {
        if (code == null) return false;
        return canonicalCodes.containsKey(normalizeCode(code));
    }
}

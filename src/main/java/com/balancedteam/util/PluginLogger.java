package com.balancedteam.util;

import com.balancedteam.BalancedTeamPlugin;
import com.balancedteam.manager.LanguageManager;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 服务端控制台日志国际化管理器
 * 根据 config.yml 中配置的语言（相对路径/代码），自动切换服务端的控制台输出日志语言。
 * 不在外部配置文件中向用户暴露日志条目修改，内置支持 zh_CN、zh_TW、en_US 等多语言。
 */
public class PluginLogger {

    public enum LogKey {
        // Banner & Startup
        BANNER_LINE(
                "==========================================",
                "==========================================",
                "=========================================="
        ),
        BANNER_ENABLING(
                "   BalancedTeam 团队插件 正在启动...",
                "   BalancedTeam 團隊插件 正在啟動...",
                "   BalancedTeam Plugin is enabling..."
        ),
        PLUGIN_ENABLED(
                "[BalancedTeam] 插件启动成功！",
                "[BalancedTeam] 插件啟動成功！",
                "[BalancedTeam] Plugin enabled successfully!"
        ),
        PLUGIN_DISABLING(
                "[BalancedTeam] 正在安全保存并关闭数据连接池与偏好数据...",
                "[BalancedTeam] 正在安全保存並關閉資料連接池與偏好數據...",
                "[BalancedTeam] Safely saving data and closing connection pools and preferences..."
        ),
        PLUGIN_DISABLED(
                "[BalancedTeam] 插件已安全卸载。",
                "[BalancedTeam] 插件已安全卸載。",
                "[BalancedTeam] Plugin disabled safely."
        ),

        // Database
        DB_POOL_INIT_SUCCESS(
                "[Database] 数据库连接池初始化成功，数据表校验完成！",
                "[Database] 資料庫連接池初始化成功，資料表校驗完成！",
                "[Database] Connection pool initialized successfully, database tables verified!"
        ),
        DB_CONNECT_FAIL(
                "[Database] 数据库连接失败！请检查 config.yml 数据库配置",
                "[Database] 資料庫連接失敗！請檢查 config.yml 資料庫配置",
                "[Database] Database connection failed! Please check database configuration in config.yml"
        ),
        DB_MYSQL_CONNECTED(
                "[Database] 成功连接至 MySQL 数据库！",
                "[Database] 成功連接至 MySQL 資料庫！",
                "[Database] Successfully connected to MySQL database!"
        ),
        DB_MYSQL_FAIL_LINE(
                "===============================================================",
                "===============================================================",
                "==============================================================="
        ),
        DB_MYSQL_FAIL_MSG(
                "[Database] 无法连接到 MySQL 数据库 ({0})",
                "[Database] 無法連接到 MySQL 資料庫 ({0})",
                "[Database] Unable to connect to MySQL database ({0})"
        ),
        DB_MYSQL_FAIL_HINT(
                "[Database] 请检查 config.yml 中的 MySQL 地址、端口、用户名和密码。",
                "[Database] 請檢查 config.yml 中的 MySQL 位址、通訊埠、使用者名稱和密碼。",
                "[Database] Please check MySQL host, port, username, and password in config.yml."
        ),
        DB_MYSQL_FAIL_FALLBACK(
                "[Database] 为确保服务器正常运行，正在自动切换为 SQLite 本地数据库...",
                "[Database] 為確保伺服器正常運行，正在自動切換為 SQLite 本地資料庫...",
                "[Database] Falling back to SQLite local database to ensure server operation..."
        ),
        DB_SQLITE_MODE(
                "[Database] 正在使用 SQLite 本地数据库模式。",
                "[Database] 正在使用 SQLite 本地資料庫模式。",
                "[Database] Using SQLite local database mode."
        ),
        DB_PREFIX_SECURITY_WARN(
                "[Database] 检测到非法数据表前缀配置 ''{0}''，已自动重置为默认前缀 ''bt_'' 以保证安全！",
                "[Database] 檢測到非法資料表前綴配置 ''{0}''，已自動重置為預設前綴 ''bt_'' 以保證安全！",
                "[Database] Invalid table prefix ''{0}'' detected, automatically reset to default prefix ''bt_'' for security!"
        ),
        DB_ROLLBACK_FAIL(
                "[Database] 事务回滚失败",
                "[Database] 事務回滾失敗",
                "[Database] Transaction rollback failed"
        ),

        // Preload Data Cache
        DB_LOADED_RELATIONS(
                "[Database] 已加载 {0} 条外交关系到内存。",
                "[Database] 已載入 {0} 條外交關係到記憶體。",
                "[Database] Loaded {0} diplomatic relation(s) into memory."
        ),
        DB_LOADED_ALLY_REQUESTS(
                "[Database] 已加载 {0} 条有效同盟申请到内存。",
                "[Database] 已載入 {0} 條有效同盟申請到記憶體。",
                "[Database] Loaded {0} valid ally request(s) into memory."
        ),
        DB_LOADED_INVITES(
                "[Database] 已加载 {0} 条有效入队邀请到内存。",
                "[Database] 已載入 {0} 條有效入隊邀請到記憶體。",
                "[Database] Loaded {0} valid team invite(s) into memory."
        ),
        DB_LOADED_APPLICATIONS(
                "[Database] 已加载 {0} 条有效入队申请到内存。",
                "[Database] 已載入 {0} 條有效入隊申請到記憶體。",
                "[Database] Loaded {0} valid team application(s) into memory."
        ),
        DB_PRELOAD_TEAMS_SUCCESS(
                "[Database] 已成功将全服 {0} 个团队数据预热至内存缓存！",
                "[Database] 已成功將全服 {0} 個團隊數據預熱至記憶體快取！",
                "[Database] Successfully preloaded all {0} team record(s) into memory cache!"
        ),

        // PlaceholderAPI
        PAPI_HOOK_SUCCESS(
                "[BalancedTeam] 检测到 PlaceholderAPI 插件，已成功注册占位符扩展！",
                "[BalancedTeam] 檢測到 PlaceholderAPI 插件，已成功註冊佔位符擴展！",
                "[BalancedTeam] PlaceholderAPI detected, expansion registered successfully!"
        ),
        PAPI_HOOK_FAILED(
                "[BalancedTeam] 检测到 PlaceholderAPI 插件，但占位符扩展注册失败！",
                "[BalancedTeam] 檢測到 PlaceholderAPI 插件，但佔位符擴展註冊失敗！",
                "[BalancedTeam] PlaceholderAPI detected, but expansion registration failed!"
        ),
        PAPI_NOT_FOUND(
                "[BalancedTeam] 未检测到 PlaceholderAPI 插件，占位符功能不可用！",
                "[BalancedTeam] 未檢測到 PlaceholderAPI 插件，佔位符功能不可用！",
                "[BalancedTeam] PlaceholderAPI not detected, placeholder features will be unavailable!"
        ),
        PAPI_REGISTER_ERROR(
                "[BalancedTeam] 注册 PlaceholderAPI 扩展失败: {0}",
                "[BalancedTeam] 註冊 PlaceholderAPI 擴展失敗: {0}",
                "[BalancedTeam] Failed to register PlaceholderAPI expansion: {0}"
        ),

        // Language Manager
        LANG_INIT_SUCCESS(
                "[Lang] 语言管理器初始化完成，已载入 {0} 个语言包，服务端默认语言为: {1}",
                "[Lang] 語言管理器初始化完成，已載入 {0} 個語言包，伺服器預設語言為: {1}",
                "[Lang] Language manager initialized, loaded {0} language pack(s), server default language: {1}"
        ),
        LANG_RELEASE_FAIL(
                "[Lang] 释放内置语言文件失败: {0}",
                "[Lang] 釋放內置語言檔案失敗: {0}",
                "[Lang] Failed to extract built-in language file: {0}"
        ),
        LANG_SAVE_COMPLETION_FAIL(
                "[Lang] 保存补全语言文件失败: {0}",
                "[Lang] 保存補全語言檔案失敗: {0}",
                "[Lang] Failed to save updated language file: {0}"
        ),
        LANG_LOAD_FAIL(
                "[Lang] 加载语言文件 {0} 失败！",
                "[Lang] 載入語言檔案 {0} 失敗！",
                "[Lang] Failed to load language file {0}!"
        ),
        LANG_SAVE_PREF_FAIL(
                "[Lang] 保存玩家语言偏好数据失败",
                "[Lang] 保存玩家語言偏好數據失敗",
                "[Lang] Failed to save player language preferences"
        ),

        // Player Listener
        PLAYER_JOIN_LOCALE(
                "[Lang] 玩家 {0} 加入，客户端 Locale: {1} -> 匹配生效语言: {2} ({3})",
                "[Lang] 玩家 {0} 加入，客戶端 Locale: {1} -> 匹配生效語言: {2} ({3})",
                "[Lang] Player {0} joined, client locale: {1} -> matched effective language: {2} ({3})"
        );

        private final String zhCn;
        private final String zhTw;
        private final String enUs;

        LogKey(String zhCn, String zhTw, String enUs) {
            this.zhCn = zhCn;
            this.zhTw = zhTw;
            this.enUs = enUs;
        }

        public String getFormat(String langCode) {
            if (langCode == null) {
                return enUs;
            }
            String norm = LanguageManager.normalizeCode(langCode);
            if (norm.startsWith("zh_tw") || norm.startsWith("zh_hk") || norm.startsWith("zh_mo")) {
                return zhTw;
            }
            if (norm.startsWith("zh")) {
                return zhCn;
            }
            return enUs;
        }
    }

    /**
     * 获取服务端当前配置的默认语言规范代码
     */
    public static String getCurrentServerLanguage() {
        BalancedTeamPlugin plugin = BalancedTeamPlugin.getInstance();
        if (plugin != null) {
            if (plugin.getLanguageManager() != null) {
                String defaultLang = plugin.getLanguageManager().getServerDefaultCanonical();
                if (defaultLang != null && !defaultLang.isEmpty()) {
                    return defaultLang;
                }
            }
            if (plugin.getConfig() != null) {
                String configLang = plugin.getConfig().getString("language");
                if (configLang != null && !configLang.trim().isEmpty()) {
                    return LanguageManager.normalizeCode(configLang);
                }
            }
        }
        return "en_US";
    }

    public static void info(LogKey key, Object... args) {
        log(Level.INFO, key, null, args);
    }

    public static void warning(LogKey key, Object... args) {
        log(Level.WARNING, key, null, args);
    }

    public static void severe(LogKey key, Object... args) {
        log(Level.SEVERE, key, null, args);
    }

    public static void log(Level level, LogKey key, Throwable thrown, Object... args) {
        BalancedTeamPlugin plugin = BalancedTeamPlugin.getInstance();
        Logger logger = plugin != null ? plugin.getLogger() : Logger.getLogger("BalancedTeam");
        String lang = getCurrentServerLanguage();
        String pattern = key.getFormat(lang);
        String message = (args != null && args.length > 0) ? MessageFormat.format(pattern, args) : pattern;
        if (thrown != null) {
            logger.log(level, message, thrown);
        } else {
            logger.log(level, message);
        }
    }

    public static void log(Level level, LogKey key, Object... args) {
        log(level, key, null, args);
    }
}

# Changelog / 更新日志

所有版本的重要变更均记录于此。  
All notable changes to this project are documented here.

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。  
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.2.1] - 2026-09-18

### 重构 / Refactored

- **多语言管理器架构拆分 (Language Architecture Decoupling)**：
  - 将原先揉合在单一管理器中的逻辑彻底拆分为两个职责明确的独立管理器：
    - `ServerLanguageManager`：专门负责管理服务端环境与控制台输出日志语言，读取 `config.yml` 中的 `server_messages_language`，直接为 `PluginLogger` 提供支持；
    - `ClientLanguageManager`：专门负责管理客户端/玩家端的多语言系统，读取 `config.yml` 中的 `language`，负责语言包文件（`lang/*.yml`）的加载与补全、玩家偏好持久化（`data/user_languages.yml`）、客户端 Locale（`Player.getLocale()`）智能模糊匹配以及 `/teamlang` 系列指令；
  - 彻底移除原历史类 `LanguageManager`，在 `BalancedTeamPlugin` 中提供 `getServerLanguageManager()` 与 `getClientLanguageManager()`，所有组件均无缝迁移。

### 新增 / Added

- **服务端日志支持多国语言扩展 (Server Language Expansion)**：
  - 服务端语言支持由原先的 3 种（`zh_CN`、`zh_TW`、`en_US`）扩展至 7 种，新增支持：
    - 日语（`ja_JP`）
    - 俄语（`ru_RU`）
    - 德语（`de_DE`）
    - 西班牙语（`es_ES`）
  - `ServerLanguageManager` 支持 `ja`、`ru`、`de`、`es` 等前缀模糊匹配；
  - `PluginLogger` 中全部 30 项控制台日志均新增了上述 4 种语言的高质量本地化翻译；
  - `config.yml` 中 `server_messages_language` 配置项注释同步更新列出 7 种内置语言。
- **多语言新增键值 (New Language Keys)**：
  - 三套语言文件（`zh_CN.yml`、`zh_TW.yml`、`en_US.yml`）新增国际化条目：
    - `lang_not_initialized`：语言管理器未初始化提示；
    - `lang_usage_set`：`/teamlang set` 指令使用说明；
    - `usage_info`：`/team info` 帮助提示；
    - `team_list_empty`：控制台全服团队列表为空时的提示；
    - `team_list_header`：控制台团队列表分页表头（支持 `{PAGE}`、`{TOTAL}` 占位符）；
    - `team_list_item`：控制台团队条目格式化输出（支持 `{TEAM}`、`{LEADER}`、`{MEMBERS}`、`{MAX}`、`{FF}`、`{ALLIES}`、`{ENEMIES}`）；
    - `team_list_footer`：控制台团队列表翻页提示（支持 `{NEXT_PAGE}` 占位符）。

### 优化与修复 / Improved & Fixed

- **清除命令类中的硬编码中文字符串 (Eliminate Command Hardcoded Strings)**：
  - 彻底清理了 `TeamCommand`（`sendConsoleTeamList`、`handleInfo`）与 `TeamLangCommand` 中残留的硬编码中文提示，全面改由多语言配置文件动态获取；
  - 队伍信息中的队长缺省名与友伤开关状态全面对接语言文件的 `time_unit.unknown` 与 `status.on` / `status.off`。
- **全量命令消息动态多语言适配 (Dynamic Sender-Aware Localization)**：
  - 全面排查并修复 `TeamCommand`、`TeamAdminCommand`、`TeamMsgCommand` 中此前未传 `sender`/`player` 的 `getMessage(...)` 与 `getMessageList(...)` 调用，确保玩家执行任何指令时均能严格根据其自身的生效语言呈现，杜绝部分指令错误回退为全服默认语言的问题。
- **版本号升级 `1.2.0` → `1.2.1`**：
  - `pom.xml` 版本号升级为 `1.2.1`。

---

### Refactored (English)

- **Language Architecture Decoupling**:
  - Decoupled the previously monolithic language system into two single-responsibility managers:
    - `ServerLanguageManager`: Exclusively manages server-side environment and console logging language, loads `server_messages_language` from `config.yml`, and backs `PluginLogger`;
    - `ClientLanguageManager`: Exclusively manages player-facing localization, loads client default `language` from `config.yml`, manages language packs (`lang/*.yml`), player preferences persistence (`data/user_languages.yml`), client locale auto-detection (`Player.getLocale()`), fuzzy dialect matching, and `/teamlang` commands;
  - Completely removed the legacy `LanguageManager` class; `BalancedTeamPlugin` now exposes `getServerLanguageManager()` and `getClientLanguageManager()`.

### Added (English)

- **Server Console Logging Multilingual Expansion**:
  - Expanded built-in server language support from 3 (`zh_CN`, `zh_TW`, `en_US`) to 7 languages, adding:
    - Japanese (`ja_JP`)
    - Russian (`ru_RU`)
    - German (`de_DE`)
    - Spanish (`es_ES`)
  - Added prefix fuzzy matching in `ServerLanguageManager` for `ja`, `ru`, `de`, and `es`;
  - Added high-quality translations for all 30 console log entries in `PluginLogger`;
  - Updated `config.yml` comments under `server_messages_language` to enumerate all 7 built-in language codes.
- **New Language Keys**:
  - Added new message keys across all three language files (`zh_CN.yml`, `zh_TW.yml`, `en_US.yml`):
    - `lang_not_initialized`: Shown when language manager is uninitialized;
    - `lang_usage_set`: Usage instructions for `/teamlang set`;
    - `usage_info`: Usage syntax for `/team info`;
    - `team_list_empty`: Console message when no teams exist on the server;
    - `team_list_header`: Paginated header for console team list with `{PAGE}` and `{TOTAL}`;
    - `team_list_item`: Formatted entry for console team list with placeholders;
    - `team_list_footer`: Pagination footer for console team list with `{NEXT_PAGE}`.

### Improved & Fixed (English)

- **Eliminated Command Hardcoded Strings**:
  - Completely replaced hardcoded Chinese strings in `TeamCommand` (`sendConsoleTeamList`, `handleInfo`) and `TeamLangCommand` with dynamic language pack lookups;
  - Fallback leader names and friendly-fire toggle statuses now resolve via `time_unit.unknown` and `status.on` / `status.off`.
- **Dynamic Sender-Aware Localization**:
  - Fixed `getMessage(...)` and `getMessageList(...)` invocations across `TeamCommand`, `TeamAdminCommand`, and `TeamMsgCommand` that were missing the `sender`/`player` argument, ensuring all command responses strictly follow each player's active language preference rather than falling back to the server default.
- **Version Bump `1.2.0` → `1.2.1`**:
  - `pom.xml` version bumped to `1.2.1`.

---

## [1.2.0] - 2026-09-12

### 新增 / Added

- **队伍聊天全局开关 `enable_chat` (Global Chat Toggle)**：
  - `config.yml` 的 `chat` 节点新增 `enable_chat` 布尔配置项（默认 `true`），管理员将其设为 `false` 后，所有队伍聊天入口（`/team chat`、`/tc`、聊天锁定模式）均立即失效；
  - `ConfigManager` 新增 `isChatEnabled()` 方法统一读取该配置项；
  - `ChatManager.sendTeamChat()` 入口增加全局开关守卫，作为消息分发的最终屏障；
  - `PlayerListener`（聊天锁定模式监听）、`TeamCommand.handleChat()`（`/team chat` 子指令）、`TeamMsgCommand.onCommand()`（`/tc` 系列指令）均同步添加 `isChatEnabled()` 前置检测；玩家处于聊天锁定模式时若全局聊天被关闭，会被自动清出该模式。
- **快捷聊天指令独立开关 `enable_tc_command` (TC Command Toggle)**：
  - `config.yml` 的 `chat` 节点已有 `enable_tc_command` 配置项，现于 `ConfigManager` 新增 `isTcCommandEnabled()` 方法统一读取；
  - `TeamMsgCommand.onCommand()` 在指令入口处先行检测该开关，`enable_tc_command: false` 时禁止玩家使用 `/tc`、`/tm`、`/teammsg` 等快捷聊天指令，并向玩家返回提示消息。
- **终端监听队伍聊天 `console_listen_team_chat` (Console Spy)**：
  - `ConfigManager` 新增 `isConsoleListenTeamChat()` 方法读取 `chat.console_listen_team_chat`（默认 `true`）；
  - `ChatManager.sendTeamChat()` 在完成队内消息分发与管理员监听分发后，若该开关开启，将使用与队内相同的格式字符串在服务器控制台打印完整的聊天行，方便服务器后台留存记录。
- **多语言新消息键 (New Language Keys)**：
  - 三套语言文件（`zh_CN.yml`、`zh_TW.yml`、`en_US.yml`）新增：
    - `chat_disabled`：全局聊天功能已被管理员关闭时的提示；
    - `chat_tc_disabled`：`/tc` 快捷指令已被管理员禁用时的提示。

### 变更 / Changed

- **`ConfigManager` 方法补全与文档完善**：
  - `getChatFormat()` 默认值修复：补回误删的 `{TEAM}` 占位符，使其与 `config.yml` 中的 `chat.format` 默认值完全对齐；
  - `getSpyFormat()` 默认值统一：将硬编码默认值中的 `Spy` 改回 `SPY`，与 `config.yml` 保持一致；
  - 两个方法的 Javadoc 改写为标准 HTML 列表格式，补全 `@return` 描述，枚举全部支持的占位符（`{TEAM}`、`{ROLE}`、`{PLAYER}`、`{MESSAGE}`）。
- **版本号升级 `1.1.7` → `1.2.0`**：
  - `pom.xml` 版本号由 `1.1.7` 升级至 `1.2.0`，反映本次功能性新增的语义化版本变更。

---

### Added (English)

- **Global Chat Toggle `enable_chat`**:
  - New boolean `enable_chat` key added under the `chat` node in `config.yml` (defaults to `true`). When set to `false`, all team chat entry points (`/team chat`, `/tc`, chat lock mode) are immediately disabled;
  - `ConfigManager` now exposes `isChatEnabled()` as a unified reader for this option;
  - `ChatManager.sendTeamChat()` enforces the global toggle as a final barrier before dispatching any message;
  - `PlayerListener` (chat lock mode), `TeamCommand.handleChat()`, and `TeamMsgCommand.onCommand()` all add a `isChatEnabled()` pre-check; players currently in chat lock mode are automatically ejected when the feature is disabled.
- **TC Command Independent Toggle `enable_tc_command`**:
  - `ConfigManager` now exposes `isTcCommandEnabled()` to read `chat.enable_tc_command`;
  - `TeamMsgCommand.onCommand()` checks this flag first; when disabled, `/tc`, `/tm`, and `/teammsg` are all blocked and a feedback message is sent to the player.
- **Console Team Chat Spy `console_listen_team_chat`**:
  - `ConfigManager` now exposes `isConsoleListenTeamChat()` to read `chat.console_listen_team_chat` (defaults to `true`);
  - `ChatManager.sendTeamChat()` prints the fully formatted chat line to the server console after member and admin dispatch, using the same `chat.format` template, enabling server-side logging.
- **New Language Keys**:
  - All three language files (`zh_CN.yml`, `zh_TW.yml`, `en_US.yml`) gain:
    - `chat_disabled`: Shown when a player attempts team chat while the feature is globally disabled;
    - `chat_tc_disabled`: Shown when a player uses `/tc` while the shortcut command is disabled.

### Changed (English)

- **`ConfigManager` Method Fixes & Documentation**:
  - `getChatFormat()` default value restored: re-added the missing `{TEAM}` placeholder to match the `chat.format` default in `config.yml`;
  - `getSpyFormat()` default value corrected: `Spy` → `SPY` in hardcoded fallback, consistent with `config.yml`;
  - Both methods' Javadoc rewritten to standard HTML list format with full `@return` descriptions and complete placeholder documentation.
- **Version Bump `1.1.7` → `1.2.0`**:
  - `pom.xml` version updated to `1.2.0` to reflect the semantic version increment for new feature additions.

---

## [1.1.7] - 2026-09-05


### 安全与修复 / Security & Fixed

- **修复 PlaceholderAPI 聊天占位符注入漏洞 (PAPI Injection Fix)**：
  - 调整 `ChatManager` 中的格式化求值顺序，确保在将玩家聊天内容拼入模板前先行解析模板中的 PAPI 变量与颜色，杜绝玩家在聊天栏输入恶意占位符变量引发的敏感信息探测与脚本执行风险；
  - 规范团队聊天颜色权限校验，仅拥有 `balancedteam.chat.color` 权限的玩家才可发送带色彩与样式的消息，并优化 `MessageUtil.sendRawMessage` 为直接发送已格式化文本，避免重复正则转色。
- **强化团队人数与外交关系并发上限防护 (Concurrency Boundary Protection)**：
  - 在 `TeamManager.addMember` 内部增加前置检查与落库后双重同步锁（`synchronized`）校验，防止多名玩家并发接受邀请/申请时突破配置的 `max_members` 上限；
  - 在 `RelationManager` 内部增加同盟数（`max_allies`）与宿敌数（`max_enemies`）的全局上限以及已有同盟/敌对互斥防御校验，杜绝并发越界。

### Security & Fixed (English)

- **Patched PlaceholderAPI Chat Injection Vulnerability**:
  - Adjusted evaluation order in `ChatManager` to resolve PAPI variables on the message template prior to inserting the user's chat message, completely mitigating placeholder injection risks (such as unauthorized script evaluation or sensitive variable inspection);
  - Added strict permission checks (`balancedteam.chat.color`) for chat color code parsing and optimized `MessageUtil.sendRawMessage` to send pre-formatted messages directly without redundant regex color passes.
- **Enhanced Concurrency Boundary Protection for Team Size & Relations**:
  - Added pre-checks and synchronized double-checked locking inside `TeamManager.addMember` to strictly enforce `max_members` during concurrent member joins;
  - Added global boundary checks for `max_allies` and `max_enemies` as well as mutual exclusion checks inside `RelationManager` to prevent relation limit bypasses during concurrent requests.

---

## [1.1.6] - 2026-08-25

### 变更 / Changed

- **扩展跨版本兼容性 (Minecraft 1.20.x - 26.2)**：
  - 增强了音频播放 (`SoundUtil`) 和伤害判定监听器等底层交互的跨版本安全容错机制，确保在 Minecraft 1.20.x 至 26.2 (包括 Bukkit, Spigot, Paper, Purpur 等衍生服务端) 上稳定运行；
  - 完善了 Java 21 运行环境兼容与文档版本徽标更新。
- **DAO 层持久化错误报告全英文标准化 (DAO English Error Reports)**：
  - 将所有 DAO 数据访问层（`TeamDao`、`MemberDao`、`RelationDao`、`InviteDao`、`ApplicationDao`、`AllyRequestDao`）以及 `DatabaseManager` / `TeamManager` 抛出的 `DatabaseException` 异常与错误信息全面替换为标准规范的英文描述，便于国际化日志收集与服务端自动化错误排查。

### Added / Changed (English)

- **Extended Cross-Version Compatibility (Minecraft 1.20.x - 26.2)**:
  - Enhanced version resilience and safe fallback in `SoundUtil` and damage listeners, ensuring smooth and flawless operation on Minecraft 1.20.x through 26.2 across Bukkit, Spigot, Paper, and Purpur platforms;
  - Updated documentation, guides, and version badges for modern Java 21+ environments.
- **Standardized English Error Messages for DAO Layer**:
  - Replaced all persistence error reports and `DatabaseException` messages across all DAO classes (`TeamDao`, `MemberDao`, `RelationDao`, `InviteDao`, `ApplicationDao`, `AllyRequestDao`) and `DatabaseManager` / `TeamManager` with clear, standardized English error messages for consistent logging and troubleshooting.

---

## [1.1.5] - 2026-08-22

### 新增

- **服务端控制台日志国际化与相对路径解析 (Server Log Localization & Relative Path Support)**：
  - `src/main/resources/config.yml` 中的 `language` 设置全面支持语言代码（如 `en_US`、`zh_CN`、`zh_TW`）、文件名（如 `en_US.yml`）或相对路径（如 `lang/en_US.yml`、`lang/zh_CN.yml`、`lang\zh_TW.yml`）指定服务端默认语言；
  - 新增 `PluginLogger` 控制台日志国际化管理器，当 `config.yml` 中的相对路径/语言发生更改或重载时，服务端控制台输出日志（包括启动横幅、数据库连接与建表校验、数据全量缓存预热、PlaceholderAPI 挂载、重载、玩家加入语言检测、安全卸载等）将自动同步切换为对应的语言输出；
  - 严格采用内置国际化模板，无需在外部配置文件中手动配置各条日志文本。

### Added

- **Server Console Log Localization & Relative Path Support**:
  - `language` in `config.yml` now supports language codes (e.g. `en_US`, `zh_CN`, `zh_TW`), file names (e.g. `en_US.yml`), and relative paths (e.g. `lang/en_US.yml`, `lang/zh_CN.yml`);
  - Added `PluginLogger` server log internationalization manager. Changing the language relative path in `config.yml` automatically switches all server console output logs (startup banner, database initialization & table verification, data preloading, PlaceholderAPI hooking, reload, player join locale detection, safe shutdown, etc.) to the configured language;
  - Server logs are cleanly managed internally without requiring manual config entries for log strings.

---

## [1.1.4] - 2026-08-22

### 新增

- **时间单位与持续时间多语言可配置化 (TimeUtil & Localization)**：
  - 在各语言配置文件（`zh_CN.yml`、`zh_TW.yml`、`en_US.yml`）中新增 `time_unit` 配置节点，全面支持天 (`day`)、时 (`hour`)、分 (`minute`)、秒 (`second`) 及缺省文本 (`unknown`) 的自定义配置；
  - `TimeUtil` 内部默认单位全面更新为 `d` / `h` / `min` / `sec` / `Unknown`；
  - `TimeUtil` 扩展支持 `formatDuration(CommandSender sender, long seconds)` 与 `formatDate(CommandSender sender, Date date)`，支持按发送者/玩家当前生效的语言动态渲染时间单位与未知文本；
  - `ConfigManager.load()` 中增加 `syncTimeUnits()` 方法，实现语言包重载时全局时间单位的自动同步。
- **控制台指令兼容与合理性支持 (Console Command Support)**：
  - 解除主指令 `/team` 入口处对控制台的全局拦截，按子指令适用性进行精准放行与处理；
  - **控制台全服团队列表展示**：控制台执行 `/team list [页码]` 时，自动转换为清晰的文本分页列表格式输出全服队伍名称、队长、人数、友伤状态及同盟/宿敌数；
  - **团队详情查询支持控制台**：控制台可通过 `/team info <团队名>` 查询指定团队的详细信息；
  - **多语言管理指令支持控制台**：控制台可通过 `/team lang` / `/teamlang` 查看当前语言模式与状态、列出所有可用语言包以及执行热重载；
  - **控制台帮助支持**：控制台执行 `/team` 或 `/team help` 时直接输出文本帮助信息；
  - **交互指令安全隔离**：对仅限玩家执行的游戏内队伍操作指令（如创建、邀请、踢人、解散、打开 GUI 界面等）进行明确拦截并友好提示 `player_only`。

### 优化与修复

- **排查并消除硬编码多语言文本**：
  - 修复 `BalancedTeamExpansion` 中 `%balancedteam_friendly_fire_formatted%` 与 `%balancedteam_ff_formatted%` 硬编码中文 `"开启"` / `"关闭"` 的问题，改为从玩家语言配置 `status.on` / `status.off` 动态读取；
  - 优化各 GUI 界面（`TeamListGui`、`TeamDetailGui`、`MemberManageGui`）及指令输出中的日期格式化调用，适配玩家本地语言。

### Added

- **Configurable Time & Duration Units Localization (TimeUtil & Localization)**:
  - Added `time_unit` configuration section across all language files (`zh_CN.yml`, `zh_TW.yml`, `en_US.yml`), supporting custom formatting for `day`, `hour`, `minute`, `second`, and `unknown`;
  - Standardized default internal time units in `TimeUtil` to `d` / `h` / `min` / `sec` / `Unknown`;
  - Enhanced `TimeUtil` with `formatDuration(CommandSender sender, long seconds)` and `formatDate(CommandSender sender, Date date)` overloads to dynamically format units based on player's active locale;
  - Added `syncTimeUnits()` in `ConfigManager.load()` to automatically sync time units upon plugin load and reload.
- **Comprehensive Console Command Support**:
  - Lifted the blanket restriction on console execution for `/team` root command, enabling reasonable sub-commands for server console;
  - **Console Text Team List**: Console executing `/team list [page]` now outputs a paginated text list with team names, leaders, member counts, friendly fire status, and ally/enemy counts;
  - **Team Info Query via Console**: Console can now query detailed information for any team via `/team info <teamName>`;
  - **Language Commands for Console**: Console can now execute `/team lang` / `/teamlang` to view language status, list loaded languages, and reload language files;
  - **Console Help Support**: Executing `/team` or `/team help` from console now outputs the command help manual;
  - **Safe Isolation of Player-only Commands**: Interactive commands (such as create, invite, kick, disband, open GUI, etc.) are strictly checked and return `player_only` message when invoked by console.

### Improved & Fixed

- **Eliminated Hardcoded Localization Strings**:
  - Fixed hardcoded `"开启"` / `"关闭"` in `%balancedteam_friendly_fire_formatted%` / `%balancedteam_ff_formatted%` within `BalancedTeamExpansion`, now dynamically resolving `status.on` / `status.off` based on player locale;
  - Refactored date formatting across all GUIs (`TeamListGui`, `TeamDetailGui`, `MemberManageGui`) and commands to adapt to the sender's language configuration.

---

## [1.1.3] - 2026-08-22

### 新增

- **原生 PlaceholderAPI (PAPI) 变量扩展支持**：
  - 新增 `BalancedTeamExpansion` 原生扩展类，统一注册标识符 `%balancedteam_<变量名>%`；
  - 提供 35+ 个占位符变量，全面支持团队基础属性、成员职位/等级、友伤状态、创建与入队时间、在线与总人数统计、盟友与敌对列表、全服统计以及动态关系判定（如 `%balancedteam_relation_<玩家名>%`、`%balancedteam_is_ally_<玩家名>%` 等）；
  - 全面支持离线玩家安全查询及未入队状态下的友好默认值兜底，避免空指针与异常。
- **双向变量解析工具 (PAPIUtil)**：
  - 增强 `PAPIUtil` 工具类，封装生命周期注册/注销以及字符串和列表的无异常安全解析；
  - 团队聊天 (`ChatManager`) 及管理员监听格式全面支持解析第三方 PAPI 占位符（如 Vault 称号、前缀等）。
- **软依赖与文档完善**：
  - `plugin.yml` 中新增 `softdepend: [PlaceholderAPI]` 确保插件加载顺序；
  - 发布了包含 8 大核心模块的完整官方 GitHub 英文 Wiki 文档。

### Added

- **Native PlaceholderAPI (PAPI) Expansion Support**:
  - Implemented `BalancedTeamExpansion` with `%balancedteam_<variable>%` identifier;
  - Added 35+ placeholders covering team identity, roles, friendly fire status, timestamps, member counts, online statuses, ally/enemy lists, global server stats, and dynamic target relation queries (e.g. `%balancedteam_relation_<player>%`, `%balancedteam_is_ally_<player>%`);
  - Safe default fallbacks for offline players and non-team members to prevent NullPointerExceptions.
- **Bidirectional Placeholder Utilities (PAPIUtil)**:
  - Enhanced `PAPIUtil` with safe expansion lifecycle management and exception-free string/list parsing;
  - Integrated PAPI parsing into team chat (`ChatManager`) and admin spy messages.
- **Soft Dependency & Documentation**:
  - Added `softdepend: [PlaceholderAPI]` to `plugin.yml`;
  - Published comprehensive 8-page English Wiki documentation on GitHub.

---

## [1.1.2] - 2026-08-21

### 新增

- **统一二次确认系统 (ConfirmGui)**：重构并统一了团队所有核心与危险操作的二次确认机制，全面支持 6 大模式：
  - `DISBAND`（解散团队）：仅队长可操作，TNT 确认图标；
  - `LEAVE`（退出团队）：非队长成员可操作，带退队冷却检测；
  - `KICK`（踢出成员）：队长及管理员可操作，带严格职位层级（`canManage`）比对；
  - `TRANSFER`（转让队长）：仅队长可操作，金冠确认图标，转让后原队长降为管理员；
  - `PROMOTE`（提升管理员）：仅队长可操作，金甲确认图标，展示具体管理权限清单；
  - `DEMOTE`（降职为队员）：仅队长可操作，铁甲确认图标，带权限回收提示。
- **全流程防误触与双重前置校验**：所有确认操作在界面唤起前和点击确认后均执行严格权限与状态双重检查，并采用单次点击处理器防止并发连击。

### 变更

- **优化成员管理界面 (MemberManageGui)**：
  - 右键踢出成员、Shift+点击转让队长、左键升职/降职均改为弹出对应的 `ConfirmGui` 二次确认弹窗，彻底杜绝误触。
- **优化管理指令交互 (TeamCommand)**：
  - `/team kick <玩家>`、`/team transfer <玩家>`、`/team promote <玩家>`、`/team demote <玩家>` 指令在校验基础参数与权限后，统一唤起 `ConfirmGui` 确认界面，保持 GUI 与指令交互一致性。
- **动态配置占位符支持**：
  - 团队菜单中邀请提示将硬编码的 60 秒替换为 `{TIMEOUT}` 动态占位符，与 `config.yml` 的 `invite_timeout_seconds` 实时绑定；
  - 团队菜单中友伤切换提示将硬编码的 30 秒替换为 `{COOLDOWN}` 动态占位符，与 `friendly_fire_cooldown_seconds` 实时绑定。
- **配置文件与多语言包同步**：
  - `config.yml`：全文注释标准化为规范英文注释；
  - `zh_CN.yml` / `zh_TW.yml` / `en_US.yml`：同步新增 `kick_confirm`、`transfer_confirm`、`promote_confirm`、`demote_confirm` 等多语言节点。

### Added

- **Unified Confirmation GUI (ConfirmGui)**：Refactored and unified confirmation dialogs for all critical and destructive operations, supporting 6 distinct modes:
  - `DISBAND` (Disband Team): Leader-only, TNT confirmation icon;
  - `LEAVE` (Leave Team): Non-leader members, with leave cooldown validation;
  - `KICK` (Kick Member): Leaders and Officers, with strict role hierarchy (`canManage`) checks;
  - `TRANSFER` (Transfer Leadership): Leader-only, Golden Helmet icon, demoting former leader to Officer;
  - `PROMOTE` (Promote to Officer): Leader-only, Golden Chestplate icon, displaying officer permission details;
  - `DEMOTE` (Demote to Member): Leader-only, Iron Chestplate icon, with permission revocation warning.
- **Double Pre-Validation & Anti-Duplication Protection**: Strict permission and team status checks are performed both before opening the GUI and upon clicking confirm, protected by one-time click handlers against rapid concurrent clicks.

### Changed

- **Member Management GUI (MemberManageGui) Optimized**:
  - Right-click kick, Shift-click leadership transfer, and Left-click promote/demote now all prompt their respective `ConfirmGui` dialogs, preventing accidental clicks.
- **Management Commands (TeamCommand) Integration**:
  - `/team kick <player>`, `/team transfer <player>`, `/team promote <player>`, and `/team demote <player>` commands now seamlessly open the corresponding `ConfirmGui` after parameter validation, harmonizing CLI and GUI user experiences.
- **Dynamic Config Placeholder Integration**:
  - Replaced hardcoded 60s in the invite lore with dynamic `{TIMEOUT}` placeholder bound to `invite_timeout_seconds` in `config.yml`;
  - Replaced hardcoded 30s in friendly fire toggle tooltip with `{COOLDOWN}` placeholder bound to `friendly_fire_cooldown_seconds`.
- **Configuration & Localization Updates**:
  - `config.yml`: Translated all comments into professional and idiomatic English;
  - `zh_CN.yml` / `zh_TW.yml` / `en_US.yml`: Synchronized with `kick_confirm`, `transfer_confirm`, `promote_confirm`, and `demote_confirm` language entries.

---

## [1.1.1] - 2026-08-21

### 变更

- **新增 GUI 返回按钮配置**：在 `GuiConfigKeys` 中加入 `DETAIL_BACK_BUTTON_IN_TEAM` 与 `DETAIL_BACK_BUTTON_NOT_IN_TEAM`，实现团队详情页面根据玩家是否在队伍中显示不同的返回按钮文字。
- **更新语言文件**：在 `zh_TW.yml`、`zh_CN.yml`、`en_US.yml` 中新增 `back_button_in_team` 与 `back_button_not_in_team` 键，分别对应"返回团队控制面板"/"返回团队列表"等文案。
- **修改 TeamDetailGui**：根据玩家所属团队动态读取对应返回按钮键，并在槽位 31 设置返回动作；在团队内返回至团队控制面板，在未加入团队时返回至团队列表。

### Changed

- **New GUI back button config**: Added `DETAIL_BACK_BUTTON_IN_TEAM` and `DETAIL_BACK_BUTTON_NOT_IN_TEAM` to `GuiConfigKeys`, enabling the Team Detail GUI to display different back button labels based on whether the player is in a team.
- **Updated language files**: Added `back_button_in_team` and `back_button_not_in_team` keys to `zh_TW.yml`, `zh_CN.yml`, and `en_US.yml`, mapping to "Back to Team Dashboard" / "Back to Team List" respectively.
- **Modified TeamDetailGui**: Dynamically resolves the correct back button key based on the player's team membership, and sets the back action at slot 31; returns to the Team Dashboard if in a team, or to the Team List if not.

---

## [1.1.0] - 2026-08-20

### 新增

- **客户端语言自动检测机制**：玩家加入服务器时（`PlayerJoinEvent`）通过 `Player.getLocale()` 自动获取客户端语言代码（如 `zh_CN`、`en_US` 等），并自动匹配加载对应的语言包文件。
- **多级智能模糊匹配与回退算法**：支持语言前缀模糊匹配（例如玩家为 `zh_HK` 或 `zh_TW` 客户端，若无完全匹配文件则优先智能匹配 `zh_*`，再回退到 `config.yml` 服务端默认语言及最终兜底语言）。
- **全量多语言内存高速缓存**：启动与重载时全量预加载 `plugins/BalancedTeam/lang/*.yml` 文件到内存缓存中，运行期发送消息与渲染 GUI 零额外磁盘 I/O。
- **独立语言指令与切换机制**：
  - 新增 `/teamlang`（别名 `/tlang` `/btlang` `/clanlang`）及 `/team lang` 指令；
  - 支持 `/teamlang list` 查看服务器所有支持的语言包及当前生效状态；
  - 支持 `/teamlang <代码>`（如 `/teamlang en_US`）手动固定语言，或 `/teamlang auto` 恢复自动检测；
  - 支持 `/teamlang reload` 管理员热重载所有语言文件；
  - 玩家语言偏好持久化保存在 `data/user_languages.yml`，离线重连不丢失。
- **GUI 与消息系统多语言全面适配**：所有图形化 GUI 界面（团队菜单、未加队面板、成员管理、通知中心、全服列表、团队详情、目标选择、确认弹窗）均全面根据操作玩家客户端生效语言动态渲染标题与 Lore。

### Added

- **Client Locale Auto-Detection**: Automatically detects players' client language settings on login (`PlayerJoinEvent`) using `Player.getLocale()` and loads the corresponding language file.
- **Smart Fuzzy Matching & Fallback Algorithm**: Supports prefix-based fuzzy matching (e.g. `zh_HK` or `zh_TW` clients match `zh_*` packs if exact matches do not exist) before falling back to server default language in `config.yml` and hardcoded fallback.
- **In-Memory Language Caching**: Scans and caches all `plugins/BalancedTeam/lang/*.yml` files in memory on startup and reload, eliminating redundant disk I/O during gameplay and GUI rendering.
- **Dedicated Language Commands & Switching**:
  - Added `/teamlang` (aliases `/tlang` `/btlang` `/clanlang`) and `/team lang` commands;
  - Added `/teamlang list` to view all available language packs and current active status;
  - Added `/teamlang <locale>` (e.g. `/teamlang en_US`) for manual language override, and `/teamlang auto` to restore auto-detection;
  - Added `/teamlang reload` for admins to hot-reload all language packs;
  - Player language preferences are persisted in `data/user_languages.yml` across disconnects.
- **Full Multi-Language Localization for GUI & Messages**: All graphical GUI interfaces (Team Menu, Not-Joined Panel, Member Management, Notification Center, Team List, Detail View, Selection, and Confirmation dialogs) dynamically render titles and lores according to each player's effective language.

---

## [1.0.2] - 2026-08-20

### 新增

- **邀请有效期动态提示**：邀请接收消息中加入 `{TIMEOUT}` 占位符，显示的有效秒数直接读取 `config.yml` 中的 `invite_timeout_seconds`，不再硬编码。

### 变更

- **邀请默认超时改为 1 小时**：`config.yml` 中 `invite_timeout_seconds` 默认值从 `60` 秒调整为 `3600` 秒（1 小时）；`ConfigManager` 的代码 fallback 同步更新。管理员可在 `config.yml` 中自由调整此值。

### Added

- **Dynamic invite expiry display**: The invite-received message now uses a `{TIMEOUT}` placeholder populated from `invite_timeout_seconds` in `config.yml`, replacing the previous hardcoded value.

### Changed

- **Invite timeout default changed to 1 hour**: `invite_timeout_seconds` in `config.yml` updated from `60` to `3600` (1 hour); the Java fallback in `ConfigManager` was synchronized. Server admins can freely adjust this value in `config.yml`.

---

## [1.0.1] - 2026-08-20

### 修复

- **同盟申请右键拒绝失效**：通知中心 GUI 中，队长右键点击同盟申请时无任何响应（任何点击均触发接受逻辑）。现已修复为左键接受、右键拒绝，并在拒绝时向申请方队长发送通知。

### 新增

- **`RelationManager.denyAllyRequest()`**：新增同盟申请拒绝方法，从内存与数据库中移除待处理申请（不建立盟友关系）。
- **拒绝通知消息**：所有三个语言文件（`zh_CN` / `zh_TW` / `en_US`）新增 `ally_request_denied`（队长侧提示）与 `ally_request_denied_notify`（申请方通知）消息键。

### 变更

- **通知 GUI lore 更新**：同盟申请条目（队长视角）的 lore 提示由"点击接受"更新为"左键接受 / 右键拒绝"（适用于三个语言文件）。
- **README 精简**：移除冗余的配置代码块和逐行展开的指令表格，以更简洁的分组格式重写双语 README，文件行数从 359 行压缩至 159 行。

### Fixed

- **Ally request right-click deny not working**: In the Notification Center GUI, right-clicking an alliance request had no effect — any click triggered the accept logic. Fixed to: left-click to accept, right-click to deny, with a denial notification sent to the requester's leader.

### Added

- **`RelationManager.denyAllyRequest()`**: New method to deny an alliance request — removes it from memory and database without creating an alliance.
- **Deny notification messages**: Added `ally_request_denied` and `ally_request_denied_notify` message keys to all three language files (`zh_CN` / `zh_TW` / `en_US`).

### Changed

- **Notification GUI lore updated**: The ally request item lore (leader view) now reads "Left-click to accept / Right-click to deny" instead of a single "Click to accept" hint, across all three language files.
- **README simplified**: Removed verbose config YAML blocks and expanded command tables. Rewrote the bilingual README in a condensed format, reducing line count from 359 to 159.

---

## [1.0.0] - 2026-08-20

### 新增

- 初始版本发布。
- 团队创建、解散、邀请、踢人、转让队长、设置副队长、申请入队。
- 外交系统：结盟 / 解盟、宣战 / 求和，含数量上限。
- 平衡机制：友伤开关（带冷却）、同盟保护、退队冷却。
- 团队聊天频道 (`/tc`)，含锁定模式与 OP 监听。
- 图形化 GUI：团队菜单、成员管理、全服列表、外交管理、通知中心。
- 双存储引擎：MySQL（HikariCP 连接池）/ SQLite。
- 自动数据库迁移。
- 内置三语言支持：`zh_CN` / `zh_TW` / `en_US`，支持自定义语言文件。

### Added

- Initial release.
- Team creation, disbanding, invite, kick, promote, transfer leadership, apply to join.
- Diplomacy system: ally / unally, war / peace, with configurable caps.
- Balance mechanics: friendly fire toggle (with cooldown), ally protection, leave cooldown.
- Team chat channel (`/tc`) with lock mode and OP spy.
- Graphical GUI: team menu, member management, server-wide list, diplomacy management, notification center.
- Dual storage: MySQL (HikariCP connection pool) / SQLite.
- Automatic database migration.
- Built-in trilingual support: `zh_CN` / `zh_TW` / `en_US`, with custom language file support.

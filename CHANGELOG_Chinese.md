# Changelog / 更新日志

所有版本的重要变更均记录于此。  
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。  

## [1.2.3] - 2026-10-01

### 变更 / Changed

- **彻底清除底层模型、界面与配置中的写死中文 (Eliminate Hardcoded Chinese Strings Across the Board)**：
  - **枚举层国际化 (Model Enums Localization)**：重构 `RelationStatus`、`RelationType` 与 `TeamRole`，移除构造器中写死的中文默认值（`"申请中"`/`"已确立"`/`"队长"`/`"管理员"`/`"队员"`），底层统一采用安全英文代码作为回退基线；
  - **多语言动态解析 (Dynamic Multi-Language Resolution)**：重写 `RelationStatus.getDisplayName()`、`RelationType.getDisplayName()` 与 `TeamRole.getDisplayName()`，默认自动接入 `ConfigManager` 动态解析当前系统语言，并增加支持发送者上下文参数的 `getDisplayName(sender)` 重载；
  - **界面与配置兜底消除 (Eliminated Hardcoded GUI & Config Fallbacks)**：消除 `TeamMenuGui`（状态前缀）、`TeamListGui`（空团队占位提示）、`ConfigManager.getNoneTeamName` 中遗留的写死中文字符串，统一改为优先通过客户端语言包/默认语言动态加载；
  - **输入取消判定国际化 (Chat Input Cancellation Localization)**：重构 `ChatInputManager` 取消逻辑，封装 `isCancelInput`，支持通过语言包配置取消关键字。

### 新增 / Added

- **占位符扩展 (PlaceholderAPI Expansion)**：在 `BalancedTeamExpansion` 中新增 `%balancedteam_relation_status_<player/team>%` 与 `%balancedteam_relation_status_formatted_<player/team>%` 占位符，支持完整的外交关系状态国际化输出。

---

## [1.2.2] - 2026-09-30

### 变更 / Changed

- **全面向下兼容至 Minecraft 1.16 及以上版本并适配最新 26.3 (Minecraft 1.16+ Compatibility & Modern 26.3 Adaptation)**：
  - 将 `plugin.yml` 的 `api-version` 调整为 `'1.16'`，确保插件在 Minecraft 1.16 及以上全系列服务端中均能正常加载，向上完美兼容 1.17 至 26.3；
  - 解决 1.16.x 环境下不存在 `Material.SPYGLASS`（望远镜）导致的 `NoSuchFieldError`：在 `ItemBuilder` 中引入版本安全枚举匹配机制 `getSafeMaterial`，在 1.16.x 优雅降级回退至 `Material.COMPASS`（指南针）；
  - 对 `DamageListener` 中 `PotionMeta` 与 `AreaEffectCloud` 的 `getBasePotionType()`（1.20.5+）调用全面升级为安全反射，低版本无缝回退至 1.16+ 的 `getBasePotionData()`，并通过 `isHarmfulPotionType` 统一兼容 1.16+ 的 `getEffectType()` 与 1.20.5+ 的 `getPotionEffects()`，彻底消除旧版 JVM 类链接验证异常与友伤漏判漏洞；
  - 增强 `ClientLanguageManager` 与 `PlayerListener` 的语言环境自动探测，采用 Bukkit 1.12+ 原生 `player.getLocale()`；
  - 深度适配 Bukkit / Paper 26.3 统一 `DamageSource` API：在 `DamageListener` 中通过安全反射读取 `event.getDamageSource().getCausingEntity()`，原生支持风弹（Wind Charge / Breeze Wind Charge）、风爆冲击（Wind Burst）、重锤（Mace）爆发及新型间接爆炸伤害的实际攻击者追溯与友伤拦截；
  - 补充对 TNT 矿车 (`ExplosiveMinecart`) 的伤害责任人精准追溯；
  - 将构建配置升级为面向 Java 11 字节码输出（`<java.version>11</java.version>`），原生兼容 Java 11、17 及 21 运行环境；
  - 全面更新 `README.md` 与 Wiki 文档中的支持版本徽章及环境要求为 `Minecraft 1.16+ (1.16 - 26.3)`、`Java 11 | 17 | 21`。
- **清除敌对管理与团队选择界面中的硬编码文本 (Eliminate GUI Hardcoded Strings)**：
  - `EnemyManageGui`：彻底重构停战求和、双方交战状态、单方面宣战等 Lore 文本，移除写死的中文字符与裸露秒数拼接，全面注册为 `GuiConfigKeys` 并接入语言文件与 `TimeUtil.formatDuration` 阶梯时间；
  - `TeamSelectGui`：将战后保护 Lore 文本全面改为从多语言文件动态加载；
  - 全面替换所有 GUI（`TeamDetailGui`、`MemberManageGui`、`NotificationGui`、`PlayerSelectGui`、`TeamListGui`、`ConfirmGui`）中残留的 `"未知"`、`"未知队伍"`、`"未知玩家"` 硬编码文本为 `time_unit.unknown` 动态多语言获取；
  - 同步更新三套内置语言包（`zh_CN.yml`、`zh_TW.yml`、`en_US.yml`），提供高质量本地化翻译。
- **优化末影水晶生成坐标匹配与内存清理 (Crystal Placement & Memory Cleanup)**：
  - `CrystalListener` 实体生成坐标匹配增强对上下各浮动 1 格（`Y - 1` 与 `Y + 1`）的双向容错，确保在所有服务端衍生版本中 100% 捕获水晶放置者；
  - 优化末影水晶长期缓存清理策略：无条件清理超过 1 小时的未引爆历史放置记录，并在记录数超过 500 时清理 30 分钟记录，杜绝长期运行下的内存泄漏。

### 修复 / Fixed

- **修复负面药水判定漏洞 (Harmful Potion Detection Fix)**：
  - 修复 `DamageListener.isHarmfulEffect` 漏判新版命名空间 ID 导致的挖掘疲劳（`MINING_FATIGUE`）与反胃（`NAUSEA`）友伤穿透漏洞；
  - 全面补充 Minecraft 1.21+ / 26.x 战斗中新增的重要负面药水与预兆效果：渗浆（`OOZING`）、寄生（`INFESTED`）、织网（`WEAVING`）、蓄风（`WIND_CHARGED`）、袭村不祥预兆（`RAID_OMEN`）与试炼不祥预兆（`TRIAL_OMEN`）；
  - 修复 `onPotionSplash` 仅检查自定义药水效果、导致原版酿造喷溅药水（如喷溅型伤害药水、剧毒药水、虚弱药水）因 `getEffects()` 为空而漏判穿透友伤保护的重大缺陷，深度整合 `PotionMeta` 的 `getBasePotionType()` (1.20.5+ / 26.3) 与 `getBasePotionData()` (旧版兼容) 判定。
- **修复滞留药水云受影响实体列表移除异常 (Area Effect Cloud Resilience)**：
  - 为 `onAreaEffectCloudApply` 的受影响实体移除逻辑增加安全防御封装，防止在特定优化端或不可变列表实现下抛出 `UnsupportedOperationException`。

---

## [1.2.1] - 2026-09-19

### 新增 / Added

- **时间格式化算法与服务端时区支持 (Timezone & Duration Formatting)**：
  - `config.yml` 新增 `timezone` 配置项（默认 `GMT+8`），支持任意标准时区 ID（如 `Asia/Shanghai`、`UTC`、`America/New_York`、`Europe/London` 等）；
  - `TimeUtil` 升级全面支持服务端配置时区，统一所有日期时间格式化（`formatDate`）的时区输出，彻底解决跨地区服务器客户端/服务端时差显示问题；
  - `TimeUtil.formatDuration` 阶梯算法重构：智能阶梯组合多单位展示（如 `1天 2小时 30分`），自适应输出语言环境时间单位；
  - 全面排查并替换控制台、命令提示（技能冷却、退队冷却、友伤切换冷却、保护期剩余、邀请/同盟/求和超时）以及 GUI 物品 Lore 中的裸露秒数为自适应阶梯时间。
- **服务端日志支持多国语言扩展 (Server Language Expansion)**：
  - 服务端语言支持由原先的 3 种扩展至 7 种（新增 `ja_JP`、`ru_RU`、`de_DE`、`es_ES`）；
  - `ServerLanguageManager` 支持前缀模糊匹配，`PluginLogger` 控制台日志全量补全翻译；
  - `config.yml` 中 `server_messages_language` 配置项注释同步更新列出 7 种内置语言。
- **多语言新增键值 (New Language Keys)**：
  - 三套语言文件新增 `lang_not_initialized`、`lang_usage_set`、`usage_info`、`team_list_empty`、`team_list_header`、`team_list_item`、`team_list_footer`、`chat_input_timeout`、`chat_input_cancelled`、`chat_input_suggest_hover`、`gui.list.empty_item_name`、`gui.list.empty_item_lore`、`gui.menu.ff_status_prefix` 等国际化条目。

### 变更 / Changed

- **多语言管理器架构拆分 (Language Architecture Decoupling)**：
  - 将单一管理器彻底解耦为 `ServerLanguageManager`（服务端与控制台日志）与 `ClientLanguageManager`（客户端玩家端多语言）；
  - 移除原历史类 `LanguageManager`，`BalancedTeamPlugin` 统一公开 `getServerLanguageManager()` 与 `getClientLanguageManager()`。
- **清除命令类与队伍聊天中的硬编码中文字符串 (Eliminate Command & Chat Hardcoded Strings)**：
  - 清理 `TeamCommand`、`TeamLangCommand`、`ChatInputManager`、`TeamMenuGui` 与 `TeamListGui` 中残留的硬编码中文提示，改由多语言配置文件动态获取；
  - 队长缺省名与友伤开关状态全面对接语言文件的 `time_unit.unknown` 与 `status.on` / `status.off`。
- **版本号升级 `1.2.0` → `1.2.1` (Version Bump)**：
  - `pom.xml` 版本号升级为 `1.2.1`。

### 修复 / Fixed

- **队伍聊天与解散广播接收端动态多语言适配 (Recipient-Aware Chat & Broadcast Localization)**：
  - `ChatManager`：团队聊天消息分发时，根据接收者语言偏好动态格式化职位名称 `{ROLE}`；
  - `TeamCommand`：修复 `handleChat`、`handleFriendlyFire`、`handleAlly`、`handleEnemy` 未传入 `Player` 实例导致非中文客户端收到服务端默认语言提示的问题；
  - `TeamManager`：修复解散团队全服广播（`team_disband_broadcast`）未传入接收玩家实例的问题；
  - `PlayerListener`：当玩家已不在队伍或全局聊天已关闭时自动退出队伍聊天模式并发送本地化提示。
- **全量命令消息动态多语言适配 (Dynamic Sender-Aware Localization)**：
  - 排查并修复 `TeamCommand`、`TeamAdminCommand`、`TeamMsgCommand` 中此前未传 `sender`/`player` 的 `getMessage(...)` 与 `getMessageList(...)` 调用，杜绝指令错误回退全服默认语言。

---

## [1.2.0] - 2026-09-12

### 新增 / Added

- **外交停战与求和机制 (Diplomacy Truce & Peace System)**：
  - 新增停战与求和完整子命令体系（`/team truce <team>`、`accept`、`deny`、`cancel`、`list`）；
  - **战后停战保护期 (Post-War Protection)**：停战达成后双方进入配置时长（默认 1800 秒）的保护期，双向禁止重新宣战，`DamageListener` 拦截两队间近战、抛射物、喷溅药水与药水云伤害；
  - **求和请求自动过期 (Truce Expiration)**：新增 `balance.truce_request_timeout_seconds` 超时自动清理机制；
  - **GUI 交互集成 (Seamless GUI Integration)**：`NotificationGui` 新增白色旗帜通知卡片，`EnemyManageGui` 增加战后保护盾牌图标与倒计时 Badge。
- **队伍聊天全局开关 `enable_chat` (Global Chat Toggle)**：
  - `config.yml` 新增 `chat.enable_chat`（默认 `true`）；关闭后立即禁用所有队伍聊天入口，并将处于聊天锁定模式的玩家自动移出。
- **快捷聊天指令独立开关 `enable_tc_command` (TC Command Toggle)**：
  - `TeamMsgCommand.onCommand()` 支持通过 `chat.enable_tc_command` 单独控制 `/tc`、`/tm`、`/teammsg` 快捷指令。
- **终端监听队伍聊天 `console_listen_team_chat` (Console Team Chat Spy)**：
  - 开启 `chat.console_listen_team_chat` 时，自动在控制台同步打印队内格式化聊天记录。
- **多语言新消息键 (New Language Keys)**：
  - 补充 `chat_disabled`、`chat_tc_disabled` 语言节点。

### 变更 / Changed

- **`ConfigManager` 方法补全与文档完善 (ConfigManager Methods & Documentation)**：
  - `getChatFormat()` 恢复 `{TEAM}` 占位符默认值；
  - `getSpyFormat()` 默认值统一对齐为 `SPY`；
  - Javadoc 改写为标准 HTML 列表格式并补全占位符说明。
- **版本号升级 `1.1.7` → `1.2.0` (Version Bump)**：
  - `pom.xml` 版本号升级至 `1.2.0`。

### 修复 / Fixed

- **安全防御加固 (Security Hardening & Friendly-Fire Fixes)**：
  - 修复 PlaceholderAPI 聊天占位符注入漏洞，调整格式化求值顺序；
  - 强化团队人数（`max_members`）、同盟数（`max_allies`）与敌对数（`max_enemies`）的并发同步锁边界防护；
  - 优化 `DamageListener` 与 `CrystalListener` 在复杂伤害源（水晶连锁爆炸、发射器火药、药水云）下的队友与盟友拦截准确率。

---

## [1.1.7] - 2026-09-05

### 修复 / Fixed

- **修复 PlaceholderAPI 聊天占位符注入漏洞 (PAPI Injection Fix)**：
  - 调整 `ChatManager` 中的格式化求值顺序，在将聊天内容拼入模板前先行解析模板中的 PAPI 变量与颜色，杜绝恶意变量注入；
  - 规范团队聊天颜色权限校验（`balancedteam.chat.color`），优化 `MessageUtil.sendRawMessage` 避免重复正则转色。
- **强化团队人数与外交关系并发上限防护 (Concurrency Boundary Protection)**：
  - `TeamManager.addMember` 增加双重同步锁（`synchronized`）校验，防止并发加入突破 `max_members` 上限；
  - `RelationManager` 内部增加 `max_allies` 与 `max_enemies` 上限防护及互斥防御校验。

---

## [1.1.6] - 2026-08-25

### 变更 / Changed

- **扩展跨版本兼容性 (Minecraft 1.20.x - 26.2 Compatibility)**：
  - 增强音频播放 (`SoundUtil`) 与伤害判定监听器的跨版本安全容错，确保在 Minecraft 1.20.x 至 26.2 衍生服务端稳定运行；
  - 完善 Java 21 运行环境兼容与文档版本徽标更新。
- **DAO 层持久化错误报告全英文标准化 (DAO English Error Reports)**：
  - 将所有 DAO 数据访问层及 `DatabaseException` 错误信息全面替换为标准英文描述。

---

## [1.1.5] - 2026-08-22

### 新增 / Added

- **服务端控制台日志国际化与相对路径解析 (Server Log Localization & Relative Path Support)**：
  - `config.yml` 中的 `language` 设置全面支持语言代码、文件名或相对路径指定服务端默认语言；
  - 新增 `PluginLogger` 控制台日志国际化管理器，语言变更或重载时自动无缝热切换服务端控制台输出语言；
  - 采用内置模板管理，无需在外部文件中维护控制台日志条目。

---

## [1.1.4] - 2026-08-22

### 新增 / Added

- **时间单位与持续时间多语言可配置化 (TimeUtil & Localization)**：
  - 在语言文件中新增 `time_unit` 节点，支持 `day`、`hour`、`minute`、`second` 及 `unknown` 自定义配置；
  - `TimeUtil` 扩展支持 `formatDuration` 与 `formatDate` 基于发送者/玩家当前语言动态渲染时间；
  - `ConfigManager.load()` 增加 `syncTimeUnits()` 实现重载时全局自动同步。
- **控制台指令兼容与合理性支持 (Console Command Support)**：
  - 解除 `/team` 主指令入口对控制台的全局拦截，按子指令放行；
  - 支持控制台执行 `/team list` 分页列表、`/team info` 团队查询、`/team lang` 多语言管理以及 `/team help` 帮助指令；
  - 交互类指令严格进行控制台隔离并提示 `player_only`。

### 修复 / Fixed

- **排查并消除硬编码多语言文本 (Eliminated Hardcoded Strings)**：
  - 修复 `BalancedTeamExpansion` 中 `%balancedteam_friendly_fire_formatted%` 硬编码中文问题，改为读取 `status.on` / `status.off`；
  - 优化各 GUI 与指令中的日期格式化调用，适配玩家本地语言。

---

## [1.1.3] - 2026-08-22

### 新增 / Added

- **原生 PlaceholderAPI (PAPI) 变量扩展支持 (Native PAPI Expansion Support)**：
  - 新增 `BalancedTeamExpansion` 原生扩展类，统一注册标识符 `%balancedteam_<变量名>%`，提供 35+ 个占位符变量；
  - 完善离线玩家查询与非队员状态下的安全默认值兜底。
- **双向变量解析工具 (PAPIUtil)**：
  - 增强 `PAPIUtil` 工具类，封装生命周期注册/注销以及字符串/列表无异常安全解析；
  - 团队聊天 (`ChatManager`) 与管理员监听支持解析第三方 PAPI 占位符。
- **软依赖与文档完善 (Soft Dependency & Wiki)**：
  - `plugin.yml` 中新增 `softdepend: [PlaceholderAPI]`；
  - 发布包含 8 大核心模块的官方 GitHub 英文 Wiki 文档。

---

## [1.1.2] - 2026-08-21

### 新增 / Added

- **统一二次确认系统 (ConfirmGui)**：
  - 重构并统一 6 大模式二次确认机制（`DISBAND`、`LEAVE`、`KICK`、`TRANSFER`、`PROMOTE`、`DEMOTE`）；
  - 全流程防误触与前置+点击双重校验，单次点击处理器防止并发连击。

### 变更 / Changed

- **优化成员管理界面 (MemberManageGui)**：
  - 右键踢出、Shift+点击转让、左键升降职统一弹出 `ConfirmGui` 二次确认弹窗。
- **优化管理指令交互 (TeamCommand)**：
  - `/team kick`、`transfer`、`promote`、`demote` 指令校验基础参数后唤起 `ConfirmGui` 确认界面。
- **动态配置占位符支持 (Dynamic Config Placeholders)**：
  - 团队菜单邀请提示硬编码 60 秒替换为 `{TIMEOUT}`；
  - 团队菜单友伤切换提示硬编码 30 秒替换为 `{COOLDOWN}`。
- **配置文件与多语言包同步 (Localization Sync)**：
  - `config.yml` 全文注释标准化为规范英文；
  - 各语言包同步补充确认弹窗相关多语言节点。

---

## [1.1.1] - 2026-08-21

### 变更 / Changed

- **新增 GUI 返回按钮配置 (GUI Back Button Config)**：
  - `GuiConfigKeys` 新增 `DETAIL_BACK_BUTTON_IN_TEAM` 与 `DETAIL_BACK_BUTTON_NOT_IN_TEAM`。
- **更新语言文件 (Language Files Updated)**：
  - 各语言文件新增 `back_button_in_team` 与 `back_button_not_in_team` 键值。
- **修改 TeamDetailGui**：
  - 槽位 31 返回按钮根据玩家在队状态动态跳转至团队控制面板或全服列表。

---

## [1.1.0] - 2026-08-20

### 新增 / Added

- **客户端语言自动检测机制 (Client Locale Auto-Detection)**：
  - 玩家登入时通过 `Player.getLocale()` 自动获取并加载匹配的语言包。
- **多级智能模糊匹配与回退算法 (Smart Fuzzy Matching)**：
  - 支持前缀模糊匹配（如 `zh_HK`/`zh_TW` 优先匹配 `zh_*`），逐级回退至服务端默认配置与内置基线。
- **全量多语言内存高速缓存 (In-Memory Language Caching)**：
  - 启动与重载时预加载语言包至内存，消除运行期磁盘 I/O。
- **独立语言指令与切换机制 (Language Commands)**：
  - 新增 `/teamlang` 系列指令，支持 `list`、手动设置、`auto` 自动检测与 `reload` 热重载；
  - 玩家语言偏好持久化保存在 `data/user_languages.yml`。
- **GUI 与消息系统多语言全面适配 (Full GUI & Message Localization)**：
  - 全量 GUI 界面动态适配操作玩家的客户端生效语言。

---

## [1.0.2] - 2026-08-20

### 新增 / Added

- **邀请有效期动态提示 (Dynamic Invite Expiry Display)**：邀请消息支持 `{TIMEOUT}` 动态占位符。

### 变更 / Changed

- **邀请默认超时调整 (Invite Timeout Default Updated)**：`config.yml` 中 `invite_timeout_seconds` 默认值调整为 `3600` 秒（1 小时）。

---

## [1.0.1] - 2026-08-20

### 修复 / Fixed

- **同盟申请右键拒绝失效 (Ally Request Right-Click Deny Fix)**：修复通知中心右键无响应缺陷，明确左键接受、右键拒绝并在拒绝时触发通知。

### 新增 / Added

- **`RelationManager.denyAllyRequest()`**：新增同盟申请拒绝方法。
- **拒绝通知消息 (Deny Notifications)**：各语言文件新增 `ally_request_denied` 与 `ally_request_denied_notify`。

### 变更 / Changed

- **通知 GUI Lore 更新**：更新同盟申请提示文本为“左键接受 / 右键拒绝”。
- **文档精简 (README Simplified)**：精简重构双语 README。

---

## [1.0.0] - 2026-08-20

### 新增 / Added

- 初始版本发布 (Initial release)。
- 团队核心管理系统：创建、解散、邀请、踢人、转让队长、设置副队长、申请入队。
- 外交系统：结盟 / 解盟、宣战 / 求和，含数量上限限制。
- 平衡机制：友伤开关（带冷却）、同盟保护、退队冷却。
- 团队聊天频道 (`/tc`)，支持锁定模式与 OP 监听。
- 全图形化 GUI 体系：团队菜单、成员管理、全服列表、外交管理、通知中心。
- 双存储引擎支持：MySQL（HikariCP 连接池）/ SQLite，支持自动数据库迁移。
- 内置三语言支持（`zh_CN` / `zh_TW` / `en_US`），支持自定义语言扩展。
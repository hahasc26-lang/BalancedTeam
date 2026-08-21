# Changelog / 更新日志

所有版本的重要变更均记录于此。  
All notable changes to this project are documented here.

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。  
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).


## [1.1.1] - 2026-08-21

### 变化 / Changed

- **新增 GUI 返回按钮配置**：在 `GuiConfigKeys` 中加入 `DETAIL_BACK_BUTTON_IN_TEAM` 与 `DETAIL_BACK_BUTTON_NOT_IN_TEAM`，对应语言文件 `back_button_in_team`、`back_button_not_in_team`，实现团队详情页面根据玩家是否在队伍中显示不同的返回按钮文字。
- **更新语言文件**：在 `zh_TW.yml`、`zh_CN.yml`、`en_US.yml` 中新增 `back_button_in_team` 与 `back_button_not_in_team` 键，分别对应 “返回团队控制面板” / “返回团队列表”等文案。
- **修改 TeamDetailGui**：根据玩家所属团队动态读取对应返回按钮键，并在槽位 31 设置返回动作；在团队内返回至团队控制面板，在未加入团队时返回至团队列表。
- **版本升级**：项目版本号更新至 1.1.1（pom.xml）。
---

## [1.1.0] - 2026-08-20

### 新增 / Added

- **客户端语言自动检测机制**：玩家加入服务器时（`PlayerJoinEvent`）通过 `Player.getLocale()` 自动获取客户端语言代码（如 `zh_CN`、`en_US` 等），并自动匹配加载对应的语言包文件。
- **Client Locale Auto-Detection**: Automatically detects players' client language settings on login (`PlayerJoinEvent`) using `Player.getLocale()` and loads the corresponding language file.
- **多级智能模糊匹配与回退算法**：支持语言前缀模糊匹配（例如玩家为 `zh_HK` 或 `zh_TW` 客户端，若无完全匹配文件则优先智能匹配 `zh_*`，再回退到 `config.yml` 服务端默认语言及最终兜底语言）。
- **Smart Fuzzy Matching & Fallback Algorithm**: Supports prefix-based fuzzy matching (e.g. `zh_HK` or `zh_TW` clients match `zh_*` packs if exact matches do not exist) before falling back to server default language in `config.yml` and hardcoded fallback.
- **全量多语言内存高速缓存**：启动与重载时全量预加载 `plugins/BalancedTeam/lang/*.yml` 文件到内存缓存中，运行期发送消息与渲染 GUI 零额外磁盘 I/O。
- **In-Memory Language Caching**: Scans and caches all `plugins/BalancedTeam/lang/*.yml` files in memory on startup and reload, eliminating redundant disk I/O during gameplay and GUI rendering.
- **独立语言指令与切换机制**：
  - 新增 `/teamlang`（别名 `/tlang` `/btlang` `/clanlang`）及 `/team lang` 指令；
  - 支持 `/teamlang list` 查看服务器所有支持的语言包及当前生效状态；
  - 支持 `/teamlang <代码>`（如 `/teamlang en_US`）手动固定语言，或 `/teamlang auto` 恢复自动检测；
  - 支持 `/teamlang reload` 管理员热重载所有语言文件；
  - 玩家语言偏好持久化保存在 `data/user_languages.yml`，离线重连不丢失。
- **Dedicated Language Commands & Switching**:
  - Added `/teamlang` (aliases `/tlang` `/btlang` `/clanlang`) and `/team lang` commands;
  - Added `/teamlang list` to view all available language packs and current active status;
  - Added `/teamlang <locale>` (e.g. `/teamlang en_US`) for manual language override, and `/teamlang auto` to restore auto-detection;
  - Added `/teamlang reload` for admins to hot-reload all language packs;
  - Player language preferences are persisted in `data/user_languages.yml` across disconnects.
- **GUI 与消息系统多语言全面适配**：所有图形化 GUI 界面（团队菜单、未加队面板、成员管理、通知中心、全服列表、团队详情、目标选择、确认弹窗）均全面根据操作玩家客户端生效语言动态渲染标题与 Lore。
- **Full Multi-Language Localization for GUI & Messages**: All graphical GUI interfaces (Team Menu, Not-Joined Panel, Member Management, Notification Center, Team List, Detail View, Selection, and Confirmation dialogs) dynamically render titles and lores according to each player's effective language.

---

## [1.0.2] - 2026-08-20

### 新增 / Added

- **邀请有效期动态提示**：邀请接收消息中加入 `{TIMEOUT}` 占位符，显示的有效秒数直接读取 `config.yml` 中的 `invite_timeout_seconds`，不再硬编码。
- **Dynamic invite expiry display**: The invite-received message now uses a `{TIMEOUT}` placeholder populated from `invite_timeout_seconds` in `config.yml`, replacing the previous hardcoded value.

### 变更 / Changed

- **邀请默认超时改为 1 小时**：`config.yml` 中 `invite_timeout_seconds` 默认值从 `60` 秒调整为 `3600` 秒（1 小时）；`ConfigManager` 的代码 fallback 同步更新。管理员可在 `config.yml` 中自由调整此值。
- **Invite timeout default changed to 1 hour**: `invite_timeout_seconds` in `config.yml` updated from `60` to `3600` (1 hour); the Java fallback in `ConfigManager` was synchronized. Server admins can freely adjust this value in `config.yml`.

---

## [1.0.1] - 2026-08-20

### 修复 / Fixed

- **同盟申请右键拒绝失效**：通知中心 GUI 中，队长右键点击同盟申请时无任何响应（任何点击均触发接受逻辑）。现已修复为左键接受、右键拒绝，并在拒绝时向申请方队长发送通知。
- **Ally request right-click deny not working**: In the Notification Center GUI, right-clicking an alliance request had no effect — any click triggered the accept logic. Fixed to: left-click to accept, right-click to deny, with a denial notification sent to the requester's leader.

### 新增 / Added

- **`RelationManager.denyAllyRequest()`**：新增同盟申请拒绝方法，从内存与数据库中移除待处理申请（不建立盟友关系）。
- **`RelationManager.denyAllyRequest()`**: New method to deny an alliance request — removes it from memory and database without creating an alliance.
- **拒绝通知消息**：所有三个语言文件（`zh_CN` / `zh_TW` / `en_US`）新增 `ally_request_denied`（队长侧提示）与 `ally_request_denied_notify`（申请方通知）消息键。
- **Deny notification messages**: Added `ally_request_denied` and `ally_request_denied_notify` message keys to all three language files (`zh_CN` / `zh_TW` / `en_US`).

### 变更 / Changed

- **通知 GUI lore 更新**：同盟申请条目（队长视角）的 lore 提示由「点击接受」更新为「左键接受 / 右键拒绝」（适用于三个语言文件）。
- **Notification GUI lore updated**: The ally request item lore (leader view) now reads "Left-click to accept / Right-click to deny" instead of a single "Click to accept" hint, across all three language files.
- **README 精简**：移除冗余的配置代码块和逐行展开的指令表格，以更简洁的分组格式重写双语 README，文件行数从 359 行压缩至 159 行。
- **README simplified**: Removed verbose config YAML blocks and expanded command tables. Rewrote the bilingual README in a condensed format, reducing line count from 359 to 159.

---

## [1.0.0] - 2026-08-20

### 新增 / Added

- 初始版本发布。
- Initial release.
- 团队创建、解散、邀请、踢人、转让队长、设置副队长、申请入队。
- Team creation, disbanding, invite, kick, promote, transfer leadership, apply to join.
- 外交系统：结盟 / 解盟、宣战 / 求和，含数量上限。
- Diplomacy system: ally / unally, war / peace, with configurable caps.
- 平衡机制：友伤开关（带冷却）、同盟保护、退队冷却。
- Balance mechanics: friendly fire toggle (with cooldown), ally protection, leave cooldown.
- 团队聊天频道 (`/tc`)，含锁定模式与 OP 监听。
- Team chat channel (`/tc`) with lock mode and OP spy.
- 图形化 GUI：团队菜单、成员管理、全服列表、外交管理、通知中心。
- Graphical GUI: team menu, member management, server-wide list, diplomacy management, notification center.
- 双存储引擎：MySQL（HikariCP 连接池）/ SQLite。
- Dual storage: MySQL (HikariCP connection pool) / SQLite.
- 自动数据库迁移。
- Automatic database migration.
- 内置三语言支持：`zh_CN` / `zh_TW` / `en_US`，支持自定义语言文件。
- Built-in trilingual support: `zh_CN` / `zh_TW` / `en_US`, with custom language file support.

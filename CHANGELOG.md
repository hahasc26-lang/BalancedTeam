# Changelog / 更新日志

所有版本的重要变更均记录于此。  
All notable changes to this project are documented here.

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。  
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

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

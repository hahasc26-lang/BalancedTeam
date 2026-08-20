# Changelog / 更新日志

所有版本的重要变更均记录于此。  
All notable changes to this project are documented here.

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)。  
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

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

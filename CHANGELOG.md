# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.3] - 2026-10-01

### Added

- **PlaceholderAPI Expansion**: Introduced `%balancedteam_relation_status_<player/team>%` and `%balancedteam_relation_status_formatted_<player/team>%` placeholders in `BalancedTeamExpansion` for fully localized diplomatic relation status display.

### Changed

- **Eliminate Hardcoded Chinese Strings Across the Board**:
  - **Model Enums Localization**: Refactored `RelationStatus`, `RelationType`, and `TeamRole` to remove hardcoded Chinese string literals in constructors (`"申请中"`, `"已确立"`, `"队长"`, `"管理员"`, `"队员"`), standardizing on safe English identifiers as fallback baselines.
  - **Dynamic Multi-Language Resolution**: Rewrote `RelationStatus.getDisplayName()`, `RelationType.getDisplayName()`, and `TeamRole.getDisplayName()` to automatically resolve display names from `ConfigManager` based on the active locale, adding overloaded `getDisplayName(sender)` methods for sender-aware resolution.
  - **GUI & Config Hardcoded Fallbacks Removed**: Replaced remaining hardcoded Chinese strings in `TeamMenuGui` (status prefix fallback), `TeamListGui` (empty list placeholder), and `ConfigManager.getNoneTeamName()` with dynamic language file resolution and neutral English fallbacks.
  - **Chat Input Cancellation Localization**: Refactored `ChatInputManager` cancellation detection to support localized cancel keywords defined in language files alongside standard commands.

---

## [1.2.2] - 2026-09-30

### Changed

- **Minecraft 1.16+ Downward Compatibility & Modern 26.3 Adaptation**:
  - Adjusted `api-version` in `plugin.yml` to `'1.16'`, ensuring Minecraft 1.16 and above (1.16.x) servers will not reject the plugin due to unsupported API version, while maintaining backwards and forwards compatibility from 1.16 through 26.3.
  - Resolved `NoSuchFieldError` on 1.16.x servers caused by nonexistent `Material.SPYGLASS`: introduced `ItemBuilder.getSafeMaterial` fallback mechanism to gracefully downgrade to `Material.COMPASS` on older versions.
  - Fully upgraded `getBasePotionType()` (1.20.5+) invocations in `DamageListener` for both `PotionMeta` and `AreaEffectCloud` to safe reflection, falling back to legacy `getBasePotionData()` on 1.16+ and bridging `getEffectType()` and `getPotionEffects()` via `isHarmfulPotionType`.
  - Standardized client locale auto-detection across `ClientLanguageManager` and `PlayerListener` using native Bukkit 1.12+ `player.getLocale()`.
  - Deep adaptation for Bukkit / Paper 26.3 unified `DamageSource` API: Safely resolved `event.getDamageSource().getCausingEntity()` via reflection in `DamageListener`, natively supporting attacker attribution and friendly-fire protection for Wind Charges (Wind Charge / Breeze Wind Charge), Wind Burst enchantments, Mace smash attacks, and modern indirect explosive damages.
  - Added accurate attacker attribution for TNT Minecarts (`ExplosiveMinecart`).
  - Configured Maven build target to Java 11 bytecode (`<java.version>11</java.version>`), natively running on Java 11, 17, and 21 runtime environments.
  - Updated compatibility badges and environment requirements across `README.md` and Wiki documentation to `Minecraft 1.16+ (1.16 - 26.3)` and `Java 11 | 17 | 21`.
- **Eliminated GUI Hardcoded Strings**:
  - `EnemyManageGui`: Completely refactored lore text for peace requests, mutual war states, and unilateral war declarations; removed hardcoded Chinese characters and raw second values, registering them under `GuiConfigKeys` with dynamic language file resolution and `TimeUtil.formatDuration` tiered formatting.
  - `TeamSelectGui`: Migrated post-war protection item lore to load dynamically from client language files.
  - Systematically replaced legacy hardcoded `"未知"` / `"未知队伍"` / `"未知玩家"` fallback strings across all GUIs (`TeamDetailGui`, `MemberManageGui`, `NotificationGui`, `PlayerSelectGui`, `TeamListGui`, `ConfirmGui`) with dynamic `time_unit.unknown` lookups.
  - Synchronized and updated all three built-in language files (`zh_CN.yml`, `zh_TW.yml`, `en_US.yml`) with high-fidelity translations.
- **Crystal Placement Matching & Memory Cleanup**:
  - Enhanced coordinate matching in `CrystalListener` with bidirectional tolerance for vertical offsets (`Y - 1` and `Y + 1`), ensuring 100% reliable crystal placer attribution across all server forks.
  - Optimized long-term cache cleanup policy: unconditionally purge unexploded crystal placements older than 1 hour, and purge entries older than 30 minutes when tracker size exceeds 500 entries, preventing memory accumulation over prolonged server uptime.

### Fixed

- **Harmful Potion Detection Fix**:
  - Fixed friendly fire penetration vulnerability where `DamageListener.isHarmfulEffect` missed modern namespaced IDs for Mining Fatigue (`MINING_FATIGUE`) and Nausea (`NAUSEA`).
  - Fully added new combat debuffs and omen effects introduced in Minecraft 1.21+ / 26.x: Oozing (`OOZING`), Infested (`INFESTED`), Weaving (`WEAVING`), Wind Charged (`WIND_CHARGED`), Raid Omen (`RAID_OMEN`), and Trial Omen (`TRIAL_OMEN`).
  - Fixed critical flaw where `onPotionSplash` only inspected custom potion effects (`getEffects()`), causing vanilla-brewed splash potions (e.g. Instant Damage, Poison, Weakness) with empty custom effects to bypass friendly-fire protection; seamlessly integrated `PotionMeta.getBasePotionType()` (1.20.5+ / 26.3) and `PotionMeta.getBasePotionData()` (legacy fallback).
- **Area Effect Cloud Resilience**:
  - Added safe defensive wrapper around affected entity iterator removal in `onAreaEffectCloudApply`, preventing `UnsupportedOperationException` on specialized server optimizations or immutable collection implementations.

---

## [1.2.1] - 2026-09-19

### Added

- **Timezone & Duration Formatting**:
  - Added `timezone` key in `config.yml` (default: `GMT+8`), supporting standard timezone IDs (e.g. `Asia/Shanghai`, `UTC`, `America/New_York`, `Europe/London`).
  - `TimeUtil` now respects the server-configured timezone for all date formatting (`formatDate`), resolving discrepancies between client and server time displays.
  - Re-engineered `TimeUtil.formatDuration` with adaptive multi-tier unit display (days, hours, minutes, seconds), eliminating redundant `0h 0m` outputs and dynamically localizing units and spacing to each player's client language.
  - Standardized time displays across all console outputs, command feedback, and GUI lore items to localized duration strings rather than raw seconds.
- **Server Console Logging Multilingual Expansion**:
  - Expanded built-in server language support from 3 (`zh_CN`, `zh_TW`, `en_US`) to 7 languages, adding:
    - Japanese (`ja_JP`)
    - Russian (`ru_RU`)
    - German (`de_DE`)
    - Spanish (`es_ES`)
  - Added prefix fuzzy matching in `ServerLanguageManager` for `ja`, `ru`, `de`, and `es`.
  - Added high-quality translations for all 30 console log entries in `PluginLogger`.
  - Updated `config.yml` comments under `server_messages_language` to enumerate all 7 built-in language codes.
- **New Language Keys**:
  - Added new message keys across all three language files (`zh_CN.yml`, `zh_TW.yml`, `en_US.yml`):
    - `lang_not_initialized`: Shown when language manager is uninitialized.
    - `lang_usage_set`: Usage instructions for `/teamlang set`.
    - `usage_info`: Usage syntax for `/team info`.
    - `team_list_empty`: Console message when no teams exist on the server.
    - `team_list_header`: Paginated header for console team list with `{PAGE}` and `{TOTAL}` placeholders.
    - `team_list_item`: Formatted entry for console team list with placeholders.
    - `team_list_footer`: Pagination footer for console team list with `{NEXT_PAGE}` placeholder.
    - `chat_input_timeout`: Chat input timeout cancellation notification.
    - `chat_input_cancelled`: Chat input explicit cancellation notification.
    - `chat_input_suggest_hover`: Chat input shortcut suggestion hover tooltip.
    - `gui.list.empty_item_name` / `gui.list.empty_item_lore`: Placeholder item name and lore when the server team list is empty.
    - `gui.menu.ff_status_prefix`: Team control dashboard friendly fire status prefix text.

### Changed

- **Language Architecture Decoupling**:
  - Decoupled the previously monolithic language system into two single-responsibility managers:
    - `ServerLanguageManager`: Exclusively manages server-side environment and console logging language, loads `server_messages_language` from `config.yml`, and backs `PluginLogger`.
    - `ClientLanguageManager`: Exclusively manages player-facing localization, loads client default `language` from `config.yml`, manages language packs (`lang/*.yml`), player preferences persistence (`data/user_languages.yml`), client locale auto-detection (`Player.getLocale()`), fuzzy dialect matching, and `/teamlang` commands.
  - Completely removed the legacy `LanguageManager` class; `BalancedTeamPlugin` now exposes `getServerLanguageManager()` and `getClientLanguageManager()`.
- **Eliminated Command & Chat Hardcoded Strings**:
  - Completely replaced hardcoded Chinese strings in `TeamCommand` (`sendConsoleTeamList`, `handleInfo`, `handleChat`, `handleFriendlyFire`, `handleAlly`, `handleEnemy`), `TeamLangCommand`, `ChatInputManager`, `TeamMenuGui`, and `TeamListGui` with dynamic language pack lookups.
  - Fallback leader names and friendly-fire toggle statuses now resolve via `time_unit.unknown` and `status.on` / `status.off`.
- **Version Bump `1.2.0` → `1.2.1`**:
  - `pom.xml` version bumped to `1.2.1`.

### Fixed

- **Recipient-Aware Chat & Broadcast Localization**:
  - `ChatManager`: Dynamically formats the role token `{ROLE}` based on the individual recipient's language preferences.
  - `TeamCommand`: Fixed `handleChat`, `handleFriendlyFire`, `handleAlly`, and `handleEnemy` omitting the `Player` instance, which caused non-Chinese clients to receive server-default language strings.
  - `TeamManager`: Fixed team disband global broadcast (`team_disband_broadcast`) omitting the recipient player instance.
  - `PlayerListener`: Optimized chat lock mode validation to automatically eject players and notify them if they are no longer in a team or if global chat was disabled.
- **Dynamic Sender-Aware Localization**:
  - Fixed `getMessage(...)` and `getMessageList(...)` invocations across `TeamCommand`, `TeamAdminCommand`, and `TeamMsgCommand` that were missing the `sender`/`player` argument, ensuring all command responses strictly follow each player's active language preference rather than falling back to the server default.

---

## [1.2.0] - 2026-09-12

### Added

- **Diplomacy Truce & Peace System (`/team truce` / `/team peace`)**:
  - Full truce and peace sub-command system:
    - `/team truce <team>`: Send a truce/peace proposal to a declared enemy team.
    - `/team truce accept <team>`: Accept an incoming truce request.
    - `/team truce deny <team>`: Reject an incoming truce request.
    - `/team truce cancel <team>`: Cancel a pending truce proposal sent by your team.
    - `/team truce list`: View all pending outgoing and incoming truce requests.
  - **Post-War Protection**:
    - Both teams automatically enter post-war protection upon truce acceptance for `balance.post_war_protection_seconds` (default: 1800s).
    - Both teams are blocked from re-declaring war during the protection window.
    - `DamageListener` strictly blocks all melee attacks, projectiles (arrows/tridents), splash potions, and lingering area effect clouds between former enemies.
  - **Truce Request Timeout**: Added `balance.truce_request_timeout_seconds` (default: 1800s) to automatically expire unresolved requests.
  - **GUI Integration**:
    - Notification Center (`NotificationGui`): White Banner cards display truce requests with time remaining and protection duration, supporting one-click accept (left-click) or deny (right-click).
    - Enemy Management GUI (`EnemyManageGui`): Send peace requests with one click; active post-war protection displays a shield icon and live countdown badge.
- **Global Chat Toggle `enable_chat`**:
  - New boolean `enable_chat` key added under the `chat` node in `config.yml` (defaults to `true`). When set to `false`, all team chat entry points (`/team chat`, `/tc`, chat lock mode) are immediately disabled.
  - `ConfigManager` now exposes `isChatEnabled()` as a unified reader for this option.
  - `ChatManager.sendTeamChat()` enforces the global toggle as a final barrier before dispatching any message.
  - `PlayerListener` (chat lock mode), `TeamCommand.handleChat()`, and `TeamMsgCommand.onCommand()` all add a `isChatEnabled()` pre-check; players currently in chat lock mode are automatically ejected when the feature is disabled.
- **TC Command Independent Toggle `enable_tc_command`**:
  - `ConfigManager` now exposes `isTcCommandEnabled()` to read `chat.enable_tc_command`.
  - `TeamMsgCommand.onCommand()` checks this flag first; when disabled, `/tc`, `/tm`, and `/teammsg` are all blocked and a feedback message is sent to the player.
- **Console Team Chat Spy `console_listen_team_chat`**:
  - `ConfigManager` now exposes `isConsoleListenTeamChat()` to read `chat.console_listen_team_chat` (defaults to `true`).
  - `ChatManager.sendTeamChat()` prints the fully formatted chat line to the server console after member and admin dispatch, using the same `chat.format` template, enabling server-side logging.
- **New Language Keys**:
  - Added new message keys across all three language files (`zh_CN.yml`, `zh_TW.yml`, `en_US.yml`):
    - `chat_disabled`: Shown when a player attempts team chat while the feature is globally disabled.
    - `chat_tc_disabled`: Shown when a player uses `/tc` while the shortcut command is disabled.

### Changed

- **`ConfigManager` Method Fixes & Documentation**:
  - `getChatFormat()` default value restored: re-added the missing `{TEAM}` placeholder to match the `chat.format` default in `config.yml`.
  - `getSpyFormat()` default value corrected: `Spy` → `SPY` in hardcoded fallback, consistent with `config.yml`.
  - Both methods' Javadoc rewritten to standard HTML list format with full `@return` descriptions and complete placeholder documentation.
- **Version Bump `1.1.7` → `1.2.0`**:
  - `pom.xml` version updated to `1.2.0` to reflect the semantic version increment for new feature additions.

### Fixed

- **Security & Stability Hardening**:
  - Fixed PlaceholderAPI chat injection vulnerability by reordering evaluation stages.
  - Added synchronized double-checked locking for team size (`max_members`) and relation limits (`max_allies`, `max_enemies`).
  - Optimized `DamageListener` and `CrystalListener` precision across projectile, potion, and CPvP vectors.

---

## [1.1.7] - 2026-09-05

### Fixed

- **Patched PlaceholderAPI Chat Injection Vulnerability**:
  - Adjusted evaluation order in `ChatManager` to resolve PAPI variables on the message template prior to inserting the user's chat message, completely mitigating placeholder injection risks (such as unauthorized script evaluation or sensitive variable inspection).
  - Added strict permission checks (`balancedteam.chat.color`) for chat color code parsing and optimized `MessageUtil.sendRawMessage` to send pre-formatted messages directly without redundant regex color passes.
- **Enhanced Concurrency Boundary Protection for Team Size & Relations**:
  - Added pre-checks and synchronized double-checked locking inside `TeamManager.addMember` to strictly enforce `max_members` during concurrent member joins.
  - Added global boundary checks for `max_allies` and `max_enemies` as well as mutual exclusion checks inside `RelationManager` to prevent relation limit bypasses during concurrent requests.

---

## [1.1.6] - 2026-08-25

### Changed

- **Extended Cross-Version Compatibility (Minecraft 1.20.x - 26.2)**:
  - Enhanced version resilience and safe fallback in `SoundUtil` and damage listeners, ensuring smooth and flawless operation on Minecraft 1.20.x through 26.2 across Bukkit, Spigot, Paper, and Purpur platforms.
  - Updated documentation, guides, and version badges for modern Java 21+ environments.
- **Standardized English Error Messages for DAO Layer**:
  - Replaced all persistence error reports and `DatabaseException` messages across all DAO classes (`TeamDao`, `MemberDao`, `RelationDao`, `InviteDao`, `ApplicationDao`, `AllyRequestDao`) and `DatabaseManager` / `TeamManager` with clear, standardized English error messages for consistent logging and troubleshooting.

---

## [1.1.5] - 2026-08-22

### Added

- **Server Console Log Localization & Relative Path Support**:
  - `language` in `config.yml` now supports language codes (e.g. `en_US`, `zh_CN`, `zh_TW`), file names (e.g. `en_US.yml`), and relative paths (e.g. `lang/en_US.yml`, `lang/zh_CN.yml`).
  - Added `PluginLogger` server log internationalization manager. Changing the language relative path in `config.yml` automatically switches all server console output logs (startup banner, database initialization & table verification, data preloading, PlaceholderAPI hooking, reload, player join locale detection, safe shutdown, etc.) to the configured language.
  - Server logs are cleanly managed internally without requiring manual config entries for log strings.

---

## [1.1.4] - 2026-08-22

### Added

- **Configurable Time & Duration Units Localization (TimeUtil & Localization)**:
  - Added `time_unit` configuration section across all language files (`zh_CN.yml`, `zh_TW.yml`, `en_US.yml`), supporting custom formatting for `day`, `hour`, `minute`, `second`, and `unknown`.
  - Standardized default internal time units in `TimeUtil` to `d` / `h` / `min` / `sec` / `Unknown`.
  - Enhanced `TimeUtil` with `formatDuration(CommandSender sender, long seconds)` and `formatDate(CommandSender sender, Date date)` overloads to dynamically format units based on player's active locale.
  - Added `syncTimeUnits()` in `ConfigManager.load()` to automatically sync time units upon plugin load and reload.
- **Comprehensive Console Command Support**:
  - Lifted the blanket restriction on console execution for `/team` root command, enabling reasonable sub-commands for server console.
  - **Console Text Team List**: Console executing `/team list [page]` now outputs a paginated text list with team names, leaders, member counts, friendly fire status, and ally/enemy counts.
  - **Team Info Query via Console**: Console can now query detailed information for any team via `/team info <teamName>`.
  - **Language Commands for Console**: Console can now execute `/team lang` / `/teamlang` to view language status, list loaded languages, and reload language files.
  - **Console Help Support**: Executing `/team` or `/team help` from console now outputs the command help manual.
  - **Safe Isolation of Player-only Commands**: Interactive commands (such as create, invite, kick, disband, open GUI, etc.) are strictly checked and return `player_only` message when invoked by console.

### Fixed

- **Eliminated Hardcoded Localization Strings**:
  - Fixed hardcoded `"开启"` / `"关闭"` in `%balancedteam_friendly_fire_formatted%` / `%balancedteam_ff_formatted%` within `BalancedTeamExpansion`, now dynamically resolving `status.on` / `status.off` based on player locale.
  - Refactored date formatting across all GUIs (`TeamListGui`, `TeamDetailGui`, `MemberManageGui`) and commands to adapt to the sender's language configuration.

---

## [1.1.3] - 2026-08-22

### Added

- **Native PlaceholderAPI (PAPI) Expansion Support**:
  - Implemented `BalancedTeamExpansion` with `%balancedteam_<variable>%` identifier.
  - Added 35+ placeholders covering team identity, roles, friendly fire status, timestamps, member counts, online statuses, ally/enemy lists, global server stats, and dynamic target relation queries (e.g. `%balancedteam_relation_<player>%`, `%balancedteam_is_ally_<player>%`).
  - Safe default fallbacks for offline players and non-team members to prevent NullPointerExceptions.
- **Bidirectional Placeholder Utilities (PAPIUtil)**:
  - Enhanced `PAPIUtil` with safe expansion lifecycle management and exception-free string/list parsing.
  - Integrated PAPI parsing into team chat (`ChatManager`) and admin spy messages.
- **Soft Dependency & Documentation**:
  - Added `softdepend: [PlaceholderAPI]` to `plugin.yml`.
  - Published comprehensive 8-page English Wiki documentation on GitHub.

---

## [1.1.2] - 2026-08-21

### Added

- **Unified Confirmation GUI (ConfirmGui)**: Refactored and unified confirmation dialogs for all critical and destructive operations, supporting 6 distinct modes:
  - `DISBAND` (Disband Team): Leader-only, TNT confirmation icon.
  - `LEAVE` (Leave Team): Non-leader members, with leave cooldown validation.
  - `KICK` (Kick Member): Leaders and Officers, with strict role hierarchy (`canManage`) checks.
  - `TRANSFER` (Transfer Leadership): Leader-only, Golden Helmet icon, demoting former leader to Officer.
  - `PROMOTE` (Promote to Officer): Leader-only, Golden Chestplate icon, displaying officer permission details.
  - `DEMOTE` (Demote to Member): Leader-only, Iron Chestplate icon, with permission revocation warning.
- **Double Pre-Validation & Anti-Duplication Protection**: Strict permission and team status checks are performed both before opening the GUI and upon clicking confirm, protected by one-time click handlers against rapid concurrent clicks.

### Changed

- **Member Management GUI (MemberManageGui) Optimized**:
  - Right-click kick, Shift-click leadership transfer, and Left-click promote/demote now all prompt their respective `ConfirmGui` dialogs, preventing accidental clicks.
- **Management Commands (TeamCommand) Integration**:
  - `/team kick <player>`, `/team transfer <player>`, `/team promote <player>`, and `/team demote <player>` commands now seamlessly open the corresponding `ConfirmGui` after parameter validation, harmonizing CLI and GUI user experiences.
- **Dynamic Config Placeholder Integration**:
  - Replaced hardcoded 60s in the invite lore with dynamic `{TIMEOUT}` placeholder bound to `invite_timeout_seconds` in `config.yml`.
  - Replaced hardcoded 30s in friendly fire toggle tooltip with `{COOLDOWN}` placeholder bound to `friendly_fire_cooldown_seconds`.
- **Configuration & Localization Updates**:
  - `config.yml`: Translated all comments into professional and idiomatic English.
  - `zh_CN.yml` / `zh_TW.yml` / `en_US.yml`: Synchronized with `kick_confirm`, `transfer_confirm`, `promote_confirm`, and `demote_confirm` language entries.

---

## [1.1.1] - 2026-08-21

### Changed

- **New GUI Back Button Config**: Added `DETAIL_BACK_BUTTON_IN_TEAM` and `DETAIL_BACK_BUTTON_NOT_IN_TEAM` to `GuiConfigKeys`, enabling the Team Detail GUI to display different back button labels based on whether the player is in a team.
- **Updated Language Files**: Added `back_button_in_team` and `back_button_not_in_team` keys to `zh_TW.yml`, `zh_CN.yml`, and `en_US.yml`, mapping to "Back to Team Dashboard" / "Back to Team List" respectively.
- **Modified TeamDetailGui**: Dynamically resolves the correct back button key based on the player's team membership, and sets the back action at slot 31; returns to the Team Dashboard if in a team, or to the Team List if not.

---

## [1.1.0] - 2026-08-20

### Added

- **Client Locale Auto-Detection**: Automatically detects players' client language settings on login (`PlayerJoinEvent`) using `Player.getLocale()` and loads the corresponding language file.
- **Smart Fuzzy Matching & Fallback Algorithm**: Supports prefix-based fuzzy matching (e.g. `zh_HK` or `zh_TW` clients match `zh_*` packs if exact matches do not exist) before falling back to server default language in `config.yml` and hardcoded fallback.
- **In-Memory Language Caching**: Scans and caches all `plugins/BalancedTeam/lang/*.yml` files in memory on startup and reload, eliminating redundant disk I/O during gameplay and GUI rendering.
- **Dedicated Language Commands & Switching**:
  - Added `/teamlang` (aliases `/tlang`, `/btlang`, `/clanlang`) and `/team lang` commands.
  - Added `/teamlang list` to view all available language packs and current active status.
  - Added `/teamlang <locale>` (e.g. `/teamlang en_US`) for manual language override, and `/teamlang auto` to restore auto-detection.
  - Added `/teamlang reload` for admins to hot-reload all language packs.
  - Player language preferences are persisted in `data/user_languages.yml` across disconnects.
- **Full Multi-Language Localization for GUI & Messages**: All graphical GUI interfaces (Team Menu, Not-Joined Panel, Member Management, Notification Center, Team List, Detail View, Selection, and Confirmation dialogs) dynamically render titles and lores according to each player's effective language.

---

## [1.0.2] - 2026-08-20

### Added

- **Dynamic Invite Expiry Display**: The invite-received message now uses a `{TIMEOUT}` placeholder populated from `invite_timeout_seconds` in `config.yml`, replacing the previous hardcoded value.

### Changed

- **Invite Timeout Default Changed to 1 Hour**: `invite_timeout_seconds` in `config.yml` updated from `60` to `3600` (1 hour); the Java fallback in `ConfigManager` was synchronized. Server admins can freely adjust this value in `config.yml`.

---

## [1.0.1] - 2026-08-20

### Added

- **`RelationManager.denyAllyRequest()`**: New method to deny an alliance request — removes it from memory and database without creating an alliance.
- **Deny Notification Messages**: Added `ally_request_denied` and `ally_request_denied_notify` message keys to all three language files (`zh_CN` / `zh_TW` / `en_US`).

### Changed

- **Notification GUI Lore Updated**: The ally request item lore (leader view) now reads "Left-click to accept / Right-click to deny" instead of a single "Click to accept" hint, across all three language files.
- **README Simplified**: Removed verbose config YAML blocks and expanded command tables. Rewrote the bilingual README in a condensed format, reducing line count from 359 to 159.

### Fixed

- **Ally Request Right-Click Deny Not Working**: In the Notification Center GUI, right-clicking an alliance request had no effect — any click triggered the accept logic. Fixed to: left-click to accept, right-click to deny, with a denial notification sent to the requester's leader.

---

## [1.0.0] - 2026-08-20

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
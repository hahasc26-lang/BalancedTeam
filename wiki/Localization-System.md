# Localization and Multi-Language System

**BalancedTeam** includes an enterprise-grade, client-aware localization engine that automatically serves messages and GUI titles to players in their preferred language with zero performance impact.

---

## Key Localization Features

- **Architectural Separation**: Decoupled into two independent engines:
  - `ServerLanguageManager`: Exclusively manages server-side environment and console logging across 7 built-in languages, backed by `PluginLogger`.
  - `ClientLanguageManager`: Manages player-facing localization, language packs (`lang/*.yml`), client auto-detection, and `/teamlang` switching.
- **Automatic Client Locale Detection**: Reads each player's Minecraft client language setting via `Player.getLocale()` when they connect.
- **Smart Prefix Fuzzy Matching**: Intelligently maps regional variations (e.g. `en_GB`, `en_CA` $\to$ `en_US`; `zh_HK`, `zh_MO` $\to$ `zh_TW`/`zh_CN`).
- **Relative Path & Code Configuration**: `config.yml` accepts language codes (e.g. `en_US`, `zh_CN`), file names (e.g. `en_US.yml`), or relative paths (e.g. `lang/en_US.yml`, `lang/zh_CN.yml`).
- **Server Console Log Localization (7 Languages)**: Server console output logs (startup banner, database lifecycle, reload, player join locale detection, and safe shutdown) automatically switch to match `server_messages_language` in `config.yml`, supporting Simplified Chinese, Traditional Chinese, English, Japanese, Russian, German, and Spanish.
- **Timezone & Adaptive Duration Formatting**:
  - `TimeUtil` respects the server's configured `timezone` (e.g. `GMT+8`, `UTC`, `America/New_York`) for all timestamp formatting (`formatDate`).
  - `TimeUtil.formatDuration` uses an intelligent ladder algorithm to display multi-unit durations (days, hours, minutes, seconds) without redundant zero-units, dynamically adapting units and spacing to the player's active client language.
- **High-Performance Memory Caching**: All language files in `plugins/BalancedTeam/lang/*.yml` are fully pre-cached in memory on startup, ensuring $O(1)$ lookup time and zero disk I/O during gameplay.
- **Auto-Completion for Missing Keys**: If a custom language pack lacks newly introduced keys, the plugin automatically completes missing entries from the default pack and saves them without breaking existing translations.
- **Configurable Time Units**: Supports custom day, hour, minute, and second duration labels configured via `time_unit` in language files.
- **Player Overrides & Persistence**: Players can lock their language preference using `/teamlang <code|auto>`, which is persisted in `data/user_languages.yml` across logins.

---

## Locale Resolution Flowchart

When sending a message or opening a GUI for a player, the localization engine evaluates language selection in this priority:

```
                  Player Joins / Requests Message
                                │
                 Is Manual Preference Set?
                       ├── YES ──► Use Selected Language Pack
                       └── NO
                                │
                Read Client Locale (Player.getLocale())
                                │
                  Exact Match in lang/*.yml?
                       ├── YES ──► Use Exact Pack (e.g. en_US.yml)
                       └── NO
                                │
                Fuzzy Match by Prefix (zh_*, en_*)?
                       ├── YES ──► Use Closest Match (e.g. zh_TW.yml)
                       └── NO
                                │
                 Fallback to config.yml Default Language
                                │
                 Fallback to Built-in zh_CN.yml (Hard Fallback)
```

---

## Built-In Language Support

### Player Client Language Packs (`lang/*.yml`)

| Language Code | File Name | Display Name |
| :--- | :--- | :--- |
| `zh_CN` | `lang/zh_CN.yml` | 简体中文 (Simplified Chinese) |
| `zh_TW` | `lang/zh_TW.yml` | 繁體中文 (Traditional Chinese) |
| `en_US` | `lang/en_US.yml` | English (US) |

### Server Console Logging Languages (`server_messages_language`)

| Language Code | Language Name | Coverage |
| :--- | :--- | :--- |
| `zh_CN` | 简体中文 (Simplified Chinese) | All 30 console log points |
| `zh_TW` | 繁體中文 (Traditional Chinese) | All 30 console log points |
| `en_US` | English (US) | All 30 console log points |
| `ja_JP` | 日本語 (Japanese) | All 30 console log points |
| `ru_RU` | Русский (Russian) | All 30 console log points |
| `de_DE` | Deutsch (German) | All 30 console log points |
| `es_ES` | Español (Spanish) | All 30 console log points |

---

## Creating a Custom Language Pack

Adding support for a new language (e.g., Japanese `ja_JP`, Russian `ru_RU`, French `fr_FR`, German `de_DE`, Spanish `es_ES`) is straightforward:

1. Navigate to `plugins/BalancedTeam/lang/`.
2. Copy `en_US.yml` and rename it to your target locale (e.g., `ja_JP.yml` or `ru_RU.yml`).
3. Set the top-level display name at the start of the file:
   ```yaml
   language_name: "日本語" # Or "Русский", "Français", "Español", "Deutsch"
   ```
4. Translate the message strings as desired. All color codes (`&a`, `&b`, etc.) and hex color codes (`&#RRGGBB` and `<#RRGGBB>`) are supported.
5. Run `/teamlang reload` or `/teamadmin reload` in game to load your new language pack immediately without server restart.
6. Verify your pack is loaded using `/teamlang list`.

---

## Player Commands for Language Switching

- **`/teamlang`**: View active language code, client detection status, and help.
- **`/teamlang list`**: Displays all supported language packs loaded on the server.
- **`/teamlang <code|auto>`**: Manually switch to a specific pack (e.g. `/teamlang en_US`) or reset to client auto-detection (`/teamlang auto`).
- **`/teamlang reload`**: Hot-reload all language files from disk (Requires `balancedteam.admin`).

---

## Navigation

| [← Diplomacy & Combat Balance](https://github.com/hahasc26-lang/BalancedTeam/wiki/Diplomacy-and-Combat-Balance) | [Next: PlaceholderAPI Reference →](https://github.com/hahasc26-lang/BalancedTeam/wiki/PlaceholderAPI-Integration) |
| :--- | :--- |

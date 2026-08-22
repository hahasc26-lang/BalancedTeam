# Localization and Multi-Language System

**BalancedTeam** includes an enterprise-grade, client-aware localization engine that automatically serves messages and GUI titles to players in their preferred language with zero performance impact.

---

## 🌟 Key Localization Features

- **Automatic Client Locale Detection**: Reads each player's Minecraft client language setting via `Player.getLocale()` when they connect.
- **Smart Prefix Fuzzy Matching**: Intelligently maps regional variations (e.g. `en_GB`, `en_CA` $\to$ `en_US`; `zh_HK`, `zh_MO` $\to$ `zh_TW`/`zh_CN`).
- **High-Performance Memory Caching**: All language files in `plugins/BalancedTeam/lang/*.yml` are fully pre-cached in memory on startup, ensuring $O(1)$ lookup time and zero disk I/O during gameplay.
- **Auto-Completion for Missing Keys**: If a custom language pack lacks newly introduced keys, the plugin automatically completes missing entries from the default pack and saves them without breaking existing translations.
- **Player Overrides & Persistence**: Players can lock their language preference using `/teamlang <code|auto>`, which is persisted in `data/user_languages.yml` across logins.

---

## 🔄 Locale Resolution Flowchart

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

## 📦 Built-In Language Packs

| Language Code | File Name | Display Name |
| :--- | :--- | :--- |
| `zh_CN` | `lang/zh_CN.yml` | 简体中文 (Simplified Chinese) |
| `zh_TW` | `lang/zh_TW.yml` | 繁體中文 (Traditional Chinese) |
| `en_US` | `lang/en_US.yml` | English (US) |

---

## ✍️ Creating a Custom Language Pack

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

## 💬 Player Commands for Language Switching

- **`/teamlang`**: View active language code, client detection status, and help.
- **`/teamlang list`**: Displays all supported language packs loaded on the server.
- **`/teamlang <code|auto>`**: Manually switch to a specific pack (e.g. `/teamlang en_US`) or reset to client auto-detection (`/teamlang auto`).
- **`/teamlang reload`**: Hot-reload all language files from disk (Requires `balancedteam.admin`).

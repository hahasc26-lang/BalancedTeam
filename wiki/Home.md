# Welcome to the BalancedTeam Wiki

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.4%2B-brightgreen?style=flat-square&logo=minecraft" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Platform-Bukkit%20%7C%20Spigot%20%7C%20Paper-purple?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License">
</p>

**BalancedTeam** is a modern, high-concurrency Clan/Team management plugin designed specifically for **Anarchy**, **Faction PvP**, and **Survival Competitive** Minecraft servers. It balances competitive gameplay with robust anti-abuse mechanisms, full asynchronous database support, dynamic GUI interfaces, client-aware multi-language localization, and native PlaceholderAPI integration.

---

## Table of Contents

| Page | Description |
| :--- | :--- |
| **[Installation & Configuration](https://github.com/hahasc26-lang/BalancedTeam/wiki/Installation-and-Configuration)** | Requirements, installation steps, database setup (MySQL & SQLite), and full `config.yml` guide. |
| **[Commands & Permissions](https://github.com/hahasc26-lang/BalancedTeam/wiki/Commands-and-Permissions)** | Complete reference for all player and administrative commands, arguments, aliases, and permission nodes. |
| **[GUI Navigation Guide](https://github.com/hahasc26-lang/BalancedTeam/wiki/GUI-System)** | Visual overview of all in-game menus, member management, diplomacy consoles, and the 6-mode confirmation system. |
| **[Diplomacy & Combat Balance](https://github.com/hahasc26-lang/BalancedTeam/wiki/Diplomacy-and-Combat-Balance)** | Alliance & war systems, anti-zerg limits, friendly fire toggles, damage protection, and combat backstab prevention. |
| **[Localization & Languages](https://github.com/hahasc26-lang/BalancedTeam/wiki/Localization-System)** | Client locale auto-detection (`Player.getLocale()`), fuzzy matching, `/teamlang` overrides, and custom language packs. |
| **[PlaceholderAPI Reference](https://github.com/hahasc26-lang/BalancedTeam/wiki/PlaceholderAPI-Integration)** | Full documentation of all 35+ `%balancedteam_*%` placeholders, default fallbacks, and chat formatting examples. |
| **[Database & Technical Architecture](https://github.com/hahasc26-lang/BalancedTeam/wiki/Database-and-Architecture)** | Asynchronous DAO patterns, HikariCP connection pooling, database schemas, and performance benchmarks. |

---

## Key Feature Highlights

### 1. Game Balance & Anti-Abuse
- **Hard-capped Alliances and Enemies**: Prevents mega-alliances from monopolizing the server economy and combat.
- **Combat Friendly Fire Cooldowns**: Configurable cooldown timers on friendly fire toggling prevent players from abusing protection mid-fight.
- **Team-Hop Backstab Prevention**: Imposes a cooldown timer after leaving or disbanding a team before creating or joining another, stopping mid-combat betrayal exploits.

### 2. High-Concurrency Asynchronous Architecture
- **Zero Main-Thread Blocking**: All database operations (CRUD, invites, alliance requests, applications) run asynchronously via `CompletableFuture`.
- **Bidirectional In-Memory Caching**: Ultra-fast lookups (`O(1)`) by Team ID, Team Name, and Player UUID with concurrent hash maps.
- **HikariCP Connection Pool**: Enterprise-grade connection pooling with automatic reconnection and SQLite / MySQL dual-engine support.

### 3. Seamless Multi-Language System
- **Per-Player Automatic Locale Detection**: Dynamically inspects incoming packets to determine player language settings via `Player.getLocale()`.
- **Smart Fallback & Fuzzy Matching**: Intelligently handles sub-locales (e.g., `zh_HK` $\to$ `zh_CN`/`zh_TW`) before falling back to server default.
- **Player Overrides**: Players can lock their desired language via `/teamlang <code|auto>` without affecting others on the server.

### 4. Dynamic & Safe GUI System
- **Interactive Chest GUIs**: Intuitive management menus for teams, members, relations, invitations, and applications.
- **Unified 6-Mode Confirmation GUI (`ConfirmGui`)**: Destructive actions (Disband, Leave, Kick, Transfer, Promote, Demote) require explicit confirmation with double pre-validation and rapid-click debounce protection.

### 5. PlaceholderAPI Integration
- **35+ Built-in Placeholders**: Export detailed team data, roles, member counts, online statuses, creation dates, and dynamic relation evaluations (`%balancedteam_relation_<player>%`) to Tablist, Scoreboards, Holograms, and Chat plugins.

---

## Quick Start in 30 Seconds

1. Drop `BalancedTeam-x.x.x.jar` into your server's `plugins/` folder.
2. Start the server (SQLite is enabled by default with zero configuration required).
3. Type `/team` in-game to open the interactive team dashboard!

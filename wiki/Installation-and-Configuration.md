# Installation and Configuration Guide

This guide walks you through installing, configuring, and optimizing **BalancedTeam** for your server environment.

---

## System Requirements

| Requirement | Minimum / Recommended |
| :--- | :--- |
| **Java Version** | **Java 21** or higher |
| **Server Platform** | Bukkit, Spigot, Paper, Purpur (1.20.4+) |
| **Database** | Built-in SQLite (Zero-config) or MySQL 5.7+ / 8.0+ / MariaDB 10.3+ |
| **Optional Dependencies** | [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (v2.12.0+) |

---

## Installation Steps

1. **Download**: Grab the latest release `BalancedTeam-x.x.x.jar` from the GitHub releases page.
2. **Deploy**: Place the jar into your server's `plugins/` directory.
3. **First Boot**: Start or restart your server to automatically generate default configuration and language files:
   - `plugins/BalancedTeam/config.yml`
   - `plugins/BalancedTeam/lang/zh_CN.yml`
   - `plugins/BalancedTeam/lang/zh_TW.yml`
   - `plugins/BalancedTeam/lang/en_US.yml`
   - `plugins/BalancedTeam/teams.db` (if using SQLite)
4. **Configure**: Customize `config.yml` to fit your server's balance rules and database setup.
5. **Reload**: Execute `/teamadmin reload` or restart the server.

---

## Configuration Reference (`config.yml`)

Below is the complete reference for all configuration options available in `config.yml`:

```yaml
# ===================================================================
# BalancedTeam Plugin Configuration (config.yml)
# Supports asynchronous MySQL / SQLite data storage with high-concurrency optimization.
# ===================================================================

# Server Default Language Setting (corresponds to file names in plugins/BalancedTeam/lang/ without .yml)
# Built-in languages: zh_CN (Simplified Chinese), zh_TW (Traditional Chinese), en_US (English)
language: "en_US"

# Database Configuration
database:
  # Storage backend type: 'mysql' or 'sqlite'
  type: "mysql"
  
  # MySQL Connection Settings
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: "your_secure_password"
    ssl: false
    table_prefix: "bt_"
    
    # HikariCP Enterprise Connection Pool Settings
    pool:
      maximum_pool_size: 10
      minimum_idle: 5
      connection_timeout: 5000 # Milliseconds
      idle_timeout: 600000     # Milliseconds (10 minutes)
      max_lifetime: 1800000    # Milliseconds (30 minutes)
      
  # SQLite Connection Settings (Used when database.type is 'sqlite')
  sqlite:
    file: "teams.db"

# Date & Time Display Format (Follows standard Java SimpleDateFormat pattern)
date_format: "yyyy-MM-dd HH:mm:ss"

# Game Balance & Anti-Abuse Configuration
balance:
  # Maximum number of members allowed per team (prevents massive monopolistic factions)
  max_members: 10
  
  # Maximum number of allied teams allowed per team (prevents mega-alliances dominating the server)
  max_allies: 3
  
  # Maximum number of marked enemy teams allowed per team
  max_enemies: 10
  
  # Whether players/leaders are allowed to toggle team friendly fire (true = toggleable, false = locked to enabled)
  allow_friendly_fire_toggle: true
  
  # Default friendly fire status when a new team is created (false = teammates protected, true = teammates can hurt each other)
  friendly_fire_default: false
  
  # Whether damage protection applies between allied teams (false = allies protected, true = allies can hurt each other)
  ally_friendly_fire: false
  
  # Cooldown in seconds for toggling friendly fire (prevents rapid toggling to evade accidental hits during PvP)
  friendly_fire_cooldown_seconds: 30
  
  # Cooldown in seconds after leaving/changing a team before joining or creating another team (prevents backstabbing during combat)
  leave_team_cooldown_seconds: 60
  
  # Minimum character length for team names
  name_min_length: 2
  
  # Maximum character length for team names
  name_max_length: 16
  
  # Regular expression pattern for validating team names (supports alphanumeric characters, underscores, and Unicode/Chinese)
  name_regex: "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$"
  
  # Team invite expiration timeout in seconds
  invite_timeout_seconds: 3600
  
  # Alliance request expiration timeout in seconds
  ally_request_timeout_seconds: 3600

# Team Chat Configuration
chat:
  # Team chat format: {TEAM}, {ROLE}, {PLAYER}, {MESSAGE} (PlaceholderAPI is also supported)
  format: "&8[&b{TEAM}&8] &7[{ROLE}&7] &f{PLAYER}&7: &b{MESSAGE}"
  
  # Admin spy chat format for monitoring team communications
  spy_format: "&8[&cSPY&8] &7[&e{TEAM}&7] &7[{ROLE}&7] &f{PLAYER}&7: &f{MESSAGE}"
  
  # Whether the quick shortcut command /tc <message> is enabled
  enable_tc_command: true
```

---

## Database Setup Details

### Option A: SQLite (Default / Single-Server Setup)
- **Zero Configuration Required**: Simply set `database.type: "sqlite"`.
- The database file is created at `plugins/BalancedTeam/teams.db`.
- Supports full WAL (Write-Ahead Logging) mode and concurrent multithreaded reads.

### Option B: MySQL / MariaDB (Recommended for High Concurrency & Networks)
- Set `database.type: "mysql"`.
- Create a dedicated database in MySQL:
  ```sql
  CREATE DATABASE minecraft CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- Configure the user, password, host, and database name in `config.yml`.
- All tables (`bt_teams`, `bt_members`, `bt_relations`, `bt_invites`, `bt_ally_requests`, `bt_applications`) are automatically generated and migrated on boot.

# BalancedTeam

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.4%2B-brightgreen?style=flat-square&logo=minecraft" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Build-Maven-blue?style=flat-square&logo=apache-maven" alt="Maven">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Platform-Bukkit%20%7C%20Spigot%20%7C%20Paper-purple?style=flat-square" alt="Platform">
</p>

<p align="center">
  <b>中文</b> | <a href="#english">English</a>
</p>

专为**无规则 (Anarchy)** 及**生存竞技**服务器设计的高性能团队插件，兼容 Bukkit / Spigot / Paper。

---

## 功能

- **团队管理** — 创建、解散、邀请、踢人、转让队长、设置副队长、申请入队
- **外交系统** — 结盟 / 解盟、宣战 / 求和，均有数量上限防止全服联合
- **平衡机制** — 友伤开关（带冷却）、同盟保护、退队冷却防战斗背刺
- **团队聊天** — `/tc` 专属频道，支持锁定模式；OP 可监听所有频道
- **图形化 GUI** — 团队菜单、成员管理、全服列表、操作确认界面
- **双存储引擎** — MySQL（生产推荐）/ SQLite（开箱即用），HikariCP 连接池
- **PlaceholderAPI 占位符支持** — 原生集成 35+ 个占位符变量，支持在计分板、Tab 列表、称号、聊天等中调用团队与玩家数据
- **自动多语言系统** — 读取 Minecraft 客户端 Locale 自动检测语言，支持前缀模糊匹配（如 `zh_HK` 自动匹配 `zh_CN`/`zh_TW`）、服务端回退与全量内存缓存；内置 `zh_CN` / `zh_TW` / `en_US`，支持玩家使用 `/teamlang` 自主切换或恢复自动
- **完整官方 Wiki** — 详细的 [GitHub Wiki 文档](https://github.com/hahasc26-lang/BalancedTeam/wiki) 涵盖安装配置、指令权限、GUI、战斗平衡与技术架构

---

## 安装

1. 从 [Releases](../../releases) 下载最新 `BalancedTeam-x.x.x.jar`
2. 放入服务器 `plugins/` 目录并重启
3. 编辑 `plugins/BalancedTeam/config.yml`（默认使用 SQLite，无需额外配置）
4. 重启或 `/reload confirm` 生效

**自行构建：**
```bash
git clone https://github.com/hahasc26-lang/BalancedTeam.git
cd BalancedTeam
mvn clean package          # 标准构建
mvn clean package -Pfatjar # Fat-Jar（兼容旧版服务端）
```

---

## 指令

**`/team`**（别名：`/t` `/clan` `/bt`）

| 指令 | 说明 |
|------|------|
| `/team` | 打开团队菜单 |
| `/team create <队名>` | 创建团队 |
| `/team invite/kick/leave` | 成员管理 |
| `/team promote/demote/transfer` | 职位管理 |
| `/team ally/unally/enemy/peace` | 外交操作 |
| `/team apply/accept/deny` | 申请与邀请 |
| `/team ff` | 切换友伤 |
| `/team info/list` | 查看信息 |
| `/team lang` | 语言设置与切换 |

**`/teamlang`**（别名：`/tlang` `/btlang` `/clanlang`）

| 指令 | 说明 |
|------|------|
| `/teamlang` | 查看当前语言状态与帮助 |
| `/teamlang list` | 查看服务器支持的所有语言包列表 |
| `/teamlang <代码>` | 手动切换指定语言（如 `/teamlang en_US`） |
| `/teamlang auto` | 恢复为跟随客户端自动检测语言 |
| `/teamlang reload` | 重载所有语言包（限管理员） |

**`/teamadmin`**（别名：`/ta`）— 强制解散、踢人、监听聊天、重载配置

**`/teammsg`**（别名：`/tc` `/tm`）— 发送或锁定团队聊天

---

## 权限

| 权限 | 默认 | 说明 |
|------|------|------|
| `balancedteam.use` | 所有人 | 基础指令 |
| `balancedteam.admin` | OP | 管理员指令与语言重载 |
| `balancedteam.admin.spy` | OP | 监听团队聊天 |

---

## PlaceholderAPI 占位符变量

插件原生集成 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)，前缀为 `%balancedteam_<变量名>%`：

| 占位符变量 | 说明 |
|------------|------|
| `%balancedteam_in_team%` / `%balancedteam_has_team%` | 是否在队伍中（`true` / `false`） |
| `%balancedteam_name%` / `%balancedteam_team_name%` | 所在团队名称 |
| `%balancedteam_id%` / `%balancedteam_team_id%` | 团队数据库 ID |
| `%balancedteam_tag%` | 团队标签格式（如 `[战队名]`） |
| `%balancedteam_leader%` / `%balancedteam_leader_name%` | 队长名称 |
| `%balancedteam_leader_uuid%` | 队长 UUID |
| `%balancedteam_is_leader%` | 是否为队长（`true` / `false`） |
| `%balancedteam_is_officer%` | 是否为管理员及以上（`true` / `false`） |
| `%balancedteam_role%` | 职位展示名（如 `队长`、`管理员`、`队员`） |
| `%balancedteam_role_raw%` | 职位枚举名（`LEADER` / `OFFICER` / `MEMBER` / `NONE`） |
| `%balancedteam_role_level%` | 职位等级数字（`3` / `2` / `1` / `0`） |
| `%balancedteam_description%` | 团队简介公告 |
| `%balancedteam_friendly_fire%` / `%balancedteam_ff%` | 友伤开关状态（`true` / `false`） |
| `%balancedteam_friendly_fire_formatted%` | 友伤展示状态（`开启` / `关闭`） |
| `%balancedteam_created_at%` | 团队创建时间 |
| `%balancedteam_joined_at%` | 玩家入队时间 |
| `%balancedteam_members%` / `%balancedteam_member_count%` | 团队当前人数 |
| `%balancedteam_max_members%` | 团队人数上限 |
| `%balancedteam_online%` / `%balancedteam_online_count%` | 团队在线人数 |
| `%balancedteam_allies%` / `%balancedteam_ally_count%` | 盟友队伍数量 |
| `%balancedteam_max_allies%` | 盟友队伍上限 |
| `%balancedteam_enemies%` / `%balancedteam_enemy_count%` | 敌对队伍数量 |
| `%balancedteam_max_enemies%` | 敌对队伍上限 |
| `%balancedteam_allies_list%` | 盟友队伍名称列表（逗号分隔） |
| `%balancedteam_enemies_list%` | 敌对队伍名称列表（逗号分隔） |
| `%balancedteam_total_teams%` | 全服团队总数 |
| `%balancedteam_total_members%` | 全服已加入团队的总玩家数 |
| `%balancedteam_relation_<玩家名>%` | 与目标玩家的关系（`SAME_TEAM` / `ALLY` / `ENEMY` / `NONE`） |
| `%balancedteam_relation_team_<队名>%` | 与目标团队的关系（`SAME_TEAM` / `ALLY` / `ENEMY` / `NONE`） |
| `%balancedteam_is_ally_<玩家名>%` | 目标玩家是否为同盟（`true` / `false`） |
| `%balancedteam_is_enemy_<玩家名>%` | 目标玩家是否为敌对（`true` / `false`） |
| `%balancedteam_is_same_team_<玩家名>%` | 目标玩家是否为同队（`true` / `false`） |
| `%balancedteam_team_leader_<队名>%` | 指定队伍的队长名 |
| `%balancedteam_team_members_<队名>%` | 指定队伍的人数 |
| `%balancedteam_team_online_<队名>%` | 指定队伍的在线人数 |
| `%balancedteam_team_desc_<队名>%` | 指定队伍的简介 |
| `%balancedteam_team_created_<队名>%` | 指定队伍的创建时间 |
| `%balancedteam_team_ff_<队名>%` | 指定队伍的友伤状态 |
| `%balancedteam_team_exists_<队名>%` | 指定队伍是否存在（`true` / `false`） |

---

## 许可证

[MIT License](LICENSE)

---

<a name="english"></a>

# BalancedTeam — English

<p align="center">
  <a href="#balancedteam">中文</a> | <b>English</b>
</p>

A high-performance Clan/Team plugin for **Anarchy** and **survival-competitive** Minecraft servers. Compatible with Bukkit / Spigot / Paper.

---

## Features

- **Team Management** — Create, disband, invite, kick, promote, transfer leadership, apply to join
- **Diplomacy** — Ally / Unally, War / Peace with configurable caps to prevent server-wide coalitions
- **Balance Mechanics** — Friendly fire toggle (with cooldown), ally protection, leave cooldown against backstabs
- **Team Chat** — `/tc` channel with lock mode; OP spy mode to monitor all channels
- **Graphical GUI** — Team menu, member management, server-wide list, confirmation dialogs
- **Dual Storage** — MySQL (production) / SQLite (zero-config), backed by HikariCP
- **PlaceholderAPI Support** — Full PlaceholderAPI integration with dozens of team and player placeholders
- **Auto Multi-Language System** — Auto-detects Minecraft client locale via `Player.getLocale()`, with smart fuzzy matching (e.g. `zh_HK` falls back to `zh_CN`/`zh_TW`), server fallbacks, and in-memory caching. Built-in `zh_CN` / `zh_TW` / `en_US`; players can override or reset via `/teamlang`
- **Comprehensive Official Wiki** — Complete [GitHub Wiki Documentation](https://github.com/hahasc26-lang/BalancedTeam/wiki) covering installation, configuration, commands, GUI guide, and combat mechanics

---

## Installation

1. Download the latest `BalancedTeam-x.x.x.jar` from [Releases](../../releases)
2. Drop it into your server's `plugins/` folder and restart
3. Edit `plugins/BalancedTeam/config.yml` (defaults to SQLite — no extra setup needed)
4. Restart or `/reload confirm` to apply

**Build from source:**
```bash
git clone https://github.com/hahasc26-lang/BalancedTeam.git
cd BalancedTeam
mvn clean package          # Standard build
mvn clean package -Pfatjar # Fat-Jar (for legacy servers)
```

---

## Commands

**`/team`** (aliases: `/t` `/clan` `/bt`)

| Command | Description |
|---------|-------------|
| `/team` | Open team menu |
| `/team create <name>` | Create a team |
| `/team invite/kick/leave` | Member management |
| `/team promote/demote/transfer` | Role management |
| `/team ally/unally/enemy/peace` | Diplomacy |
| `/team apply/accept/deny` | Applications & invitations |
| `/team ff` | Toggle friendly fire |
| `/team info/list` | View info |
| `/team lang` | Language settings |

**`/teamlang`** (aliases: `/tlang` `/btlang` `/clanlang`)

| Command | Description |
|---------|-------------|
| `/teamlang` | View current language status and help |
| `/teamlang list` | View all supported language packs |
| `/teamlang <code>` | Manually switch language (e.g. `/teamlang en_US`) |
| `/teamlang auto` | Reset to client auto-detection mode |
| `/teamlang reload` | Reload all language files (Admin only) |

**`/teamadmin`** (alias: `/ta`) — Force disband, kick, spy on chat, reload configs

**`/teammsg`** (aliases: `/tc` `/tm`) — Send or lock team chat

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `balancedteam.use` | Everyone | Basic commands |
| `balancedteam.admin` | OP | Admin commands and language reload |
| `balancedteam.admin.spy` | OP | Monitor team chats |

---

## PlaceholderAPI Placeholders

Native [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) expansion with `%balancedteam_<placeholder>%`:

| Placeholder | Description |
|-------------|-------------|
| `%balancedteam_in_team%` / `%balancedteam_has_team%` | Whether player is in a team (`true` / `false`) |
| `%balancedteam_name%` / `%balancedteam_team_name%` | Player's team name |
| `%balancedteam_id%` / `%balancedteam_team_id%` | Player's team ID |
| `%balancedteam_tag%` | Formatted team tag (e.g. `[TeamName]`) |
| `%balancedteam_leader%` / `%balancedteam_leader_name%` | Leader username |
| `%balancedteam_leader_uuid%` | Leader UUID |
| `%balancedteam_is_leader%` | Whether player is team leader (`true` / `false`) |
| `%balancedteam_is_officer%` | Whether player is officer or leader (`true` / `false`) |
| `%balancedteam_role%` | Role display name (e.g. `Leader`, `Officer`, `Member`) |
| `%balancedteam_role_raw%` | Role enum name (`LEADER` / `OFFICER` / `MEMBER` / `NONE`) |
| `%balancedteam_role_level%` | Role level number (`3` / `2` / `1` / `0`) |
| `%balancedteam_description%` | Team description |
| `%balancedteam_friendly_fire%` / `%balancedteam_ff%` | Friendly fire status (`true` / `false`) |
| `%balancedteam_friendly_fire_formatted%` | Formatted friendly fire status |
| `%balancedteam_created_at%` | Team creation time |
| `%balancedteam_joined_at%` | Player team join time |
| `%balancedteam_members%` / `%balancedteam_member_count%` | Current member count |
| `%balancedteam_max_members%` | Max allowed members |
| `%balancedteam_online%` / `%balancedteam_online_count%` | Online member count |
| `%balancedteam_allies%` / `%balancedteam_ally_count%` | Allied teams count |
| `%balancedteam_max_allies%` | Max allowed allies |
| `%balancedteam_enemies%` / `%balancedteam_enemy_count%` | Enemy teams count |
| `%balancedteam_max_enemies%` | Max allowed enemies |
| `%balancedteam_allies_list%` | List of ally team names |
| `%balancedteam_enemies_list%` | List of enemy team names |
| `%balancedteam_total_teams%` | Total teams on server |
| `%balancedteam_total_members%` | Total players in teams across server |
| `%balancedteam_relation_<player>%` | Relation with target player (`SAME_TEAM` / `ALLY` / `ENEMY` / `NONE`) |
| `%balancedteam_relation_team_<teamName>%` | Relation with target team (`SAME_TEAM` / `ALLY` / `ENEMY` / `NONE`) |
| `%balancedteam_is_ally_<player>%` | Whether target player is ally (`true` / `false`) |
| `%balancedteam_is_enemy_<player>%` | Whether target player is enemy (`true` / `false`) |
| `%balancedteam_is_same_team_<player>%` | Whether target player is teammate (`true` / `false`) |
| `%balancedteam_team_leader_<teamName>%` | Leader of specific team |
| `%balancedteam_team_members_<teamName>%` | Member count of specific team |
| `%balancedteam_team_online_<teamName>%` | Online member count of specific team |
| `%balancedteam_team_desc_<teamName>%` | Description of specific team |
| `%balancedteam_team_created_<teamName>%` | Creation time of specific team |
| `%balancedteam_team_ff_<teamName>%` | Friendly fire of specific team |
| `%balancedteam_team_exists_<teamName>%` | Whether specific team exists (`true` / `false`) |

---

## License

[MIT License](LICENSE)
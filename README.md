# BalancedTeam

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.x--26.2-brightgreen?style=flat-square&logo=minecraft" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Build-Maven-blue?style=flat-square&logo=apache-maven" alt="Maven">
  <img src="https://img.shields.io/badge/PlaceholderAPI-Supported-blueviolet?style=flat-square" alt="PlaceholderAPI">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Platform-Bukkit%20%7C%20Spigot%20%7C%20Paper-purple?style=flat-square" alt="Platform">
</p>

<p align="center">
  <a href="https://ko-fi.com/X5Z326V65O"><img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="ko-fi"></a>
</p>

- **Modrinth**:[https://modrinth.com/plugin/balancedteam](https://modrinth.com/plugin/balancedteam)
- **Spigot**:[https://www.spigotmc.org/resources/balancedteam.138143/](https://www.spigotmc.org/resources/balancedteam.138143/)

---

<p align="center">
  <a href="#chinese">中文</a> | <b>English</b>
</p>

<a name="english"></a>


A high-performance Clan/Team plugin for **Anarchy** and **survival-competitive** Minecraft servers. Compatible with Bukkit / Spigot / Paper with native **PlaceholderAPI** integration.

---

## Features

- **Team Management** — Create, disband, invite, kick, promote, transfer leadership, apply to join
- **Diplomacy** — Ally / Unally, War / Peace with configurable caps to prevent server-wide coalitions
- **Balance Mechanics** — Friendly fire toggle (with cooldown), ally protection, leave cooldown against backstabs
- **Team Chat** — `/tc` channel with lock mode; OP spy mode to monitor all channels (can disable)
- **Graphical GUI** — Team menu, member management, server-wide list, confirmation dialogs
- **Dual Storage** — MySQL (production) / SQLite (zero-config), backed by HikariCP
- **PlaceholderAPI Support** — Full PlaceholderAPI integration with dozens of team and player placeholders
- **Auto Multi-Language System** — Auto-detects Minecraft client locale via `Player.getLocale()`, with smart fuzzy matching (e.g. `zh_HK` falls back to `zh_CN`/`zh_TW`), server fallbacks, and in-memory caching. Built-in `zh_CN` / `zh_TW` / `en_US`; players can override or reset via `/teamlang`; supports language codes or relative paths (e.g. `lang/en_US.yml` or `en_US`) in `config.yml`, which automatically adapts all server console output logs (startup banner, database, cache preloading, lifecycle, etc.) to the configured language
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

<details>
<summary><b>Click to expand / collapse Command List</b></summary>

<br>

**`/team`** (aliases: `/t` `/clan` `/bt`)

| Command | Description | Permission / Role |
|---------|-------------|-------------------|
| `/team` (or `/team menu`) | Open interactive GUI dashboard | Everyone |
| `/team create <name>` | Create a new team | Everyone (No team) |
| `/team disband` | Disband your team (requires confirmation) | Leader |
| `/team invite <player>` | Invite a player to join | Leader / Officer |
| `/team kick <player>` | Kick a member from your team | Leader / Officer |
| `/team leave` | Leave current team (subject to cooldown) | Member / Officer |
| `/team promote <player>` | Promote a member to officer | Leader |
| `/team demote <player>` | Demote an officer to member | Leader |
| `/team transfer <player>` | Transfer team leadership | Leader |
| `/team apply <team>` | Apply to join a team | Everyone (No team) |
| `/team accept <target>` | Accept incoming member invite or alliance request | Leader / Officer |
| `/team deny <target>` | Reject incoming member invite or alliance request | Leader / Officer |
| `/team ally <add\|accept\|remove> <team>` | Send, accept, or break formal team alliance | Leader / Officer |
| `/team enemy <add\|remove> <team>` | Declare hostility or remove enemy status | Leader / Officer |
| `/team truce <team>` | Send truce/peace request to an enemy team | Leader / Officer |
| `/team truce accept <team>` | Accept truce request & enter post-war protection | Leader / Officer |
| `/team truce deny <team>` | Reject incoming truce request | Leader / Officer |
| `/team truce cancel <team>` | Cancel outgoing truce request | Leader / Officer |
| `/team truce list` | View active incoming and outgoing truce requests | Leader / Officer |
| `/team ff` | Toggle friendly fire protection (subject to cooldown) | Leader / Officer |
| `/team chat [message]` | Send team chat message or toggle chat lock mode | Team Members |
| `/team info [team]` | View details of your team or another team | Everyone |
| `/team members [page]` | View member list and roles | Everyone |
| `/team list [page]` | Browse server team list GUI or chat | Everyone |
| `/team lang [code\|auto\|list]` | View or change personal client language preference | Everyone |

**`/teamlang`** (aliases: `/tlang` `/btlang` `/clanlang`)

| Command | Description | Permission |
|---------|-------------|------------|
| `/teamlang` | View current language code, detection mode, and help | `balancedteam.use` |
| `/teamlang list` | View all supported language packs loaded on server | `balancedteam.use` |
| `/teamlang <code>` | Manually switch to specific language (e.g. `en_US`, `zh_CN`) | `balancedteam.use` |
| `/teamlang auto` | Reset to automatic Minecraft client locale detection | `balancedteam.use` |
| `/teamlang reload` | Hot-reload all language configuration files | `balancedteam.admin` |

**`/teamadmin`** (aliases: `/ta` `/btadmin`)

| Command | Description | Permission |
|---------|-------------|------------|
| `/teamadmin disband <team>` | Force disband any team immediately | `balancedteam.admin` |
| `/teamadmin kick <player>` | Force remove a player from their team | `balancedteam.admin` |
| `/teamadmin spy` | Toggle admin spy mode to monitor all team chats | `balancedteam.admin.spy` |
| `/teamadmin reload` | Hot-reload `config.yml` and all language files | `balancedteam.admin` |
| `/teamadmin help` | Display administrator command help | `balancedteam.admin` |

**`/teammsg`** (aliases: `/tc` `/tm` `/teamchat`)

| Command | Description | Permission |
|---------|-------------|------------|
| `/teammsg <message>` | Send a quick message to your team channel | `balancedteam.use` |
| `/teammsg` | Toggle team chat lock mode (all chats route to team) | `balancedteam.use` |

</details>

---

## Permissions

<details>
<summary><b>Click to expand / collapse Permissions List</b></summary>

<br>

| Permission | Default | Description |
|------------|---------|-------------|
| `balancedteam.use` | Everyone | Basic commands |
| `balancedteam.admin` | OP | Admin commands and language reload |
| `balancedteam.admin.spy` | OP | Monitor team chats |

</details>

---

## PlaceholderAPI Placeholders

<details>
<summary><b>Click to expand / collapse Placeholders & Configuration Examples</b></summary>

<br>

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

**Configuration Examples (e.g. TAB / Scoreboards / Chat Format):**
```yaml
# Chat format example
format: '{balancedteam_tag} &7[{balancedteam_role}&7] &f{DISPLAYNAME}&7: &f{MESSAGE}'

# Scoreboard example
lines:
  - '&b&lMY TEAM'
  - '&7Team: &f%balancedteam_name%'
  - '&7Role: &e%balancedteam_role%'
  - '&7Members: &a%balancedteam_online%&7/&f%balancedteam_members%'
  - '&7Allies: &b%balancedteam_allies%&7/&f%balancedteam_max_allies%'
```

</details>

---

## Support

If you find this project helpful, consider buying me a coffee!

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/X5Z326V65O)

---

## License

[MIT License](LICENSE)

---

<a name="chinese"></a>

<p align="center">
  <b>中文</b> | <a href="#english">English</a>
</p>

专为**无规则 (Anarchy)** 及**生存竞技**服务器设计的高性能团队插件，兼容 Bukkit / Spigot / Paper，原生支持 **PlaceholderAPI**。

---

## 功能

- **团队管理** — 创建、解散、邀请、踢人、转让队长、设置副队长、申请入队
- **外交系统** — 结盟 / 解盟、宣战 / 求和，均有数量上限防止全服联合
- **平衡机制** — 友伤开关（带冷却）、同盟保护、退队冷却防战斗背刺
- **团队聊天** — `/tc` 专属频道，支持锁定模式；OP 可监听所有频道 (可以关闭全服聊天和监听)
- **图形化 GUI** — 团队菜单、成员管理、全服列表、操作确认界面
- **双存储引擎** — MySQL（生产推荐）/ SQLite（开箱即用），HikariCP 连接池
- **PlaceholderAPI 占位符支持** — 原生集成 35+ 个占位符变量，支持在计分板、Tab 列表、称号、聊天等中调用团队与玩家数据
- **自动多语言系统** — 读取 Minecraft 客户端 Locale 自动检测语言，支持前缀模糊匹配（如 `zh_HK` 自动匹配 `zh_CN`/`zh_TW`）、服务端回退与全量内存缓存；内置 `zh_CN` / `zh_TW` / `en_US`，支持玩家使用 `/teamlang` 自主切换或恢复自动；`config.yml` 支持通过语言代码或相对路径（如 `lang/en_US.yml`）配置服务端默认语言，修改后服务端控制台输出日志（启动横幅、数据库连接、缓存预热、生命周期等）将同步自适应切换
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

<details>
<summary><b>点击展开 / 折叠完整指令列表</b></summary>

<br>

**`/team`**（别名：`/t` `/clan` `/bt`）

| 指令 | 说明 | 权限 / 角色要求 |
|------|------|-----------------|
| `/team`（或 `/team menu`） | 打开团队交互式 GUI 控制台 | 所有人 |
| `/team create <队名>` | 创建新团队 | 所有人（无队伍） |
| `/team disband` | 解散当前团队（二次确认保护） | 队长 |
| `/team invite <玩家>` | 邀请在线玩家加入团队 | 队长 / 管理员 |
| `/team kick <玩家>` | 踢出指定团队成员 | 队长 / 管理员 |
| `/team leave` | 退出当前团队（受退队冷却保护） | 普通成员 / 管理员 |
| `/team promote <玩家>` | 晋升普通成员为管理员 | 队长 |
| `/team demote <玩家>` | 将管理员降级为普通成员 | 队长 |
| `/team transfer <玩家>` | 将队长职位转让给其他成员 | 队长 |
| `/team apply <队伍>` | 申请加入指定团队 | 所有人（无队伍） |
| `/team accept <目标>` | 接受入队邀请或同盟申请 | 队长 / 管理员 |
| `/team deny <目标>` | 拒绝入队邀请或同盟申请 | 队长 / 管理员 |
| `/team ally <add\|accept\|remove> <队伍>` | 发起同盟申请、接受同盟或解除盟约 | 队长 / 管理员 |
| `/team enemy <add\|remove> <队伍>` | 标记敌对宣战或撤销敌对关系 | 队长 / 管理员 |
| `/team truce <队伍>` | 向敌对团队发起停战求和请求（别名 `/team peace`） | 队长 / 管理员 |
| `/team truce accept <队伍>` | 接受停战请求并进入战后保护期 | 队长 / 管理员 |
| `/team truce deny <队伍>` | 拒绝敌对团队的求和请求 | 队长 / 管理员 |
| `/team truce cancel <队伍>` | 撤销己方发出的求和请求 | 队长 / 管理员 |
| `/team truce list` | 查看等待中与收到的停战求和列表 | 队长 / 管理员 |
| `/team ff` | 切换队伍友伤开关（受切换冷却保护） | 队长 / 管理员 |
| `/team chat [消息]` | 发送队伍聊天或切换聊天锁定模式 | 团队成员 |
| `/team info [队伍]` | 查看自己或指定团队的详细信息 | 所有人 |
| `/team members [页码]` | 查看团队成员及职务列表 | 所有人 |
| `/team list [页码]` | 查看全服团队列表 GUI 或聊天栏列表 | 所有人 |
| `/team lang [代码\|auto\|list]` | 查看或切换玩家个人客户端多语言偏好 | 所有人 |

**`/teamlang`**（别名：`/tlang` `/btlang` `/clanlang`）

| 指令 | 说明 | 权限 |
|------|------|------|
| `/teamlang` | 查看当前生效语言、检测模式与帮助 | `balancedteam.use` |
| `/teamlang list` | 查看服务器加载支持的全部语言包列表 | `balancedteam.use` |
| `/teamlang <代码>` | 手动切换为指定语言（如 `en_US`、`zh_CN`） | `balancedteam.use` |
| `/teamlang auto` | 恢复跟随 Minecraft 客户端自动检测匹配 | `balancedteam.use` |
| `/teamlang reload` | 热重载所有语言配置文件 | `balancedteam.admin` |

**`/teamadmin`**（别名：`/ta` `/btadmin`）

| 指令 | 说明 | 权限 |
|------|------|------|
| `/teamadmin disband <队伍>` | 强制解散指定团队 | `balancedteam.admin` |
| `/teamadmin kick <玩家>` | 强制将指定玩家移出团队 | `balancedteam.admin` |
| `/teamadmin spy` | 开启/关闭管理监听模式（实时监看全服队伍聊天） | `balancedteam.admin.spy` |
| `/teamadmin reload` | 热重载 `config.yml` 与所有语言文件 | `balancedteam.admin` |
| `/teamadmin help` | 查看管理员维护指令帮助 | `balancedteam.admin` |

**`/teammsg`**（别名：`/tc` `/tm` `/teamchat`）

| 指令 | 说明 | 权限 |
|------|------|------|
| `/teammsg <消息>` | 快速向队伍频道发送一条消息 | `balancedteam.use` |
| `/teammsg` | 切换队伍聊天锁定模式（聊天栏消息自动定向至队伍） | `balancedteam.use` |

</details>

---

## 权限

<details>
<summary><b>点击展开 / 折叠权限节点列表</b></summary>

<br>

| 权限 | 默认 | 说明 |
|------|------|------|
| `balancedteam.use` | 所有人 | 基础指令 |
| `balancedteam.admin` | OP | 管理员指令与语言重载 |
| `balancedteam.admin.spy` | OP | 监听团队聊天 |

</details>

---

## PlaceholderAPI 占位符变量

<details>
<summary><b>点击展开 / 折叠占位符变量列表与配置示例</b></summary>

<br>

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

**配置示例 (例如 TAB / 计分板 / 聊天插件)：**
```yaml
# 聊天格式调用示例
format: '{balancedteam_tag} &7[{balancedteam_role}&7] &f{DISPLAYNAME}&7: &f{MESSAGE}'

# 计分板调用示例
lines:
  - '&b&l我的战队'
  - '&7战队: &f%balancedteam_name%'
  - '&7职位: &e%balancedteam_role%'
  - '&7人数: &a%balancedteam_online%&7/&f%balancedteam_members%'
  - '&7同盟: &b%balancedteam_allies%&7/&f%balancedteam_max_allies%'
```

</details>

---

## 赞助

如果觉得这个项目对你有帮助，欢迎请作者喝一杯咖啡！

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/X5Z326V65O)

---

## 许可证

[MIT License](LICENSE)

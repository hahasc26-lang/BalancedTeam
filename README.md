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

---

## 简介

**BalancedTeam** 是一个专为**无规则 (Anarchy)** 及**生存竞技服务器**设计的高性能团队/公会插件，兼容 Bukkit、Spigot 和 Paper 全系服务端。

插件的核心理念是**平衡性**：通过限制队伍规模、同盟数量以及内置防战斗作弊机制，防止单一势力垄断服务器，让每一位玩家都能享受公平的对抗体验。

---

## ✨ 功能特性

### 📋 团队管理
- **创建 / 解散团队**：支持自定义队名（正则校验，防特殊字符）
- **成员管理**：邀请、踢出、转让队长，设置副队长
- **申请入队**：玩家主动申请，队长/副队长审批
- **成员上限**：可配置最大成员数，防止超大公会垄断

### 🤝 外交系统
- **结盟 / 解盟**：与其他队伍建立或解除同盟关系（需对方同意）
- **宣战 / 求和**：单方面宣战，支持双向求和
- **同盟上限**：限制每队最大结盟数，防止全服联合

### ⚔️ 平衡性机制
- **友伤控制**：队长可开启/关闭队内友伤，支持冷却时间（防战斗中秒切）
- **同盟友伤保护**：可选同盟间无法互相伤害
- **退队冷却**：离队或换队需等待冷却，防止战斗背刺

### 💬 团队聊天
- `/teammsg` (`/tc`, `/tm`) — 发送团队专属聊天，支持锁定模式（持续团队聊天）
- **管理员监听模式** — OP 可监听所有团队聊天频道
- 完全可自定义的聊天格式

### 🖥️ 图形化 GUI
- 团队主菜单（可视化管理）
- 成员管理界面
- 全服团队列表
- 解散/离队确认界面

### 🗄️ 数据存储
- **双存储引擎**：支持 **MySQL**（推荐生产环境）和 **SQLite**（轻量本地）
- **HikariCP 连接池**：高并发下稳定可靠
- **异步全量缓存**：启动时预热全部数据到内存，运行期零阻塞
- **自动数据库迁移**：版本升级自动执行 Schema 变更

### 🌐 多语言支持
- 内置 **简体中文 (zh_CN)**、**繁体中文 (zh_TW)**、**English (en_US)**
- 支持在 `lang/` 目录下自定义任意语言文件

---

## 🚀 快速开始

### 环境要求
| 项目 | 要求 |
|------|------|
| Minecraft 服务端 | Bukkit / Spigot / Paper 1.20.4+ |
| Java | 21+ |
| 数据库 | MySQL 8.0+ 或内置 SQLite |

### 安装步骤

1. 从 [Releases](../../releases) 下载最新的 `BalancedTeam-x.x.x.jar`
2. 将 jar 文件放入服务器的 `plugins/` 目录
3. 重启服务器，插件会自动生成配置文件
4. 编辑 `plugins/BalancedTeam/config.yml` 配置数据库连接
5. 执行 `/reload confirm` 或重启服务器使配置生效

### 自行构建

```bash
git clone https://github.com/hahasc26-lang/BalancedTeam.git
cd BalancedTeam

# 标准构建（依赖由服务端的 libraries 系统自动加载）
mvn clean package

# Fat-Jar 构建（适用于不支持 libraries 的旧版服务端）
mvn clean package -Pfatjar
```

构建产物位于 `target/BalancedTeam-x.x.x.jar`。

---

## ⚙️ 配置说明

主配置文件位于 `plugins/BalancedTeam/config.yml`，关键配置项如下：

---
# 语言设置
language: "zh_CN"  # zh_CN | zh_TW | en_US

# 数据库
database:
  type: "mysql"  # mysql 或 sqlite
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: "password"

# 平衡性配置
balance:
  max_members: 10              # 团队最大成员数
  max_allies: 3                # 最大结盟数
  friendly_fire_default: false # 默认是否开启友伤
  leave_team_cooldown_seconds: 60  # 退队冷却（秒）
```

---

## 📜 指令一览

### 玩家指令 `/team` (别名: `/t`, `/clan`, `/bt`)

| 指令 | 说明 |
|------|------|
| `/team` | 打开团队主菜单 |
| `/team help` | 显示帮助 |
| `/team create <队名>` | 创建团队 |
| `/team disband` | 解散团队 |
| `/team invite <玩家>` | 邀请玩家 |
| `/team kick <玩家>` | 踢出成员 |
| `/team leave` | 离开团队 |
| `/team promote <玩家>` | 晋升为副队长 |
| `/team demote <玩家>` | 降级为成员 |
| `/team transfer <玩家>` | 转让队长 |
| `/team accept <队名>` | 接受邀请 |
| `/team deny <队名>` | 拒绝邀请 |
| `/team apply <队名>` | 申请入队 |
| `/team ally <队名>` | 申请/接受结盟 |
| `/team unally <队名>` | 解除同盟 |
| `/team enemy <队名>` | 宣战 |
| `/team peace <队名>` | 申请/接受求和 |
| `/team ff` | 切换友伤状态 |
| `/team info [队名]` | 查看团队信息 |
| `/team list [页码]` | 查看团队列表 |

### 管理员指令 `/teamadmin` (别名: `/ta`, `/btadmin`)

| 指令 | 说明 |
|------|------|
| `/teamadmin disband <队名>` | 强制解散团队 |
| `/teamadmin kick <玩家>` | 强制踢出成员 |
| `/teamadmin spy` | 切换团队聊天监听 |

### 聊天指令 `/teammsg` (别名: `/tc`, `/tm`, `/teamchat`)

| 指令 | 说明 |
|------|------|
| `/teammsg <消息>` | 发送团队消息 |
| `/teammsg` | 切换团队聊天锁定模式 |

---

## 🔐 权限节点

| 权限 | 默认 | 说明 |
|------|------|------|
| `balancedteam.use` | 所有人 | 使用团队基础指令 |
| `balancedteam.admin` | OP | 使用管理员指令 |
| `balancedteam.admin.spy` | OP | 监听所有团队聊天 |

---

## 📁 项目结构

```
src/main/java/com/balancedteam/
├── BalancedTeamPlugin.java   # 插件主类
├── command/                  # 指令处理器
├── config/                   # 配置管理
├── database/                 # 数据库层 (DAO + 迁移)
├── gui/                      # GUI 界面
├── listener/                 # 事件监听器
├── manager/                  # 业务逻辑管理器
├── model/                    # 数据模型
└── util/                     # 工具类
```

---

## 📄 开源协议

本项目基于 [MIT License](LICENSE) 开源。

---

<a name="english"></a>

# BalancedTeam — English

<p align="center">
  <a href="#简介">中文</a> | <b>English</b>
</p>

## Introduction

**BalancedTeam** is a high-performance Clan/Team plugin designed for **Anarchy** and **survival-competitive** Minecraft servers, fully compatible with Bukkit, Spigot, and Paper.

Its core philosophy is **balance**: by restricting team sizes, alliance counts, and providing built-in anti-exploit combat mechanics, it prevents any single faction from monopolizing the server and ensures a fair experience for every player.

---

## ✨ Features

### 📋 Team Management
- **Create / Disband Teams** — Custom names with regex validation to block special characters
- **Member Management** — Invite, kick, promote to vice-leader, transfer leadership
- **Join Applications** — Players can apply; leader/vice-leader approves
- **Member Cap** — Configurable max member count prevents mega-guilds

### 🤝 Diplomacy System
- **Ally / Unally** — Form or break alliances with mutual consent
- **War / Peace** — Unilateral declaration of war; bilateral peace requests
- **Alliance Cap** — Limits max allies per team to prevent server-wide coalitions

### ⚔️ Balance Mechanics
- **Friendly Fire Control** — Leaders can toggle FF with cooldown (prevents mid-combat switching)
- **Ally Friendly Fire Protection** — Optional: allies cannot damage each other
- **Leave Cooldown** — Prevents betrayal backstabs by enforcing a cooldown on leaving/switching teams

### 💬 Team Chat
- `/teammsg` (`/tc`, `/tm`) — Dedicated team chat channel with lock mode (continuous team chat)
- **Admin Spy Mode** — OPs can monitor all team chat channels
- Fully customizable chat format

### 🖥️ Graphical GUI
- Team main menu (visual management)
- Member management interface
- Server-wide team list browser
- Disband/leave confirmation dialogs

### 🗄️ Data Storage
- **Dual Storage Engine**: **MySQL** (recommended for production) and **SQLite** (lightweight local)
- **HikariCP Connection Pool**: Stable and reliable under high concurrency
- **Async In-Memory Cache**: All data preloaded on startup, zero blocking during runtime
- **Auto Database Migration**: Schema upgrades applied automatically on version update

### 🌐 Multi-Language Support
- Built-in: **Simplified Chinese (zh_CN)**, **Traditional Chinese (zh_TW)**, **English (en_US)**
- Add custom language files to the `lang/` directory

---

## 🚀 Quick Start

### Requirements
| Item | Requirement |
|------|-------------|
| Minecraft Server | Bukkit / Spigot / Paper 1.20.4+ |
| Java | 21+ |
| Database | MySQL 8.0+ or built-in SQLite |

### Installation

1. Download the latest `BalancedTeam-x.x.x.jar` from [Releases](../../releases)
2. Place the jar in your server's `plugins/` folder
3. Restart the server — config files will be generated automatically
4. Edit `plugins/BalancedTeam/config.yml` to configure your database
5. Reload or restart the server to apply changes

### Build from Source

```bash
git clone https://github.com/hahasc26-lang/BalancedTeam.git
cd BalancedTeam

# Standard build (dependencies loaded by server's libraries system)
mvn clean package

# Fat-Jar build (for legacy servers without library support)
mvn clean package -Pfatjar
```

Output: `target/BalancedTeam-x.x.x.jar`

---

## ⚙️ Configuration

Main config: `plugins/BalancedTeam/config.yml`

---

## 📜 Commands

### Player Commands `/team` (aliases: `/t`, `/clan`, `/bt`)

| Command | Description |
|---------|-------------|
| `/team` | Open team main menu |
| `/team help` | Show help |
| `/team create <name>` | Create a team |
| `/team disband` | Disband your team |
| `/team invite <player>` | Invite a player |
| `/team kick <player>` | Kick a member |
| `/team leave` | Leave your team |
| `/team promote <player>` | Promote to vice-leader |
| `/team demote <player>` | Demote to member |
| `/team transfer <player>` | Transfer leadership |
| `/team accept <team>` | Accept an invitation |
| `/team deny <team>` | Deny an invitation |
| `/team apply <team>` | Apply to join a team |
| `/team ally <team>` | Request / accept alliance |
| `/team unally <team>` | Break alliance |
| `/team enemy <team>` | Declare war |
| `/team peace <team>` | Request / accept peace |
| `/team ff` | Toggle friendly fire |
| `/team info [team]` | View team info |
| `/team list [page]` | Browse team list |

### Admin Commands `/teamadmin` (aliases: `/ta`, `/btadmin`)

| Command | Description |
|---------|-------------|
| `/teamadmin disband <team>` | Force-disband a team |
| `/teamadmin kick <player>` | Force-kick a player |
| `/teamadmin spy` | Toggle team chat spy |

### Chat Command `/teammsg` (aliases: `/tc`, `/tm`, `/teamchat`)

| Command | Description |
|---------|-------------|
| `/teammsg <message>` | Send a team message |
| `/teammsg` | Toggle team chat lock mode |

---

## 🔐 Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `balancedteam.use` | Everyone | Use basic team commands |
| `balancedteam.admin` | OP | Use admin commands |
| `balancedteam.admin.spy` | OP | Monitor all team chats |

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

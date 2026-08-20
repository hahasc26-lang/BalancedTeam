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

## ✨ 功能

- **团队管理** — 创建、解散、邀请、踢人、转让队长、设置副队长、申请入队
- **外交系统** — 结盟 / 解盟、宣战 / 求和，均有数量上限防止全服联合
- **平衡机制** — 友伤开关（带冷却）、同盟保护、退队冷却防战斗背刺
- **团队聊天** — `/tc` 专属频道，支持锁定模式；OP 可监听所有频道
- **图形化 GUI** — 团队菜单、成员管理、全服列表、操作确认界面
- **双存储引擎** — MySQL（生产推荐）/ SQLite（开箱即用），HikariCP 连接池
- **多语言** — 内置 `zh_CN` / `zh_TW` / `en_US`，支持自定义语言文件

---

## 🚀 安装

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

## 📜 指令

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

**`/teamadmin`**（别名：`/ta`）— 强制解散、踢人、监听聊天

**`/teammsg`**（别名：`/tc` `/tm`）— 发送或锁定团队聊天

---

## 🔐 权限

| 权限 | 默认 | 说明 |
|------|------|------|
| `balancedteam.use` | 所有人 | 基础指令 |
| `balancedteam.admin` | OP | 管理员指令 |
| `balancedteam.admin.spy` | OP | 监听团队聊天 |

---

## 📄 许可证

[MIT License](LICENSE)

---

<a name="english"></a>

# BalancedTeam — English

<p align="center">
  <a href="#balancedteam">中文</a> | <b>English</b>
</p>

A high-performance Clan/Team plugin for **Anarchy** and **survival-competitive** Minecraft servers. Compatible with Bukkit / Spigot / Paper.

---

## ✨ Features

- **Team Management** — Create, disband, invite, kick, promote, transfer leadership, apply to join
- **Diplomacy** — Ally / Unally, War / Peace with configurable caps to prevent server-wide coalitions
- **Balance Mechanics** — Friendly fire toggle (with cooldown), ally protection, leave cooldown against backstabs
- **Team Chat** — `/tc` channel with lock mode; OP spy mode to monitor all channels
- **Graphical GUI** — Team menu, member management, server-wide list, confirmation dialogs
- **Dual Storage** — MySQL (production) / SQLite (zero-config), backed by HikariCP
- **Multi-Language** — Built-in `zh_CN` / `zh_TW` / `en_US`; custom language files supported

---

## 🚀 Installation

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

## 📜 Commands

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

**`/teamadmin`** (alias: `/ta`) — Force disband, kick, spy on chat

**`/teammsg`** (aliases: `/tc` `/tm`) — Send or lock team chat

---

## 🔐 Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `balancedteam.use` | Everyone | Basic commands |
| `balancedteam.admin` | OP | Admin commands |
| `balancedteam.admin.spy` | OP | Monitor team chats |

---

## 📄 License

[MIT License](LICENSE)
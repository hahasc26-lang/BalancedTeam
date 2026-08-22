# Commands and Permissions Reference

This page provides an exhaustive reference for all player and administrator commands, arguments, aliases, permissions, and tab-completion options in **BalancedTeam**.

---

## Player Commands (`/team`)

**Main Command:** `/team`  
**Aliases:** `/t`, `/clan`, `/bt`  
**Permission Required:** `balancedteam.use` (Default: `true`)

| Command | Arguments | Description | Role Required |
| :--- | :--- | :--- | :--- |
| `/team` | *(None)* | Opens the interactive graphical GUI Dashboard. | Any |
| `/team help` | `[page]` | Displays command usage and help menu. | Any |
| `/team create` | `<name>` | Creates a new team with the specified name. | None (Must not be in a team) |
| `/team info` | `[teamName]` | Views detailed information for your team or a target team. | Any |
| `/team list` | `[page]` | Opens the server-wide team list GUI or views list in chat. | Any |
| `/team invite` | `<player>` | Sends a team membership invitation to a player. | Leader / Officer |
| `/team kick` | `<player>` | Kicks a member from the team (prompts `ConfirmGui`). | Leader / Officer (> Target Level) |
| `/team leave` | *(None)* | Leaves your current team (prompts `ConfirmGui`). | Member / Officer (Not Leader) |
| `/team disband` | *(None)* | Permanently disbands the team (prompts `ConfirmGui`). | Leader |
| `/team transfer` | `<player>` | Transfers leadership to a member (prompts `ConfirmGui`). | Leader |
| `/team promote` | `<player>` | Promotes a Member to Officer (prompts `ConfirmGui`). | Leader |
| `/team demote` | `<player>` | Demotes an Officer to Member (prompts `ConfirmGui`). | Leader |
| `/team desc` | `<text...>` | Sets or updates your team's description/announcement. | Leader / Officer |
| `/team ff` | *(None)* | Toggles friendly fire damage protection on or off. | Leader / Officer |
| `/team ally` | `<teamName>` | Sends a formal alliance request to another team. | Leader / Officer |
| `/team unally` | `<teamName>` | Breaks an active alliance with another team. | Leader / Officer |
| `/team enemy` | `<teamName>` | Marks another team as an enemy / declares war. | Leader / Officer |
| `/team peace` | `<teamName>` | Removes enemy status and restores neutral relations. | Leader / Officer |
| `/team apply` | `<teamName>` | Submits an application to join an existing team. | None (Must not be in a team) |
| `/team accept` | `<target>` | Accepts an incoming invitation or alliance request. | Leader / Officer |
| `/team deny` | `<target>` | Rejects an incoming invitation or alliance request. | Leader / Officer |
| `/team lang` | `[code|auto|list]` | View or change your personal client language preference. | Any |

---

## Team Chat Commands (`/teammsg`)

**Main Command:** `/teammsg`  
**Aliases:** `/tc`, `/tm`, `/teamchat`  
**Permission Required:** `balancedteam.use` (Default: `true`)

| Command | Usage | Description |
| :--- | :--- | :--- |
| `/teammsg <message>` | Quick Message | Sends a message directly to your team members without switching chat modes. |
| `/teammsg` | Toggle Lock | Toggles **Team Chat Lock Mode**. When enabled, all standard chat messages you type in game will be automatically sent to the team channel. |

---

## Language Management Commands (`/teamlang`)

**Main Command:** `/teamlang`  
**Aliases:** `/tlang`, `/btlang`, `/clanlang`  
**Permission Required:** `balancedteam.use` (Switching) / `balancedteam.admin` (Reloading)

| Command | Arguments | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/teamlang` | *(None)* | Displays your active language code, detection mode, and help. | `balancedteam.use` |
| `/teamlang list` | *(None)* | Lists all loaded language packs supported by the server. | `balancedteam.use` |
| `/teamlang <code|auto>` | `<zh_CN\|zh_TW\|en_US\|auto>` | Sets your personal language or resets to client auto-detection. | `balancedteam.use` |
| `/teamlang reload` | *(None)* | Hot-reloads all language configuration files from disk. | `balancedteam.admin` |

---

## Administrator Commands (`/teamadmin`)

**Main Command:** `/teamadmin`  
**Aliases:** `/ta`, `/btadmin`  
**Permission Required:** `balancedteam.admin` (Default: `op`)

| Command | Arguments | Description |
| :--- | :--- | :--- |
| `/teamadmin disband <team>` | `<teamName>` | Force disbands any target team on the server immediately. |
| `/teamadmin kick <player>` | `<playerName>` | Force removes any player from their current team. |
| `/teamadmin spy` | *(None)* | Toggles **Admin Spy Mode** to listen to all private team communications across the server in real time. |
| `/teamadmin reload` | *(None)* | Reloads `config.yml` and all language files without restarting the server. |
| `/teamadmin help` | *(None)* | Displays administrative command help. |

---

## Permissions Node Reference

| Node | Default | Description |
| :--- | :--- | :--- |
| `balancedteam.use` | `true` | Allows players to use standard team commands, open GUIs, and participate in team chat. |
| `balancedteam.admin` | `op` | Grants access to `/teamadmin` commands, configuration reload, and administrative overrides. |
| `balancedteam.admin.spy` | `op` | Allows administrators to toggle `/teamadmin spy` and monitor all team chats. |

---

## Navigation

| [← Installation & Configuration](https://github.com/hahasc26-lang/BalancedTeam/wiki/Installation-and-Configuration) | [Next: GUI Navigation Guide →](https://github.com/hahasc26-lang/BalancedTeam/wiki/GUI-System) |
| :--- | :--- |

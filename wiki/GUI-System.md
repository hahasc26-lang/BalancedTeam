# Graphical GUI System Guide

**BalancedTeam** features an interactive, chest-inventory GUI system built to eliminate cumbersome text commands while maintaining high performance and zero main-thread hitching.

All menus are dynamically rendered according to each player's active language locale and include sound cues and responsive click handlers.

---

## Menu Architecture Overview

```
                      /team Command
                            │
               ┌────────────┴────────────┐
               ▼                         ▼
      [In a Team]                  [Not in a Team]
     Team Dashboard                No-Team Portal
     ├── Member Management         ├── Create Team (Chat Input)
     ├── Diplomacy (Allies/Wars)   ├── Browse Team List
     ├── Notification Center       └── Notification Center
     ├── Friendly Fire Toggle
     └── Leave / Disband (Confirm)
```

---

## Core Menus

### 1. Team Dashboard (`TeamDashboardGui`)
Accessed by typing `/team` while currently belonging to a team:
- **Team Flag Banner**: Displays team name, leader, creation date, member counts, and public description.
- **Member Management (Player Head)**: Opens the member roster to manage roles or kick members.
- **Diplomacy Console (Crossed Swords / Golden Apple)**: View and manage allied factions and declared enemies.
- **Notification Center (Bell / Paper)**: View pending incoming join applications and alliance requests.
- **Friendly Fire Toggle (Bow / Diamond Sword)**: Instant toggle for team damage with cooldown indicator.
- **Team Chat Toggle (Book & Quill)**: Toggle quick team messaging mode.
- **Disband / Leave Team (Barrier / TNT)**: Opens the confirmation dialogue.

### 2. No-Team Portal (`TeamNoTeamGui`)
Accessed by typing `/team` when not currently belonging to any team:
- **Create Team (Anvil)**: Prompts an interactive chat prompt with regex and length validation to name your new team.
- **Browse Team List (Compass)**: Opens the paginated server-wide team roster with sorting and details.
- **Invitations & Applications (Bell)**: View pending team invitations sent to you by recruiters.

---

## Member Management (`MemberManageGui`)

Manage team members with intuitive mouse interactions:

| Click Action | Operation | Triggered Action |
| :--- | :--- | :--- |
| **Left Click** | Promote / Demote | Prompts `ConfirmGui` (`PROMOTE` to Officer or `DEMOTE` to Member). |
| **Right Click** | Kick Member | Prompts `ConfirmGui` (`KICK`) with role hierarchy verification. |
| **Shift + Click** | Transfer Leadership | Prompts `ConfirmGui` (`TRANSFER`) to pass leader rank to member. |

---

## Notification Center (`NotificationGui`)

The Notification Center centralizes all incoming requests into a single, clean management screen:
1. **Pending Invitations**: Invitations received from other teams allowing one-click **Accept** or **Deny**.
2. **Pending Applications**: Applications submitted by unaligned players wanting to join your team (Officers/Leaders only).
3. **Pending Alliance Requests**: Alliance pact offers sent by foreign teams.

---

## Unified 6-Mode Confirmation System (`ConfirmGui`)

To completely prevent accidental clicks and catastrophic mistakes, all destructive or sensitive team actions are routed through a standardized confirmation dialogue:

| Mode | Icon | Operation | Permissions & Rules |
| :--- | :--- | :--- | :--- |
| `DISBAND` | TNT | Disband Team | Leader only. Permanently deletes the team and cancels all pacts. |
| `LEAVE` | Oak Door | Leave Team | Non-leader members. Enforces leave-team cooldown against combat-hopping. |
| `KICK` | Boots | Kick Member | Leader & Officers. Caller's role level must strictly exceed target's level. |
| `TRANSFER` | Gold Helmet | Transfer Leader | Leader only. Passes leader status to member and demotes former leader to Officer. |
| `PROMOTE` | Gold Armor | Promote Member | Leader only. Promotes Member (Level 1) to Officer (Level 2). |
| `DEMOTE` | Iron Armor | Demote Officer | Leader only. Demotes Officer (Level 2) back to Member (Level 1). |

### Built-in Safety Features
- **Double Pre-Validation**: Verifies player permissions, team membership, and target validity both *before* opening the GUI and *at the exact moment* the confirm button is pressed.
- **Single-Execution Click Guard**: Disables the confirm button immediately upon first click to prevent double-firing caused by rapid clicks or packet spam.
- **Audio Feedback**: Plays distinct anvil/experience sounds on success and villager denial sounds on rejection.

---

## Navigation

| [← Commands & Permissions](https://github.com/hahasc26-lang/BalancedTeam/wiki/Commands-and-Permissions) | [Next: Diplomacy & Combat Balance →](https://github.com/hahasc26-lang/BalancedTeam/wiki/Diplomacy-and-Combat-Balance) |
| :--- | :--- |

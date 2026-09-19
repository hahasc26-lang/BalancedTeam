# Diplomacy and Combat Balance Mechanics

**BalancedTeam** was designed specifically with competitive gameplay, faction PvP, and anarchy balance in mind. This document outlines the diplomacy mechanics, anti-zerg constraints, and combat protection algorithms.

---

## The Diplomacy System

Diplomacy allows teams to establish formal relationships with other teams, altering damage calculation rules and communication options.

```
                  ┌───────────────────────────────┐
                  │       Diplomatic Status       │
                  └──────────────┬────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
   [Allied Team]          [Neutral Team]          [Enemy Team]
 • Damage Protected*    • Default PvP           • Hostile Target
 • Shared Coordination  • Standard Interaction  • Highlighted Tracking
 • Maximum Cap (e.g. 3)                         • Maximum Cap (e.g. 10)
```
*\*Configurable via `balance.ally_friendly_fire` in `config.yml`.*

---

## Alliances (Allies)

- **Bilateral Agreement**: To form an alliance, an Officer or Leader must send an alliance request (`/team ally <teamName>`). The target team's Officers or Leader must accept the request before the pact takes effect.
- **Alliance Request Timeout**: Unanswered alliance requests expire automatically after `balance.ally_request_timeout_seconds` (default: 3600 seconds).
- **Anti-Zerg Hard Cap (`balance.max_allies`)**: Servers can set a strict limit on the number of allies a team can hold (default: 3). This effectively prevents "mega-coalitions" from dominating server resources.
- **Ally Damage Protection (`balance.ally_friendly_fire`)**: When set to `false`, players cannot damage members of allied teams with weapons, bows, tridents, or negative potions.
- **Dissolving Alliances**: Either team can unilaterally terminate the alliance at any time via `/team unally <teamName>` or through the GUI.

---

## Hostilities (Enemies) & The Truce System

### 1. Declaring War (Enemies)
- **Unilateral Declaration**: Any Officer or Leader can mark another team as an enemy (`/team enemy <teamName>`) without requiring the target team's consent.
- **Enemy Cap (`balance.max_enemies`)**: Configurable limit (default: 10) on active marked enemy teams to prevent excessive server tracking overhead.
- **Removing Enemy Status**: Direct removal can be executed via `/team enemy remove <teamName>`.

### 2. Formal Truce & Peace Protocol (`/team truce` / `/team peace`)
To prevent exploitation and chaotic re-declarations of war, BalancedTeam provides a bilateral **Truce & Peace System**:
- **Sending a Truce Proposal**: Propose peace to an enemy team via `/team truce <teamName>` (or through the White Banner in `EnemyManageGui`).
- **Truce Request Expiration (`balance.truce_request_timeout_seconds`)**: Unanswered truce proposals expire automatically after the configured duration (default: 1800 seconds / 30 minutes).
- **Accepting or Denying**: The target team's Officers or Leader can accept (`/team truce accept <teamName>`) or reject (`/team truce deny <teamName>`) the proposal via commands or the Notification Center.
- **Canceling Proposals**: The initiating team can retract a proposal before acceptance via `/team truce cancel <teamName>`.
- **Viewing Active Truces**: Use `/team truce list` to review all pending proposals.

### 3. Post-War Protection (`balance.post_war_protection_seconds`)
- **Anti-Fake Peace Exploits**: In competitive PvP servers, teams often attempt "fake peace" maneuvers—accepting peace to lower defenses, then immediately re-declaring war and ambushing the opposing team.
- **Automatic Cooldown**: Once a truce is accepted, both teams automatically enter **Post-War Protection** for the duration specified by `balance.post_war_protection_seconds` (default: 1800 seconds / 30 minutes).
- **Protection Rules**:
  - **No War Re-declaration**: Neither team can re-declare war against the other while the protection period is active.
  - **Damage Interception**: All melee attacks, arrows, tridents, and harmful splash/lingering potion effects between members of the two teams are strictly neutralized by `DamageListener`.
  - **GUI Indicator**: The active protection status and formatted remaining time are prominently displayed with a Shield icon badge in `EnemyManageGui`.

---

## Combat Balance & Anti-Abuse Rules

### 1. Global Friendly Fire Protection Switch (`balance.enable_friendly_fire_protection`)
- **Master Toggle**: Allows server administrators to completely enable or disable all friendly fire and alliance damage protection across the server.
- **Default (`true`)**: Friendly fire and alliance protection operate according to team settings and alliance permissions.
- **Disabled (`false`)**: All friendly fire protection is globally bypassed. Players can attack each other without restriction, and commands/GUIs for friendly fire toggling display a system-disabled status.

### 2. Friendly Fire Toggle with PvP Cooldown
- **The Problem**: In vanilla team systems, players frequently toggle friendly fire on/off rapidly during team fights to avoid accidental team hits while firing weapons or throwing splash potions.
- **The Solution**: BalancedTeam introduces `balance.friendly_fire_cooldown_seconds` (default: 30s). When friendly fire is changed, the player is placed on a cooldown preventing further toggles until the timer expires.

### 3. Team-Hop Anti-Backstab Cooldown
- **The Problem**: During intense fights or base sieges, malicious players may leave their team instantly, gain damage immunity or switch to an enemy team, and backstab their former comrades from inside their base.
- **The Solution**: BalancedTeam enforces `balance.leave_team_cooldown_seconds` (default: 60s). After leaving or disbanding a team, players must wait for the cooldown to elapse before creating or joining another team.

---

## Damage Interception Matrix (`DamageListener`)

BalancedTeam intercepts and evaluates damage in real-time across all possible Bukkit attack vectors:

| Damage Vector | Supported Checks | Protection Triggered |
| :--- | :--- | :--- |
| **Direct Melee / Fist Attack** | `EntityDamageByEntityEvent` | Canceled if Teammate/Ally & FF disabled |
| **Arrow / Bow / Crossbow** | Projectile Shooter Source Resolution | Canceled if Shooter is Teammate/Ally |
| **Trident Throw** | Projectile Shooter Source Resolution | Canceled if Shooter is Teammate/Ally |
| **Potion / Lingering Splash** | `ThrownPotion` / Area Effect Cloud Source | Negative potion effects blocked |
| **Sweeping Edge Attack** | Secondary nearby entity damage | Teammates/Allies excluded from sweep damage |
| **Fireworks / Crossbow Rockets** | Firework shooter attribution | Teammates/Allies protected from blast |
| **End Crystal Explosion (CPvP)** | `CrystalListener` Placer/Detonator tracking & chain detection | Teammates/Allies protected from blast; friendly grief prevention |
| **Primed TNT** | Igniter player attribution | Teammates/Allies protected from explosion |

---

### 4. End Crystal Listener (`CrystalListener`)
In anarchy and competitive faction servers, **Crystal PvP (CPvP)** is the primary method of high-tier player combat. BalancedTeam provides dedicated End Crystal tracking and protection:
- **Placement Tracking**: Dual-layer detection via `EntityPlaceEvent` and `PlayerInteractEvent` ensures all crystals placed on Obsidian or Bedrock are recorded regardless of server core (Spigot, Paper, Purpur).
- **Detonation Resolution**: Captures direct attacks, projectile triggers (arrows, tridents, snowballs), and chain explosions (Crystal A detonating Crystal B), passing responsibility back to the initiating player.
- **Friendly Fire & Alliance Interception**: End Crystal explosions cannot harm teammates or allies when friendly fire is disabled, closing the classic "crystal-bypass" exploit.
- **Configurable Options**:
  - `balance.prevent_friendly_crystal_break`: Optionally prevents teammates/allies from breaking or detonating crystals placed by friends when friendly fire is disabled (default: `false`).

---

## Navigation

| [← GUI Navigation Guide](https://github.com/hahasc26-lang/BalancedTeam/wiki/GUI-System) | [Next: Localization & Languages →](https://github.com/hahasc26-lang/BalancedTeam/wiki/Localization-System) |
| :--- | :--- |

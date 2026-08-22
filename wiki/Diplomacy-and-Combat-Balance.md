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

## Hostilities (Enemies)

- **War Declaration**: Any Officer or Leader can mark another team as an enemy (`/team enemy <teamName>`) without requiring the target's consent.
- **Enemy Cap (`balance.max_enemies`)**: Configurable limit (default: 10) on active marked enemy teams.
- **Making Peace**: Reverting enemy status to neutral can be executed via `/team peace <teamName>`.

---

## Combat Balance & Anti-Abuse Rules

### 1. Friendly Fire Toggle with PvP Cooldown
- **The Problem**: In vanilla team systems, players frequently toggle friendly fire on/off rapidly during team fights to avoid accidental team hits while firing weapons or throwing splash potions.
- **The Solution**: BalancedTeam introduces `balance.friendly_fire_cooldown_seconds` (default: 30s). When friendly fire is changed, the player is placed on a cooldown preventing further toggles until the timer expires.

### 2. Team-Hop Anti-Backstab Cooldown
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

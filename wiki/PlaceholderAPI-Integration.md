# PlaceholderAPI Integration Reference

**BalancedTeam** comes with a fully native [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) expansion. It exposes team membership, roles, diplomatic relationships, online member counts, and server-wide statistics to your tablist, scoreboards, chat formats, name tags, and holograms.

---

## Placeholder Format

- **Expansion Identifier**: `balancedteam`
- **Pattern**: `%balancedteam_<variable>%`
- **Soft Dependency**: Supported automatically when PlaceholderAPI is installed on the server.

---

## Comprehensive Placeholder Reference Table

### 1. Player & Current Team Identity
| Placeholder | Description | Example Output | Fallback (No Team) |
| :--- | :--- | :--- | :--- |
| `%balancedteam_in_team%` | Whether player is currently in a team | `true` / `false` | `false` |
| `%balancedteam_has_team%` | Alias for `in_team` | `true` / `false` | `false` |
| `%balancedteam_name%` | Player's team name | `Knights` | `""` |
| `%balancedteam_team_name%` | Alias for `name` | `Knights` | `""` |
| `%balancedteam_id%` | Player's team database ID | `42` | `""` |
| `%balancedteam_tag%` | Formatted team tag | `[Knights]` | `""` |
| `%balancedteam_leader%` | Team leader's username | `Steve` | `""` |
| `%balancedteam_leader_uuid%` | Team leader's UUID | `8c60f7e4-...` | `""` |
| `%balancedteam_is_leader%` | Whether player is team leader | `true` / `false` | `false` |
| `%balancedteam_is_officer%` | Whether player is officer or higher | `true` / `false` | `false` |
| `%balancedteam_description%` | Team's public description/notice | `Join our discord!` | `""` |

### 2. Player Role & Permission Level
| Placeholder | Description | Example Output | Fallback (No Team) |
| :--- | :--- | :--- | :--- |
| `%balancedteam_role%` | Localized role display name | `Leader`, `Officer`, `Member` | `Unknown` / `None` |
| `%balancedteam_role_raw%` | Role enum constant | `LEADER`, `OFFICER`, `MEMBER` | `NONE` |
| `%balancedteam_role_level%` | Role permission level integer | `3` (Leader), `2` (Officer), `1` (Member) | `0` |

### 3. Combat, Protection & Timestamps
| Placeholder | Description | Example Output | Fallback (No Team) |
| :--- | :--- | :--- | :--- |
| `%balancedteam_friendly_fire%` | Team friendly fire boolean | `true` / `false` | `false` |
| `%balancedteam_ff%` | Alias for `friendly_fire` | `true` / `false` | `false` |
| `%balancedteam_friendly_fire_formatted%` | Localized friendly fire status | `Enabled` / `Disabled` | `Disabled` |
| `%balancedteam_created_at%` | Team creation date/time | `2026-08-22 12:00:00` | `""` |
| `%balancedteam_joined_at%` | Player team join date/time | `2026-08-22 12:30:00` | `""` |

### 4. Team Member & Relation Counts
| Placeholder | Description | Example Output | Fallback (No Team) |
| :--- | :--- | :--- | :--- |
| `%balancedteam_members%` | Current member count | `8` | `0` |
| `%balancedteam_member_count%` | Alias for `members` | `8` | `0` |
| `%balancedteam_max_members%` | Configured max member limit | `10` | `10` |
| `%balancedteam_online%` | Current online member count | `4` | `0` |
| `%balancedteam_online_count%` | Alias for `online` | `4` | `0` |
| `%balancedteam_allies%` | Number of allied teams | `2` | `0` |
| `%balancedteam_max_allies%` | Configured max allies limit | `3` | `3` |
| `%balancedteam_enemies%` | Number of marked enemy teams | `1` | `0` |
| `%balancedteam_max_enemies%` | Configured max enemies limit | `10` | `10` |
| `%balancedteam_allies_list%` | Comma-separated list of ally team names | `Spartans, Vikings` | `""` |
| `%balancedteam_enemies_list%` | Comma-separated list of enemy team names | `Bandits` | `""` |

### 5. Server-Wide Statistics (Global)
| Placeholder | Description | Example Output |
| :--- | :--- | :--- |
| `%balancedteam_total_teams%` | Total registered teams on the server | `18` |
| `%balancedteam_total_members%` | Total players belonging to a team across the server | `94` |

### 6. Dynamic Relations with Target Players & Teams
| Placeholder | Description | Example Output |
| :--- | :--- | :--- |
| `%balancedteam_relation_<player>%` | Relationship with target player | `SAME_TEAM`, `ALLY`, `ENEMY`, `NONE` |
| `%balancedteam_relation_team_<teamName>%` | Relationship with target team | `SAME_TEAM`, `ALLY`, `ENEMY`, `NONE` |
| `%balancedteam_is_ally_<player>%` | Whether target player is in an allied team | `true` / `false` |
| `%balancedteam_is_enemy_<player>%` | Whether target player is in an enemy team | `true` / `false` |
| `%balancedteam_is_same_team_<player>%` | Whether target player is your teammate | `true` / `false` |

### 7. Target Team Queries
| Placeholder | Description | Example Output |
| :--- | :--- | :--- |
| `%balancedteam_team_exists_<teamName>%` | Whether specific team exists | `true` / `false` |
| `%balancedteam_team_leader_<teamName>%` | Leader name of target team | `Alex` |
| `%balancedteam_team_members_<teamName>%` | Total member count of target team | `6` |
| `%balancedteam_team_online_<teamName>%` | Online member count of target team | `2` |
| `%balancedteam_team_desc_<teamName>%` | Description of target team | `Best clan ever` |
| `%balancedteam_team_created_<teamName>%` | Creation timestamp of target team | `2026-08-20 14:00:00` |
| `%balancedteam_team_ff_<teamName>%` | Friendly fire status of target team | `true` / `false` |

---

## Example Configurations

### 1. Chat Formatting (e.g. TAB or EssentialsX Chat)
```yaml
# Display team tag and role in chat
format: '{balancedteam_tag} &7[{balancedteam_role}&7] &f{DISPLAYNAME}&7: &f{MESSAGE}'
```

### 2. Scoreboard (e.g. TAB / FeatherBoard)
```yaml
lines:
  - '&b&lMY TEAM'
  - '&7Name: &f%balancedteam_name%'
  - '&7Role: &e%balancedteam_role%'
  - '&7Members: &a%balancedteam_online%&7/&f%balancedteam_members%'
  - '&7Allies: &b%balancedteam_allies%&7/&f%balancedteam_max_allies%'
```

---

## Navigation

| [← Localization & Languages](https://github.com/hahasc26-lang/BalancedTeam/wiki/Localization-System) | [Next: Database & Technical Architecture →](https://github.com/hahasc26-lang/BalancedTeam/wiki/Database-and-Architecture) |
| :--- | :--- |

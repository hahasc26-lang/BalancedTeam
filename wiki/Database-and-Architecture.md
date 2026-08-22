# Database & Technical Architecture

**BalancedTeam** is engineered with a strict asynchronous, high-concurrency architecture to guarantee that heavy database queries or network lag never cause tick loss or TPS drops on your Minecraft server.

---

## Layered Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       Bukkit Layer                          │
│        Listeners  •  Commands  •  Interactive GUIs          │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                      Manager Layer                          │
│  TeamManager • RelationManager • InviteManager • LangManager│
│      (Thread-Safe Bidirectional In-Memory Caching)          │
└──────────────────────────────┬──────────────────────────────┘
                               │ CompletableFuture<T> (Async)
┌──────────────────────────────▼──────────────────────────────┐
│                        DAO Layer                            │
│ TeamDao • MemberDao • RelationDao • InviteDao • RequestDao  │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Connection Pool Layer                    │
│      HikariCP (MySQL)   /   Synchronized SQLite Pool        │
└─────────────────────────────────────────────────────────────┘
```

---

## Database Table Schemas

All tables use the configured table prefix (default: `bt_`).

### 1. Teams Table (`bt_teams`)
Stores primary team metadata and settings.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `INT AUTO_INCREMENT` | Primary Key. Unique Team ID. |
| `name` | `VARCHAR(32) UNIQUE` | Team unique display name. |
| `leader_uuid` | `VARCHAR(36)` | UUID of the current team leader. |
| `friendly_fire` | `TINYINT(1)` | Whether friendly fire is enabled (`1` = true, `0` = false). |
| `description` | `VARCHAR(255)` | Team public announcement or description. |
| `created_at` | `TIMESTAMP` | Timestamp when the team was founded. |

### 2. Members Table (`bt_members`)
Stores membership and permission levels.

| Column | Type | Description |
| :--- | :--- | :--- |
| `uuid` | `VARCHAR(36) PRIMARY KEY`| Unique UUID of the player. |
| `team_id` | `INT` | Foreign key referencing `bt_teams(id)`. |
| `role` | `INT` | Role level integer: `3` (Leader), `2` (Officer), `1` (Member). |
| `joined_at` | `TIMESTAMP` | Timestamp when player joined the team. |

### 3. Relations Table (`bt_relations`)
Stores active diplomatic statuses between factions.

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `INT AUTO_INCREMENT` | Primary Key. |
| `team1_id` | `INT` | Origin Team ID. |
| `team2_id` | `INT` | Target Team ID. |
| `type` | `VARCHAR(16)` | `ALLY` or `ENEMY`. |
| `status` | `VARCHAR(16)` | `PENDING` or `ACCEPTED`. |
| `created_at` | `TIMESTAMP` | Timestamp when the pact/war was established. |

### 4. Invites Table (`bt_invites`)
Stores pending membership invitations.

| Column | Type | Description |
| :--- | :--- | :--- |
| `team_id` | `INT` | Team offering membership. |
| `invitee_uuid`| `VARCHAR(36)` | UUID of the invited player. |
| `inviter_uuid`| `VARCHAR(36)` | UUID of the officer/leader who sent the invite. |
| `expires_at` | `BIGINT` | Epoch millisecond timestamp when the invite expires. |

### 5. Alliance Requests Table (`bt_ally_requests`)
Stores pending bilateral alliance proposals.

| Column | Type | Description |
| :--- | :--- | :--- |
| `from_team_id`| `INT` | Requesting Team ID. |
| `to_team_id` | `INT` | Target Team ID. |
| `expires_at` | `BIGINT` | Epoch millisecond timestamp when request expires. |

### 6. Applications Table (`bt_applications`)
Stores pending player join applications.

| Column | Type | Description |
| :--- | :--- | :--- |
| `team_id` | `INT` | Target Team ID. |
| `applicant_uuid`| `VARCHAR(36)` | UUID of the applying player. |
| `expires_at` | `BIGINT` | Epoch millisecond timestamp when application expires. |

---

## Concurrency & In-Memory Performance Guarantees

1. **Pre-Warmed In-Memory Cache**:
   - During startup, `TeamManager.loadAllData()` pre-loads all teams, members, and relations into memory.
   - Lookups by Player UUID (`playerTeamMap`), Team ID (`teamsById`), and Team Name (`teamsByName`) run in $\mathcal{O}(1)$ time.
2. **Asynchronous Execution (`CompletableFuture`)**:
   - All disk and database transactions (saving descriptions, updating roles, disbanding, processing invites) are offloaded to asynchronous worker threads.
   - The Bukkit main thread is never blocked waiting for SQL operations.
3. **Thread-Safe Data Structures**:
   - Uses `ConcurrentHashMap` and thread-safe collections to prevent concurrent modification exceptions across asynchronous database workers and Bukkit event loops.

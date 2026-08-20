package com.balancedteam.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库表结构初始化与迁移工具
 */
public class DatabaseMigrator {

    private final DatabaseManager databaseManager;
    private final String prefix;
    private final boolean isMySQL;

    public DatabaseMigrator(DatabaseManager databaseManager, String prefix, boolean isMySQL) {
        this.databaseManager = databaseManager;
        this.prefix = prefix;
        this.isMySQL = isMySQL;
    }

    public void migrate() throws SQLException {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            String autoIncrement = isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT";
            String charset = isMySQL ? "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" : ";";

            // 1. 团队表 bt_teams
            String createTeamsTable = "CREATE TABLE IF NOT EXISTS `" + prefix + "teams` (" +
                    "`id` INTEGER PRIMARY KEY " + autoIncrement + ", " +
                    "`name` VARCHAR(32) NOT NULL UNIQUE, " +
                    "`leader_uuid` VARCHAR(36) NOT NULL, " +
                    "`friendly_fire` BOOLEAN NOT NULL DEFAULT 0, " +
                    "`description` VARCHAR(128) DEFAULT '', " +
                    "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") " + charset;
            stmt.executeUpdate(createTeamsTable);

            // 2. 成员表 bt_members (role: 3=队长, 2=管理员, 1=队员)
            String createMembersTable = "CREATE TABLE IF NOT EXISTS `" + prefix + "members` (" +
                    "`uuid` VARCHAR(36) PRIMARY KEY, " +
                    "`team_id` INTEGER NOT NULL, " +
                    "`role` INT NOT NULL DEFAULT 1, " +
                    "`joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") " + charset;
            stmt.executeUpdate(createMembersTable);

            // 3. 外交关系表 bt_relations
            String createRelationsTable = "CREATE TABLE IF NOT EXISTS `" + prefix + "relations` (" +
                    "`id` INTEGER PRIMARY KEY " + autoIncrement + ", " +
                    "`team_id_1` INTEGER NOT NULL, " +
                    "`team_id_2` INTEGER NOT NULL, " +
                    "`relation_type` VARCHAR(16) NOT NULL, " +
                    "`status` VARCHAR(16) NOT NULL, " +
                    "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") " + charset;
            stmt.executeUpdate(createRelationsTable);

            // 4. 入队邀请表 bt_invites
            String createInvitesTable = "CREATE TABLE IF NOT EXISTS `" + prefix + "invites` (" +
                    "`id` INTEGER PRIMARY KEY " + autoIncrement + ", " +
                    "`team_id` INTEGER NOT NULL, " +
                    "`target_uuid` VARCHAR(36) NOT NULL, " +
                    "`inviter_uuid` VARCHAR(36) NOT NULL, " +
                    "`expire_time` BIGINT NOT NULL, " +
                    "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") " + charset;
            stmt.executeUpdate(createInvitesTable);

            // 5. 同盟申请表 bt_ally_requests
            String createAllyRequestsTable = "CREATE TABLE IF NOT EXISTS `" + prefix + "ally_requests` (" +
                    "`id` INTEGER PRIMARY KEY " + autoIncrement + ", " +
                    "`from_team_id` INTEGER NOT NULL, " +
                    "`to_team_id` INTEGER NOT NULL, " +
                    "`expire_time` BIGINT NOT NULL, " +
                    "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") " + charset;
            stmt.executeUpdate(createAllyRequestsTable);

            // 6. 入队申请表 bt_team_applications
            String createApplicationsTable = "CREATE TABLE IF NOT EXISTS `" + prefix + "team_applications` (" +
                    "`id` INTEGER PRIMARY KEY " + autoIncrement + ", " +
                    "`team_id` INTEGER NOT NULL, " +
                    "`player_uuid` VARCHAR(36) NOT NULL, " +
                    "`player_name` VARCHAR(32) NOT NULL, " +
                    "`expire_time` BIGINT NOT NULL, " +
                    "`created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ") " + charset;
            stmt.executeUpdate(createApplicationsTable);

            // 创建索引加速查询
            try {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_members_team_id` ON `" + prefix + "members` (`team_id`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_relations_team1` ON `" + prefix + "relations` (`team_id_1`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_relations_team2` ON `" + prefix + "relations` (`team_id_2`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_relations_teams` ON `" + prefix + "relations` (`team_id_1`, `team_id_2`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_invites_target` ON `" + prefix + "invites` (`target_uuid`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_invites_team` ON `" + prefix + "invites` (`team_id`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_invites_expire` ON `" + prefix + "invites` (`expire_time`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_ally_req_to` ON `" + prefix + "ally_requests` (`to_team_id`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_ally_req_from` ON `" + prefix + "ally_requests` (`from_team_id`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_ally_req_expire` ON `" + prefix + "ally_requests` (`expire_time`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_app_team` ON `" + prefix + "team_applications` (`team_id`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_app_player` ON `" + prefix + "team_applications` (`player_uuid`);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `idx_app_expire` ON `" + prefix + "team_applications` (`expire_time`);");
            } catch (SQLException ignored) {
                // 部分 SQLite/MySQL 索引语法兼容容错
            }
        }
    }
}

package com.balancedteam.database;

import com.balancedteam.BalancedTeamPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * 数据库连接池与异步调度管理器
 */
public class DatabaseManager {

    private final BalancedTeamPlugin plugin;
    private HikariDataSource dataSource;
    private final ExecutorService asyncExecutor;
    private String tablePrefix = "bt_";
    private boolean isMySQL = true;

    public DatabaseManager(BalancedTeamPlugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "BalancedTeam-DB-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void init() throws SQLException {
        String dbType = plugin.getConfig().getString("database.type", "mysql").toLowerCase();

        if ("mysql".equals(dbType)) {
            try {
                initMySQL();
                this.isMySQL = true;
                plugin.getLogger().info("[Database] 成功连接至 MySQL 数据库！");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "===============================================================");
                plugin.getLogger().log(Level.WARNING, "[Database] 无法连接到 MySQL 数据库 (" + e.getMessage() + ")");
                plugin.getLogger().log(Level.WARNING, "[Database] 请检查 config.yml 中的 MySQL 地址、端口、用户名和密码。");
                plugin.getLogger().log(Level.WARNING, "[Database] 为确保服务器正常运行，正在自动切换为 SQLite 本地数据库...");
                plugin.getLogger().log(Level.WARNING, "===============================================================");
                initSQLite();
                this.isMySQL = false;
            }
        } else {
            initSQLite();
            this.isMySQL = false;
            plugin.getLogger().info("[Database] 正在使用 SQLite 本地数据库模式。");
        }

        // 初始化数据表结构
        DatabaseMigrator migrator = new DatabaseMigrator(this, tablePrefix, isMySQL);
        migrator.migrate();
    }

    private void initMySQL() {
        ConfigurationSection mysqlSection = plugin.getConfig().getConfigurationSection("database.mysql");
        if (mysqlSection == null) {
            throw new IllegalStateException("Missing database.mysql section in config.yml");
        }
        String host = mysqlSection.getString("host", "localhost");
        int port = mysqlSection.getInt("port", 3306);
        String database = mysqlSection.getString("database", "minecraft");
        String username = mysqlSection.getString("username", "root");
        String password = mysqlSection.getString("password", "");
        boolean useSSL = mysqlSection.getBoolean("ssl", false);
        this.tablePrefix = sanitizeTablePrefix(mysqlSection.getString("table_prefix", "bt_"));

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%b&characterEncoding=utf8&useUnicode=true&autoReconnect=true&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                host, port, database, useSSL
        );

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 连接池调优
        hikariConfig.setMaximumPoolSize(mysqlSection.getInt("pool.maximum_pool_size", 10));
        hikariConfig.setMinimumIdle(mysqlSection.getInt("pool.minimum_idle", 5));
        hikariConfig.setConnectionTimeout(mysqlSection.getLong("pool.connection_timeout", 5000));
        hikariConfig.setIdleTimeout(mysqlSection.getLong("pool.idle_timeout", 600000));
        hikariConfig.setMaxLifetime(mysqlSection.getLong("pool.max_lifetime", 1800000));
        hikariConfig.setInitializationFailTimeout(4000);

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        hikariConfig.setPoolName("BalancedTeam-MySQL");
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    private void initSQLite() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }

        File dbFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("database.sqlite.file", "teams.db"));
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setPoolName("BalancedTeam-SQLite");

        this.tablePrefix = "bt_";
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    /**
     * 表名前缀白名单校验（仅允许英文字母、数字和下划线，1~32位），防止 SQL 注入
     */
    private String sanitizeTablePrefix(String prefix) {
        if (prefix == null || !prefix.matches("^[a-zA-Z0-9_]{1,32}$")) {
            plugin.getLogger().warning("[Database] 检测到非法数据表前缀配置 '" + prefix + "'，已自动重置为默认前缀 'bt_' 以保证安全！");
            return "bt_";
        }
        return prefix;
    }

    public boolean isMySQL() {
        return isMySQL;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource is closed or not initialized!");
        }
        return dataSource.getConnection();
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, asyncExecutor);
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    /**
     * 事务执行函数式接口（带返回值）
     */
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction(Connection conn) throws SQLException;
    }

    /**
     * 事务执行函数式接口（无返回值）
     */
    @FunctionalInterface
    public interface TransactionVoidCallback {
        void doInTransaction(Connection conn) throws SQLException;
    }

    /**
     * 在同一个事务中异步执行多个数据库操作（带返回值）
     * 保证原子性（要么全部提交，要么全部回滚）
     */
    public <T> CompletableFuture<T> executeTransaction(TransactionCallback<T> callback) {
        return supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                boolean initialAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try {
                    T result = callback.doInTransaction(conn);
                    conn.commit();
                    return result;
                } catch (SQLException | RuntimeException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        plugin.getLogger().log(Level.SEVERE, "[Database] 事务回滚失败", rollbackEx);
                    }
                    throw new com.balancedteam.database.exception.DatabaseException("数据库事务执行失败: " + e.getMessage(), e);
                } finally {
                    try {
                        conn.setAutoCommit(initialAutoCommit);
                    } catch (SQLException ignored) {}
                }
            } catch (SQLException e) {
                throw new com.balancedteam.database.exception.DatabaseException("获取数据库连接失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 在同一个事务中异步执行多个数据库操作（无返回值）
     * 保证原子性（要么全部提交，要么全部回滚）
     */
    public CompletableFuture<Void> runTransaction(TransactionVoidCallback callback) {
        return executeTransaction(conn -> {
            callback.doInTransaction(conn);
            return null;
        });
    }

    public void close() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
            }
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}

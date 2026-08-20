package com.balancedteam;

import com.balancedteam.command.TeamAdminCommand;
import com.balancedteam.command.TeamCommand;
import com.balancedteam.config.ConfigManager;
import com.balancedteam.database.DatabaseManager;
import com.balancedteam.database.dao.AllyRequestDao;
import com.balancedteam.database.dao.InviteDao;
import com.balancedteam.database.dao.MemberDao;
import com.balancedteam.database.dao.RelationDao;
import com.balancedteam.database.dao.TeamDao;
import com.balancedteam.listener.DamageListener;
import com.balancedteam.listener.GuiListener;
import com.balancedteam.listener.PlayerListener;
import com.balancedteam.manager.ChatInputManager;
import com.balancedteam.manager.ChatManager;
import com.balancedteam.manager.InviteManager;
import com.balancedteam.manager.RelationManager;
import com.balancedteam.manager.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * BalancedTeam 插件主类
 */
public class BalancedTeamPlugin extends JavaPlugin {

    private static BalancedTeamPlugin instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;

    private TeamDao teamDao;
    private MemberDao memberDao;
    private RelationDao relationDao;
    private InviteDao inviteDao;
    private AllyRequestDao allyRequestDao;
    private com.balancedteam.database.dao.ApplicationDao applicationDao;

    private TeamManager teamManager;
    private RelationManager relationManager;
    private InviteManager inviteManager;
    private com.balancedteam.manager.ApplicationManager applicationManager;
    private ChatManager chatManager;
    private ChatInputManager chatInputManager;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("==========================================");
        getLogger().info("   BalancedTeam 团队插件 正在启动...");
        getLogger().info("==========================================");

        // 1. 初始化配置
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // 2. 初始化数据库连接池
        this.databaseManager = new DatabaseManager(this);
        try {
            this.databaseManager.init();
            getLogger().info("[Database] 数据库连接池初始化成功，数据表校验完成！");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "[Database] 数据库连接失败！请检查 config.yml 数据库配置", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. 初始化 DAO 与管理器
        this.teamDao = new TeamDao(databaseManager);
        this.memberDao = new MemberDao(databaseManager);
        this.relationDao = new RelationDao(databaseManager);
        this.inviteDao = new InviteDao(databaseManager);
        this.allyRequestDao = new AllyRequestDao(databaseManager);
        this.applicationDao = new com.balancedteam.database.dao.ApplicationDao(databaseManager);

        this.inviteManager = new InviteManager(inviteDao);
        this.applicationManager = new com.balancedteam.manager.ApplicationManager(applicationDao);
        this.relationManager = new RelationManager(relationDao, allyRequestDao);
        this.chatManager = new ChatManager(this);
        this.chatInputManager = new ChatInputManager(this);
        this.teamManager = new TeamManager(this, teamDao, memberDao);

        // 4. 异步全量预热内存缓存
        relationDao.loadAllRelations().thenAccept(relations -> {
            relationManager.init(relations);
            getLogger().info("[Database] 已加载 " + relations.size() + " 条外交关系到内存。");
        });

        allyRequestDao.loadAllValidRequests(System.currentTimeMillis()).thenAccept(requests -> {
            relationManager.initRequests(requests);
            int count = requests.values().stream().mapToInt(map -> map.size()).sum();
            getLogger().info("[Database] 已加载 " + count + " 条有效同盟申请到内存。");
        });

        inviteDao.loadAllValidInvites(System.currentTimeMillis()).thenAccept(invites -> {
            inviteManager.init(invites);
            int count = invites.values().stream().mapToInt(map -> map.size()).sum();
            getLogger().info("[Database] 已加载 " + count + " 条有效入队邀请到内存。");
        });

        applicationDao.loadAllValidApplications(System.currentTimeMillis()).thenAccept(applications -> {
            applicationManager.init(applications);
            int count = applications.values().stream().mapToInt(map -> map.size()).sum();
            getLogger().info("[Database] 已加载 " + count + " 条有效入队申请到内存。");
        });

        teamManager.loadAllData().thenRun(() -> {
            getLogger().info("[Database] 已成功将全服 " + teamManager.getAllTeams().size() + " 个团队数据预热至内存缓存！");
        });

        // 5. 注册事件监听器
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        // 6. 注册指令
        TeamCommand teamCommand = new TeamCommand(this);
        if (getCommand("team") != null) {
            getCommand("team").setExecutor(teamCommand);
            getCommand("team").setTabCompleter(teamCommand);
        }

        TeamAdminCommand adminCommand = new TeamAdminCommand(this);
        if (getCommand("teamadmin") != null) {
            getCommand("teamadmin").setExecutor(adminCommand);
            getCommand("teamadmin").setTabCompleter(adminCommand);
        }

        com.balancedteam.command.TeamMsgCommand teamMsgCommand = new com.balancedteam.command.TeamMsgCommand(this);
        if (getCommand("teammsg") != null) {
            getCommand("teammsg").setExecutor(teamMsgCommand);
            getCommand("teammsg").setTabCompleter(teamMsgCommand);
        }

        getLogger().info("[BalancedTeam] 插件启动成功！");
    }

    @Override
    public void onDisable() {
        getLogger().info("[BalancedTeam] 正在安全保存并关闭数据连接池...");
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("[BalancedTeam] 插件已安全卸载。");
    }

    public static BalancedTeamPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public RelationManager getRelationManager() {
        return relationManager;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public com.balancedteam.manager.ApplicationManager getApplicationManager() {
        return applicationManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }
}

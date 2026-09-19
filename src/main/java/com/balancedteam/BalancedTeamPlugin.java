package com.balancedteam;

import com.balancedteam.command.TeamAdminCommand;
import com.balancedteam.command.TeamCommand;
import com.balancedteam.command.TeamLangCommand;
import com.balancedteam.config.ConfigManager;
import com.balancedteam.database.DatabaseManager;
import com.balancedteam.database.dao.AllyRequestDao;
import com.balancedteam.database.dao.InviteDao;
import com.balancedteam.database.dao.MemberDao;
import com.balancedteam.database.dao.RelationDao;
import com.balancedteam.database.dao.TeamDao;
import com.balancedteam.listener.CrystalListener;
import com.balancedteam.listener.DamageListener;
import com.balancedteam.listener.GuiListener;
import com.balancedteam.listener.PlayerListener;
import com.balancedteam.manager.ChatInputManager;
import com.balancedteam.manager.ChatManager;
import com.balancedteam.manager.InviteManager;
import com.balancedteam.manager.ClientLanguageManager;
import com.balancedteam.manager.ServerLanguageManager;
import com.balancedteam.manager.RelationManager;
import com.balancedteam.manager.TeamManager;
import com.balancedteam.util.PAPIUtil;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

/**
 * BalancedTeam 插件主类
 */
public class BalancedTeamPlugin extends JavaPlugin {

    private static BalancedTeamPlugin instance;

    private ServerLanguageManager serverLanguageManager;
    private ClientLanguageManager clientLanguageManager;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;

    private TeamDao teamDao;
    private MemberDao memberDao;
    private RelationDao relationDao;
    private InviteDao inviteDao;
    private AllyRequestDao allyRequestDao;
    private com.balancedteam.database.dao.ApplicationDao applicationDao;
    private com.balancedteam.database.dao.TruceDao truceDao;

    private TeamManager teamManager;
    private RelationManager relationManager;
    private InviteManager inviteManager;
    private com.balancedteam.manager.ApplicationManager applicationManager;
    private ChatManager chatManager;
    private ChatInputManager chatInputManager;
    private CrystalListener crystalListener;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 初始化服务端与客户端多语言管理器及配置管理器
        this.serverLanguageManager = new ServerLanguageManager(this);
        this.clientLanguageManager = new ClientLanguageManager(this);
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.BANNER_LINE);
        com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.BANNER_ENABLING);
        com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.BANNER_LINE);

        // 2. 初始化数据库连接池
        this.databaseManager = new DatabaseManager(this);
        try {
            this.databaseManager.init();
            com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.DB_POOL_INIT_SUCCESS);
        } catch (SQLException e) {
            com.balancedteam.util.PluginLogger.log(Level.SEVERE, com.balancedteam.util.PluginLogger.LogKey.DB_CONNECT_FAIL, e);
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
        this.truceDao = new com.balancedteam.database.dao.TruceDao(databaseManager);

        this.inviteManager = new InviteManager(inviteDao);
        this.applicationManager = new com.balancedteam.manager.ApplicationManager(applicationDao);
        this.relationManager = new RelationManager(this, relationDao, allyRequestDao, truceDao);
        this.chatManager = new ChatManager(this);
        this.chatInputManager = new ChatInputManager(this);
        this.teamManager = new TeamManager(this, teamDao, memberDao);

        // 4. 异步全量预热内存缓存
        relationDao.loadAllRelations().thenAccept(relations -> {
            relationManager.init(relations);
            com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.DB_LOADED_RELATIONS, relations.size());
        });

        allyRequestDao.loadAllValidRequests(System.currentTimeMillis()).thenAccept(requests -> {
            relationManager.initRequests(requests);
            int count = requests.values().stream().mapToInt(map -> map.size()).sum();
            com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.DB_LOADED_ALLY_REQUESTS, count);
        });

        truceDao.loadAllValidTruceRequests(System.currentTimeMillis()).thenAccept(requests -> {
            relationManager.initTruceRequests(requests);
            int count = requests.values().stream().mapToInt(map -> map.size()).sum();
            com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.DB_LOADED_TRUCE_REQUESTS, count);
        });

        truceDao.loadAllValidProtections(System.currentTimeMillis()).thenAccept(protections -> {
            relationManager.initProtections(protections);
            com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.DB_LOADED_POST_WAR_PROTECTIONS, protections.size());
        });

        inviteDao.loadAllValidInvites(System.currentTimeMillis()).thenAccept(invites -> {
            inviteManager.init(invites);
            int count = invites.values().stream().mapToInt(map -> map.size()).sum();
            com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.DB_LOADED_INVITES, count);
        });

        applicationDao.loadAllValidApplications(System.currentTimeMillis()).thenAccept(applications -> {
            applicationManager.init(applications);
            int count = applications.values().stream().mapToInt(map -> map.size()).sum();
            com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.DB_LOADED_APPLICATIONS, count);
        });

        teamManager.loadAllData().thenRun(() -> {
            com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.DB_PRELOAD_TEAMS_SUCCESS, teamManager.getAllTeams().size());
        });

        // 5. 注册事件监听器
        this.crystalListener = new CrystalListener(this);
        getServer().getPluginManager().registerEvents(this.crystalListener, this);
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

        TeamLangCommand teamLangCommand = new TeamLangCommand(this);
        if (getCommand("teamlang") != null) {
            getCommand("teamlang").setExecutor(teamLangCommand);
            getCommand("teamlang").setTabCompleter(teamLangCommand);
        }

        // 7. 注册 PlaceholderAPI 扩展
        if (PAPIUtil.hasPAPI()) {
            if (PAPIUtil.registerExpansion(this)) {
                com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.PAPI_HOOK_SUCCESS);
            } else {
                com.balancedteam.util.PluginLogger.warning(com.balancedteam.util.PluginLogger.LogKey.PAPI_HOOK_FAILED);
            }
        } else {
            com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.PAPI_NOT_FOUND);
        }

        com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.PLUGIN_ENABLED);
    }

    @Override
    public void onDisable() {
        com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.PLUGIN_DISABLING);
        PAPIUtil.unregisterExpansion();
        if (crystalListener != null) {
            crystalListener.clear();
        }
        if (clientLanguageManager != null) {
            clientLanguageManager.saveUserPreferences();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        com.balancedteam.util.PluginLogger.info(com.balancedteam.util.PluginLogger.LogKey.PLUGIN_DISABLED);
    }

    public static BalancedTeamPlugin getInstance() {
        return instance;
    }

    public ServerLanguageManager getServerLanguageManager() {
        return serverLanguageManager;
    }

    public ClientLanguageManager getClientLanguageManager() {
        return clientLanguageManager;
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

    public CrystalListener getCrystalListener() {
        return crystalListener;
    }

    public com.balancedteam.database.dao.TruceDao getTruceDao() {
        return truceDao;
    }
}

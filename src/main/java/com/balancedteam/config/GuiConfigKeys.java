package com.balancedteam.config;

/**
 * 集中管理所有 GUI 相关的语言配置键常量 (lang/*.yml)
 * 避免在各个 GUI 类中硬编码字符串，杜绝拼写错误
 */
public final class GuiConfigKeys {

    private GuiConfigKeys() {
    }

    // ==========================================
    // 1. /team list 队伍列表界面
    // ==========================================
    public static final String LIST_TITLE = "gui.list.title";
    public static final String LIST_TEAM_ITEM_NAME = "gui.list.team_item_name";
    public static final String LIST_TEAM_ITEM_LORE = "gui.list.team_item_lore";
    public static final String LIST_PREV_PAGE = "gui.list.prev_page";
    public static final String LIST_NEXT_PAGE = "gui.list.next_page";
    public static final String LIST_SUMMARY_ITEM_NAME = "gui.list.summary_item_name";
    public static final String LIST_SUMMARY_ITEM_LORE = "gui.list.summary_item_lore";

    // ==========================================
    // 2. 团队详情界面
    // ==========================================
    public static final String DETAIL_TITLE = "gui.detail.title";
    public static final String DETAIL_LEADER_ITEM_NAME = "gui.detail.leader_item_name";
    public static final String DETAIL_LEADER_ITEM_LORE = "gui.detail.leader_item_lore";
    public static final String DETAIL_MEMBERS_ITEM_NAME = "gui.detail.members_item_name";
    public static final String DETAIL_RELATION_ITEM_NAME = "gui.detail.relation_item_name";
    public static final String DETAIL_RELATION_ALLIES_HEADER = "gui.detail.relation_allies_header";
    public static final String DETAIL_RELATION_ENEMIES_HEADER = "gui.detail.relation_enemies_header";
    public static final String DETAIL_RELATION_ALLY_PREFIX = "gui.detail.relation_ally_prefix";
    public static final String DETAIL_RELATION_ENEMY_PREFIX = "gui.detail.relation_enemy_prefix";
    public static final String DETAIL_BACK_BUTTON_IN_TEAM = "gui.detail.back_button_in_team";
    public static final String DETAIL_BACK_BUTTON_NOT_IN_TEAM = "gui.detail.back_button_not_in_team";
    public static final String DETAIL_APPLY_BUTTON_NAME = "gui.detail.apply_button_name";
    public static final String DETAIL_APPLY_BUTTON_LORE = "gui.detail.apply_button_lore";
    public static final String DETAIL_APPLIED_ITEM_NAME = "gui.detail.applied_item_name";
    public static final String DETAIL_APPLIED_ITEM_LORE = "gui.detail.applied_item_lore";
    public static final String DETAIL_FULL_ITEM_NAME = "gui.detail.full_item_name";
    public static final String DETAIL_FULL_ITEM_LORE = "gui.detail.full_item_lore";

    // ==========================================
    // 3. 个人团队控制面板 (/team menu)
    // ==========================================
    public static final String MENU_TITLE = "gui.menu.title";
    public static final String MENU_INFO_ITEM_NAME = "gui.menu.info_item_name";
    public static final String MENU_INFO_ITEM_LORE = "gui.menu.info_item_lore";
    public static final String MENU_FF_ITEM_NAME = "gui.menu.ff_item_name";
    public static final String MENU_FF_STATUS_ON = "gui.menu.ff_status_on";
    public static final String MENU_FF_STATUS_OFF = "gui.menu.ff_status_off";
    public static final String MENU_FF_LEADER_TIP = "gui.menu.ff_leader_tip";
    public static final String MENU_FF_NON_LEADER_TIP = "gui.menu.ff_non_leader_tip";
    public static final String MENU_FF_DISABLED_TIP = "gui.menu.ff_disabled_tip";
    public static final String MENU_INVITE_ITEM_NAME = "gui.menu.invite_item_name";
    public static final String MENU_INVITE_ITEM_LORE = "gui.menu.invite_item_lore";
    public static final String MENU_DISBAND_ITEM_NAME = "gui.menu.disband_item_name";
    public static final String MENU_DISBAND_ITEM_LORE = "gui.menu.disband_item_lore";
    public static final String MENU_LEAVE_ITEM_NAME = "gui.menu.leave_item_name";
    public static final String MENU_LEAVE_ITEM_LORE = "gui.menu.leave_item_lore";
    public static final String MENU_MEMBERS_BUTTON_NAME = "gui.menu.members_button_name";
    public static final String MENU_MEMBERS_BUTTON_LORE = "gui.menu.members_button_lore";
    public static final String MENU_ALLY_BUTTON_NAME = "gui.menu.ally_button_name";
    public static final String MENU_ALLY_BUTTON_LORE = "gui.menu.ally_button_lore";
    public static final String MENU_NOTIFICATION_BUTTON_NAME = "gui.menu.notification_button_name";
    public static final String MENU_NOTIFICATION_BUTTON_LORE = "gui.menu.notification_button_lore";
    public static final String MENU_NOTIFICATION_BUTTON_LORE_PENDING = "gui.menu.notification_button_lore_pending";
    public static final String MENU_ENEMY_BUTTON_NAME = "gui.menu.enemy_button_name";
    public static final String MENU_ENEMY_BUTTON_LORE = "gui.menu.enemy_button_lore";

    // ==========================================
    // 4. 解散团队确认界面 与 退出团队确认界面
    // ==========================================
    public static final String DISBAND_CONFIRM_TITLE = "gui.disband_confirm.title";
    public static final String DISBAND_CONFIRM_NAME = "gui.disband_confirm.confirm_name";
    public static final String DISBAND_CONFIRM_LORE = "gui.disband_confirm.confirm_lore";
    public static final String DISBAND_CANCEL_NAME = "gui.disband_confirm.cancel_name";
    public static final String DISBAND_CANCEL_LORE = "gui.disband_confirm.cancel_lore";

    public static final String LEAVE_CONFIRM_TITLE = "gui.leave_confirm.title";
    public static final String LEAVE_CONFIRM_NAME = "gui.leave_confirm.confirm_name";
    public static final String LEAVE_CONFIRM_LORE = "gui.leave_confirm.confirm_lore";
    public static final String LEAVE_CANCEL_NAME = "gui.leave_confirm.cancel_name";
    public static final String LEAVE_CANCEL_LORE = "gui.leave_confirm.cancel_lore";

    // ==========================================
    // 5. 盟友管理界面
    // ==========================================
    public static final String ALLY_MANAGE_TITLE = "gui.ally_manage.title";
    public static final String ALLY_MANAGE_ITEM_NAME = "gui.ally_manage.ally_item_name";
    public static final String ALLY_MANAGE_ITEM_LORE = "gui.ally_manage.ally_item_lore";
    public static final String ALLY_MANAGE_ITEM_LORE_LEADER = "gui.ally_manage.ally_item_lore_leader";
    public static final String ALLY_MANAGE_NO_ALLY_NAME = "gui.ally_manage.no_ally_name";
    public static final String ALLY_MANAGE_NO_ALLY_LORE = "gui.ally_manage.no_ally_lore";
    public static final String ALLY_MANAGE_ADD_BUTTON_NAME = "gui.ally_manage.add_button_name";
    public static final String ALLY_MANAGE_ADD_BUTTON_LORE = "gui.ally_manage.add_button_lore";
    public static final String ALLY_MANAGE_ADD_BUTTON_LORE_LEADER = "gui.ally_manage.add_button_lore_leader";
    public static final String ALLY_MANAGE_ADD_TIP = "gui.ally_manage.add_tip";
    public static final String ALLY_MANAGE_BACK_BUTTON = "gui.ally_manage.back_button";
    public static final String ALLY_MANAGE_PREV_PAGE = "gui.ally_manage.prev_page";
    public static final String ALLY_MANAGE_NEXT_PAGE = "gui.ally_manage.next_page";

    // ==========================================
    // 6. 敌对管理界面
    // ==========================================
    public static final String ENEMY_MANAGE_TITLE = "gui.enemy_manage.title";
    public static final String ENEMY_MANAGE_ITEM_NAME = "gui.enemy_manage.enemy_item_name";
    public static final String ENEMY_MANAGE_ITEM_LORE = "gui.enemy_manage.enemy_item_lore";
    public static final String ENEMY_MANAGE_ITEM_LORE_LEADER = "gui.enemy_manage.enemy_item_lore_leader";
    public static final String ENEMY_MANAGE_NO_ENEMY_NAME = "gui.enemy_manage.no_enemy_name";
    public static final String ENEMY_MANAGE_NO_ENEMY_LORE = "gui.enemy_manage.no_enemy_lore";
    public static final String ENEMY_MANAGE_ADD_BUTTON_NAME = "gui.enemy_manage.add_button_name";
    public static final String ENEMY_MANAGE_ADD_BUTTON_LORE = "gui.enemy_manage.add_button_lore";
    public static final String ENEMY_MANAGE_ADD_BUTTON_LORE_LEADER = "gui.enemy_manage.add_button_lore_leader";
    public static final String ENEMY_MANAGE_ADD_TIP = "gui.enemy_manage.add_tip";
    public static final String ENEMY_MANAGE_BACK_BUTTON = "gui.enemy_manage.back_button";
    public static final String ENEMY_MANAGE_PREV_PAGE = "gui.enemy_manage.prev_page";
    public static final String ENEMY_MANAGE_NEXT_PAGE = "gui.enemy_manage.next_page";

    // ==========================================
    // 7. 通知中心界面 (入队邀请与同盟申请)
    // ==========================================
    public static final String NOTIFICATION_TITLE = "gui.notification.title";
    public static final String NOTIFICATION_INVITE_ITEM_NAME = "gui.notification.invite_item_name";
    public static final String NOTIFICATION_INVITE_ITEM_LORE = "gui.notification.invite_item_lore";
    public static final String NOTIFICATION_REQUEST_ITEM_NAME = "gui.notification.request_item_name";
    public static final String NOTIFICATION_REQUEST_ITEM_LORE = "gui.notification.request_item_lore";
    public static final String NOTIFICATION_REQUEST_ITEM_LORE_LEADER = "gui.notification.request_item_lore_leader";
    public static final String NOTIFICATION_APPLICATION_ITEM_NAME = "gui.notification.application_item_name";
    public static final String NOTIFICATION_APPLICATION_ITEM_LORE = "gui.notification.application_item_lore";
    public static final String NOTIFICATION_NO_REQUEST_NAME = "gui.notification.no_request_name";
    public static final String NOTIFICATION_NO_REQUEST_LORE = "gui.notification.no_request_lore";
    public static final String NOTIFICATION_BACK_BUTTON = "gui.notification.back_button";
    public static final String NOTIFICATION_PREV_PAGE = "gui.notification.prev_page";
    public static final String NOTIFICATION_NEXT_PAGE = "gui.notification.next_page";

    // ==========================================
    // 8. 未加入队伍主菜单界面
    // ==========================================
    public static final String NOT_JOINED_TITLE = "gui.not_joined.title";
    public static final String NOT_JOINED_LIST_BUTTON_NAME = "gui.not_joined.list_button_name";
    public static final String NOT_JOINED_LIST_BUTTON_LORE = "gui.not_joined.list_button_lore";
    public static final String NOT_JOINED_CREATE_INFO_NAME = "gui.not_joined.create_info_name";
    public static final String NOT_JOINED_CREATE_INFO_LORE = "gui.not_joined.create_info_lore";
    public static final String NOT_JOINED_NOTIFICATION_BUTTON_NAME = "gui.not_joined.notification_button_name";
    public static final String NOT_JOINED_NOTIFICATION_BUTTON_LORE = "gui.not_joined.notification_button_lore";
    public static final String NOT_JOINED_NOTIFICATION_BUTTON_LORE_PENDING = "gui.not_joined.notification_button_lore_pending";
    public static final String NOT_JOINED_CLOSE_BUTTON = "gui.not_joined.close_button";

    // ==========================================
    // 9. 邀请成员 - 在线玩家选择界面
    // ==========================================
    public static final String PLAYER_SELECT_TITLE = "gui.player_select.title";
    public static final String PLAYER_SELECT_ITEM_AVAILABLE_NAME = "gui.player_select.player_item_available_name";
    public static final String PLAYER_SELECT_ITEM_AVAILABLE_LORE = "gui.player_select.player_item_available_lore";
    public static final String PLAYER_SELECT_ITEM_IN_TEAM_NAME = "gui.player_select.player_item_in_team_name";
    public static final String PLAYER_SELECT_ITEM_IN_TEAM_LORE = "gui.player_select.player_item_in_team_lore";
    public static final String PLAYER_SELECT_ITEM_IN_MY_TEAM_NAME = "gui.player_select.player_item_in_my_team_name";
    public static final String PLAYER_SELECT_ITEM_IN_MY_TEAM_LORE = "gui.player_select.player_item_in_my_team_lore";
    public static final String PLAYER_SELECT_ITEM_ALREADY_INVITED_NAME = "gui.player_select.player_item_already_invited_name";
    public static final String PLAYER_SELECT_ITEM_ALREADY_INVITED_LORE = "gui.player_select.player_item_already_invited_lore";
    public static final String PLAYER_SELECT_NO_PLAYERS_NAME = "gui.player_select.no_players_name";
    public static final String PLAYER_SELECT_NO_PLAYERS_LORE = "gui.player_select.no_players_lore";
    public static final String PLAYER_SELECT_MANUAL_INPUT_NAME = "gui.player_select.manual_input_name";
    public static final String PLAYER_SELECT_MANUAL_INPUT_LORE = "gui.player_select.manual_input_lore";
    public static final String PLAYER_SELECT_CHAT_PROMPT = "gui.player_select.chat_prompt";
    public static final String PLAYER_SELECT_BACK_BUTTON = "gui.player_select.back_button";
    public static final String PLAYER_SELECT_PREV_PAGE = "gui.player_select.prev_page";
    public static final String PLAYER_SELECT_NEXT_PAGE = "gui.player_select.next_page";

    // ==========================================
    // 10. 团队成员管理界面
    // ==========================================
    public static final String MEMBER_MANAGE_TITLE = "gui.member_manage.title";
    public static final String MEMBER_MANAGE_ITEM_NAME = "gui.member_manage.member_item_name";
    public static final String MEMBER_MANAGE_ITEM_LORE = "gui.member_manage.member_item_lore";
    public static final String MEMBER_MANAGE_TIP_SELF = "gui.member_manage.tip_self";
    public static final String MEMBER_MANAGE_TIP_PROMOTE = "gui.member_manage.tip_promote";
    public static final String MEMBER_MANAGE_TIP_DEMOTE = "gui.member_manage.tip_demote";
    public static final String MEMBER_MANAGE_TIP_KICK = "gui.member_manage.tip_kick";
    public static final String MEMBER_MANAGE_TIP_TRANSFER = "gui.member_manage.tip_transfer";
    public static final String MEMBER_MANAGE_TIP_NO_PERM = "gui.member_manage.tip_no_perm";
    public static final String MEMBER_MANAGE_TIP_VIEW_ONLY = "gui.member_manage.tip_view_only";
    public static final String MEMBER_MANAGE_INVITE_BUTTON_NAME = "gui.member_manage.invite_button_name";
    public static final String MEMBER_MANAGE_INVITE_BUTTON_LORE = "gui.member_manage.invite_button_lore";
    public static final String MEMBER_MANAGE_BACK_BUTTON = "gui.member_manage.back_button";
    public static final String MEMBER_MANAGE_PREV_PAGE = "gui.member_manage.prev_page";
    public static final String MEMBER_MANAGE_NEXT_PAGE = "gui.member_manage.next_page";

    // ==========================================
    // 11. 目标团队选择界面 (同盟申请 / 宣战快速选择)
    // ==========================================
    public static final String TEAM_SELECT_TITLE_ALLY = "gui.team_select.title_ally";
    public static final String TEAM_SELECT_TITLE_ENEMY = "gui.team_select.title_enemy";
    public static final String TEAM_SELECT_ITEM_AVAILABLE_ALLY_NAME = "gui.team_select.item_available_ally_name";
    public static final String TEAM_SELECT_ITEM_AVAILABLE_ALLY_LORE = "gui.team_select.item_available_ally_lore";
    public static final String TEAM_SELECT_ITEM_AVAILABLE_ENEMY_NAME = "gui.team_select.item_available_enemy_name";
    public static final String TEAM_SELECT_ITEM_AVAILABLE_ENEMY_LORE = "gui.team_select.item_available_enemy_lore";
    public static final String TEAM_SELECT_ITEM_IS_ALLY_NAME = "gui.team_select.item_is_ally_name";
    public static final String TEAM_SELECT_ITEM_IS_ALLY_LORE = "gui.team_select.item_is_ally_lore";
    public static final String TEAM_SELECT_ITEM_IS_ENEMY_NAME = "gui.team_select.item_is_enemy_name";
    public static final String TEAM_SELECT_ITEM_IS_ENEMY_LORE = "gui.team_select.item_is_enemy_lore";
    public static final String TEAM_SELECT_ITEM_PENDING_NAME = "gui.team_select.item_pending_name";
    public static final String TEAM_SELECT_ITEM_PENDING_LORE = "gui.team_select.item_pending_lore";
    public static final String TEAM_SELECT_ITEM_MAX_REACHED_NAME = "gui.team_select.item_max_reached_name";
    public static final String TEAM_SELECT_ITEM_MAX_REACHED_LORE = "gui.team_select.item_max_reached_lore";
    public static final String TEAM_SELECT_NO_TEAMS_NAME = "gui.team_select.no_teams_name";
    public static final String TEAM_SELECT_NO_TEAMS_LORE = "gui.team_select.no_teams_lore";
    public static final String TEAM_SELECT_MANUAL_INPUT_NAME = "gui.team_select.manual_input_name";
    public static final String TEAM_SELECT_MANUAL_INPUT_LORE = "gui.team_select.manual_input_lore";
    public static final String TEAM_SELECT_CHAT_PROMPT_ALLY = "gui.team_select.chat_prompt_ally";
    public static final String TEAM_SELECT_CHAT_PROMPT_ENEMY = "gui.team_select.chat_prompt_enemy";
    public static final String TEAM_SELECT_BACK_BUTTON = "gui.team_select.back_button";
    public static final String TEAM_SELECT_PREV_PAGE = "gui.team_select.prev_page";
    public static final String TEAM_SELECT_NEXT_PAGE = "gui.team_select.next_page";
}

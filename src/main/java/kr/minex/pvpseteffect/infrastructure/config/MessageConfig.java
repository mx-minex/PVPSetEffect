package kr.minex.pvpseteffect.infrastructure.config;

import org.bukkit.ChatColor;

/**
 * 메시지 설정
 *
 * 모든 플러그인 메시지를 중앙에서 관리합니다.
 *
 * @author Junseo5
 */
public final class MessageConfig {

    // ===== 기본 프리픽스 =====
    public static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "세트효과" + ChatColor.GRAY + "] " + ChatColor.WHITE;
    public static final String DIVIDER = ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

    // ===== 성공 메시지 =====
    public static final String SET_CREATED = PREFIX + ChatColor.GREEN + "✔ " + ChatColor.YELLOW + "%s" + ChatColor.WHITE + " 세트효과가 생성되었습니다.";
    public static final String SET_DELETED = PREFIX + ChatColor.GREEN + "✔ " + ChatColor.YELLOW + "%s" + ChatColor.WHITE + " 세트효과가 삭제되었습니다.";
    public static final String SET_CONFIGURED = PREFIX + ChatColor.GREEN + "✔ " + ChatColor.YELLOW + "%s" + ChatColor.WHITE + " 세트효과가 설정되었습니다.";

    // ===== 목록 메시지 =====
    public static final String SET_LIST_HEADER = PREFIX + ChatColor.GOLD + "세트효과 목록";
    public static final String SET_LIST_ITEM = ChatColor.GRAY + "  " + ChatColor.DARK_GRAY + "[" + ChatColor.YELLOW + "%d" + ChatColor.DARK_GRAY + "] " + ChatColor.WHITE + "%s";
    public static final String SET_LIST_EMPTY = PREFIX + ChatColor.GRAY + "등록된 세트효과가 없습니다.";

    // ===== 보너스 설정 메시지 =====
    public static final String ABILITY_SET = PREFIX + ChatColor.GREEN + "✔ " + ChatColor.YELLOW + "%s" + ChatColor.GRAY + " - " +
            ChatColor.AQUA + "%d세트" + ChatColor.WHITE + " 능력: " + ChatColor.GOLD + "%s +%d";
    public static final String POTION_SET = PREFIX + ChatColor.GREEN + "✔ " + ChatColor.YELLOW + "%s" + ChatColor.GRAY + " - " +
            ChatColor.AQUA + "%d세트" + ChatColor.WHITE + " 포션: " + ChatColor.LIGHT_PURPLE + "%s Lv.%d";
    public static final String BONUS_REMOVED = PREFIX + ChatColor.GREEN + "✔ " + ChatColor.YELLOW + "%s" + ChatColor.GRAY + " - " +
            ChatColor.AQUA + "%d세트" + ChatColor.WHITE + " 보너스가 삭제되었습니다.";

    // ===== 보너스 조회 메시지 =====
    public static final String BONUS_VIEW_HEADER = PREFIX + ChatColor.GOLD + "%s" + ChatColor.WHITE + " 보너스 정보";
    public static final String BONUS_VIEW_ITEM = ChatColor.GRAY + "  " + ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "%d세트" + ChatColor.DARK_GRAY + "] " + ChatColor.WHITE + "%s";
    public static final String BONUS_VIEW_EMPTY = ChatColor.GRAY + "  " + ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "%d세트" + ChatColor.DARK_GRAY + "] " + ChatColor.DARK_GRAY + "미설정";

    // ===== 오류 메시지 =====
    public static final String ERROR_SET_EXISTS = PREFIX + ChatColor.RED + "✘ 이미 존재하는 세트효과입니다.";
    public static final String ERROR_SET_NOT_FOUND = PREFIX + ChatColor.RED + "✘ 존재하지 않는 세트효과입니다.";
    public static final String ERROR_NAME_REQUIRED = PREFIX + ChatColor.RED + "✘ 세트효과 이름을 입력해 주세요.";
    public static final String ERROR_NAME_NO_SPACE = PREFIX + ChatColor.GRAY + "  → 이름에 띄어쓰기는 사용할 수 없습니다.";
    public static final String ERROR_NAME_INVALID = PREFIX + ChatColor.RED + "✘ 이름은 한글, 영문, 숫자, '_', '-'만 사용 가능합니다. (최대 32자)";
    public static final String ERROR_PIECES_REQUIRED = PREFIX + ChatColor.RED + "✘ 세트 수를 입력해 주세요. (1~5)";
    public static final String ERROR_PIECES_INVALID = PREFIX + ChatColor.RED + "✘ 세트 수는 1~5 사이의 숫자로 입력해 주세요.";
    public static final String ERROR_PIECES_NUMBER = PREFIX + ChatColor.RED + "✘ 세트 수는 숫자로 입력해 주세요.";
    public static final String ERROR_ABILITY_REQUIRED = PREFIX + ChatColor.RED + "✘ 능력을 입력해 주세요.";
    public static final String ERROR_ABILITY_INVALID = PREFIX + ChatColor.RED + "✘ 올바른 능력을 입력해 주세요. (/세트효과 능력목록)";
    public static final String ERROR_POTION_REQUIRED = PREFIX + ChatColor.RED + "✘ 포션을 입력해 주세요.";
    public static final String ERROR_POTION_INVALID = PREFIX + ChatColor.RED + "✘ 올바른 포션을 입력해 주세요. (/세트효과 포션목록)";
    public static final String ERROR_VALUE_REQUIRED = PREFIX + ChatColor.RED + "✘ 수치를 입력해 주세요.";
    public static final String ERROR_VALUE_NUMBER = PREFIX + ChatColor.RED + "✘ 수치는 숫자로 입력해 주세요.";
    public static final String ERROR_NO_PERMISSION = PREFIX + ChatColor.RED + "✘ 이 명령어를 사용할 권한이 없습니다.";
    public static final String ERROR_PLAYER_ONLY = PREFIX + ChatColor.RED + "✘ 플레이어만 사용할 수 있는 명령어입니다.";

    // ===== 능력/포션 목록 =====
    public static final String ABILITY_LIST_HEADER = PREFIX + ChatColor.GOLD + "사용 가능한 능력 목록";
    public static final String ABILITY_LIST = ChatColor.GRAY + "  " + ChatColor.YELLOW + "%s";
    public static final String ABILITY_LIST_NOTE1 = ChatColor.GRAY + "  → " + ChatColor.WHITE + "회피율, 치명타_확률" + ChatColor.GRAY + "은 100분율 단위 (예: 15 = 15%)";
    public static final String ABILITY_LIST_NOTE2 = ChatColor.GRAY + "  → " + ChatColor.WHITE + "체력" + ChatColor.GRAY + "은 1당 0.5 하트 (예: 10 = 하트 5칸)";

    public static final String POTION_LIST_HEADER = PREFIX + ChatColor.GOLD + "사용 가능한 포션 목록";
    public static final String POTION_LIST = ChatColor.GRAY + "  " + ChatColor.LIGHT_PURPLE + "%s";

    // ===== 도움말 =====
    public static final String HELP_HEADER = DIVIDER;
    public static final String HELP_TITLE = ChatColor.AQUA + "" + ChatColor.BOLD + "    세트효과 시스템" + ChatColor.GRAY + " - 도움말";
    public static final String HELP_CREATE = ChatColor.YELLOW + "  /세트효과 제작 <이름>" + ChatColor.GRAY + " - 새 세트 생성";
    public static final String HELP_DELETE = ChatColor.YELLOW + "  /세트효과 삭제 <이름>" + ChatColor.GRAY + " - 세트 삭제";
    public static final String HELP_LIST = ChatColor.YELLOW + "  /세트효과 목록" + ChatColor.GRAY + " - 전체 목록 조회";
    public static final String HELP_CONFIG = ChatColor.YELLOW + "  /세트효과 설정 <이름>" + ChatColor.GRAY + " - GUI로 장비 설정";
    public static final String HELP_ABILITY = ChatColor.YELLOW + "  /세트효과 능력 <이름> <세트수> <능력> <수치>";
    public static final String HELP_ABILITY_EX = ChatColor.GRAY + "    예) " + ChatColor.WHITE + "/세트효과 능력 드래곤 2 공격력 5";
    public static final String HELP_POTION = ChatColor.YELLOW + "  /세트효과 포션 <이름> <세트수> <포션> <레벨>";
    public static final String HELP_POTION_EX = ChatColor.GRAY + "    예) " + ChatColor.WHITE + "/세트효과 포션 드래곤 3 신속 1";
    public static final String HELP_VIEW = ChatColor.YELLOW + "  /세트효과 능력보기 <이름>" + ChatColor.GRAY + " - 보너스 확인";
    public static final String HELP_ABILITY_LIST = ChatColor.YELLOW + "  /세트효과 능력목록" + ChatColor.GRAY + " - 능력 목록";
    public static final String HELP_POTION_LIST = ChatColor.YELLOW + "  /세트효과 포션목록" + ChatColor.GRAY + " - 포션 목록";
    public static final String HELP_REMOVE_BONUS = ChatColor.YELLOW + "  /세트효과 삭제보너스 <이름> <세트수>" + ChatColor.GRAY + " - 보너스 삭제";
    public static final String HELP_FOOTER = ChatColor.GRAY + "  ※ 영문 명령어: " + ChatColor.WHITE + "/seteffect" + ChatColor.GRAY + " 또는 " + ChatColor.WHITE + "/se";

    private MessageConfig() {
        // Utility class
    }

    public static String format(String message, Object... args) {
        return String.format(message, args);
    }
}

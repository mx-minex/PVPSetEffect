package kr.minex.pvpseteffect.domain.vo;

import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 포션 효과 타입 정의 (한글 매핑)
 *
 * 1.20.1 호환성을 위해 필드 참조 방식 사용
 * 1.20.3+ 에서 getByName()이 deprecated 되었으나
 * 1.20.1 타겟이므로 필드 직접 참조 방식 유지
 */
public enum PotionType {
    STRENGTH("힘", "INCREASE_DAMAGE"),
    POISON("독", "POISON"),
    BLINDNESS("실명", "BLINDNESS"),
    REGENERATION("재생", "REGENERATION"),
    INSTANT_DAMAGE("즉시데미지", "HARM"),
    JUMP_BOOST("점프강화", "JUMP"),
    WATER_BREATHING("친수성", "WATER_BREATHING"),
    SLOWNESS("구속", "SLOW"),
    NIGHT_VISION("야간투시", "NIGHT_VISION"),
    HASTE("성급함", "FAST_DIGGING"),
    INSTANT_HEALTH("즉시회복", "HEAL"),
    FIRE_RESISTANCE("화염저항", "FIRE_RESISTANCE"),
    RESISTANCE("저항", "DAMAGE_RESISTANCE"),
    SPEED("신속", "SPEED"),
    NAUSEA("멀미", "CONFUSION"),
    HUNGER("배고픔", "HUNGER"),
    INVISIBILITY("투명", "INVISIBILITY"),
    WEAKNESS("나약함", "WEAKNESS"),
    MINING_FATIGUE("피로", "SLOW_DIGGING"),
    WITHER("위더", "WITHER");

    private static final Map<String, PotionType> KOREAN_NAME_INDEX = new HashMap<>();
    private static final Map<PotionEffectType, PotionType> BUKKIT_TYPE_INDEX = new HashMap<>();
    private static final String ALL_KOREAN_NAMES;

    static {
        StringBuilder sb = new StringBuilder();
        for (PotionType type : values()) {
            KOREAN_NAME_INDEX.put(type.koreanName, type);
            PotionEffectType bukkitType = type.resolveBukkitType();
            if (bukkitType != null) {
                BUKKIT_TYPE_INDEX.put(bukkitType, type);
            }
            if (sb.length() > 0) sb.append(", ");
            sb.append(type.koreanName);
        }
        ALL_KOREAN_NAMES = sb.toString();
    }

    private final String koreanName;
    private final String bukkitTypeName;
    private volatile PotionEffectType cachedBukkitType;

    PotionType(String koreanName, String bukkitTypeName) {
        this.koreanName = koreanName;
        this.bukkitTypeName = bukkitTypeName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    /**
     * Bukkit PotionEffectType 반환
     * 지연 초기화로 서버 시작 전 호출 문제 방지
     */
    public PotionEffectType getBukkitType() {
        if (cachedBukkitType == null) {
            cachedBukkitType = resolveBukkitType();
        }
        return cachedBukkitType;
    }

    @SuppressWarnings("deprecation")
    private PotionEffectType resolveBukkitType() {
        // 1.20.1에서는 getByName 사용 가능
        // 1.20.3+에서 deprecated 되었지만 하위 호환성 유지
        return PotionEffectType.getByName(bukkitTypeName);
    }

    public static PotionType fromKoreanName(String name) {
        if (name == null) return null;
        return KOREAN_NAME_INDEX.get(name);
    }

    public static PotionType fromBukkitType(PotionEffectType bukkitType) {
        if (bukkitType == null) return null;
        return BUKKIT_TYPE_INDEX.get(bukkitType);
    }

    public static String getAllKoreanNames() {
        return ALL_KOREAN_NAMES;
    }

    /**
     * 주어진 이름이 유효한 포션 타입인지 확인
     */
    public static boolean isValidKoreanName(String name) {
        return name != null && KOREAN_NAME_INDEX.containsKey(name);
    }
}

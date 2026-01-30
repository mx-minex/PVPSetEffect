package kr.minex.pvpseteffect.domain.vo;

import java.util.HashMap;
import java.util.Map;

/**
 * 능력 보너스 타입 정의
 *
 * O(1) 시간 복잡도의 인덱스를 사용하여 빠른 검색을 지원합니다.
 */
public enum AbilityType {
    ATTACK_DAMAGE("공격력", "attack_damage"),
    DEFENSE("방어력", "defense"),
    EVASION("회피율", "evasion"),
    LIFESTEAL("흡혈력", "lifesteal"),
    CRITICAL_CHANCE("치명타_확률", "critical_chance"),
    CRITICAL_DAMAGE("치명타_데미지", "critical_damage"),
    REGENERATION("재생력", "regeneration"),
    MAX_HEALTH("체력", "max_health");

    private static final Map<String, AbilityType> KOREAN_NAME_INDEX = new HashMap<>();
    private static final Map<String, AbilityType> CONFIG_KEY_INDEX = new HashMap<>();
    private static final String ALL_KOREAN_NAMES;

    static {
        StringBuilder sb = new StringBuilder();
        for (AbilityType type : values()) {
            KOREAN_NAME_INDEX.put(type.koreanName, type);
            CONFIG_KEY_INDEX.put(type.configKey, type);
            if (sb.length() > 0) sb.append(", ");
            sb.append(type.koreanName);
        }
        ALL_KOREAN_NAMES = sb.toString();
    }

    private final String koreanName;
    private final String configKey;

    AbilityType(String koreanName, String configKey) {
        this.koreanName = koreanName;
        this.configKey = configKey;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getConfigKey() {
        return configKey;
    }

    /**
     * 한글 이름으로 AbilityType 조회 (O(1))
     *
     * @param name 한글 이름
     * @return 매칭되는 타입 또는 null
     */
    public static AbilityType fromKoreanName(String name) {
        if (name == null) return null;
        return KOREAN_NAME_INDEX.get(name);
    }

    /**
     * 설정 키로 AbilityType 조회 (O(1))
     *
     * @param key 설정 키
     * @return 매칭되는 타입 또는 null
     */
    public static AbilityType fromConfigKey(String key) {
        if (key == null) return null;
        return CONFIG_KEY_INDEX.get(key);
    }

    /**
     * 모든 한글 이름 목록 반환
     *
     * @return 콤마로 구분된 한글 이름 목록
     */
    public static String getAllKoreanNames() {
        return ALL_KOREAN_NAMES;
    }

    /**
     * 주어진 이름이 유효한 능력 타입인지 확인
     *
     * @param name 한글 이름
     * @return 유효한 경우 true
     */
    public static boolean isValidKoreanName(String name) {
        return name != null && KOREAN_NAME_INDEX.containsKey(name);
    }
}

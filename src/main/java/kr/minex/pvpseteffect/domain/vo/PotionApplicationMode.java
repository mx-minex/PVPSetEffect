package kr.minex.pvpseteffect.domain.vo;

/**
 * 포션 효과 적용 모드
 *
 * 세트 효과로 부여되는 포션 효과의 적용 방식을 결정합니다.
 */
public enum PotionApplicationMode {
    /**
     * 즉시 모드 (기본값)
     *
     * - 장비 변경 시 기존 세트 포션 효과 즉시 제거
     * - 새로운 세트 효과 즉시 적용
     * - 빠른 장비 변환에 유리 (효과가 즉시 사라짐)
     */
    IMMEDIATE,

    /**
     * 자연 만료 모드
     *
     * - 포션 효과가 짧은 지속시간(예: 3초)으로 적용됨
     * - 장비 변경 시 기존 효과를 제거하지 않음 (자연 만료 대기)
     * - 주기적인 스케줄러가 효과를 재적용
     * - 부드러운 효과 전환에 유리
     */
    NATURAL;

    /**
     * 문자열에서 모드 파싱
     *
     * @param value 설정 값 (대소문자 무관)
     * @return 파싱된 모드, 잘못된 값이면 IMMEDIATE 반환
     */
    public static PotionApplicationMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return IMMEDIATE;
        }
        try {
            return valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return IMMEDIATE;
        }
    }
}

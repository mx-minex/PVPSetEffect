package kr.minex.pvpseteffect.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PotionType 단위 테스트
 *
 * 참고: getBukkitType()은 서버 환경에서만 테스트 가능
 */
@DisplayName("PotionType 테스트")
class PotionTypeTest {

    @Test
    @DisplayName("20개의 포션 타입이 정의되어 있어야 한다")
    void shouldHaveTwentyPotionTypes() {
        assertEquals(20, PotionType.values().length);
    }

    @Test
    @DisplayName("힘 포션을 한글 이름으로 찾을 수 있어야 한다")
    void shouldFindStrengthByKoreanName() {
        PotionType found = PotionType.fromKoreanName("힘");

        assertNotNull(found);
        assertEquals(PotionType.STRENGTH, found);
    }

    @Test
    @DisplayName("신속 포션을 한글 이름으로 찾을 수 있어야 한다")
    void shouldFindSpeedByKoreanName() {
        PotionType found = PotionType.fromKoreanName("신속");

        assertNotNull(found);
        assertEquals(PotionType.SPEED, found);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown", "invalid", "strength", "STRENGTH"})
    @DisplayName("잘못된 한글 이름은 null을 반환해야 한다")
    void shouldReturnNullForInvalidKoreanName(String invalidName) {
        assertNull(PotionType.fromKoreanName(invalidName));
    }

    @Test
    @DisplayName("getAllKoreanNames가 모든 한글 이름을 포함해야 한다")
    void shouldGetAllKoreanNames() {
        String allNames = PotionType.getAllKoreanNames();

        assertNotNull(allNames);
        assertTrue(allNames.contains("힘"));
        assertTrue(allNames.contains("신속"));
        assertTrue(allNames.contains("독"));
        assertTrue(allNames.contains("재생"));
    }

    @Test
    @DisplayName("isValidKoreanName이 유효한 이름에 대해 true를 반환해야 한다")
    void shouldValidateValidKoreanNames() {
        assertTrue(PotionType.isValidKoreanName("힘"));
        assertTrue(PotionType.isValidKoreanName("신속"));
        assertTrue(PotionType.isValidKoreanName("독"));
    }

    @Test
    @DisplayName("isValidKoreanName이 잘못된 이름에 대해 false를 반환해야 한다")
    void shouldInvalidateInvalidKoreanNames() {
        assertFalse(PotionType.isValidKoreanName(null));
        assertFalse(PotionType.isValidKoreanName(""));
        assertFalse(PotionType.isValidKoreanName("unknown"));
    }

    @Test
    @DisplayName("각 포션 타입의 한글 이름이 비어있지 않아야 한다")
    void shouldHaveNonEmptyKoreanNames() {
        for (PotionType type : PotionType.values()) {
            assertNotNull(type.getKoreanName());
            assertFalse(type.getKoreanName().isEmpty());
        }
    }

    @Test
    @DisplayName("한글 이름으로 조회한 결과가 자기 자신이어야 한다")
    void shouldFindSelfByKoreanName() {
        for (PotionType type : PotionType.values()) {
            PotionType found = PotionType.fromKoreanName(type.getKoreanName());
            assertEquals(type, found,
                    "포션 " + type + "을(를) 한글 이름 '" + type.getKoreanName() + "'으로 찾을 수 없습니다");
        }
    }

    @Test
    @DisplayName("모든 한글 이름이 고유해야 한다")
    void shouldHaveUniqueKoreanNames() {
        PotionType[] types = PotionType.values();

        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i].getKoreanName(), types[j].getKoreanName(),
                        "포션 " + types[i] + "와 " + types[j] + "의 한글 이름이 중복됩니다");
            }
        }
    }
}

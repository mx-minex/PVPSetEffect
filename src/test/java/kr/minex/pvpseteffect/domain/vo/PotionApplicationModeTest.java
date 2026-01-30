package kr.minex.pvpseteffect.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PotionApplicationMode 열거형 테스트
 */
class PotionApplicationModeTest {

    @Test
    @DisplayName("두 가지 모드가 존재해야 한다")
    void 모드_개수_테스트() {
        assertEquals(2, PotionApplicationMode.values().length);
    }

    @Test
    @DisplayName("IMMEDIATE 모드가 존재해야 한다")
    void IMMEDIATE_모드_존재_테스트() {
        assertNotNull(PotionApplicationMode.IMMEDIATE);
        assertEquals("IMMEDIATE", PotionApplicationMode.IMMEDIATE.name());
    }

    @Test
    @DisplayName("NATURAL 모드가 존재해야 한다")
    void NATURAL_모드_존재_테스트() {
        assertNotNull(PotionApplicationMode.NATURAL);
        assertEquals("NATURAL", PotionApplicationMode.NATURAL.name());
    }

    @ParameterizedTest
    @CsvSource({
            "IMMEDIATE, IMMEDIATE",
            "immediate, IMMEDIATE",
            "Immediate, IMMEDIATE",
            "NATURAL, NATURAL",
            "natural, NATURAL",
            "Natural, NATURAL"
    })
    @DisplayName("대소문자 무관하게 올바른 모드를 파싱해야 한다")
    void 문자열_파싱_테스트(String input, PotionApplicationMode expected) {
        assertEquals(expected, PotionApplicationMode.fromString(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "invalid", "UNKNOWN", "123"})
    @DisplayName("잘못된 값은 IMMEDIATE를 반환해야 한다")
    void 잘못된_값_기본값_테스트(String input) {
        assertEquals(PotionApplicationMode.IMMEDIATE, PotionApplicationMode.fromString(input));
    }

    @Test
    @DisplayName("공백이 포함된 문자열도 올바르게 파싱해야 한다")
    void 공백_포함_파싱_테스트() {
        assertEquals(PotionApplicationMode.NATURAL, PotionApplicationMode.fromString("  NATURAL  "));
        assertEquals(PotionApplicationMode.IMMEDIATE, PotionApplicationMode.fromString("  IMMEDIATE  "));
    }
}

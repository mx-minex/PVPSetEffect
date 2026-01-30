package kr.minex.pvpseteffect.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbilityType 단위 테스트
 */
@DisplayName("AbilityType 테스트")
class AbilityTypeTest {

    @Test
    @DisplayName("모든 능력 타입이 정의되어 있어야 한다")
    void shouldHaveAllAbilityTypes() {
        assertEquals(8, AbilityType.values().length);
    }

    @ParameterizedTest
    @CsvSource({
            "공격력, ATTACK_DAMAGE",
            "방어력, DEFENSE",
            "회피율, EVASION",
            "흡혈력, LIFESTEAL",
            "치명타_확률, CRITICAL_CHANCE",
            "치명타_데미지, CRITICAL_DAMAGE",
            "재생력, REGENERATION",
            "체력, MAX_HEALTH"
    })
    @DisplayName("한글 이름으로 능력 타입을 찾을 수 있어야 한다")
    void shouldFindByKoreanName(String koreanName, String expectedTypeName) {
        AbilityType found = AbilityType.fromKoreanName(koreanName);

        assertNotNull(found);
        assertEquals(expectedTypeName, found.name());
    }

    @ParameterizedTest
    @CsvSource({
            "attack_damage, ATTACK_DAMAGE",
            "defense, DEFENSE",
            "evasion, EVASION",
            "lifesteal, LIFESTEAL",
            "critical_chance, CRITICAL_CHANCE",
            "critical_damage, CRITICAL_DAMAGE",
            "regeneration, REGENERATION",
            "max_health, MAX_HEALTH"
    })
    @DisplayName("설정 키로 능력 타입을 찾을 수 있어야 한다")
    void shouldFindByConfigKey(String configKey, String expectedTypeName) {
        AbilityType found = AbilityType.fromConfigKey(configKey);

        assertNotNull(found);
        assertEquals(expectedTypeName, found.name());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown", "invalid", "힘", "speed"})
    @DisplayName("잘못된 한글 이름은 null을 반환해야 한다")
    void shouldReturnNullForInvalidKoreanName(String invalidName) {
        assertNull(AbilityType.fromKoreanName(invalidName));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"unknown", "invalid", "attack", "damage"})
    @DisplayName("잘못된 설정 키는 null을 반환해야 한다")
    void shouldReturnNullForInvalidConfigKey(String invalidKey) {
        assertNull(AbilityType.fromConfigKey(invalidKey));
    }

    @Test
    @DisplayName("getAllKoreanNames가 모든 한글 이름을 포함해야 한다")
    void shouldGetAllKoreanNames() {
        String allNames = AbilityType.getAllKoreanNames();

        assertNotNull(allNames);
        assertTrue(allNames.contains("공격력"));
        assertTrue(allNames.contains("방어력"));
        assertTrue(allNames.contains("회피율"));
        assertTrue(allNames.contains("흡혈력"));
        assertTrue(allNames.contains("치명타_확률"));
        assertTrue(allNames.contains("치명타_데미지"));
        assertTrue(allNames.contains("재생력"));
        assertTrue(allNames.contains("체력"));
    }

    @Test
    @DisplayName("isValidKoreanName이 유효한 이름에 대해 true를 반환해야 한다")
    void shouldValidateValidKoreanNames() {
        assertTrue(AbilityType.isValidKoreanName("공격력"));
        assertTrue(AbilityType.isValidKoreanName("방어력"));
    }

    @Test
    @DisplayName("isValidKoreanName이 잘못된 이름에 대해 false를 반환해야 한다")
    void shouldInvalidateInvalidKoreanNames() {
        assertFalse(AbilityType.isValidKoreanName(null));
        assertFalse(AbilityType.isValidKoreanName(""));
        assertFalse(AbilityType.isValidKoreanName("unknown"));
    }

    @Test
    @DisplayName("각 능력 타입의 한글 이름과 설정 키가 일치해야 한다")
    void shouldHaveConsistentNamesAndKeys() {
        for (AbilityType type : AbilityType.values()) {
            assertNotNull(type.getKoreanName());
            assertNotNull(type.getConfigKey());
            assertFalse(type.getKoreanName().isEmpty());
            assertFalse(type.getConfigKey().isEmpty());

            // 한글 이름으로 조회한 결과가 자기 자신이어야 한다
            assertEquals(type, AbilityType.fromKoreanName(type.getKoreanName()));
            // 설정 키로 조회한 결과가 자기 자신이어야 한다
            assertEquals(type, AbilityType.fromConfigKey(type.getConfigKey()));
        }
    }
}

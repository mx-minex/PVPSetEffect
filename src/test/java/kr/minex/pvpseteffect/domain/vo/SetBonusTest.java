package kr.minex.pvpseteffect.domain.vo;

import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * SetBonus 단위 테스트
 */
@DisplayName("SetBonus 테스트")
class SetBonusTest {

    @Test
    @DisplayName("능력 보너스를 생성할 수 있어야 한다")
    void shouldCreateAbilityBonus() {
        SetBonus bonus = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);

        assertNotNull(bonus);
        assertEquals(2, bonus.getRequiredPieces());
        assertEquals(SetBonus.BonusCategory.ABILITY, bonus.getCategory());
        assertEquals(AbilityType.ATTACK_DAMAGE, bonus.getAbilityType());
        assertEquals(10, bonus.getValue());
        assertTrue(bonus.isAbilityBonus());
        assertFalse(bonus.isPotionBonus());
        assertNull(bonus.getPotionType());
    }

    @Test
    @DisplayName("포션 보너스를 생성할 수 있어야 한다")
    void shouldCreatePotionBonus() {
        PotionEffectType mockPotionType = mock(PotionEffectType.class);
        SetBonus bonus = SetBonus.createPotionBonus(3, mockPotionType, 2);

        assertNotNull(bonus);
        assertEquals(3, bonus.getRequiredPieces());
        assertEquals(SetBonus.BonusCategory.POTION, bonus.getCategory());
        assertEquals(mockPotionType, bonus.getPotionType());
        assertEquals(2, bonus.getValue());
        assertFalse(bonus.isAbilityBonus());
        assertTrue(bonus.isPotionBonus());
        assertNull(bonus.getAbilityType());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("유효한 세트 수(1-5)로 보너스를 생성할 수 있어야 한다")
    void shouldCreateBonusWithValidPieces(int pieces) {
        SetBonus bonus = SetBonus.createAbilityBonus(pieces, AbilityType.DEFENSE, 5);

        assertNotNull(bonus);
        assertEquals(pieces, bonus.getRequiredPieces());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 10, 100})
    @DisplayName("잘못된 세트 수로 보너스 생성 시 예외가 발생해야 한다")
    void shouldThrowExceptionForInvalidPieces(int invalidPieces) {
        assertThrows(IllegalArgumentException.class, () ->
                SetBonus.createAbilityBonus(invalidPieces, AbilityType.DEFENSE, 5));
    }

    @Test
    @DisplayName("null 능력 타입으로 보너스 생성 시 예외가 발생해야 한다")
    void shouldThrowExceptionForNullAbilityType() {
        assertThrows(NullPointerException.class, () ->
                SetBonus.createAbilityBonus(2, null, 5));
    }

    @Test
    @DisplayName("null 포션 타입으로 보너스 생성 시 예외가 발생해야 한다")
    void shouldThrowExceptionForNullPotionType() {
        assertThrows(NullPointerException.class, () ->
                SetBonus.createPotionBonus(2, null, 1));
    }

    @Test
    @DisplayName("동일한 능력 보너스는 equals가 true여야 한다")
    void shouldBeEqualForSameAbilityBonus() {
        SetBonus bonus1 = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);
        SetBonus bonus2 = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);

        assertEquals(bonus1, bonus2);
        assertEquals(bonus1.hashCode(), bonus2.hashCode());
    }

    @Test
    @DisplayName("다른 능력 보너스는 equals가 false여야 한다")
    void shouldNotBeEqualForDifferentAbilityBonus() {
        SetBonus bonus1 = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);
        SetBonus bonus2 = SetBonus.createAbilityBonus(2, AbilityType.DEFENSE, 10);
        SetBonus bonus3 = SetBonus.createAbilityBonus(3, AbilityType.ATTACK_DAMAGE, 10);
        SetBonus bonus4 = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 15);

        assertNotEquals(bonus1, bonus2);
        assertNotEquals(bonus1, bonus3);
        assertNotEquals(bonus1, bonus4);
    }

    @Test
    @DisplayName("toString이 올바른 형식을 반환해야 한다")
    void shouldReturnCorrectToString() {
        SetBonus bonus = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);

        String str = bonus.toString();
        assertTrue(str.contains("2세트"));
        assertTrue(str.contains("공격력"));
        assertTrue(str.contains("10"));
    }

    @Test
    @DisplayName("음수 값도 허용해야 한다 (디버프용)")
    void shouldAllowNegativeValues() {
        SetBonus bonus = SetBonus.createAbilityBonus(1, AbilityType.DEFENSE, -5);

        assertEquals(-5, bonus.getValue());
    }

    @Test
    @DisplayName("0 값도 허용해야 한다")
    void shouldAllowZeroValue() {
        SetBonus bonus = SetBonus.createAbilityBonus(1, AbilityType.DEFENSE, 0);

        assertEquals(0, bonus.getValue());
    }
}

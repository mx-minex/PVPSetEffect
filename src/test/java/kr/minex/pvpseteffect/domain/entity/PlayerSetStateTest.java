package kr.minex.pvpseteffect.domain.entity;

import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import kr.minex.pvpseteffect.domain.vo.AbilityType;
import kr.minex.pvpseteffect.domain.vo.SetBonus;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * PlayerSetState 단위 테스트
 */
@DisplayName("PlayerSetState 테스트")
class PlayerSetStateTest {

    private UUID playerId;
    private PlayerSetState state;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        state = new PlayerSetState(playerId);
    }

    @Test
    @DisplayName("새로운 상태는 비어있어야 한다")
    void shouldBeEmptyOnCreation() {
        assertFalse(state.hasAnyBonus());
        assertFalse(state.hasAnyActiveSet());
        assertEquals(playerId, state.getPlayerId());
        assertTrue(state.getLastUpdated() > 0);
    }

    @Test
    @DisplayName("null playerId로 생성 시 예외가 발생해야 한다")
    void shouldThrowExceptionForNullPlayerId() {
        assertThrows(NullPointerException.class, () -> new PlayerSetState(null));
    }

    @Test
    @DisplayName("활성 세트 조각을 설정하고 조회할 수 있어야 한다")
    void shouldSetAndGetActiveSetPieces() {
        state.setActiveSetPieces("testSet", 3);

        assertEquals(3, state.getActiveSetPieces("testSet"));
        assertTrue(state.hasAnyActiveSet());
    }

    @Test
    @DisplayName("0 이하의 조각 수는 세트를 제거해야 한다")
    void shouldRemoveSetWhenPiecesIsZeroOrLess() {
        state.setActiveSetPieces("testSet", 3);
        state.setActiveSetPieces("testSet", 0);

        assertEquals(0, state.getActiveSetPieces("testSet"));
        assertFalse(state.hasAnyActiveSet());
    }

    @Test
    @DisplayName("능력 보너스를 적용하고 조회할 수 있어야 한다")
    void shouldApplyAndGetAbilityBonuses() {
        SetBonus attackBonus = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);
        SetBonus defenseBonus = SetBonus.createAbilityBonus(3, AbilityType.DEFENSE, 5);

        state.applyBonuses(Arrays.asList(attackBonus, defenseBonus));

        assertEquals(10.0, state.getAttackBonus());
        assertEquals(5.0, state.getDefenseBonus());
        assertTrue(state.hasAnyBonus());
    }

    @Test
    @DisplayName("같은 능력 보너스는 누적되어야 한다")
    void shouldAccumulateAbilityBonuses() {
        SetBonus bonus1 = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);
        SetBonus bonus2 = SetBonus.createAbilityBonus(3, AbilityType.ATTACK_DAMAGE, 5);

        state.applyBonuses(Arrays.asList(bonus1, bonus2));

        assertEquals(15.0, state.getAttackBonus());
    }

    @Test
    @DisplayName("포션 보너스는 최대값을 유지해야 한다")
    void shouldKeepMaxPotionBonus() {
        PotionEffectType mockType = mock(PotionEffectType.class);

        SetBonus bonus1 = SetBonus.createPotionBonus(2, mockType, 1);
        SetBonus bonus2 = SetBonus.createPotionBonus(3, mockType, 3);
        SetBonus bonus3 = SetBonus.createPotionBonus(4, mockType, 2);

        state.applyBonuses(Arrays.asList(bonus1, bonus2, bonus3));

        assertEquals(3, state.getPotionLevel(mockType));
    }

    @Test
    @DisplayName("존재하지 않는 포션의 레벨은 -1이어야 한다")
    void shouldReturnMinusOneForNonExistentPotion() {
        PotionEffectType mockType = mock(PotionEffectType.class);

        assertEquals(-1, state.getPotionLevel(mockType));
        assertFalse(state.hasPotionBonus(mockType));
    }

    @Test
    @DisplayName("clear 호출 시 모든 상태가 초기화되어야 한다")
    void shouldClearAllState() {
        SetBonus bonus = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);
        state.setActiveSetPieces("testSet", 3);
        state.applyBonuses(Arrays.asList(bonus));

        state.clear();

        assertFalse(state.hasAnyBonus());
        assertFalse(state.hasAnyActiveSet());
        assertEquals(0.0, state.getAttackBonus());
    }

    @Test
    @DisplayName("모든 개별 능력 getter가 올바르게 동작해야 한다")
    void shouldGetAllIndividualAbilities() {
        state.applyBonuses(Arrays.asList(
                SetBonus.createAbilityBonus(1, AbilityType.ATTACK_DAMAGE, 10),
                SetBonus.createAbilityBonus(1, AbilityType.DEFENSE, 20),
                SetBonus.createAbilityBonus(1, AbilityType.EVASION, 30),
                SetBonus.createAbilityBonus(1, AbilityType.LIFESTEAL, 40),
                SetBonus.createAbilityBonus(1, AbilityType.CRITICAL_CHANCE, 50),
                SetBonus.createAbilityBonus(1, AbilityType.CRITICAL_DAMAGE, 60),
                SetBonus.createAbilityBonus(1, AbilityType.REGENERATION, 70),
                SetBonus.createAbilityBonus(1, AbilityType.MAX_HEALTH, 80)
        ));

        assertEquals(10.0, state.getAttackBonus());
        assertEquals(20.0, state.getDefenseBonus());
        assertEquals(30.0, state.getEvasionChance());
        assertEquals(40.0, state.getLifestealAmount());
        assertEquals(50.0, state.getCriticalChance());
        assertEquals(60.0, state.getCriticalDamage());
        assertEquals(70.0, state.getRegenerationBonus());
        assertEquals(80.0, state.getMaxHealthBonus());
    }

    @Test
    @DisplayName("getAllActiveSetPieces는 unmodifiable 맵을 반환해야 한다")
    void shouldReturnUnmodifiableActiveSetPieces() {
        state.setActiveSetPieces("testSet", 3);

        assertThrows(UnsupportedOperationException.class, () ->
                state.getAllActiveSetPieces().put("newSet", 1));
    }

    @Test
    @DisplayName("getAllAbilityBonuses는 unmodifiable 맵을 반환해야 한다")
    void shouldReturnUnmodifiableAbilityBonuses() {
        SetBonus bonus = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);
        state.applyBonuses(Arrays.asList(bonus));

        assertThrows(UnsupportedOperationException.class, () ->
                state.getAllAbilityBonuses().put(AbilityType.DEFENSE, 5.0));
    }

    @Test
    @DisplayName("getAllPotionBonuses는 unmodifiable 맵을 반환해야 한다")
    void shouldReturnUnmodifiablePotionBonuses() {
        PotionEffectType mockType = mock(PotionEffectType.class);
        SetBonus bonus = SetBonus.createPotionBonus(2, mockType, 1);
        state.applyBonuses(Arrays.asList(bonus));

        assertThrows(UnsupportedOperationException.class, () ->
                state.getAllPotionBonuses().put(mockType, 5));
    }

    @Test
    @DisplayName("toString이 유효한 문자열을 반환해야 한다")
    void shouldReturnValidToString() {
        String str = state.toString();

        assertNotNull(str);
        assertTrue(str.contains("PlayerSetState"));
        assertTrue(str.contains(playerId.toString()));
    }

    @Test
    @DisplayName("lastUpdated가 상태 변경 시 업데이트되어야 한다")
    void shouldUpdateLastUpdatedOnStateChange() throws InterruptedException {
        long initial = state.getLastUpdated();

        Thread.sleep(10);
        state.setActiveSetPieces("testSet", 1);

        assertTrue(state.getLastUpdated() >= initial);
    }
}

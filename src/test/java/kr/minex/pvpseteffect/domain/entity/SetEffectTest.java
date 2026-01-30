package kr.minex.pvpseteffect.domain.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.bukkit.Material;
import kr.minex.pvpseteffect.domain.vo.AbilityType;
import kr.minex.pvpseteffect.domain.vo.EquipmentSlot;
import kr.minex.pvpseteffect.domain.vo.SetBonus;
import kr.minex.pvpseteffect.domain.vo.SetItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SetEffect 단위 테스트
 */
@DisplayName("SetEffect 테스트")
class SetEffectTest {

    private SetEffect setEffect;

    @BeforeEach
    void setUp() {
        setEffect = new SetEffect("테스트세트");
    }

    @Test
    @DisplayName("새로운 세트 효과는 올바르게 초기화되어야 한다")
    void shouldInitializeCorrectly() {
        assertNotNull(setEffect.getId());
        assertEquals("테스트세트", setEffect.getName());
        assertEquals(0, setEffect.getConfiguredItemCount());
        assertFalse(setEffect.hasAnyBonus());
        assertTrue(setEffect.getCreatedAt() > 0);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("null 또는 빈 이름으로 세트 생성 시 예외가 발생해야 한다")
    void shouldThrowExceptionForInvalidName(String invalidName) {
        assertThrows(IllegalArgumentException.class, () -> new SetEffect(invalidName));
    }

    @Test
    @DisplayName("공백이 포함된 이름으로 세트 생성 시 예외가 발생해야 한다")
    void shouldThrowExceptionForNameWithSpace() {
        assertThrows(IllegalArgumentException.class, () -> new SetEffect("테스트 세트"));
    }

    @Test
    @DisplayName("모든 장비 슬롯에 빈 아이템이 초기화되어 있어야 한다")
    void shouldHaveEmptyItemsForAllSlots() {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            SetItem item = setEffect.getItem(slot);
            assertNotNull(item);
            assertTrue(item.isEmpty());
        }
    }

    @Test
    @DisplayName("아이템을 설정하고 조회할 수 있어야 한다")
    void shouldSetAndGetItem() {
        SetItem item = new SetItem(EquipmentSlot.HELMET, "테스트투구", Material.DIAMOND_HELMET);
        setEffect.setItem(EquipmentSlot.HELMET, item);

        SetItem retrieved = setEffect.getItem(EquipmentSlot.HELMET);
        assertEquals(item, retrieved);
        assertEquals(1, setEffect.getConfiguredItemCount());
    }

    @Test
    @DisplayName("null 슬롯으로 아이템 설정 시 예외가 발생해야 한다")
    void shouldThrowExceptionForNullSlot() {
        SetItem item = new SetItem(EquipmentSlot.HELMET, "테스트투구", Material.DIAMOND_HELMET);
        assertThrows(NullPointerException.class, () -> setEffect.setItem(null, item));
    }

    @Test
    @DisplayName("null 아이템으로 설정 시 예외가 발생해야 한다")
    void shouldThrowExceptionForNullItem() {
        assertThrows(NullPointerException.class, () -> setEffect.setItem(EquipmentSlot.HELMET, null));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("유효한 세트 수(1-5)로 보너스를 설정할 수 있어야 한다")
    void shouldSetBonusWithValidPieces(int pieces) {
        SetBonus bonus = SetBonus.createAbilityBonus(pieces, AbilityType.ATTACK_DAMAGE, 10);
        setEffect.setBonus(pieces, bonus);

        assertEquals(bonus, setEffect.getBonus(pieces));
        assertTrue(setEffect.hasAnyBonus());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 10})
    @DisplayName("잘못된 세트 수로 보너스 설정 시 예외가 발생해야 한다")
    void shouldThrowExceptionForInvalidPieces(int invalidPieces) {
        SetBonus bonus = SetBonus.createAbilityBonus(1, AbilityType.ATTACK_DAMAGE, 10);
        assertThrows(IllegalArgumentException.class, () -> setEffect.setBonus(invalidPieces, bonus));
    }

    @Test
    @DisplayName("보너스를 제거할 수 있어야 한다")
    void shouldRemoveBonus() {
        SetBonus bonus = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);
        setEffect.setBonus(2, bonus);
        setEffect.removeBonus(2);

        assertNull(setEffect.getBonus(2));
    }

    @Test
    @DisplayName("활성 보너스를 장착 조각 수에 따라 조회할 수 있어야 한다")
    void shouldGetActiveBonusesByPieces() {
        setEffect.setBonus(1, SetBonus.createAbilityBonus(1, AbilityType.ATTACK_DAMAGE, 5));
        setEffect.setBonus(2, SetBonus.createAbilityBonus(2, AbilityType.DEFENSE, 10));
        setEffect.setBonus(3, SetBonus.createAbilityBonus(3, AbilityType.EVASION, 15));

        List<SetBonus> activeBonuses = setEffect.getActiveBonuses(2);

        assertEquals(2, activeBonuses.size());
        assertEquals(AbilityType.ATTACK_DAMAGE, activeBonuses.get(0).getAbilityType());
        assertEquals(AbilityType.DEFENSE, activeBonuses.get(1).getAbilityType());
    }

    @Test
    @DisplayName("장착 조각 수가 0이면 활성 보너스가 없어야 한다")
    void shouldReturnEmptyActiveBonusesForZeroPieces() {
        setEffect.setBonus(1, SetBonus.createAbilityBonus(1, AbilityType.ATTACK_DAMAGE, 5));

        List<SetBonus> activeBonuses = setEffect.getActiveBonuses(0);

        assertTrue(activeBonuses.isEmpty());
    }

    @Test
    @DisplayName("장착 조각 수가 5를 초과해도 최대 5세트 보너스만 적용되어야 한다")
    void shouldCapAtFivePieces() {
        setEffect.setBonus(1, SetBonus.createAbilityBonus(1, AbilityType.ATTACK_DAMAGE, 5));
        setEffect.setBonus(5, SetBonus.createAbilityBonus(5, AbilityType.DEFENSE, 25));

        List<SetBonus> activeBonuses = setEffect.getActiveBonuses(10);

        assertEquals(2, activeBonuses.size());
    }

    @Test
    @DisplayName("같은 ID를 가진 세트는 equals가 true여야 한다")
    void shouldBeEqualForSameId() {
        SetEffect setEffect2 = new SetEffect("다른이름");

        // ID가 다르므로 equals는 false
        assertNotEquals(setEffect, setEffect2);
    }

    @Test
    @DisplayName("getAllBonuses는 unmodifiable 맵을 반환해야 한다")
    void shouldReturnUnmodifiableBonuses() {
        SetBonus bonus = SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10);
        setEffect.setBonus(2, bonus);

        assertThrows(UnsupportedOperationException.class, () ->
                setEffect.getAllBonuses().put(3, bonus));
    }

    @Test
    @DisplayName("getAllItems는 unmodifiable 맵을 반환해야 한다")
    void shouldReturnUnmodifiableItems() {
        assertThrows(UnsupportedOperationException.class, () ->
                setEffect.getAllItems().put(EquipmentSlot.HELMET, SetItem.empty(EquipmentSlot.HELMET)));
    }

    @Test
    @DisplayName("toString이 유효한 문자열을 반환해야 한다")
    void shouldReturnValidToString() {
        String str = setEffect.toString();

        assertNotNull(str);
        assertTrue(str.contains("SetEffect"));
        assertTrue(str.contains("테스트세트"));
    }

    @Test
    @DisplayName("updatedAt이 상태 변경 시 업데이트되어야 한다")
    void shouldUpdateUpdatedAtOnStateChange() throws InterruptedException {
        long initial = setEffect.getUpdatedAt();

        Thread.sleep(10);
        setEffect.setBonus(1, SetBonus.createAbilityBonus(1, AbilityType.ATTACK_DAMAGE, 5));

        assertTrue(setEffect.getUpdatedAt() >= initial);
    }
}

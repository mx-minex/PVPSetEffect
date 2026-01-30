package kr.minex.pvpseteffect.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EquipmentSlot 단위 테스트
 */
@DisplayName("EquipmentSlot 테스트")
class EquipmentSlotTest {

    @Test
    @DisplayName("5개의 장비 슬롯이 정의되어 있어야 한다")
    void shouldHaveFiveSlots() {
        assertEquals(5, EquipmentSlot.values().length);
        assertEquals(5, EquipmentSlot.getTotalSlots());
    }

    @ParameterizedTest
    @CsvSource({
            "0, HELMET, 투구",
            "1, CHESTPLATE, 갑옷",
            "2, LEGGINGS, 레깅스",
            "3, BOOTS, 부츠",
            "4, WEAPON, 무기"
    })
    @DisplayName("각 슬롯의 인덱스와 한글 이름이 올바르게 설정되어 있어야 한다")
    void shouldHaveCorrectIndexAndDisplayName(int index, String slotName, String displayName) {
        EquipmentSlot slot = EquipmentSlot.valueOf(slotName);

        assertEquals(index, slot.getIndex());
        assertEquals(displayName, slot.getDisplayName());
    }

    @ParameterizedTest
    @CsvSource({
            "0, HELMET",
            "1, CHESTPLATE",
            "2, LEGGINGS",
            "3, BOOTS",
            "4, WEAPON"
    })
    @DisplayName("인덱스로 슬롯을 찾을 수 있어야 한다")
    void shouldFindByIndex(int index, String expectedSlotName) {
        EquipmentSlot slot = EquipmentSlot.fromIndex(index);

        assertNotNull(slot);
        assertEquals(expectedSlotName, slot.name());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 5, 10, 100, -100})
    @DisplayName("잘못된 인덱스는 null을 반환해야 한다")
    void shouldReturnNullForInvalidIndex(int invalidIndex) {
        assertNull(EquipmentSlot.fromIndex(invalidIndex));
    }

    @Test
    @DisplayName("모든 슬롯의 인덱스가 고유해야 한다")
    void shouldHaveUniqueIndices() {
        EquipmentSlot[] slots = EquipmentSlot.values();

        for (int i = 0; i < slots.length; i++) {
            for (int j = i + 1; j < slots.length; j++) {
                assertNotEquals(slots[i].getIndex(), slots[j].getIndex(),
                        "슬롯 " + slots[i] + "와 " + slots[j] + "의 인덱스가 중복됩니다");
            }
        }
    }

    @Test
    @DisplayName("인덱스가 0부터 연속으로 할당되어 있어야 한다")
    void shouldHaveConsecutiveIndices() {
        for (int i = 0; i < EquipmentSlot.getTotalSlots(); i++) {
            EquipmentSlot slot = EquipmentSlot.fromIndex(i);
            assertNotNull(slot, "인덱스 " + i + "에 해당하는 슬롯이 없습니다");
        }
    }
}

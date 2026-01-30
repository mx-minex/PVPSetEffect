package kr.minex.pvpseteffect.application.service;

import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import kr.minex.pvpseteffect.domain.entity.SetEffect;
import kr.minex.pvpseteffect.domain.repository.SetEffectRepository;
import kr.minex.pvpseteffect.domain.vo.AbilityType;
import kr.minex.pvpseteffect.domain.vo.EquipmentSlot;
import kr.minex.pvpseteffect.domain.vo.SetBonus;
import kr.minex.pvpseteffect.domain.vo.SetItem;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SetEffectService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SetEffectService 테스트")
class SetEffectServiceTest {

    @Mock
    private SetEffectRepository repository;

    private SetEffectService service;

    @BeforeEach
    void setUp() {
        service = new SetEffectService(repository);
    }

    @Test
    @DisplayName("새로운 세트 효과를 생성할 수 있어야 한다")
    void shouldCreateNewSetEffect() {
        when(repository.existsByName("테스트세트")).thenReturn(false);

        SetEffect result = service.createSetEffect("테스트세트");

        assertNotNull(result);
        assertEquals("테스트세트", result.getName());
        verify(repository).save(any(SetEffect.class));
    }

    @Test
    @DisplayName("이미 존재하는 이름으로 세트 생성 시 예외가 발생해야 한다")
    void shouldThrowExceptionForDuplicateName() {
        when(repository.existsByName("존재하는세트")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                service.createSetEffect("존재하는세트"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("세트 효과를 삭제할 수 있어야 한다")
    void shouldDeleteSetEffect() {
        SetEffect existing = new SetEffect("삭제테스트");
        when(repository.findByName("삭제테스트")).thenReturn(Optional.of(existing));

        boolean result = service.deleteSetEffect("삭제테스트");

        assertTrue(result);
        verify(repository).deleteByName("삭제테스트");
    }

    @Test
    @DisplayName("존재하지 않는 세트 삭제 시 false를 반환해야 한다")
    void shouldReturnFalseForNonExistentDelete() {
        when(repository.findByName("없는세트")).thenReturn(Optional.empty());

        boolean result = service.deleteSetEffect("없는세트");

        assertFalse(result);
        verify(repository, never()).deleteByName(any());
    }

    @Test
    @DisplayName("이름으로 세트 효과를 조회할 수 있어야 한다")
    void shouldGetSetEffectByName() {
        SetEffect effect = new SetEffect("조회테스트");
        when(repository.findByName("조회테스트")).thenReturn(Optional.of(effect));

        Optional<SetEffect> result = service.getSetEffect("조회테스트");

        assertTrue(result.isPresent());
        assertEquals("조회테스트", result.get().getName());
    }

    @Test
    @DisplayName("ID로 세트 효과를 조회할 수 있어야 한다")
    void shouldGetSetEffectById() {
        SetEffect effect = new SetEffect("조회테스트");
        when(repository.findById(effect.getId())).thenReturn(Optional.of(effect));

        Optional<SetEffect> result = service.getSetEffectById(effect.getId());

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("모든 세트 효과를 조회할 수 있어야 한다")
    void shouldGetAllSetEffects() {
        SetEffect effect1 = new SetEffect("세트1");
        SetEffect effect2 = new SetEffect("세트2");
        when(repository.findAll()).thenReturn(Arrays.asList(effect1, effect2));

        Collection<SetEffect> result = service.getAllSetEffects();

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("세트 존재 여부를 확인할 수 있어야 한다")
    void shouldCheckSetExists() {
        when(repository.existsByName("존재함")).thenReturn(true);
        when(repository.existsByName("존재안함")).thenReturn(false);

        assertTrue(service.exists("존재함"));
        assertFalse(service.exists("존재안함"));
    }

    @Test
    @DisplayName("아이템을 설정할 수 있어야 한다")
    void shouldSetItem() {
        SetEffect effect = new SetEffect("아이템테스트");
        when(repository.findByName("아이템테스트")).thenReturn(Optional.of(effect));

        SetItem item = new SetItem(EquipmentSlot.HELMET, "테스트투구", null);
        service.setItem("아이템테스트", EquipmentSlot.HELMET, item);

        assertEquals(item, effect.getItem(EquipmentSlot.HELMET));
        verify(repository).save(effect);
    }

    @Test
    @DisplayName("존재하지 않는 세트에 아이템 설정 시 예외가 발생해야 한다")
    void shouldThrowExceptionForSetItemOnNonExistentSet() {
        when(repository.findByName("없는세트")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                service.setItem("없는세트", EquipmentSlot.HELMET, SetItem.empty(EquipmentSlot.HELMET)));
    }

    @Test
    @DisplayName("능력 보너스를 설정할 수 있어야 한다")
    void shouldSetAbilityBonus() {
        SetEffect effect = new SetEffect("능력테스트");
        when(repository.findByName("능력테스트")).thenReturn(Optional.of(effect));

        service.setAbilityBonus("능력테스트", 2, AbilityType.ATTACK_DAMAGE, 10);

        SetBonus bonus = effect.getBonus(2);
        assertNotNull(bonus);
        assertEquals(AbilityType.ATTACK_DAMAGE, bonus.getAbilityType());
        assertEquals(10, bonus.getValue());
        verify(repository).save(effect);
    }

    @Test
    @DisplayName("포션 보너스를 설정할 수 있어야 한다")
    void shouldSetPotionBonus() {
        SetEffect effect = new SetEffect("포션테스트");
        when(repository.findByName("포션테스트")).thenReturn(Optional.of(effect));
        PotionEffectType mockType = mock(PotionEffectType.class);

        service.setPotionBonus("포션테스트", 3, mockType, 2);

        SetBonus bonus = effect.getBonus(3);
        assertNotNull(bonus);
        assertEquals(mockType, bonus.getPotionType());
        assertEquals(2, bonus.getValue());
        verify(repository).save(effect);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 10})
    @DisplayName("잘못된 세트 수로 보너스 설정 시 예외가 발생해야 한다")
    void shouldThrowExceptionForInvalidPieces(int invalidPieces) {
        SetEffect effect = new SetEffect("검증테스트");
        when(repository.findByName("검증테스트")).thenReturn(Optional.of(effect));

        assertThrows(IllegalArgumentException.class, () ->
                service.setAbilityBonus("검증테스트", invalidPieces, AbilityType.ATTACK_DAMAGE, 10));
    }

    @Test
    @DisplayName("보너스를 제거할 수 있어야 한다")
    void shouldRemoveBonus() {
        SetEffect effect = new SetEffect("삭제테스트");
        effect.setBonus(2, SetBonus.createAbilityBonus(2, AbilityType.ATTACK_DAMAGE, 10));
        when(repository.findByName("삭제테스트")).thenReturn(Optional.of(effect));

        service.removeBonus("삭제테스트", 2);

        assertNull(effect.getBonus(2));
        verify(repository).save(effect);
    }

    @Test
    @DisplayName("saveAll이 repository의 saveAll을 호출해야 한다")
    void shouldCallRepositorySaveAll() {
        service.saveAll();

        verify(repository).saveAll();
    }

    @Test
    @DisplayName("loadAll이 repository의 loadAll을 호출해야 한다")
    void shouldCallRepositoryLoadAll() {
        service.loadAll();

        verify(repository).loadAll();
    }
}

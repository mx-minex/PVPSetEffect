package kr.minex.pvpseteffect.application.service;

import kr.minex.pvpseteffect.domain.entity.SetEffect;
import kr.minex.pvpseteffect.domain.repository.SetEffectRepository;
import kr.minex.pvpseteffect.domain.vo.*;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.Optional;

/**
 * 세트 효과 관리 서비스
 */
public class SetEffectService {

    private final SetEffectRepository repository;

    public SetEffectService(SetEffectRepository repository) {
        this.repository = repository;
    }

    public SetEffect createSetEffect(String name) {
        if (repository.existsByName(name)) {
            throw new IllegalArgumentException("이미 존재하는 세트효과 이름입니다: " + name);
        }

        SetEffect setEffect = new SetEffect(name);
        repository.save(setEffect);
        return setEffect;
    }

    public boolean deleteSetEffect(String name) {
        Optional<SetEffect> existing = repository.findByName(name);
        if (existing.isEmpty()) {
            return false;
        }

        repository.deleteByName(name);
        return true;
    }

    public Optional<SetEffect> getSetEffect(String name) {
        return repository.findByName(name);
    }

    public Optional<SetEffect> getSetEffectById(String id) {
        return repository.findById(id);
    }

    public Collection<SetEffect> getAllSetEffects() {
        return repository.findAll();
    }

    public boolean exists(String name) {
        return repository.existsByName(name);
    }

    public void setItem(String setName, EquipmentSlot slot, SetItem item) {
        SetEffect setEffect = repository.findByName(setName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세트효과: " + setName));

        setEffect.setItem(slot, item);
        repository.save(setEffect);
    }

    public void setAbilityBonus(String setName, int pieces, AbilityType abilityType, int value) {
        SetEffect setEffect = repository.findByName(setName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세트효과: " + setName));

        validatePieces(pieces);
        SetBonus bonus = SetBonus.createAbilityBonus(pieces, abilityType, value);
        setEffect.setBonus(pieces, bonus);
        repository.save(setEffect);
    }

    public void setPotionBonus(String setName, int pieces, PotionEffectType potionType, int level) {
        SetEffect setEffect = repository.findByName(setName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세트효과: " + setName));

        validatePieces(pieces);
        SetBonus bonus = SetBonus.createPotionBonus(pieces, potionType, level);
        setEffect.setBonus(pieces, bonus);
        repository.save(setEffect);
    }

    public void removeBonus(String setName, int pieces) {
        SetEffect setEffect = repository.findByName(setName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세트효과: " + setName));

        validatePieces(pieces);
        setEffect.removeBonus(pieces);
        repository.save(setEffect);
    }

    public void saveAll() {
        repository.saveAll();
    }

    public void loadAll() {
        repository.loadAll();
    }

    private void validatePieces(int pieces) {
        if (pieces < 1 || pieces > 5) {
            throw new IllegalArgumentException("세트 수는 1~5 사이여야 합니다.");
        }
    }
}

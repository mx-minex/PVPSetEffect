package kr.minex.pvpseteffect.domain.entity;

import kr.minex.pvpseteffect.domain.vo.AbilityType;
import kr.minex.pvpseteffect.domain.vo.SetBonus;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 플레이어의 현재 세트 효과 상태
 *
 * Thread-safe 구현으로 비동기 환경에서 안전하게 사용 가능합니다.
 * 모든 상태 변경은 원자적으로 처리됩니다.
 */
public class PlayerSetState {

    private final UUID playerId;
    private final Map<String, Integer> activeSetPieces;
    private final Map<AbilityType, Double> abilityBonuses;
    private final Map<PotionEffectType, Integer> potionBonuses;
    private final AtomicLong lastUpdated;

    public PlayerSetState(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        this.playerId = playerId;
        this.activeSetPieces = new ConcurrentHashMap<>();
        this.abilityBonuses = new ConcurrentHashMap<>();
        this.potionBonuses = new ConcurrentHashMap<>();
        this.lastUpdated = new AtomicLong(System.currentTimeMillis());
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getLastUpdated() {
        return lastUpdated.get();
    }

    public void clear() {
        activeSetPieces.clear();
        abilityBonuses.clear();
        potionBonuses.clear();
        markUpdated();
    }

    public void setActiveSetPieces(String setId, int pieces) {
        if (pieces > 0) {
            activeSetPieces.put(setId, pieces);
        } else {
            activeSetPieces.remove(setId);
        }
        markUpdated();
    }

    public int getActiveSetPieces(String setId) {
        return activeSetPieces.getOrDefault(setId, 0);
    }

    public Map<String, Integer> getAllActiveSetPieces() {
        return Collections.unmodifiableMap(activeSetPieces);
    }

    public void applyBonuses(List<SetBonus> bonuses) {
        for (SetBonus bonus : bonuses) {
            if (bonus.isAbilityBonus()) {
                // atomic 복합 연산: read-modify-write를 하나의 연산으로
                abilityBonuses.compute(bonus.getAbilityType(),
                        (type, current) -> (current == null ? 0.0 : current) + bonus.getValue());
            } else if (bonus.isPotionBonus()) {
                // atomic 복합 연산: 최대값 유지
                potionBonuses.compute(bonus.getPotionType(),
                        (type, current) -> {
                            int bonusValue = bonus.getValue();
                            return current == null ? bonusValue : Math.max(current, bonusValue);
                        });
            }
        }
        markUpdated();
    }

    public double getAbilityBonus(AbilityType type) {
        return abilityBonuses.getOrDefault(type, 0.0);
    }

    public double getAttackBonus() {
        return getAbilityBonus(AbilityType.ATTACK_DAMAGE);
    }

    public double getDefenseBonus() {
        return getAbilityBonus(AbilityType.DEFENSE);
    }

    public double getEvasionChance() {
        return getAbilityBonus(AbilityType.EVASION);
    }

    public double getLifestealAmount() {
        return getAbilityBonus(AbilityType.LIFESTEAL);
    }

    public double getCriticalChance() {
        return getAbilityBonus(AbilityType.CRITICAL_CHANCE);
    }

    public double getCriticalDamage() {
        return getAbilityBonus(AbilityType.CRITICAL_DAMAGE);
    }

    public double getRegenerationBonus() {
        return getAbilityBonus(AbilityType.REGENERATION);
    }

    public double getMaxHealthBonus() {
        return getAbilityBonus(AbilityType.MAX_HEALTH);
    }

    public Map<AbilityType, Double> getAllAbilityBonuses() {
        return Collections.unmodifiableMap(abilityBonuses);
    }

    public Map<PotionEffectType, Integer> getAllPotionBonuses() {
        return Collections.unmodifiableMap(potionBonuses);
    }

    public boolean hasPotionBonus(PotionEffectType type) {
        return potionBonuses.containsKey(type);
    }

    public int getPotionLevel(PotionEffectType type) {
        return potionBonuses.getOrDefault(type, -1);
    }

    public boolean hasAnyBonus() {
        return !abilityBonuses.isEmpty() || !potionBonuses.isEmpty();
    }

    public boolean hasAnyActiveSet() {
        return !activeSetPieces.isEmpty();
    }

    private void markUpdated() {
        this.lastUpdated.set(System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return String.format("PlayerSetState{playerId=%s, activeSets=%d, abilities=%d, potions=%d}",
                playerId, activeSetPieces.size(), abilityBonuses.size(), potionBonuses.size());
    }
}

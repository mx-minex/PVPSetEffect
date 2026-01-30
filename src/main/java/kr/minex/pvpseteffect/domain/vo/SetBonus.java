package kr.minex.pvpseteffect.domain.vo;

import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

/**
 * 세트 보너스 Value Object
 * 불변 객체로 설계하여 스레드 안전성 보장
 */
public final class SetBonus {

    public enum BonusCategory {
        ABILITY,
        POTION
    }

    private final int requiredPieces;
    private final BonusCategory category;
    private final AbilityType abilityType;
    private final PotionEffectType potionType;
    private final int value;

    private SetBonus(int requiredPieces, BonusCategory category,
                     AbilityType abilityType, PotionEffectType potionType, int value) {
        this.requiredPieces = requiredPieces;
        this.category = category;
        this.abilityType = abilityType;
        this.potionType = potionType;
        this.value = value;
    }

    public static SetBonus createAbilityBonus(int requiredPieces, AbilityType abilityType, int value) {
        validatePieces(requiredPieces);
        Objects.requireNonNull(abilityType, "abilityType cannot be null");
        return new SetBonus(requiredPieces, BonusCategory.ABILITY, abilityType, null, value);
    }

    public static SetBonus createPotionBonus(int requiredPieces, PotionEffectType potionType, int value) {
        validatePieces(requiredPieces);
        Objects.requireNonNull(potionType, "potionType cannot be null");
        return new SetBonus(requiredPieces, BonusCategory.POTION, null, potionType, value);
    }

    private static void validatePieces(int pieces) {
        if (pieces < 1 || pieces > 5) {
            throw new IllegalArgumentException("requiredPieces must be between 1 and 5");
        }
    }

    public int getRequiredPieces() {
        return requiredPieces;
    }

    public BonusCategory getCategory() {
        return category;
    }

    public AbilityType getAbilityType() {
        return abilityType;
    }

    public PotionEffectType getPotionType() {
        return potionType;
    }

    public int getValue() {
        return value;
    }

    public boolean isAbilityBonus() {
        return category == BonusCategory.ABILITY;
    }

    public boolean isPotionBonus() {
        return category == BonusCategory.POTION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetBonus setBonus = (SetBonus) o;
        return requiredPieces == setBonus.requiredPieces &&
                value == setBonus.value &&
                category == setBonus.category &&
                abilityType == setBonus.abilityType &&
                Objects.equals(potionType, setBonus.potionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requiredPieces, category, abilityType, potionType, value);
    }

    @Override
    public String toString() {
        if (isAbilityBonus()) {
            return String.format("[%d세트] %s %d", requiredPieces, abilityType.getKoreanName(), value);
        } else {
            PotionType pt = PotionType.fromBukkitType(potionType);
            String name = pt != null ? pt.getKoreanName() : potionType.getName();
            return String.format("[%d세트] %s %d", requiredPieces, name, value);
        }
    }
}

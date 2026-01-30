package kr.minex.pvpseteffect.domain.vo;

import org.bukkit.Material;

/**
 * 세트 효과에서 사용하는 장비 슬롯 정의
 */
public enum EquipmentSlot {
    HELMET(0, "투구"),
    CHESTPLATE(1, "갑옷"),
    LEGGINGS(2, "레깅스"),
    BOOTS(3, "부츠"),
    WEAPON(4, "무기");

    private final int index;
    private final String displayName;

    EquipmentSlot(int index, String displayName) {
        this.index = index;
        this.displayName = displayName;
    }

    public int getIndex() {
        return index;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EquipmentSlot fromIndex(int index) {
        for (EquipmentSlot slot : values()) {
            if (slot.index == index) {
                return slot;
            }
        }
        return null;
    }

    public static int getTotalSlots() {
        return values().length;
    }

    /**
     * 주어진 Material이 이 슬롯에 적합한지 확인
     *
     * @param material 확인할 Material
     * @return 적합하면 true
     */
    public boolean isValidMaterial(Material material) {
        if (material == null) {
            return false;
        }

        String name = material.name();

        switch (this) {
            case HELMET:
                return name.endsWith("_HELMET") || name.endsWith("_CAP") ||
                       name.equals("CARVED_PUMPKIN") || name.equals("PLAYER_HEAD") ||
                       name.equals("CREEPER_HEAD") || name.equals("ZOMBIE_HEAD") ||
                       name.equals("SKELETON_SKULL") || name.equals("WITHER_SKELETON_SKULL") ||
                       name.equals("DRAGON_HEAD") || name.equals("PIGLIN_HEAD") ||
                       name.equals("TURTLE_HELMET");

            case CHESTPLATE:
                return name.endsWith("_CHESTPLATE") || name.equals("ELYTRA");

            case LEGGINGS:
                return name.endsWith("_LEGGINGS");

            case BOOTS:
                return name.endsWith("_BOOTS");

            case WEAPON:
                // 무기 슬롯은 모든 아이템 허용 (검, 도끼, 활, 삼지창 등)
                return true;

            default:
                return false;
        }
    }

    /**
     * GUI 슬롯 번호로 EquipmentSlot 반환
     * GUI에서 장비 슬롯은 2~6번
     */
    public static EquipmentSlot fromGUISlot(int guiSlot) {
        if (guiSlot < 2 || guiSlot > 6) {
            return null;
        }
        return fromIndex(guiSlot - 2);
    }
}

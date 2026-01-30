package kr.minex.pvpseteffect.domain.vo;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * 세트 아이템 정보를 담는 Value Object
 *
 * 아이템 식별 우선순위:
 * 1. DisplayName (커스텀 이름이 있는 경우)
 * 2. Material + "기본" 표시 (커스텀 이름이 없는 경우)
 */
public final class SetItem {

    private final EquipmentSlot slot;
    private final String itemName;
    private final Material material;
    private final boolean hasCustomName;

    public SetItem(EquipmentSlot slot, String itemName, Material material, boolean hasCustomName) {
        this.slot = Objects.requireNonNull(slot, "slot cannot be null");
        this.itemName = itemName;
        this.material = material;
        this.hasCustomName = hasCustomName;
    }

    public SetItem(EquipmentSlot slot, String itemName, Material material) {
        this(slot, itemName, material, itemName != null);
    }

    public static SetItem empty(EquipmentSlot slot) {
        return new SetItem(slot, null, null, false);
    }

    public static SetItem fromItemStack(EquipmentSlot slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return empty(slot);
        }

        // LIGHT_GRAY_STAINED_GLASS_PANE은 빈 슬롯 표시용이므로 무시
        if (item.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
            return empty(slot);
        }

        String name = null;
        boolean hasCustom = false;

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                name = meta.getDisplayName();
                hasCustom = true;
            }
        }

        // 커스텀 이름이 없으면 Material 이름을 사용
        if (name == null) {
            name = formatMaterialName(item.getType());
            hasCustom = false;
        }

        return new SetItem(slot, name, item.getType(), hasCustom);
    }

    /**
     * Material 이름을 읽기 쉬운 형태로 변환
     */
    private static String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        // 첫 글자 대문자
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public boolean hasCustomName() {
        return hasCustomName;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public String getItemName() {
        return itemName;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isEmpty() {
        return material == null;
    }

    /**
     * 주어진 아이템이 이 세트 아이템과 일치하는지 확인합니다.
     *
     * 매칭 로직:
     * - 커스텀 이름이 있는 세트 아이템: displayName이 정확히 일치해야 함
     * - 커스텀 이름이 없는 세트 아이템: Material이 일치하고 대상 아이템도 커스텀 이름이 없어야 함
     *
     * @param item 비교할 아이템
     * @return 일치하면 true
     */
    public boolean matches(ItemStack item) {
        if (isEmpty()) {
            return false;
        }

        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        // Material이 다르면 무조건 불일치
        if (material != item.getType()) {
            return false;
        }

        // 커스텀 이름이 있는 세트 아이템인 경우
        if (hasCustomName) {
            if (!item.hasItemMeta()) {
                return false;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) {
                return false;
            }
            return itemName.equals(meta.getDisplayName());
        }

        // 커스텀 이름이 없는 세트 아이템인 경우
        // 대상 아이템도 커스텀 이름이 없어야 매칭
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return false; // 대상에 커스텀 이름이 있으면 불일치
            }
        }

        return true; // Material 일치 + 둘 다 커스텀 이름 없음
    }

    /**
     * ItemStack으로 변환 (GUI 표시용)
     */
    public ItemStack toItemStack() {
        if (isEmpty()) {
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null && hasCustomName) {
            meta.setDisplayName(itemName);
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetItem setItem = (SetItem) o;
        return slot == setItem.slot &&
                hasCustomName == setItem.hasCustomName &&
                Objects.equals(itemName, setItem.itemName) &&
                material == setItem.material;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, itemName, material, hasCustomName);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return String.format("[%s] 없음", slot.getDisplayName());
        }
        return String.format("[%s] %s", slot.getDisplayName(), itemName);
    }
}

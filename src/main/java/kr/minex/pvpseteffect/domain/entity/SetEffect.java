package kr.minex.pvpseteffect.domain.entity;

import kr.minex.pvpseteffect.domain.vo.EquipmentSlot;
import kr.minex.pvpseteffect.domain.vo.SetBonus;
import kr.minex.pvpseteffect.domain.vo.SetItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 세트 효과 Entity
 */
public class SetEffect {

    private final String id;
    private final String name;
    private final Map<EquipmentSlot, SetItem> items;
    private final Map<Integer, SetBonus> bonuses;
    private final long createdAt;
    private long updatedAt;

    public SetEffect(String name) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = validateName(name);
        this.items = new ConcurrentHashMap<>();
        this.bonuses = new ConcurrentHashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            items.put(slot, SetItem.empty(slot));
        }
    }

    public SetEffect(String id, String name, long createdAt) {
        this.id = id;
        this.name = name;
        this.items = new ConcurrentHashMap<>();
        this.bonuses = new ConcurrentHashMap<>();
        this.createdAt = createdAt;
        this.updatedAt = createdAt;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            items.put(slot, SetItem.empty(slot));
        }
    }

    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("세트 이름은 비어있을 수 없습니다.");
        }
        if (name.contains(" ")) {
            throw new IllegalArgumentException("세트 이름에 공백은 사용할 수 없습니다.");
        }
        return name.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setItem(EquipmentSlot slot, SetItem item) {
        Objects.requireNonNull(slot, "slot cannot be null");
        Objects.requireNonNull(item, "item cannot be null");
        items.put(slot, item);
        markUpdated();
    }

    public SetItem getItem(EquipmentSlot slot) {
        return items.get(slot);
    }

    public Map<EquipmentSlot, SetItem> getAllItems() {
        return Collections.unmodifiableMap(items);
    }

    public List<SetItem> getItemsAsList() {
        List<SetItem> list = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            list.add(items.get(slot));
        }
        return list;
    }

    public int getConfiguredItemCount() {
        int count = 0;
        for (SetItem item : items.values()) {
            if (!item.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public void setBonus(int pieces, SetBonus bonus) {
        if (pieces < 1 || pieces > 5) {
            throw new IllegalArgumentException("세트 수는 1~5 사이여야 합니다.");
        }
        bonuses.put(pieces, bonus);
        markUpdated();
    }

    public void removeBonus(int pieces) {
        bonuses.remove(pieces);
        markUpdated();
    }

    public SetBonus getBonus(int pieces) {
        return bonuses.get(pieces);
    }

    public Map<Integer, SetBonus> getAllBonuses() {
        return Collections.unmodifiableMap(bonuses);
    }

    public List<SetBonus> getActiveBonuses(int equippedPieces) {
        List<SetBonus> active = new ArrayList<>();
        for (int i = 1; i <= equippedPieces && i <= 5; i++) {
            SetBonus bonus = bonuses.get(i);
            if (bonus != null) {
                active.add(bonus);
            }
        }
        return active;
    }

    public boolean hasAnyBonus() {
        return !bonuses.isEmpty();
    }

    private void markUpdated() {
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetEffect setEffect = (SetEffect) o;
        return Objects.equals(id, setEffect.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("SetEffect{id='%s', name='%s', items=%d, bonuses=%d}",
                id, name, getConfiguredItemCount(), bonuses.size());
    }
}

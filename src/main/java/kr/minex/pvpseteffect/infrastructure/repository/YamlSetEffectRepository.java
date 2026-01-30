package kr.minex.pvpseteffect.infrastructure.repository;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import kr.minex.pvpseteffect.domain.entity.SetEffect;
import kr.minex.pvpseteffect.domain.repository.SetEffectRepository;
import kr.minex.pvpseteffect.domain.vo.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * YAML 기반 세트 효과 저장소 구현
 */
public class YamlSetEffectRepository implements SetEffectRepository {

    private final Plugin plugin;
    private final File dataFile;
    private final Map<String, SetEffect> cache;
    private final Map<String, String> nameIndex;

    public YamlSetEffectRepository(Plugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "seteffects.yml");
        this.cache = new ConcurrentHashMap<>();
        this.nameIndex = new ConcurrentHashMap<>();
    }

    @Override
    public void save(SetEffect setEffect) {
        cache.put(setEffect.getId(), setEffect);
        nameIndex.put(setEffect.getName(), setEffect.getId());
    }

    @Override
    public Optional<SetEffect> findById(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public Optional<SetEffect> findByName(String name) {
        String id = nameIndex.get(name);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public Collection<SetEffect> findAll() {
        return Collections.unmodifiableCollection(cache.values());
    }

    @Override
    public void delete(String id) {
        SetEffect removed = cache.remove(id);
        if (removed != null) {
            nameIndex.remove(removed.getName());
        }
    }

    @Override
    public void deleteByName(String name) {
        String id = nameIndex.remove(name);
        if (id != null) {
            cache.remove(id);
        }
    }

    @Override
    public boolean existsByName(String name) {
        return nameIndex.containsKey(name);
    }

    @Override
    public int count() {
        return cache.size();
    }

    @Override
    public void saveAll() {
        YamlConfiguration config = new YamlConfiguration();

        for (SetEffect setEffect : cache.values()) {
            String path = "sets." + setEffect.getId();

            config.set(path + ".name", setEffect.getName());
            config.set(path + ".created_at", setEffect.getCreatedAt());
            config.set(path + ".updated_at", setEffect.getUpdatedAt());

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                SetItem item = setEffect.getItem(slot);
                String itemPath = path + ".items." + slot.name();

                if (item != null && !item.isEmpty()) {
                    config.set(itemPath + ".name", item.getItemName());
                    config.set(itemPath + ".has_custom_name", item.hasCustomName());
                    if (item.getMaterial() != null) {
                        config.set(itemPath + ".material", item.getMaterial().name());
                    }
                }
            }

            for (Map.Entry<Integer, SetBonus> entry : setEffect.getAllBonuses().entrySet()) {
                int pieces = entry.getKey();
                SetBonus bonus = entry.getValue();
                String bonusPath = path + ".bonuses." + pieces;

                config.set(bonusPath + ".category", bonus.getCategory().name());
                config.set(bonusPath + ".value", bonus.getValue());

                if (bonus.isAbilityBonus()) {
                    config.set(bonusPath + ".ability_type", bonus.getAbilityType().getConfigKey());
                } else if (bonus.isPotionBonus()) {
                    config.set(bonusPath + ".potion_type", bonus.getPotionType().getName());
                }
            }
        }

        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            config.save(dataFile);
            plugin.getLogger().info("세트 효과 데이터 저장 완료: " + cache.size() + "개");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "세트 효과 데이터 저장 실패", e);
        }
    }

    @Override
    public void loadAll() {
        cache.clear();
        nameIndex.clear();

        if (!dataFile.exists()) {
            plugin.getLogger().info("세트 효과 데이터 파일이 없습니다. 새로 생성됩니다.");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection setsSection = config.getConfigurationSection("sets");

        if (setsSection == null) {
            return;
        }

        for (String id : setsSection.getKeys(false)) {
            try {
                ConfigurationSection setSection = setsSection.getConfigurationSection(id);
                if (setSection == null) {
                    plugin.getLogger().warning("세트 효과 섹션을 찾을 수 없음: " + id);
                    continue;
                }

                SetEffect setEffect = loadSetEffect(setSection, id);
                if (setEffect != null) {
                    cache.put(setEffect.getId(), setEffect);
                    nameIndex.put(setEffect.getName(), setEffect.getId());
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "세트 효과 로드 실패: " + id, e);
            }
        }

        plugin.getLogger().info("세트 효과 데이터 로드 완료: " + cache.size() + "개");
    }

    private SetEffect loadSetEffect(ConfigurationSection section, String id) {
        String name = section.getString("name");
        if (name == null) {
            return null;
        }

        long createdAt = section.getLong("created_at", System.currentTimeMillis());
        SetEffect setEffect = new SetEffect(id, name, createdAt);

        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String slotName : itemsSection.getKeys(false)) {
                try {
                    EquipmentSlot slot = EquipmentSlot.valueOf(slotName);
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotName);

                    if (itemSection != null) {
                        String itemName = itemSection.getString("name");
                        String materialName = itemSection.getString("material");
                        boolean hasCustomName = itemSection.getBoolean("has_custom_name", true);
                        Material material = materialName != null ? Material.getMaterial(materialName) : null;

                        if (material != null) {
                            setEffect.setItem(slot, new SetItem(slot, itemName, material, hasCustomName));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("알 수 없는 장비 슬롯: " + slotName);
                }
            }
        }

        ConfigurationSection bonusesSection = section.getConfigurationSection("bonuses");
        if (bonusesSection != null) {
            for (String piecesStr : bonusesSection.getKeys(false)) {
                try {
                    int pieces = Integer.parseInt(piecesStr);
                    ConfigurationSection bonusSection = bonusesSection.getConfigurationSection(piecesStr);

                    if (bonusSection != null) {
                        SetBonus bonus = loadBonus(bonusSection, pieces);
                        if (bonus != null) {
                            setEffect.setBonus(pieces, bonus);
                        }
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("잘못된 세트 수: " + piecesStr);
                }
            }
        }

        return setEffect;
    }

    /**
     * 보너스 정보 로드
     *
     * @param section 설정 섹션
     * @param pieces 세트 수
     * @return 로드된 보너스 또는 null
     */
    @SuppressWarnings("deprecation")
    private SetBonus loadBonus(ConfigurationSection section, int pieces) {
        String categoryStr = section.getString("category");
        int value = section.getInt("value");

        if (categoryStr == null) {
            return null;
        }

        SetBonus.BonusCategory category;
        try {
            category = SetBonus.BonusCategory.valueOf(categoryStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("알 수 없는 보너스 카테고리: " + categoryStr);
            return null;
        }

        if (category == SetBonus.BonusCategory.ABILITY) {
            String abilityKey = section.getString("ability_type");
            AbilityType abilityType = AbilityType.fromConfigKey(abilityKey);
            if (abilityType != null) {
                return SetBonus.createAbilityBonus(pieces, abilityType, value);
            } else {
                plugin.getLogger().warning("알 수 없는 능력 타입: " + abilityKey);
            }
        } else if (category == SetBonus.BonusCategory.POTION) {
            String potionName = section.getString("potion_type");
            // 1.20.1 호환성: getByName 사용 (1.20.3+에서 deprecated)
            PotionEffectType potionType = PotionEffectType.getByName(potionName);
            if (potionType != null) {
                return SetBonus.createPotionBonus(pieces, potionType, value);
            } else {
                plugin.getLogger().warning("알 수 없는 포션 타입: " + potionName);
            }
        }

        return null;
    }
}

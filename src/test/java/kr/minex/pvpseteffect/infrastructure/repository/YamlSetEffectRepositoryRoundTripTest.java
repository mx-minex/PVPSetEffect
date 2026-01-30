package kr.minex.pvpseteffect.infrastructure.repository;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import kr.minex.pvpseteffect.domain.entity.SetEffect;
import kr.minex.pvpseteffect.domain.vo.AbilityType;
import kr.minex.pvpseteffect.domain.vo.EquipmentSlot;
import kr.minex.pvpseteffect.domain.vo.SetBonus;
import kr.minex.pvpseteffect.domain.vo.SetItem;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class YamlSetEffectRepositoryRoundTripTest {

    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void saveAndLoadRoundTripPreservesCoreFields() {
        YamlSetEffectRepository repo = new YamlSetEffectRepository(plugin);

        SetEffect set = new SetEffect("라운드트립");
        set.setItem(EquipmentSlot.HELMET, new SetItem(EquipmentSlot.HELMET, "helmet", Material.DIAMOND_HELMET, false));
        set.setBonus(1, SetBonus.createAbilityBonus(1, AbilityType.ATTACK_DAMAGE, 10));
        set.setBonus(2, SetBonus.createPotionBonus(2, PotionEffectType.SPEED, 2));

        repo.save(set);
        repo.saveAll();

        YamlSetEffectRepository repo2 = new YamlSetEffectRepository(plugin);
        repo2.loadAll();

        Optional<SetEffect> loadedOpt = repo2.findByName("라운드트립");
        assertTrue(loadedOpt.isPresent());

        SetEffect loaded = loadedOpt.get();
        assertEquals(set.getId(), loaded.getId());
        assertEquals(set.getName(), loaded.getName());
        assertNotNull(loaded.getItem(EquipmentSlot.HELMET));
        assertEquals(Material.DIAMOND_HELMET, loaded.getItem(EquipmentSlot.HELMET).getMaterial());
        assertNotNull(loaded.getBonus(1));
        assertTrue(loaded.getBonus(1).isAbilityBonus());
        assertEquals(10, loaded.getBonus(1).getValue());
        assertNotNull(loaded.getBonus(2));
        assertTrue(loaded.getBonus(2).isPotionBonus());
        assertEquals(PotionEffectType.SPEED, loaded.getBonus(2).getPotionType());
        assertEquals(2, loaded.getBonus(2).getValue());
    }
}

package kr.minex.pvpseteffect.application.service;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import kr.minex.pvpseteffect.domain.entity.SetEffect;
import kr.minex.pvpseteffect.domain.vo.EquipmentSlot;
import kr.minex.pvpseteffect.domain.vo.SetBonus;
import kr.minex.pvpseteffect.domain.vo.SetItem;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PlayerEffectServicePotionSafetyTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void doesNotOverrideStrongerExternalPotionEffect() {
        PlayerMock player = server.addPlayer();
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));

        SetEffect set = new SetEffect("포션테스트");
        set.setItem(EquipmentSlot.HELMET, new SetItem(EquipmentSlot.HELMET, "helmet", Material.DIAMOND_HELMET, false));
        set.setBonus(1, SetBonus.createPotionBonus(1, PotionEffectType.SPEED, 1)); // amplifier 0

        SetEffectService setEffectService = Mockito.mock(SetEffectService.class);
        when(setEffectService.getAllSetEffects()).thenReturn(List.of(set));

        PlayerEffectService playerEffectService = new PlayerEffectService(setEffectService, 20 * 60 * 60);

        // External strong effect (amplifier 5)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 5, true, true), true);

        playerEffectService.recalculateAndApply(player);

        PotionEffect after = player.getPotionEffect(PotionEffectType.SPEED);
        assertNotNull(after);
        assertEquals(5, after.getAmplifier(), "must not override stronger external effect");
    }

    @Test
    void doesNotRemoveExternalPotionEffectOnClearIfAmplifierDiffers() {
        PlayerMock player = server.addPlayer();
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));

        SetEffect set = new SetEffect("포션테스트");
        set.setItem(EquipmentSlot.HELMET, new SetItem(EquipmentSlot.HELMET, "helmet", Material.DIAMOND_HELMET, false));
        set.setBonus(1, SetBonus.createPotionBonus(1, PotionEffectType.SPEED, 1)); // amplifier 0

        SetEffectService setEffectService = Mockito.mock(SetEffectService.class);
        when(setEffectService.getAllSetEffects()).thenReturn(List.of(set));

        PlayerEffectService playerEffectService = new PlayerEffectService(setEffectService, 20 * 60 * 60);

        // Plugin applies amplifier 0.
        playerEffectService.recalculateAndApply(player);
        PotionEffect pluginApplied = player.getPotionEffect(PotionEffectType.SPEED);
        assertNotNull(pluginApplied);
        assertEquals(0, pluginApplied.getAmplifier());

        // Simulate an external plugin taking ownership of the same potion type.
        // (MockBukkit doesn't perfectly emulate Bukkit's potion override rules for long-duration effects.)
        player.removePotionEffect(PotionEffectType.SPEED);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 3, true, true), true);
        PotionEffect externalNow = player.getPotionEffect(PotionEffectType.SPEED);
        assertNotNull(externalNow);
        assertEquals(3, externalNow.getAmplifier());

        // Clear should not remove external amplifier 3 (tracked was 0).
        playerEffectService.clearPlayerEffects(player);
        PotionEffect afterClear = player.getPotionEffect(PotionEffectType.SPEED);
        assertNotNull(afterClear);
        assertEquals(3, afterClear.getAmplifier());
    }
}

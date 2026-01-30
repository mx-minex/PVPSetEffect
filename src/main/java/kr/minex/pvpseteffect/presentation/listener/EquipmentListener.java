package kr.minex.pvpseteffect.presentation.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import kr.minex.pvpseteffect.application.service.PlayerEffectService;
import kr.minex.pvpseteffect.infrastructure.scheduler.PlayerRecalculationScheduler;
import kr.minex.pvpseteffect.presentation.gui.SetEffectGUI;

/**
 * 장비 변경 감지 및 효과 업데이트 리스너
 */
public class EquipmentListener implements Listener {

    private final PlayerEffectService playerEffectService;
    private final SetEffectGUI setEffectGUI;
    private final PlayerRecalculationScheduler recalculationScheduler;

    public EquipmentListener(PlayerEffectService playerEffectService,
                             SetEffectGUI setEffectGUI,
                             PlayerRecalculationScheduler recalculationScheduler) {
        this.playerEffectService = playerEffectService;
        this.setEffectGUI = setEffectGUI;
        this.recalculationScheduler = recalculationScheduler;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        recalculationScheduler.request(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // 1. 스케줄러 취소
        recalculationScheduler.cancel(player);
        // 2. GUI 상태 정리 (메모리 누수 방지)
        setEffectGUI.handleClose(player);
        // 3. 플레이어 효과 정리
        playerEffectService.clearPlayerEffects(player);
        playerEffectService.removePlayerState(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        if (!setEffectGUI.isConfigGUI(event.getView().getTitle())) {
            recalculationScheduler.request(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        recalculationScheduler.request(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        recalculationScheduler.request(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (setEffectGUI.isConfigGUI(event.getView().getTitle())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        InventoryType.SlotType slotType = event.getSlotType();
        if (slotType == InventoryType.SlotType.ARMOR ||
                slotType == InventoryType.SlotType.QUICKBAR ||
                event.isShiftClick()) {

            recalculationScheduler.request(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        recalculationScheduler.request(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        playerEffectService.clearPlayerEffects(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        recalculationScheduler.request(event.getPlayer());
    }
}

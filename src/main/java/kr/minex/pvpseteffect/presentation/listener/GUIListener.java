package kr.minex.pvpseteffect.presentation.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import kr.minex.pvpseteffect.domain.vo.EquipmentSlot;
import kr.minex.pvpseteffect.infrastructure.config.MessageConfig;
import kr.minex.pvpseteffect.presentation.gui.SetEffectGUI;

import java.util.logging.Logger;

/**
 * GUI 인벤토리 이벤트 리스너
 *
 * 기능:
 * - 장비 슬롯 클릭: 아이템 배치/교체
 * - 우클릭: 아이템 제거
 * - 슬롯 유효성 검증: 잘못된 장비 타입 경고
 * - GUI 닫기: 자동 저장 + GUI 내 아이템 반환
 *
 * @author Junseo5
 */
public class GUIListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(GUIListener.class.getName());
    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "세트효과" + ChatColor.GRAY + "] " + ChatColor.WHITE;

    private final Plugin plugin;
    private final SetEffectGUI setEffectGUI;

    public GUIListener(Plugin plugin, SetEffectGUI setEffectGUI) {
        this.plugin = plugin;
        this.setEffectGUI = setEffectGUI;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (!setEffectGUI.isConfigGUI(title)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        Inventory topInventory = event.getView().getTopInventory();

        // 상단 인벤토리(GUI) 클릭
        if (rawSlot >= 0 && rawSlot < topInventory.getSize()) {
            handleTopInventoryClick(event, player, topInventory, rawSlot);
        }
        // 하단 인벤토리(플레이어 인벤토리) 클릭
        else {
            handleBottomInventoryClick(event, player, topInventory);
        }
    }

    private void handleTopInventoryClick(InventoryClickEvent event, Player player,
                                          Inventory topInventory, int slot) {
        // 장비 슬롯 클릭
        if (setEffectGUI.isEquipmentSlot(slot)) {
            handleEquipmentSlotClick(event, player, topInventory, slot);
            return;
        }

        // 저장 버튼
        if (setEffectGUI.isSaveButton(slot)) {
            event.setCancelled(true);
            setEffectGUI.saveFromGUI(player, topInventory);
            player.sendMessage(PREFIX + ChatColor.GREEN + "✔ 세트효과가 저장되었습니다.");
            player.closeInventory();
            return;
        }

        // 닫기 버튼
        if (setEffectGUI.isCloseButton(slot)) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        // 다른 슬롯은 클릭 방지
        event.setCancelled(true);
    }

    private void handleEquipmentSlotClick(InventoryClickEvent event, Player player,
                                           Inventory topInventory, int guiSlot) {
        event.setCancelled(true);

        EquipmentSlot equipSlot = EquipmentSlot.fromGUISlot(guiSlot);
        if (equipSlot == null) {
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack current = topInventory.getItem(guiSlot);
        ClickType clickType = event.getClick();

        // 우클릭: 아이템 제거 (설정에서만 제거. 실제 아이템 지급/회수는 하지 않음)
        if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
            if (current != null && current.getType() != Material.AIR) {
                topInventory.setItem(guiSlot, null);
                player.sendMessage(PREFIX + ChatColor.RED + "✘ " + ChatColor.YELLOW + equipSlot.getDisplayName() +
                        ChatColor.WHITE + " 슬롯 아이템이 제거되었습니다.");
            }
            return;
        }

        // 커서에 아이템이 있으면 "템플릿"으로 복사해 배치 (플레이어 인벤토리/커서는 변경하지 않음)
        if (cursor != null && cursor.getType() != Material.AIR) {
            // 슬롯 유효성 검증 (무기 슬롯은 모든 아이템 허용)
            if (!equipSlot.isValidMaterial(cursor.getType())) {
                player.sendMessage(PREFIX + ChatColor.RED + "✘ 이 아이템은 " +
                        ChatColor.YELLOW + equipSlot.getDisplayName() +
                        ChatColor.RED + " 슬롯에 배치할 수 없습니다.");
                player.sendMessage(ChatColor.GRAY + "    → 올바른 장비 타입의 아이템을 사용하세요.");
                return;
            }

            // 아이템 배치 (복사본, 수량 1로 정규화)
            ItemStack toPlace = cursor.clone();
            toPlace.setAmount(1);
            Bukkit.getScheduler().runTask(plugin, () -> {
                topInventory.setItem(guiSlot, toPlace);
                player.updateInventory();
            });

            player.sendMessage(PREFIX + ChatColor.GREEN + "✔ " + ChatColor.YELLOW + equipSlot.getDisplayName() +
                    ChatColor.WHITE + " 슬롯에 아이템이 배치되었습니다.");
            return;
        }

        // 좌클릭: 아이템 제거 (회수/복사는 하지 않음)
        if (clickType == ClickType.LEFT && current != null && current.getType() != Material.AIR) {
            topInventory.setItem(guiSlot, null);
            player.sendMessage(PREFIX + ChatColor.RED + "✘ " + ChatColor.YELLOW + equipSlot.getDisplayName() +
                    ChatColor.WHITE + " 슬롯 아이템이 제거되었습니다.");
        }
    }

    private void handleBottomInventoryClick(InventoryClickEvent event, Player player,
                                             Inventory topInventory) {
        // Shift 클릭: 빈 장비 슬롯으로 이동 시도
        if (event.isShiftClick()) {
            event.setCancelled(true);

            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) {
                return;
            }

            // 적합한 빈 슬롯 찾기
            int targetSlot = findSuitableSlot(topInventory, item.getType());

            if (targetSlot == -1) {
                player.sendMessage(PREFIX + ChatColor.RED + "✘ 배치할 수 있는 빈 슬롯이 없습니다.");
                return;
            }

            EquipmentSlot equipSlot = EquipmentSlot.fromGUISlot(targetSlot);
            if (equipSlot != null && !equipSlot.isValidMaterial(item.getType())) {
                player.sendMessage(PREFIX + ChatColor.RED + "✘ 이 아이템에 적합한 슬롯이 없습니다.");
                return;
            }

            final int finalTargetSlot = targetSlot;
            final ItemStack itemCopy = item.clone();
            itemCopy.setAmount(1);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    topInventory.setItem(finalTargetSlot, itemCopy);
                    player.updateInventory();

                    EquipmentSlot slot = EquipmentSlot.fromGUISlot(finalTargetSlot);
                    if (slot != null) {
                        player.sendMessage(PREFIX + ChatColor.GREEN + "✔ " + ChatColor.YELLOW + slot.getDisplayName() +
                                ChatColor.WHITE + " 슬롯에 아이템이 배치되었습니다.");
                    }
                }
            });
        }
    }

    /**
     * 아이템에 적합한 빈 장비 슬롯 찾기
     */
    private int findSuitableSlot(Inventory inventory, Material material) {
        // 먼저 정확히 맞는 슬롯 찾기
        for (int guiSlot = 2; guiSlot <= 6; guiSlot++) {
            EquipmentSlot slot = EquipmentSlot.fromGUISlot(guiSlot);
            if (slot == null) continue;

            ItemStack existing = inventory.getItem(guiSlot);
            boolean isEmpty = existing == null || existing.getType() == Material.AIR;

            if (isEmpty && slot.isValidMaterial(material)) {
                return guiSlot;
            }
        }
        return -1;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();

        if (!setEffectGUI.isConfigGUI(title)) {
            return;
        }

        // 드래그는 장비 슬롯에서도 금지 (복잡성 방지)
        for (int slot : event.getRawSlots()) {
            if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if (!setEffectGUI.isConfigGUI(title)) {
            return;
        }

        // 자동 저장
        setEffectGUI.saveFromGUI(player, event.getInventory());

        // GUI 정리
        setEffectGUI.handleClose(player);

        player.sendMessage(PREFIX + ChatColor.GREEN + "✔ " + ChatColor.WHITE + "세트효과 설정이 자동 저장되었습니다.");
    }
}

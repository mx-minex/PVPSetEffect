package kr.minex.pvpseteffect.presentation.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import kr.minex.pvpseteffect.application.service.SetEffectService;
import kr.minex.pvpseteffect.domain.entity.SetEffect;
import kr.minex.pvpseteffect.domain.vo.EquipmentSlot;
import kr.minex.pvpseteffect.domain.vo.SetBonus;
import kr.minex.pvpseteffect.domain.vo.SetItem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 세트 효과 GUI 관리
 *
 * Thread-safe 구현으로 여러 플레이어가 동시에 GUI를 사용해도 안전합니다.
 *
 * @author Junseo5
 */
public class SetEffectGUI {

    private static final Logger LOGGER = Logger.getLogger(SetEffectGUI.class.getName());
    private static final String GUI_PREFIX = ChatColor.DARK_GRAY + "[ " + ChatColor.AQUA + "세트효과" + ChatColor.DARK_GRAY + " ] ";
    private static final String CONFIG_GUI_IDENTIFIER = "\u00A7r\u00A70\u00A7e\u00A7c";
    private static final int MAX_TITLE_LENGTH = 32;

    private final SetEffectService setEffectService;
    private final Map<UUID, String> openGUIs;

    public SetEffectGUI(SetEffectService setEffectService) {
        this.setEffectService = Objects.requireNonNull(setEffectService, "setEffectService cannot be null");
        this.openGUIs = new ConcurrentHashMap<>();
    }

    public void openConfigGUI(Player player, SetEffect setEffect) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(setEffect, "setEffect cannot be null");

        String title = buildConfigTitle(setEffect.getName());
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        LOGGER.fine(() -> String.format("플레이어 %s가 세트 %s GUI 열기", player.getName(), setEffect.getName()));

        int[] equipmentSlots = {2, 3, 4, 5, 6};

        // 장비 슬롯에 저장된 아이템 또는 빈 슬롯 표시 배치
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            SetItem setItem = setEffect.getItem(slot);
            int guiSlot = equipmentSlots[slot.getIndex()];

            if (setItem != null && !setItem.isEmpty()) {
                // 저장된 아이템을 실제 ItemStack으로 변환하여 표시
                ItemStack displayItem = setItem.toItemStack();
                if (displayItem != null) {
                    // 아이템 lore에 슬롯 정보 추가
                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add("");
                        lore.add(ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━");
                        lore.add(ChatColor.GRAY + "슬롯: " + ChatColor.WHITE + slot.getDisplayName());
                        if (!setItem.hasCustomName()) {
                            lore.add(ChatColor.GRAY + "매칭: " + ChatColor.YELLOW + "Material 기준");
                        } else {
                            lore.add(ChatColor.GRAY + "매칭: " + ChatColor.GREEN + "이름 기준");
                        }
                        lore.add(ChatColor.YELLOW + "우클릭: 제거");
                        meta.setLore(lore);
                        displayItem.setItemMeta(meta);
                    }
                    inventory.setItem(guiSlot, displayItem);
                } else {
                    inventory.setItem(guiSlot, null);
                }
            } else {
                // 빈 슬롯은 null로 설정 (아이템 놓을 수 있도록)
                inventory.setItem(guiSlot, null);
            }
        }

        // 라벨 슬롯
        int[] labelSlots = {11, 12, 13, 14, 15};
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            inventory.setItem(labelSlots[slot.getIndex()], createLabelItem(slot));
        }

        // 기타 버튼
        inventory.setItem(20, createBonusInfoItem(setEffect));
        inventory.setItem(22, createSaveButton());
        inventory.setItem(24, createCloseButton());

        // 나머지 빈 공간을 유리로 채움
        ItemStack glass = createGlassPane();
        for (int i = 0; i < 27; i++) {
            if (inventory.getItem(i) == null && !isEquipmentSlot(i)) {
                inventory.setItem(i, glass);
            }
        }

        openGUIs.put(player.getUniqueId(), setEffect.getId());
        player.openInventory(inventory);
    }

    public void saveFromGUI(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return;
        }

        String setEffectId = openGUIs.get(player.getUniqueId());
        if (setEffectId == null) {
            LOGGER.fine(() -> String.format("플레이어 %s의 열린 GUI 없음", player.getName()));
            return;
        }

        Optional<SetEffect> setEffectOpt = setEffectService.getSetEffectById(setEffectId);
        if (setEffectOpt.isEmpty()) {
            LOGGER.warning(() -> String.format("세트 효과 ID %s를 찾을 수 없음", setEffectId));
            return;
        }

        SetEffect setEffect = setEffectOpt.get();
        int[] equipmentSlots = {2, 3, 4, 5, 6};

        try {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                int guiSlot = equipmentSlots[slot.getIndex()];
                ItemStack item = inventory.getItem(guiSlot);

                SetItem setItem = SetItem.fromItemStack(slot, item);
                setEffectService.setItem(setEffect.getName(), slot, setItem);
            }

            setEffectService.saveAll();
            LOGGER.info(() -> String.format("플레이어 %s가 세트 %s 저장 완료", player.getName(), setEffect.getName()));
        } catch (Exception e) {
            LOGGER.warning(() -> String.format("세트 %s 저장 중 오류: %s", setEffect.getName(), e.getMessage()));
        }
    }

    public void handleClose(Player player) {
        openGUIs.remove(player.getUniqueId());
    }

    public boolean isConfigGUI(String title) {
        return title != null && title.endsWith(CONFIG_GUI_IDENTIFIER);
    }

    private String buildConfigTitle(String setName) {
        String base = GUI_PREFIX + ChatColor.YELLOW + (setName == null ? "" : setName);
        int maxBaseLength = Math.max(0, MAX_TITLE_LENGTH - CONFIG_GUI_IDENTIFIER.length());
        String truncated = truncateWithoutDanglingColorCode(base, maxBaseLength);
        return truncated + CONFIG_GUI_IDENTIFIER;
    }

    private String truncateWithoutDanglingColorCode(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        String cut = text.substring(0, maxLength);
        if (cut.endsWith("§")) {
            return cut.substring(0, cut.length() - 1);
        }
        return cut;
    }

    public String getOpenSetEffectId(UUID playerId) {
        return openGUIs.get(playerId);
    }

    public boolean isEquipmentSlot(int slot) {
        return slot >= 2 && slot <= 6;
    }

    public boolean isSaveButton(int slot) {
        return slot == 22;
    }

    public boolean isCloseButton(int slot) {
        return slot == 24;
    }

    private ItemStack createDisplayItem(SetItem setItem) {
        Material material = setItem.getMaterial() != null ? setItem.getMaterial() : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(setItem.getItemName());
            meta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "슬롯: " + ChatColor.WHITE + setItem.getSlot().getDisplayName(),
                    "",
                    ChatColor.YELLOW + "클릭하여 변경"
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createEmptySlotItem(EquipmentSlot slot) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "[빈 슬롯]");
            meta.setLore(Arrays.asList(
                    "",
                    ChatColor.WHITE + slot.getDisplayName() + " 슬롯",
                    "",
                    ChatColor.YELLOW + "아이템을 놓아주세요"
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createLabelItem(EquipmentSlot slot) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + slot.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "위 슬롯에 아이템을 등록하세요");
            lore.add("");
            lore.add(ChatColor.WHITE + "사용법:");
            lore.add(ChatColor.YELLOW + " 좌클릭" + ChatColor.GRAY + " - 아이템 배치/회수");
            lore.add(ChatColor.YELLOW + " 우클릭" + ChatColor.GRAY + " - 아이템 제거");
            lore.add(ChatColor.YELLOW + " Shift+클릭" + ChatColor.GRAY + " - 빠른 배치");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createBonusInfoItem(SetEffect setEffect) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "세트 보너스 정보");

            List<String> lore = new ArrayList<>();
            lore.add("");

            for (int i = 1; i <= 5; i++) {
                SetBonus bonus = setEffect.getBonus(i);
                if (bonus != null) {
                    lore.add(ChatColor.GREEN + "[" + i + "세트] " + ChatColor.WHITE + formatBonus(bonus));
                } else {
                    lore.add(ChatColor.GRAY + "[" + i + "세트] 미설정");
                }
            }

            lore.add("");
            lore.add(ChatColor.YELLOW + "/세트효과 능력 또는 /세트효과 포션");
            lore.add(ChatColor.YELLOW + "명령어로 보너스를 설정하세요");

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }

        return item;
    }

    private String formatBonus(SetBonus bonus) {
        if (bonus.isAbilityBonus()) {
            return bonus.getAbilityType().getKoreanName() + " +" + bonus.getValue();
        } else {
            kr.minex.pvpseteffect.domain.vo.PotionType pt =
                    kr.minex.pvpseteffect.domain.vo.PotionType.fromBukkitType(bonus.getPotionType());
            String name = pt != null ? pt.getKoreanName() : bonus.getPotionType().getName();
            return name + " Lv." + bonus.getValue();
        }
    }

    private ItemStack createSaveButton() {
        ItemStack item = new ItemStack(Material.LIME_WOOL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "저장");
            meta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "현재 설정을 저장합니다",
                    "",
                    ChatColor.YELLOW + "클릭하여 저장"
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "닫기");
            meta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "변경사항을 저장하지 않고 닫습니다"
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }

        return item;
    }
}

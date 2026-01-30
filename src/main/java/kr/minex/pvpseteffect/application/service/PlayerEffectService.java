package kr.minex.pvpseteffect.application.service;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import kr.minex.pvpseteffect.domain.entity.PlayerSetState;
import kr.minex.pvpseteffect.domain.entity.SetEffect;
import kr.minex.pvpseteffect.domain.vo.EquipmentSlot;
import kr.minex.pvpseteffect.domain.vo.PotionApplicationMode;
import kr.minex.pvpseteffect.domain.vo.SetBonus;
import kr.minex.pvpseteffect.domain.vo.SetItem;
import kr.minex.pvpseteffect.infrastructure.config.PluginSettings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

/**
 * 플레이어 효과 관리 서비스
 *
 * 플레이어의 장비를 분석하여 세트 효과를 적용하고 관리합니다.
 * Thread-safe 구현으로 멀티스레드 환경에서 안전합니다.
 */
public class PlayerEffectService {

    private static final Logger LOGGER = Logger.getLogger(PlayerEffectService.class.getName());

    /**
     * 포션 효과 지속 시간 (틱 단위)
     * IMMEDIATE 모드: Integer.MAX_VALUE 사용하여 실질적 무한 지속
     * NATURAL 모드: 짧은 지속시간 (예: 60틱 = 3초)
     */
    private final int potionDurationTicks;
    private static final int POTION_APPLIER_DURATION_GUARD_TICKS = 20 * 60; // >= 60s면 "우리 효과"로 간주(보수적)
    private static final int MAX_POTION_AMPLIFIER = 255;

    /**
     * 포션 적용 모드
     */
    private volatile PotionApplicationMode potionApplicationMode = PotionApplicationMode.IMMEDIATE;

    /**
     * NATURAL 모드 전용: 효과 지속 시간 (틱)
     */
    private volatile int naturalEffectDuration = 60;

    /**
     * 체력 AttributeModifier 식별용 상수
     */
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String HEALTH_MODIFIER_NAME = "pvpseteffect.max_health";

    private final SetEffectService setEffectService;
    private final Map<UUID, PlayerSetState> playerStates;
    private final Map<UUID, Map<PotionEffectType, Integer>> appliedPotions;
    private final Map<UUID, Double> appliedHealthBonuses;

    /**
     * 전투 설정 (체력 스케일 등)
     */
    private volatile PluginSettings.CombatSettings combatSettings;

    // Lightweight runtime metrics for production debugging.
    private final LongAdder recalculationCount = new LongAdder();
    private final LongAdder recalculationNanos = new LongAdder();
    private final LongAdder potionApplyCount = new LongAdder();

    public PlayerEffectService(SetEffectService setEffectService) {
        this(setEffectService, Integer.MAX_VALUE);
    }

    /**
     * @param potionDurationTicks applied potion duration in ticks. Use Integer.MAX_VALUE for "infinite".
     */
    public PlayerEffectService(SetEffectService setEffectService, int potionDurationTicks) {
        this.setEffectService = setEffectService;
        this.playerStates = new ConcurrentHashMap<>();
        this.appliedPotions = new ConcurrentHashMap<>();
        this.appliedHealthBonuses = new ConcurrentHashMap<>();
        this.potionDurationTicks = Math.max(1, potionDurationTicks);
        // 기본 전투 설정 (체력 스케일 1.0)
        this.combatSettings = new PluginSettings.CombatSettings(
                2.0, 2.0, 100.0, 100.0, 100.0,
                new PluginSettings.LifestealSettings(0.37, 2.0),
                2.0, 0.0, 1.0
        );
    }

    /**
     * 전투 설정 적용
     *
     * @param settings 전투 설정 (null이면 무시)
     */
    public void setCombatSettings(PluginSettings.CombatSettings settings) {
        if (settings != null) {
            this.combatSettings = settings;
        }
    }

    /**
     * 포션 설정 적용
     *
     * @param settings 포션 설정 (null이면 무시)
     */
    public void setPotionSettings(PluginSettings.PotionSettings settings) {
        if (settings != null) {
            this.potionApplicationMode = settings.applicationMode();
            if (settings.natural() != null) {
                this.naturalEffectDuration = settings.natural().effectDuration();
            }
        }
    }

    /**
     * 현재 포션 적용 모드 반환
     */
    public PotionApplicationMode getPotionApplicationMode() {
        return potionApplicationMode;
    }

    public void recalculateAndApply(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID playerId = player.getUniqueId();

        try {
            long startNanos = System.nanoTime();

            PlayerSetState newState = calculatePlayerState(player);
            playerStates.put(playerId, newState);

            syncPotionEffects(player, newState);
            syncMaxHealth(player, newState);

            if (newState.hasAnyBonus()) {
                LOGGER.fine(() -> String.format("플레이어 %s 세트 효과 적용: %s",
                        player.getName(), newState.toString()));
            }

            recalculationCount.increment();
            recalculationNanos.add(System.nanoTime() - startNanos);
        } catch (Exception e) {
            LOGGER.warning(() -> String.format("플레이어 %s 세트 효과 계산 중 오류: %s",
                    player.getName(), e.getMessage()));
        }
    }

    private PlayerSetState calculatePlayerState(Player player) {
        PlayerSetState state = new PlayerSetState(player.getUniqueId());

        Map<EquipmentSlot, ItemStack> equipment = getPlayerEquipment(player);

        for (SetEffect setEffect : setEffectService.getAllSetEffects()) {
            int matchedPieces = countMatchedPieces(setEffect, equipment);

            if (matchedPieces > 0) {
                state.setActiveSetPieces(setEffect.getId(), matchedPieces);

                List<SetBonus> activeBonuses = setEffect.getActiveBonuses(matchedPieces);
                state.applyBonuses(activeBonuses);
            }
        }

        return state;
    }

    private Map<EquipmentSlot, ItemStack> getPlayerEquipment(Player player) {
        Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

        equipment.put(EquipmentSlot.HELMET, player.getInventory().getHelmet());
        equipment.put(EquipmentSlot.CHESTPLATE, player.getInventory().getChestplate());
        equipment.put(EquipmentSlot.LEGGINGS, player.getInventory().getLeggings());
        equipment.put(EquipmentSlot.BOOTS, player.getInventory().getBoots());
        equipment.put(EquipmentSlot.WEAPON, player.getInventory().getItemInMainHand());

        return equipment;
    }

    private int countMatchedPieces(SetEffect setEffect, Map<EquipmentSlot, ItemStack> equipment) {
        int count = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            SetItem setItem = setEffect.getItem(slot);
            ItemStack playerItem = equipment.get(slot);

            if (setItem != null && setItem.matches(playerItem)) {
                count++;
            }
        }

        return count;
    }

    private void syncPotionEffects(Player player, PlayerSetState state) {
        UUID playerId = player.getUniqueId();
        Map<PotionEffectType, Integer> previous = appliedPotions.getOrDefault(playerId, Collections.emptyMap());
        Map<PotionEffectType, Integer> desired = new HashMap<>();

        for (Map.Entry<PotionEffectType, Integer> entry : state.getAllPotionBonuses().entrySet()) {
            PotionEffectType type = entry.getKey();
            if (type == null) {
                continue;
            }

            int level = entry.getValue();
            int amplifier = Math.max(0, Math.min(MAX_POTION_AMPLIFIER, level - 1));
            desired.put(type, amplifier);
        }

        // IMMEDIATE 모드에서만 기존 효과 제거 (NATURAL 모드는 자연 만료)
        if (potionApplicationMode == PotionApplicationMode.IMMEDIATE) {
            for (Map.Entry<PotionEffectType, Integer> prev : previous.entrySet()) {
                PotionEffectType type = prev.getKey();
                if (!desired.containsKey(type)) {
                    conditionalRemovePotion(player, type, prev.getValue());
                }
            }
        }

        Map<PotionEffectType, Integer> actuallyApplied = new HashMap<>();

        // Apply/upgrade desired potions.
        for (Map.Entry<PotionEffectType, Integer> want : desired.entrySet()) {
            PotionEffectType type = want.getKey();
            int amplifier = want.getValue();

            // NATURAL 모드
            if (potionApplicationMode == PotionApplicationMode.NATURAL) {
                // 새로 장착한 경우 (previous에 없음) vs 스케줄러 재적용 구분
                boolean isNewApplication = !previous.containsKey(type);
                if (tryApplyPotionNatural(player, type, amplifier, isNewApplication)) {
                    actuallyApplied.put(type, amplifier);
                }
                continue;
            }

            // IMMEDIATE 모드: 변경 없으면 스킵
            Integer prevAmp = previous.get(type);
            if (prevAmp != null && prevAmp == amplifier) {
                actuallyApplied.put(type, amplifier);
                continue;
            }

            if (tryApplyPotion(player, type, amplifier)) {
                actuallyApplied.put(type, amplifier);
            }
        }

        if (actuallyApplied.isEmpty()) {
            appliedPotions.remove(playerId);
        } else {
            appliedPotions.put(playerId, actuallyApplied);
        }
    }

    private boolean tryApplyPotion(Player player, PotionEffectType type, int amplifier) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing != null && existing.getAmplifier() > amplifier) {
            // Do not override stronger external effects.
            return false;
        }

        // Use force only when we need to upgrade/replace.
        boolean force = existing == null || existing.getAmplifier() <= amplifier;
        boolean applied = player.addPotionEffect(new PotionEffect(type, potionDurationTicks, amplifier, true, false, false), force);
        if (applied) {
            potionApplyCount.increment();
        }
        return applied;
    }

    /**
     * NATURAL 모드용 포션 적용 (짧은 지속시간)
     *
     * @param player 대상 플레이어
     * @param type 포션 효과 타입
     * @param amplifier 포션 레벨 (0부터 시작)
     * @param isNewApplication 새로 장착한 경우 true (스케줄러 재적용은 false)
     * @return 적용 성공 여부
     */
    private boolean tryApplyPotionNatural(Player player, PotionEffectType type, int amplifier, boolean isNewApplication) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing != null && existing.getAmplifier() > amplifier) {
            // 더 강한 외부 효과가 있으면 덮어쓰지 않음
            return false;
        }

        // 스케줄러 재적용 시에만 스킵 로직 적용 (새로 장착한 경우는 즉시 적용)
        if (!isNewApplication && existing != null && existing.getAmplifier() == amplifier) {
            // 남은 시간이 재적용 간격의 절반 이상이면 스킵
            int halfInterval = naturalEffectDuration / 2;
            if (existing.getDuration() >= halfInterval) {
                return true; // 이미 적용됨으로 간주
            }
        }

        boolean force = existing == null || existing.getAmplifier() <= amplifier;
        boolean applied = player.addPotionEffect(
                new PotionEffect(type, naturalEffectDuration, amplifier, true, false, false),
                force
        );
        if (applied) {
            potionApplyCount.increment();
        }
        return applied;
    }

    private void conditionalRemovePotion(Player player, PotionEffectType type, int amplifier) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing == null) {
            return;
        }
        // Remove only if it still looks like the effect we applied.
        if (existing.getAmplifier() == amplifier && existing.getDuration() >= POTION_APPLIER_DURATION_GUARD_TICKS) {
            player.removePotionEffect(type);
        }
    }

    /**
     * NATURAL 모드용 포션 제거 (플레이어 퇴장/정리 시)
     * 지속시간과 무관하게 우리가 적용한 레벨과 같으면 제거
     */
    private void forceRemovePotion(Player player, PotionEffectType type, int amplifier) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing == null) {
            return;
        }
        // 우리가 적용한 레벨과 같으면 제거 (외부 효과 보호)
        if (existing.getAmplifier() == amplifier) {
            player.removePotionEffect(type);
        }
    }

    /**
     * 최대 체력 보너스 동기화
     * AttributeModifier를 사용하여 최대 체력을 변경합니다.
     *
     * @param player 대상 플레이어
     * @param state  플레이어 세트 상태
     */
    private void syncMaxHealth(Player player, PlayerSetState state) {
        UUID playerId = player.getUniqueId();
        double healthBonus = state.getMaxHealthBonus();
        Double previousBonus = appliedHealthBonuses.get(playerId);

        // 변경이 없으면 스킵
        if (previousBonus != null && Math.abs(previousBonus - healthBonus) < 0.001) {
            return;
        }

        try {
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute == null) {
                return;
            }

            // 기존 modifier 제거
            removeHealthModifier(attribute);

            // 새 modifier 적용
            if (healthBonus > 0) {
                double actualBonus = healthBonus / combatSettings.healthScale();
                AttributeModifier modifier = new AttributeModifier(
                        HEALTH_MODIFIER_UUID,
                        HEALTH_MODIFIER_NAME,
                        actualBonus,
                        AttributeModifier.Operation.ADD_NUMBER
                );
                attribute.addModifier(modifier);
                appliedHealthBonuses.put(playerId, healthBonus);

                LOGGER.fine(() -> String.format("플레이어 %s 최대 체력 +%.1f (보너스: %.1f)",
                        player.getName(), actualBonus, healthBonus));
            } else {
                appliedHealthBonuses.remove(playerId);
            }

            // 현재 체력이 최대 체력을 초과하면 조정
            double maxHealth = attribute.getValue();
            if (player.getHealth() > maxHealth) {
                player.setHealth(maxHealth);
            }

        } catch (Exception e) {
            LOGGER.warning(() -> String.format("플레이어 %s 최대 체력 설정 중 오류: %s",
                    player.getName(), e.getMessage()));
        }
    }

    /**
     * 플러그인이 적용한 체력 modifier 제거
     */
    private void removeHealthModifier(AttributeInstance attribute) {
        // Some implementations back this by a mutable set; copy first to avoid ConcurrentModificationException.
        new ArrayList<>(attribute.getModifiers()).stream()
                .filter(m -> HEALTH_MODIFIER_UUID.equals(m.getUniqueId()) ||
                        HEALTH_MODIFIER_NAME.equals(m.getName()))
                .forEach(attribute::removeModifier);
    }

    /**
     * 플레이어의 체력 modifier 제거 (퇴장/정리 시)
     */
    private void clearHealthModifier(Player player) {
        try {
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute != null) {
                removeHealthModifier(attribute);
            }
        } catch (Exception e) {
            LOGGER.fine(() -> String.format("체력 modifier 제거 중 오류 (무시됨): %s", e.getMessage()));
        }
        appliedHealthBonuses.remove(player.getUniqueId());
    }

    public PlayerSetState getPlayerState(UUID playerId) {
        return playerStates.get(playerId);
    }

    public PlayerSetState getPlayerStateOrEmpty(UUID playerId) {
        return playerStates.getOrDefault(playerId, new PlayerSetState(playerId));
    }

    public void removePlayerState(UUID playerId) {
        playerStates.remove(playerId);
        appliedPotions.remove(playerId);
        appliedHealthBonuses.remove(playerId);
    }

    public void clearPlayerEffects(Player player) {
        Map<PotionEffectType, Integer> potions = appliedPotions.remove(player.getUniqueId());
        if (potions != null) {
            for (Map.Entry<PotionEffectType, Integer> e : potions.entrySet()) {
                // NATURAL 모드에서는 짧은 지속시간이므로 강제 제거
                if (potionApplicationMode == PotionApplicationMode.NATURAL) {
                    forceRemovePotion(player, e.getKey(), e.getValue());
                } else {
                    conditionalRemovePotion(player, e.getKey(), e.getValue());
                }
            }
        }
        clearHealthModifier(player);
        playerStates.remove(player.getUniqueId());
    }

    public void clearAll() {
        playerStates.clear();
        appliedPotions.clear();
        appliedHealthBonuses.clear();
    }

    public int getTrackedPlayerCount() {
        return playerStates.size();
    }

    public String getMetricsSnapshot() {
        long count = recalculationCount.sum();
        long nanos = recalculationNanos.sum();
        long potions = potionApplyCount.sum();
        double avgMs = count == 0 ? 0.0 : (nanos / 1_000_000.0) / count;
        return String.format("recalculations=%d avgRecalcMs=%.3f potionApplies=%d trackedPlayers=%d",
                count, avgMs, potions, getTrackedPlayerCount());
    }
}

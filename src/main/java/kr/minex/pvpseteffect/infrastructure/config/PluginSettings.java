package kr.minex.pvpseteffect.infrastructure.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import kr.minex.pvpseteffect.domain.vo.PotionApplicationMode;

import java.util.Objects;

/**
 * Typed view over config.yml with validation/clamping.
 *
 * Keep this class free of heavy dependencies so it can be reused by tests and future reload support.
 */
public final class PluginSettings {

    public record RecalculationSettings(long debounceTicks) { }

    /**
     * 자연 만료 모드 전용 설정
     *
     * @param effectDuration 포션 효과 지속 시간 (틱)
     * @param reapplyInterval 재적용 주기 (틱)
     */
    public record NaturalPotionSettings(int effectDuration, long reapplyInterval) { }

    /**
     * 포션 효과 설정
     *
     * @param applicationMode 적용 모드 (IMMEDIATE 또는 NATURAL)
     * @param durationTicks IMMEDIATE 모드 전용: 지속 시간
     * @param natural NATURAL 모드 전용 설정
     */
    public record PotionSettings(
            PotionApplicationMode applicationMode,
            int durationTicks,
            NaturalPotionSettings natural
    ) { }

    public record LifestealSettings(double triggerChance, double healScale) { }

    public record CombatSettings(
            double attackScale,
            double defenseScale,
            double evasionMaxPercent,
            double criticalChanceMaxPercent,
            double criticalDamageScalePercent,
            LifestealSettings lifesteal,
            double regenScale,
            double minDamage,
            double healthScale
    ) { }

    public record MetricsSettings(int intervalMinutes) { }

    private final RecalculationSettings recalculation;
    private final PotionSettings potion;
    private final CombatSettings combat;
    private final MetricsSettings metrics;

    private PluginSettings(RecalculationSettings recalculation, PotionSettings potion, CombatSettings combat, MetricsSettings metrics) {
        this.recalculation = recalculation;
        this.potion = potion;
        this.combat = combat;
        this.metrics = metrics;
    }

    public static PluginSettings load(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        FileConfiguration c = plugin.getConfig();

        long debounceTicks = clampLong(c.getLong("recalculation.debounceTicks", 1L), 0L, 20L * 5L);

        // 포션 설정
        PotionApplicationMode applicationMode = PotionApplicationMode.fromString(
                c.getString("potion.applicationMode", "IMMEDIATE"));
        int potionDuration = clampInt(c.getInt("potion.durationTicks", Integer.MAX_VALUE), 1, Integer.MAX_VALUE);
        int naturalEffectDuration = clampInt(c.getInt("potion.natural.effectDuration", 60), 20, 20 * 30);
        long naturalReapplyInterval = clampLong(c.getLong("potion.natural.reapplyInterval", 40L), 10L, 20L * 30L);

        double attackScale = clampDouble(c.getDouble("combat.attackScale", 2.0), 0.1, 1000.0);
        double defenseScale = clampDouble(c.getDouble("combat.defenseScale", 2.0), 0.1, 1000.0);
        double evasionMax = clampDouble(c.getDouble("combat.evasionMaxPercent", 100.0), 0.0, 100.0);
        double critChanceMax = clampDouble(c.getDouble("combat.criticalChanceMaxPercent", 100.0), 0.0, 100.0);
        double critDamageScale = clampDouble(c.getDouble("combat.criticalDamageScalePercent", 100.0), 0.1, 1000.0);

        double lifestealChance = clampDouble(c.getDouble("combat.lifesteal.triggerChance", 0.37), 0.0, 1.0);
        double lifestealHealScale = clampDouble(c.getDouble("combat.lifesteal.healScale", 2.0), 0.1, 1000.0);

        double regenScale = clampDouble(c.getDouble("combat.regenScale", 2.0), 0.1, 1000.0);
        double minDamage = clampDouble(c.getDouble("combat.minDamage", 0.0), 0.0, 10_000.0);
        double healthScale = clampDouble(c.getDouble("combat.healthScale", 1.0), 0.1, 1000.0);

        int metricsIntervalMinutes = clampInt(c.getInt("metrics.intervalMinutes", 5), 0, 24 * 60);

        return new PluginSettings(
                new RecalculationSettings(debounceTicks),
                new PotionSettings(
                        applicationMode,
                        potionDuration,
                        new NaturalPotionSettings(naturalEffectDuration, naturalReapplyInterval)
                ),
                new CombatSettings(
                        attackScale,
                        defenseScale,
                        evasionMax,
                        critChanceMax,
                        critDamageScale,
                        new LifestealSettings(lifestealChance, lifestealHealScale),
                        regenScale,
                        minDamage,
                        healthScale
                ),
                new MetricsSettings(metricsIntervalMinutes)
        );
    }

    public RecalculationSettings recalculation() {
        return recalculation;
    }

    public PotionSettings potion() {
        return potion;
    }

    public CombatSettings combat() {
        return combat;
    }

    public MetricsSettings metrics() {
        return metrics;
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static long clampLong(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clampDouble(double v, double min, double max) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return min;
        }
        return Math.max(min, Math.min(max, v));
    }
}


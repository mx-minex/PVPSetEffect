package kr.minex.pvpseteffect.presentation.listener;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import kr.minex.pvpseteffect.application.service.PlayerEffectService;
import kr.minex.pvpseteffect.domain.entity.PlayerSetState;
import kr.minex.pvpseteffect.infrastructure.config.PluginSettings;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * 전투 관련 세트 효과 적용 리스너
 *
 * 공격/방어 보너스, 회피, 치명타, 흡혈 등의 전투 효과를 처리합니다.
 * Thread-safe Random 사용으로 동시 전투에서도 안전합니다.
 */
public class CombatListener implements Listener {

    private static final Logger LOGGER = Logger.getLogger(CombatListener.class.getName());

    private final PlayerEffectService playerEffectService;
    private volatile BiFunction<Player, Player, Boolean> teamChecker;
    private volatile PluginSettings.CombatSettings settings;

    public CombatListener(PlayerEffectService playerEffectService) {
        this.playerEffectService = Objects.requireNonNull(playerEffectService, "playerEffectService cannot be null");
        this.teamChecker = null;
        this.settings = new PluginSettings.CombatSettings(
                2.0, 2.0,
                100.0,
                100.0,
                100.0,
                new PluginSettings.LifestealSettings(0.37, 2.0),
                2.0,
                0.0,
                1.0
        );
    }

    public void setSettings(PluginSettings.CombatSettings settings) {
        if (settings != null) {
            this.settings = settings;
        }
    }

    public void setTeamChecker(BiFunction<Player, Player, Boolean> checker) {
        this.teamChecker = checker;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Thread-safe random
        ThreadLocalRandom random = ThreadLocalRandom.current();
        PluginSettings.CombatSettings s = this.settings;

        // 피해자 방어 처리
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            PlayerSetState victimState = playerEffectService.getPlayerState(victim.getUniqueId());

            if (victimState != null) {
                // 회피 판정
                double evasionChance = victimState.getEvasionChance();
                if (evasionChance > 0) {
                    double clamped = Math.min(evasionChance, s.evasionMaxPercent());
                    if (random.nextDouble() * 100 < clamped) {
                        event.setCancelled(true);
                        LOGGER.fine(() -> String.format("플레이어 %s 회피 성공 (%.1f%%)", victim.getName(), clamped));
                        return;
                    }
                }

                // 방어력 보너스
                double defenseBonus = victimState.getDefenseBonus();
                if (defenseBonus > 0) {
                    double originalDamage = event.getDamage();
                    double newDamage = Math.max(s.minDamage(), originalDamage - (defenseBonus / s.defenseScale()));
                    event.setDamage(newDamage);
                }
            }
        }

        // 공격자 공격 처리
        if (event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player attacker = (Player) event.getDamager();
            LivingEntity victim = (LivingEntity) event.getEntity();

            PlayerSetState attackerState = playerEffectService.getPlayerState(attacker.getUniqueId());

            if (attackerState != null) {
                double baseDamage = event.getDamage();

                // 공격력 보너스
                double attackBonus = attackerState.getAttackBonus();
                if (attackBonus > 0) {
                    baseDamage += (attackBonus / s.attackScale());
                }

                // 치명타 판정
                double critChance = attackerState.getCriticalChance();
                double critDamage = attackerState.getCriticalDamage();

                if (critChance > 0) {
                    double clampedChance = Math.min(critChance, s.criticalChanceMaxPercent());
                    if (random.nextDouble() * 100 < clampedChance) {
                        double critMultiplier = 1.0 + (critDamage / s.criticalDamageScalePercent());
                        baseDamage *= critMultiplier;
                        LOGGER.fine(() -> String.format("플레이어 %s 치명타 발생 (%.1f%%)", attacker.getName(), clampedChance));
                    }
                }

                event.setDamage(baseDamage);

                // 흡혈 처리 (PVP에서만)
                if (victim instanceof Player) {
                    Player victimPlayer = (Player) victim;
                    double lifesteal = attackerState.getLifestealAmount();

                    if (lifesteal > 0) {
                        boolean sameTeam = false;
                        BiFunction<Player, Player, Boolean> checker = this.teamChecker;
                        if (checker != null) {
                            try {
                                Boolean result = checker.apply(attacker, victimPlayer);
                                sameTeam = result != null && result;
                            } catch (Exception e) {
                                LOGGER.warning(() -> String.format("팀 체크 중 오류: %s", e.getMessage()));
                            }
                        }

                        if (!sameTeam && random.nextDouble() < s.lifesteal().triggerChance()) {
                            double healAmount = lifesteal / s.lifesteal().healScale();
                            double currentHealth = attacker.getHealth();
                            double maxHealth = 20.0;
                            try {
                                var attr = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                                if (attr != null) {
                                    maxHealth = attr.getValue();
                                }
                            } catch (Throwable ignored) {
                                // Keep compatibility with older APIs / mocks.
                            }
                            if (maxHealth <= 0) {
                                maxHealth = 20.0;
                            }
                            double newHealth = Math.min(currentHealth + healAmount, maxHealth);

                            if (newHealth > currentHealth) {
                                attacker.setHealth(newHealth);
                            }
                        }
                    }
                }
            }
        }

        // 최소 데미지 보장
        if (event.getDamage() < s.minDamage()) {
            event.setDamage(s.minDamage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED ||
                event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {

            Player player = (Player) event.getEntity();
            PlayerSetState state = playerEffectService.getPlayerState(player.getUniqueId());

            if (state != null) {
                double regenBonus = state.getRegenerationBonus();
                if (regenBonus > 0) {
                    PluginSettings.CombatSettings s = this.settings;
                    double bonusHeal = regenBonus / s.regenScale();
                    event.setAmount(event.getAmount() + bonusHeal);
                }
            }
        }
    }
}

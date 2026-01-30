package kr.minex.pvpseteffect.infrastructure.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import kr.minex.pvpseteffect.application.service.PlayerEffectService;
import kr.minex.pvpseteffect.domain.vo.PotionApplicationMode;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * NATURAL 모드 포션 효과 재적용 스케줄러
 *
 * 짧은 지속시간의 포션 효과가 만료되기 전에 주기적으로 재적용합니다.
 * 장비 변경과 독립적으로 효과가 유지되도록 합니다.
 */
public final class PotionReapplyScheduler {

    private static final Logger LOGGER = Logger.getLogger(PotionReapplyScheduler.class.getName());

    private final Plugin plugin;
    private final PlayerEffectService playerEffectService;
    private final long intervalTicks;

    private BukkitTask task;

    /**
     * @param plugin 플러그인 인스턴스
     * @param playerEffectService 플레이어 효과 서비스
     * @param intervalTicks 재적용 주기 (틱)
     */
    public PotionReapplyScheduler(Plugin plugin, PlayerEffectService playerEffectService, long intervalTicks) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.playerEffectService = Objects.requireNonNull(playerEffectService, "playerEffectService cannot be null");
        this.intervalTicks = Math.max(10L, intervalTicks);
    }

    /**
     * 스케줄러 시작
     * NATURAL 모드일 때만 실제로 작동합니다.
     */
    public void start() {
        if (task != null && !task.isCancelled()) {
            return; // 이미 실행 중
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::reapplyAllPlayerPotions, intervalTicks, intervalTicks);
        LOGGER.info(() -> String.format("포션 재적용 스케줄러 시작 (주기: %d틱)", intervalTicks));
    }

    /**
     * 스케줄러 정지
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            LOGGER.info("포션 재적용 스케줄러 정지");
        }
    }

    /**
     * 스케줄러 실행 여부 확인
     */
    public boolean isRunning() {
        return task != null && !task.isCancelled();
    }

    /**
     * 모든 온라인 플레이어에게 포션 효과 재적용
     */
    private void reapplyAllPlayerPotions() {
        // NATURAL 모드가 아니면 스킵
        if (playerEffectService.getPotionApplicationMode() != PotionApplicationMode.NATURAL) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                // recalculateAndApply는 내부적으로 syncPotionEffects를 호출하여 효과 재적용
                playerEffectService.recalculateAndApply(player);
            } catch (Exception e) {
                LOGGER.fine(() -> String.format("플레이어 %s 포션 재적용 중 오류 (무시됨): %s",
                        player.getName(), e.getMessage()));
            }
        }
    }
}

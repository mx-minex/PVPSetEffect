package kr.minex.pvpseteffect.infrastructure.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import kr.minex.pvpseteffect.application.service.PlayerEffectService;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Coalesces frequent "recalculate" requests into at most one scheduled recalculation per player.
 *
 * Inventory events can fire in bursts (shift-click, drag, swap hand, etc). Without debouncing, a single
 * player can enqueue dozens of 1-tick tasks, creating avoidable main-thread pressure on busy servers.
 */
public final class PlayerRecalculationScheduler {

    private static final Logger LOGGER = Logger.getLogger(PlayerRecalculationScheduler.class.getName());

    private final Plugin plugin;
    private final PlayerEffectService playerEffectService;
    private final ConcurrentMap<UUID, BukkitTask> pending = new ConcurrentHashMap<>();
    private final long delayTicks;

    public PlayerRecalculationScheduler(Plugin plugin, PlayerEffectService playerEffectService, long delayTicks) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.playerEffectService = Objects.requireNonNull(playerEffectService, "playerEffectService cannot be null");
        this.delayTicks = Math.max(0L, delayTicks);
    }

    public void request(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();

        pending.compute(playerId, (id, existing) -> {
            if (existing != null && !existing.isCancelled()) {
                return existing; // already scheduled this tick-window
            }

            return Bukkit.getScheduler().runTaskLater(plugin, () -> {
                pending.remove(id);
                try {
                    // 플레이어가 오프라인이면 스킵
                    if (player.isOnline()) {
                        playerEffectService.recalculateAndApply(player);
                    }
                } catch (Exception e) {
                    LOGGER.warning(() -> "Recalculation task failed for " + player.getName() + ": " + e.getMessage());
                }
            }, delayTicks);
        });
    }

    public void cancel(Player player) {
        if (player == null) {
            return;
        }
        BukkitTask task = pending.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public int getPendingCount() {
        return pending.size();
    }

    /**
     * 모든 pending 태스크 취소 (플러그인 언로드 시 호출)
     */
    public void cancelAll() {
        for (BukkitTask task : pending.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        pending.clear();
    }
}


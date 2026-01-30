package kr.minex.pvpseteffect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import kr.minex.pvpseteffect.application.service.PlayerEffectService;
import kr.minex.pvpseteffect.application.service.SetEffectService;
import kr.minex.pvpseteffect.domain.repository.SetEffectRepository;
import kr.minex.pvpseteffect.domain.vo.PotionApplicationMode;
import kr.minex.pvpseteffect.infrastructure.config.PluginSettings;
import kr.minex.pvpseteffect.infrastructure.repository.YamlSetEffectRepository;
import kr.minex.pvpseteffect.infrastructure.scheduler.PlayerRecalculationScheduler;
import kr.minex.pvpseteffect.infrastructure.scheduler.PotionReapplyScheduler;
import kr.minex.pvpseteffect.presentation.command.SetEffectCommand;
import kr.minex.pvpseteffect.presentation.gui.SetEffectGUI;
import kr.minex.pvpseteffect.presentation.listener.CombatListener;
import kr.minex.pvpseteffect.presentation.listener.EquipmentListener;
import kr.minex.pvpseteffect.presentation.listener.GUIListener;

import java.util.Objects;

/**
 * PVP 세트 효과 플러그인 메인 클래스
 *
 * @author Junseo5
 * @version 1.0.0
 */
public final class PVPSetEffectPlugin extends JavaPlugin {

    private static final String PLUGIN_VERSION = "1.0.0";
    private static final String PLUGIN_AUTHOR = "Junseo5";
    private static final String DISCORD_CONTACT = "Junseo5#3213";

    private static PVPSetEffectPlugin instance;

    private SetEffectRepository setEffectRepository;
    private SetEffectService setEffectService;
    private PlayerEffectService playerEffectService;
    private SetEffectGUI setEffectGUI;
    private CombatListener combatListener;
    private PlayerRecalculationScheduler recalculationScheduler;
    private PotionReapplyScheduler potionReapplyScheduler;
    private int metricsTaskId = -1;
    private PluginSettings settings;

    @Override
    public void onEnable() {
        instance = this;

        printStartupBanner();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();
        reloadConfig();
        this.settings = PluginSettings.load(this);

        initializeDependencies();
        loadData();
        registerCommands();
        registerListeners();
        applyEffectsToOnlinePlayers();
        startMetricsLogging();

        getLogger().info("플러그인이 성공적으로 활성화되었습니다!");
        getLogger().info("등록된 세트 효과: " + setEffectService.getAllSetEffects().size() + "개");
    }

    private void printStartupBanner() {
        getServer().getConsoleSender().sendMessage("");
        getServer().getConsoleSender().sendMessage("§b[PVPSetEffect] §f========================================");
        getServer().getConsoleSender().sendMessage("§b[PVPSetEffect] §f  §ePVPSetEffect Plugin §av" + PLUGIN_VERSION);
        getServer().getConsoleSender().sendMessage("§b[PVPSetEffect] §f  §7Created by §f" + PLUGIN_AUTHOR);
        getServer().getConsoleSender().sendMessage("§b[PVPSetEffect] §f  §7Bug reports: §fDiscord - " + DISCORD_CONTACT);
        getServer().getConsoleSender().sendMessage("§b[PVPSetEffect] §f========================================");
        getServer().getConsoleSender().sendMessage("");
    }

    @Override
    public void onDisable() {
        // 1. 스케줄러 태스크 취소 (먼저 실행)
        if (recalculationScheduler != null) {
            recalculationScheduler.cancelAll();
        }

        // 2. 포션 재적용 스케줄러 정지
        if (potionReapplyScheduler != null) {
            potionReapplyScheduler.stop();
        }

        // 3. 메트릭스 태스크 취소
        if (metricsTaskId != -1) {
            getServer().getScheduler().cancelTask(metricsTaskId);
            metricsTaskId = -1;
        }

        // 4. 모든 플레이어 효과 정리
        if (playerEffectService != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerEffectService.clearPlayerEffects(player);
            }
            playerEffectService.clearAll();
        }

        // 5. 데이터 저장
        if (setEffectService != null) {
            setEffectService.saveAll();
            getLogger().info("세트 효과 데이터가 저장되었습니다.");
        }

        // 6. 모든 플러그인 태스크 취소 (안전망)
        getServer().getScheduler().cancelTasks(this);

        // 7. static 참조 제거
        instance = null;
        printShutdownBanner();
    }

    private void printShutdownBanner() {
        getServer().getConsoleSender().sendMessage("§b[PVPSetEffect] §f========================================");
        getServer().getConsoleSender().sendMessage("§b[PVPSetEffect] §c  플러그인이 비활성화되었습니다.");
        getServer().getConsoleSender().sendMessage("§b[PVPSetEffect] §7  이용해 주셔서 감사합니다!");
        getServer().getConsoleSender().sendMessage("§b[PVPSetEffect] §f========================================");
    }

    private void initializeDependencies() {
        this.setEffectRepository = new YamlSetEffectRepository(this);
        this.setEffectService = new SetEffectService(setEffectRepository);
        int potionDurationTicks = settings != null ? settings.potion().durationTicks() : Integer.MAX_VALUE;
        this.playerEffectService = new PlayerEffectService(setEffectService, potionDurationTicks);
        this.setEffectGUI = new SetEffectGUI(setEffectService);
        this.combatListener = new CombatListener(playerEffectService);

        if (settings != null) {
            this.combatListener.setSettings(settings.combat());
            this.playerEffectService.setCombatSettings(settings.combat());
            this.playerEffectService.setPotionSettings(settings.potion());
        }

        long debounceTicks = settings != null ? settings.recalculation().debounceTicks() : 1L;
        this.recalculationScheduler = new PlayerRecalculationScheduler(this, playerEffectService, debounceTicks);

        // NATURAL 모드일 경우 포션 재적용 스케줄러 시작
        if (settings != null && settings.potion().applicationMode() == PotionApplicationMode.NATURAL) {
            long reapplyInterval = settings.potion().natural().reapplyInterval();
            this.potionReapplyScheduler = new PotionReapplyScheduler(this, playerEffectService, reapplyInterval);
            this.potionReapplyScheduler.start();
            getLogger().info("포션 적용 모드: NATURAL (재적용 주기: " + reapplyInterval + "틱)");
        } else {
            getLogger().info("포션 적용 모드: IMMEDIATE");
        }
    }

    private void loadData() {
        setEffectService.loadAll();
    }

    private void registerCommands() {
        SetEffectCommand command = new SetEffectCommand(setEffectService, setEffectGUI);

        Objects.requireNonNull(getCommand("세트효과")).setExecutor(command);
        Objects.requireNonNull(getCommand("세트효과")).setTabCompleter(command);
        Objects.requireNonNull(getCommand("seteffect")).setExecutor(command);
        Objects.requireNonNull(getCommand("seteffect")).setTabCompleter(command);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new GUIListener(this, setEffectGUI), this);

        getServer().getPluginManager().registerEvents(
                new EquipmentListener(playerEffectService, setEffectGUI, recalculationScheduler), this);

        getServer().getPluginManager().registerEvents(combatListener, this);
    }

    private void applyEffectsToOnlinePlayers() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerEffectService.recalculateAndApply(player);
            }
        }, 20L);
    }

    private void startMetricsLogging() {
        if (settings != null && settings.metrics().intervalMinutes() <= 0) {
            return;
        }

        int intervalMinutes = settings != null ? settings.metrics().intervalMinutes() : 5;
        long intervalTicks = 20L * 60L * Math.max(1L, intervalMinutes);

        // Keep it low-noise: configurable interval, INFO level. Useful on production when investigating lag spikes.
        metricsTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (playerEffectService == null) {
                return;
            }
            getLogger().info("[metrics] " + playerEffectService.getMetricsSnapshot());
            if (recalculationScheduler != null && recalculationScheduler.getPendingCount() > 0) {
                getLogger().fine("[metrics] pendingRecalculations=" + recalculationScheduler.getPendingCount());
            }
        }, intervalTicks, intervalTicks);
    }

    public static PVPSetEffectPlugin getInstance() {
        return instance;
    }

    public SetEffectService getSetEffectService() {
        return setEffectService;
    }

    public PlayerEffectService getPlayerEffectService() {
        return playerEffectService;
    }

    public CombatListener getCombatListener() {
        return combatListener;
    }

    public void setTeamChecker(java.util.function.BiFunction<Player, Player, Boolean> checker) {
        if (combatListener != null) {
            combatListener.setTeamChecker(checker);
            getLogger().info("팀 체크 함수가 등록되었습니다.");
        }
    }
}

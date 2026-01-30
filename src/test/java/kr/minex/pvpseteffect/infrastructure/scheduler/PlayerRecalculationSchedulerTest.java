package kr.minex.pvpseteffect.infrastructure.scheduler;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import kr.minex.pvpseteffect.application.service.PlayerEffectService;
import kr.minex.pvpseteffect.infrastructure.scheduler.PlayerRecalculationScheduler;
import org.mockito.Mockito;
import org.bukkit.plugin.Plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

class PlayerRecalculationSchedulerTest {

    private ServerMock server;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void coalescesBurstRequestsIntoSingleRecalculation() {
        PlayerMock player = server.addPlayer();
        PlayerEffectService service = Mockito.mock(PlayerEffectService.class);
        PlayerRecalculationScheduler scheduler = new PlayerRecalculationScheduler(plugin, service, 1L);

        scheduler.request(player);
        scheduler.request(player);
        scheduler.request(player);

        // Still pending; not executed yet.
        assertEquals(1, scheduler.getPendingCount());

        server.getScheduler().performTicks(1);

        Mockito.verify(service, times(1)).recalculateAndApply(player);
        assertEquals(0, scheduler.getPendingCount());
    }

    @Test
    void cancelPreventsScheduledRecalculation() {
        PlayerMock player = server.addPlayer();
        PlayerEffectService service = Mockito.mock(PlayerEffectService.class);
        PlayerRecalculationScheduler scheduler = new PlayerRecalculationScheduler(plugin, service, 1L);

        scheduler.request(player);
        scheduler.cancel(player);

        server.getScheduler().performTicks(1);
        Mockito.verify(service, times(0)).recalculateAndApply(player);
    }
}

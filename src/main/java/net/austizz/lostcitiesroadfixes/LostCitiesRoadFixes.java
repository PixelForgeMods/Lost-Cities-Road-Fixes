package net.austizz.lostcitiesroadfixes;

import com.mojang.logging.LogUtils;
import net.austizz.lostcitiesroadfixes.compat.LostCitiesCompatibility;
import net.austizz.lostcitiesroadfixes.config.RoadFixesServerConfig;
import net.austizz.lostcitiesroadfixes.diagnostics.RoadFixesCommands;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesignReloadListener;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeDesignResources;
import net.austizz.lostcitiesroadfixes.integration.RoadGenerationRuntime;
import net.austizz.lostcitiesroadfixes.theme.RoadThemeReloadListener;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

@Mod(LostCitiesRoadFixes.MOD_ID)
public final class LostCitiesRoadFixes {
    public static final String MOD_ID = "lostcitiesroadfixes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LostCitiesRoadFixes(ModContainer container, IEventBus modBus) {
        LostCitiesCompatibility.requireCompatible();
        container.registerConfig(ModConfig.Type.SERVER, RoadFixesServerConfig.SPEC);
        modBus.addListener(RoadFixesServerConfig::onLoading);
        modBus.addListener(RoadFixesServerConfig::onReloading);
        NeoForge.EVENT_BUS.addListener(LostCitiesRoadFixes::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(RoadFixesCommands::registerEvent);
        NeoForge.EVENT_BUS.addListener(LostCitiesRoadFixes::logStoppingDiagnostics);
        LOGGER.info("Loading {} {}", container.getModInfo().getDisplayName(), container.getModInfo().getVersion());
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new RoadThemeReloadListener());
        event.addListener(new InterchangeDesignReloadListener(
                InterchangeDesignResources.repository(),
                RoadGenerationRuntime::invalidatePlans));
    }

    private static void logStoppingDiagnostics(ServerStoppingEvent event) {
        LOGGER.info("Road generation shutdown snapshot: {}",
                RoadGenerationRuntime.diagnostics().compactLine());
    }
}

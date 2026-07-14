package net.austizz.lostcitiesroadfixes;

import com.mojang.logging.LogUtils;
import net.austizz.lostcitiesroadfixes.compat.LostCitiesCompatibility;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(LostCitiesRoadFixes.MOD_ID)
public final class LostCitiesRoadFixes {
    public static final String MOD_ID = "lostcitiesroadfixes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LostCitiesRoadFixes(ModContainer container) {
        LostCitiesCompatibility.requireCompatible();
        LOGGER.info("Loading {} {}", container.getModInfo().getDisplayName(), container.getModInfo().getVersion());
    }
}

package net.austizz.lostcitiesroadfixes.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.austizz.lostcitiesroadfixes.LostCitiesRoadFixes;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public final class RoadThemeReloadListener
        extends SimplePreparableReloadListener<Map<RoadThemeId, RoadTheme>> {
    public static final String DIRECTORY = "lostcities_road_fixes/road_themes";

    @Override
    protected Map<RoadThemeId, RoadTheme> prepare(
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        FileToIdConverter converter = FileToIdConverter.json(DIRECTORY);
        Map<RoadThemeId, RoadTheme> themes = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry
                : converter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourceId = converter.fileToId(entry.getKey());
            RoadThemeId id = RoadThemeId.parse(resourceId.toString());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                RoadTheme previous = themes.put(id, RoadThemeJson.parseElement(id, json));
                if (previous != null) {
                    throw new IllegalStateException("Duplicate road theme " + id);
                }
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Could not load road theme " + id + " from " + entry.getKey(),
                        exception);
            }
        }
        return Map.copyOf(themes);
    }

    @Override
    protected void apply(
            Map<RoadThemeId, RoadTheme> themes,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        RoadThemeResources.install(themes);
        LostCitiesRoadFixes.LOGGER.info(
                "Loaded {} custom road theme(s); active theme is {}",
                themes.size(),
                RoadThemeResources.active().id());
    }
}

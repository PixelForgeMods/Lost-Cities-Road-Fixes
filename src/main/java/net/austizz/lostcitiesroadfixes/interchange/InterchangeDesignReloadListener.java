package net.austizz.lostcitiesroadfixes.interchange;

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

public final class InterchangeDesignReloadListener
        extends SimplePreparableReloadListener<Map<InterchangeDesignId, InterchangeDesign>> {
    public static final String DIRECTORY = "lostcities_road_fixes/interchanges";

    private final InterchangeDesignRepository repository;

    public InterchangeDesignReloadListener(InterchangeDesignRepository repository) {
        this.repository = repository;
    }

    @Override
    protected Map<InterchangeDesignId, InterchangeDesign> prepare(
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        FileToIdConverter converter = FileToIdConverter.json(DIRECTORY);
        Map<InterchangeDesignId, InterchangeDesign> designs = new HashMap<>();

        for (Map.Entry<ResourceLocation, Resource> entry
                : converter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation resourceId = converter.fileToId(entry.getKey());
            InterchangeDesignId id = InterchangeDesignId.parse(resourceId.toString());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                InterchangeDesign previous = designs.put(id, InterchangeDesignJson.parseElement(id, json));
                if (previous != null) {
                    throw new IllegalStateException("Duplicate interchange design " + id);
                }
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Could not load interchange design " + id + " from " + entry.getKey(),
                        exception);
            }
        }
        return Map.copyOf(designs);
    }

    @Override
    protected void apply(
            Map<InterchangeDesignId, InterchangeDesign> designs,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        repository.replaceCustom(designs);
        LostCitiesRoadFixes.LOGGER.info(
                "Loaded {} custom interchange design(s); {} total design(s) available",
                designs.size(),
                repository.snapshot().size());
    }
}

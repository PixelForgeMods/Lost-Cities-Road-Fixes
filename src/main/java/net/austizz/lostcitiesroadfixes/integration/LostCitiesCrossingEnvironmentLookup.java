package net.austizz.lostcitiesroadfixes.integration;

import mcjty.lostcities.api.LostChunkCharacteristics;
import mcjty.lostcities.api.MultiPos;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeBuildingFootprint;
import net.austizz.lostcitiesroadfixes.interchange.InterchangeEnvironment;
import net.austizz.lostcitiesroadfixes.interchange.planning.CrossingEnvironmentLookup;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class LostCitiesCrossingEnvironmentLookup implements CrossingEnvironmentLookup {
    private final IDimensionInfo provider;
    private final Map<ChunkPoint, RawChunk> cache = new HashMap<>();

    LostCitiesCrossingEnvironmentLookup(IDimensionInfo provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public InterchangeEnvironment survey(ChunkPoint center, int radiusBlocks) {
        Objects.requireNonNull(center, "center");
        if (radiusBlocks < 0) {
            throw new IllegalArgumentException("Environment radius cannot be negative");
        }
        long centerBlockX = Math.addExact(Math.multiplyExact((long) center.x(), 16L), 8L);
        long centerBlockZ = Math.addExact(Math.multiplyExact((long) center.z(), 16L), 8L);
        int minX = Math.toIntExact(Math.floorDiv(centerBlockX - radiusBlocks, 16L));
        int maxX = Math.toIntExact(Math.floorDiv(centerBlockX + radiusBlocks, 16L));
        int minZ = Math.toIntExact(Math.floorDiv(centerBlockZ - radiusBlocks, 16L));
        int maxZ = Math.toIntExact(Math.floorDiv(centerBlockZ + radiusBlocks, 16L));

        int surveyed = 0;
        int city = 0;
        List<InterchangeBuildingFootprint> buildings = new ArrayList<>();
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                surveyed++;
                RawChunk raw = raw(new ChunkPoint(x, z));
                if (raw.city()) {
                    city++;
                }
                if (raw.building() != null) {
                    buildings.add(raw.building());
                }
            }
        }
        return new InterchangeEnvironment(center, surveyed, city, buildings);
    }

    private RawChunk raw(ChunkPoint chunk) {
        return cache.computeIfAbsent(chunk, ignored -> {
            LostChunkCharacteristics characteristics = BuildingInfo.getChunkCharacteristics(
                    new ChunkCoord(provider.getType(), chunk.x(), chunk.z()), provider);
            InterchangeBuildingFootprint footprint = characteristics.couldHaveBuilding
                    ? footprint(chunk, characteristics.multiPos, characteristics.cityLevel)
                    : null;
            return new RawChunk(characteristics.isCity, footprint);
        });
    }

    private static InterchangeBuildingFootprint footprint(
            ChunkPoint chunk,
            MultiPos multiPos,
            int cityLevel) {
        if (multiPos == null || multiPos.isSingle()) {
            return new InterchangeBuildingFootprint(
                    chunk.x(), chunk.x(), chunk.z(), chunk.z(), cityLevel);
        }
        int minX = Math.subtractExact(chunk.x(), multiPos.x());
        int minZ = Math.subtractExact(chunk.z(), multiPos.z());
        return new InterchangeBuildingFootprint(
                minX,
                Math.addExact(minX, multiPos.w() - 1),
                minZ,
                Math.addExact(minZ, multiPos.h() - 1),
                cityLevel);
    }

    private record RawChunk(boolean city, InterchangeBuildingFootprint building) {
    }
}

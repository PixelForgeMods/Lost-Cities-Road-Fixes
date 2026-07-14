package net.austizz.lostcitiesroadfixes.integration;

import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.Highway;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadAxis;
import net.austizz.lostcitiesroadfixes.planning.continuity.RoadObservationSource;
import net.austizz.lostcitiesroadfixes.road.ChunkPoint;

import java.util.Objects;
import java.util.OptionalInt;

public final class LostCitiesRoadObservationSource implements RoadObservationSource {
    private final IDimensionInfo provider;
    private final LostCityProfile profile;

    public LostCitiesRoadObservationSource(IDimensionInfo provider, LostCityProfile profile) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    @Override
    public OptionalInt roadLevel(ChunkPoint chunk, RoadAxis axis) {
        ChunkCoord coordinate = new ChunkCoord(provider.getType(), chunk.x(), chunk.z());
        int level = switch (axis) {
            case X -> Highway.getXHighwayLevel(coordinate, provider, profile);
            case Z -> Highway.getZHighwayLevel(coordinate, provider, profile);
        };
        return level < 0 ? OptionalInt.empty() : OptionalInt.of(level);
    }
}

package net.austizz.lostcitiesroadfixes.interchange;

import net.austizz.lostcitiesroadfixes.road.RoadDesignStandard;

public final class InterchangeDesignResources {
    private static final InterchangeDesignRepository REPOSITORY =
            new InterchangeDesignRepository(InterchangeCatalogue.builtIns());

    private InterchangeDesignResources() {
    }

    public static InterchangeDesignRepository repository() {
        return REPOSITORY;
    }

    public static InterchangeSelector selector() {
        return new InterchangeSelector(REPOSITORY.snapshot(), RoadDesignStandard.DEFAULT);
    }
}

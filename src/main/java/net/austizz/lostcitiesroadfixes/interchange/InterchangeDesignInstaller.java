package net.austizz.lostcitiesroadfixes.interchange;

import java.util.Map;
import java.util.Objects;

public final class InterchangeDesignInstaller {
    private final InterchangeDesignRepository repository;
    private final Runnable plansInvalidator;

    public InterchangeDesignInstaller(
            InterchangeDesignRepository repository,
            Runnable plansInvalidator) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.plansInvalidator = Objects.requireNonNull(plansInvalidator, "plansInvalidator");
    }

    public void install(Map<InterchangeDesignId, InterchangeDesign> designs) {
        repository.replaceCustom(designs);
        plansInvalidator.run();
    }
}

package net.austizz.lostcitiesroadfixes.interchange;

public enum TrafficDemand {
    LOCAL,
    REGIONAL,
    HIGH;

    public boolean supports(TrafficDemand required) {
        return ordinal() >= required.ordinal();
    }

    public int excessCapacityOver(TrafficDemand required) {
        return ordinal() - required.ordinal();
    }
}

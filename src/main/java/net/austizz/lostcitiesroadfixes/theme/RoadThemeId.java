package net.austizz.lostcitiesroadfixes.theme;

import java.util.Objects;
import java.util.regex.Pattern;

public record RoadThemeId(String namespace, String path) implements Comparable<RoadThemeId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public RoadThemeId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid road theme namespace '" + namespace + "'");
        }
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid road theme path '" + path + "'");
        }
    }

    public static RoadThemeId parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':')
                || separator == value.length() - 1) {
            throw new IllegalArgumentException(
                    "Road theme IDs must have the form namespace:path: '" + value + "'");
        }
        return new RoadThemeId(value.substring(0, separator), value.substring(separator + 1));
    }

    @Override
    public int compareTo(RoadThemeId other) {
        int byNamespace = namespace.compareTo(other.namespace);
        return byNamespace != 0 ? byNamespace : path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + ':' + path;
    }
}

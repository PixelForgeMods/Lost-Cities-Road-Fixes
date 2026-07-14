package net.austizz.lostcitiesroadfixes.interchange;

import java.util.Objects;
import java.util.regex.Pattern;

public record InterchangeDesignId(String namespace, String path)
        implements Comparable<InterchangeDesignId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public InterchangeDesignId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid interchange namespace '" + namespace + "'");
        }
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid interchange path '" + path + "'");
        }
    }

    public static InterchangeDesignId parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            throw new IllegalArgumentException(
                    "Interchange design IDs must have the form namespace:path: '" + value + "'");
        }
        return new InterchangeDesignId(value.substring(0, separator), value.substring(separator + 1));
    }

    @Override
    public int compareTo(InterchangeDesignId other) {
        int namespaceOrder = namespace.compareTo(other.namespace);
        return namespaceOrder != 0 ? namespaceOrder : path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + ':' + path;
    }
}

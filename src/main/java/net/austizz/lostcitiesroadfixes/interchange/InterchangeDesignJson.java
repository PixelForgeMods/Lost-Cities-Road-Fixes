package net.austizz.lostcitiesroadfixes.interchange;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class InterchangeDesignJson {
    private static final int FORMAT = 1;
    private static final Set<String> FIELDS = Set.of(
            "format",
            "family",
            "junction_form",
            "minimum_radius_blocks",
            "required_quadrants",
            "minimum_approach_run_blocks",
            "structure_levels",
            "uses_loop_ramps",
            "all_movements_free_flow",
            "capacity",
            "free_flow_movement_count",
            "construction_complexity",
            "__comment__");

    private InterchangeDesignJson() {
    }

    public static InterchangeDesign parse(InterchangeDesignId id, String json) {
        try {
            return parseElement(id, JsonParser.parseString(json));
        } catch (JsonParseException exception) {
            throw invalid(id, "is not valid JSON", exception);
        }
    }

    static InterchangeDesign parseElement(InterchangeDesignId id, JsonElement json) {
        if (!json.isJsonObject()) {
            throw invalid(id, "root must be an object");
        }
        JsonObject root = json.getAsJsonObject();
        Set<String> unknown = new TreeSet<>(root.keySet());
        unknown.removeAll(FIELDS);
        if (!unknown.isEmpty()) {
            throw invalid(id, "unknown field(s): " + String.join(", ", unknown));
        }

        int format = integer(id, root, "format");
        if (format != FORMAT) {
            throw invalid(id, "field 'format' must be " + FORMAT + " but was " + format);
        }

        try {
            return new InterchangeDesign(
                    id,
                    enumeration(id, root, "family", InterchangeType.class),
                    enumeration(id, root, "junction_form", JunctionForm.class),
                    integer(id, root, "minimum_radius_blocks"),
                    integer(id, root, "required_quadrants"),
                    integer(id, root, "minimum_approach_run_blocks"),
                    integer(id, root, "structure_levels"),
                    bool(id, root, "uses_loop_ramps"),
                    bool(id, root, "all_movements_free_flow"),
                    enumeration(id, root, "capacity", TrafficDemand.class),
                    integer(id, root, "free_flow_movement_count"),
                    integer(id, root, "construction_complexity"));
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains(id.toString())) {
                throw exception;
            }
            throw invalid(id, exception.getMessage(), exception);
        }
    }

    private static int integer(InterchangeDesignId id, JsonObject root, String field) {
        JsonPrimitive primitive = primitive(id, root, field);
        if (!primitive.isNumber()) {
            throw invalid(id, "field '" + field + "' must be an integer");
        }
        try {
            return new BigDecimal(primitive.getAsString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalid(id, "field '" + field + "' must be a 32-bit integer", exception);
        }
    }

    private static boolean bool(InterchangeDesignId id, JsonObject root, String field) {
        JsonPrimitive primitive = primitive(id, root, field);
        if (!primitive.isBoolean()) {
            throw invalid(id, "field '" + field + "' must be a boolean");
        }
        return primitive.getAsBoolean();
    }

    private static <E extends Enum<E>> E enumeration(
            InterchangeDesignId id,
            JsonObject root,
            String field,
            Class<E> type) {
        JsonPrimitive primitive = primitive(id, root, field);
        if (!primitive.isString()) {
            throw invalid(id, "field '" + field + "' must be a string");
        }
        String value = primitive.getAsString().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            Set<String> choices = new HashSet<>();
            for (E constant : type.getEnumConstants()) {
                choices.add(constant.name().toLowerCase(Locale.ROOT));
            }
            throw invalid(id, "field '" + field + "' must be one of " + new TreeSet<>(choices), exception);
        }
    }

    private static JsonPrimitive primitive(InterchangeDesignId id, JsonObject root, String field) {
        JsonElement value = root.get(field);
        if (value == null) {
            throw invalid(id, "field '" + field + "' is required");
        }
        if (!value.isJsonPrimitive()) {
            throw invalid(id, "field '" + field + "' must be a primitive value");
        }
        return value.getAsJsonPrimitive();
    }

    private static IllegalArgumentException invalid(InterchangeDesignId id, String detail) {
        return new IllegalArgumentException("Invalid interchange design " + id + ": " + detail);
    }

    private static IllegalArgumentException invalid(
            InterchangeDesignId id,
            String detail,
            Throwable cause) {
        return new IllegalArgumentException("Invalid interchange design " + id + ": " + detail, cause);
    }
}

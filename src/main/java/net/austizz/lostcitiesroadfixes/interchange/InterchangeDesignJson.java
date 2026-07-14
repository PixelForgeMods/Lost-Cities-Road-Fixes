package net.austizz.lostcitiesroadfixes.interchange;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.austizz.lostcitiesroadfixes.interchange.layout.ApproachDirection;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeGeometryBlueprint;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeMovement;
import net.austizz.lostcitiesroadfixes.interchange.layout.InterchangeMovementBlueprint;
import net.austizz.lostcitiesroadfixes.interchange.layout.MovementKind;
import net.austizz.lostcitiesroadfixes.interchange.layout.RampControl;
import net.austizz.lostcitiesroadfixes.interchange.layout.RampForm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class InterchangeDesignJson {
    private static final int LEGACY_FORMAT = 1;
    private static final int GEOMETRY_FORMAT = 2;
    private static final Set<String> ROOT_FIELDS = Set.of(
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
            "geometry",
            "__comment__");
    private static final Set<String> GEOMETRY_FIELDS = Set.of(
            "movements",
            "__comment__");
    private static final Set<String> MOVEMENT_FIELDS = Set.of(
            "from",
            "to",
            "form",
            "control",
            "width_blocks",
            "structure_level",
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
        rejectUnknown(id, root, ROOT_FIELDS, "root");

        int format = integer(id, root, "format", "format");
        if (format != LEGACY_FORMAT && format != GEOMETRY_FORMAT) {
            throw invalid(id, "field 'format' must be 1 or 2 but was " + format);
        }
        Optional<InterchangeGeometryBlueprint> geometry;
        if (format == LEGACY_FORMAT) {
            if (root.has("geometry")) {
                throw invalid(id, "field 'geometry' requires format 2");
            }
            geometry = Optional.empty();
        } else {
            geometry = Optional.of(geometry(id, root));
        }

        try {
            return new InterchangeDesign(
                    id,
                    enumeration(id, root, "family", "family", InterchangeType.class),
                    enumeration(id, root, "junction_form", "junction_form", JunctionForm.class),
                    integer(id, root, "minimum_radius_blocks", "minimum_radius_blocks"),
                    integer(id, root, "required_quadrants", "required_quadrants"),
                    integer(id, root, "minimum_approach_run_blocks", "minimum_approach_run_blocks"),
                    integer(id, root, "structure_levels", "structure_levels"),
                    bool(id, root, "uses_loop_ramps", "uses_loop_ramps"),
                    bool(id, root, "all_movements_free_flow", "all_movements_free_flow"),
                    enumeration(id, root, "capacity", "capacity", TrafficDemand.class),
                    integer(id, root, "free_flow_movement_count", "free_flow_movement_count"),
                    integer(id, root, "construction_complexity", "construction_complexity"),
                    geometry);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains(id.toString())) {
                throw exception;
            }
            throw invalid(id, exception.getMessage(), exception);
        }
    }

    private static InterchangeGeometryBlueprint geometry(
            InterchangeDesignId id,
            JsonObject root) {
        try {
            return parseGeometry(id, root);
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().contains(id.toString())) {
                throw exception;
            }
            throw invalid(id, exception.getMessage(), exception);
        }
    }

    private static InterchangeGeometryBlueprint parseGeometry(
            InterchangeDesignId id,
            JsonObject root) {
        JsonObject geometry = object(id, root, "geometry", "geometry");
        rejectUnknown(id, geometry, GEOMETRY_FIELDS, "geometry");
        JsonArray movements = array(id, geometry, "movements", "geometry.movements");
        List<InterchangeMovementBlueprint> blueprints = new ArrayList<>(movements.size());
        for (int index = 0; index < movements.size(); index++) {
            String path = "geometry.movements[" + index + "]";
            JsonElement element = movements.get(index);
            if (!element.isJsonObject()) {
                throw invalid(id, "field '" + path + "' must be an object");
            }
            JsonObject movement = element.getAsJsonObject();
            rejectUnknown(id, movement, MOVEMENT_FIELDS, path);
            ApproachDirection from = enumeration(
                    id, movement, "from", path + ".from", ApproachDirection.class);
            ApproachDirection to = enumeration(
                    id, movement, "to", path + ".to", ApproachDirection.class);
            blueprints.add(new InterchangeMovementBlueprint(
                    movement(id, from, to, path),
                    enumeration(id, movement, "form", path + ".form", RampForm.class),
                    enumeration(id, movement, "control", path + ".control", RampControl.class),
                    integer(id, movement, "width_blocks", path + ".width_blocks"),
                    integer(id, movement, "structure_level", path + ".structure_level")));
        }
        return new InterchangeGeometryBlueprint(blueprints);
    }

    private static InterchangeMovement movement(
            InterchangeDesignId id,
            ApproachDirection from,
            ApproachDirection to,
            String path) {
        MovementKind kind;
        if (to == from.opposite()) {
            kind = MovementKind.STRAIGHT;
        } else if (to == from.rightTurnDestination()) {
            kind = MovementKind.RIGHT;
        } else if (to == from.leftTurnDestination()) {
            kind = MovementKind.LEFT;
        } else {
            throw invalid(id, "field '" + path + ".to' cannot make a U-turn");
        }
        return new InterchangeMovement(from, to, kind);
    }

    private static int integer(
            InterchangeDesignId id,
            JsonObject root,
            String field,
            String path) {
        JsonPrimitive primitive = primitive(id, root, field, path);
        if (!primitive.isNumber()) {
            throw invalid(id, "field '" + path + "' must be an integer");
        }
        try {
            return new BigDecimal(primitive.getAsString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalid(id, "field '" + path + "' must be a 32-bit integer", exception);
        }
    }

    private static boolean bool(
            InterchangeDesignId id,
            JsonObject root,
            String field,
            String path) {
        JsonPrimitive primitive = primitive(id, root, field, path);
        if (!primitive.isBoolean()) {
            throw invalid(id, "field '" + path + "' must be a boolean");
        }
        return primitive.getAsBoolean();
    }

    private static <E extends Enum<E>> E enumeration(
            InterchangeDesignId id,
            JsonObject root,
            String field,
            String path,
            Class<E> type) {
        JsonPrimitive primitive = primitive(id, root, field, path);
        if (!primitive.isString()) {
            throw invalid(id, "field '" + path + "' must be a string");
        }
        String value = primitive.getAsString().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            Set<String> choices = new HashSet<>();
            for (E constant : type.getEnumConstants()) {
                choices.add(constant.name().toLowerCase(Locale.ROOT));
            }
            throw invalid(id, "field '" + path + "' must be one of "
                    + new TreeSet<>(choices), exception);
        }
    }

    private static JsonPrimitive primitive(
            InterchangeDesignId id,
            JsonObject root,
            String field,
            String path) {
        JsonElement value = root.get(field);
        if (value == null) {
            throw invalid(id, "field '" + path + "' is required");
        }
        if (!value.isJsonPrimitive()) {
            throw invalid(id, "field '" + path + "' must be a primitive value");
        }
        return value.getAsJsonPrimitive();
    }

    private static JsonObject object(
            InterchangeDesignId id,
            JsonObject root,
            String field,
            String path) {
        JsonElement value = root.get(field);
        if (value == null) {
            throw invalid(id, "field '" + path + "' is required");
        }
        if (!value.isJsonObject()) {
            throw invalid(id, "field '" + path + "' must be an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray array(
            InterchangeDesignId id,
            JsonObject root,
            String field,
            String path) {
        JsonElement value = root.get(field);
        if (value == null) {
            throw invalid(id, "field '" + path + "' is required");
        }
        if (!value.isJsonArray()) {
            throw invalid(id, "field '" + path + "' must be an array");
        }
        return value.getAsJsonArray();
    }

    private static void rejectUnknown(
            InterchangeDesignId id,
            JsonObject object,
            Set<String> fields,
            String path) {
        Set<String> unknown = new TreeSet<>(object.keySet());
        unknown.removeAll(fields);
        if (!unknown.isEmpty()) {
            String location = path.equals("root") ? "" : " in '" + path + "'";
            throw invalid(id, "unknown field(s)" + location + ": "
                    + String.join(", ", unknown));
        }
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

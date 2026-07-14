package net.austizz.lostcitiesroadfixes.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.austizz.lostcitiesroadfixes.render.RoadSurfaceRole;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class RoadThemeJson {
    private static final int FORMAT = 1;
    private static final Set<String> ROOT_FIELDS = Set.of(
            "format",
            "full_blocks",
            "bottom_slabs",
            "foundation",
            "support",
            "maximum_support_depth_blocks",
            "__comment__");
    private static final Set<String> ROLE_FIELDS = java.util.Arrays.stream(
                    RoadSurfaceRole.values())
            .map(role -> role.name().toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private RoadThemeJson() {
    }

    public static RoadTheme parse(RoadThemeId id, String json) {
        try {
            return parseElement(id, JsonParser.parseString(json));
        } catch (JsonParseException exception) {
            throw invalid(id, "is not valid JSON", exception);
        }
    }

    static RoadTheme parseElement(RoadThemeId id, JsonElement json) {
        if (!json.isJsonObject()) {
            throw invalid(id, "root must be an object");
        }
        JsonObject root = json.getAsJsonObject();
        rejectUnknown(id, root, ROOT_FIELDS, "root");
        int format = integer(id, root, "format", "format");
        if (format != FORMAT) {
            throw invalid(id, "field 'format' must be 1 but was " + format);
        }
        try {
            EnumMap<RoadSurfaceRole, String> full = roleMap(
                    id, root, "full_blocks");
            EnumMap<RoadSurfaceRole, String> slabs = roleMap(
                    id, root, "bottom_slabs");
            return new RoadTheme(
                    id,
                    full,
                    slabs,
                    declaration(id, root, "foundation", "foundation"),
                    declaration(id, root, "support", "support"),
                    integer(
                            id,
                            root,
                            "maximum_support_depth_blocks",
                            "maximum_support_depth_blocks"));
        } catch (IllegalArgumentException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().contains(id.toString())) {
                throw exception;
            }
            throw invalid(id, exception.getMessage(), exception);
        }
    }

    private static EnumMap<RoadSurfaceRole, String> roleMap(
            RoadThemeId id,
            JsonObject root,
            String field) {
        JsonElement value = root.get(field);
        if (value == null) {
            throw invalid(id, "field '" + field + "' is required");
        }
        if (!value.isJsonObject()) {
            throw invalid(id, "field '" + field + "' must be an object");
        }
        JsonObject map = value.getAsJsonObject();
        rejectUnknown(id, map, ROLE_FIELDS, field);
        EnumMap<RoadSurfaceRole, String> result = new EnumMap<>(RoadSurfaceRole.class);
        for (RoadSurfaceRole role : RoadSurfaceRole.values()) {
            String key = role.name().toLowerCase(Locale.ROOT);
            result.put(role, declaration(id, map, key, field + '.' + key));
        }
        return result;
    }

    private static String declaration(
            RoadThemeId id,
            JsonObject object,
            String field,
            String path) {
        JsonPrimitive primitive = primitive(id, object, field, path);
        if (!primitive.isString()) {
            throw invalid(id, "field '" + path + "' must be a block-state string");
        }
        return primitive.getAsString();
    }

    private static int integer(
            RoadThemeId id,
            JsonObject object,
            String field,
            String path) {
        JsonPrimitive primitive = primitive(id, object, field, path);
        if (!primitive.isNumber()) {
            throw invalid(id, "field '" + path + "' must be an integer");
        }
        try {
            return new BigDecimal(primitive.getAsString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalid(id, "field '" + path + "' must be a 32-bit integer", exception);
        }
    }

    private static JsonPrimitive primitive(
            RoadThemeId id,
            JsonObject object,
            String field,
            String path) {
        JsonElement value = object.get(field);
        if (value == null) {
            throw invalid(id, "field '" + path + "' is required");
        }
        if (!value.isJsonPrimitive()) {
            throw invalid(id, "field '" + path + "' must be a primitive value");
        }
        return value.getAsJsonPrimitive();
    }

    private static void rejectUnknown(
            RoadThemeId id,
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

    private static IllegalArgumentException invalid(RoadThemeId id, String detail) {
        return new IllegalArgumentException("Invalid road theme " + id + ": " + detail);
    }

    private static IllegalArgumentException invalid(
            RoadThemeId id,
            String detail,
            Throwable cause) {
        return new IllegalArgumentException(
                "Invalid road theme " + id + ": " + detail, cause);
    }
}

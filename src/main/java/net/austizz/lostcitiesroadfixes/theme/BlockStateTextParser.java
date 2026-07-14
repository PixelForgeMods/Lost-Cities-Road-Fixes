package net.austizz.lostcitiesroadfixes.theme;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

final class BlockStateTextParser {
    private BlockStateTextParser() {
    }

    static BlockState parse(String declaration) {
        if (declaration == null || declaration.isBlank()) {
            throw new IllegalArgumentException("Block state cannot be blank");
        }
        String text = declaration.trim();
        int bracket = text.indexOf('[');
        String blockText = bracket < 0 ? text : text.substring(0, bracket);
        ResourceLocation blockId = ResourceLocation.tryParse(blockText);
        if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) {
            throw new IllegalArgumentException("Unknown block '" + blockText + "'");
        }
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        BlockState state = block.defaultBlockState();
        if (bracket < 0) {
            return state;
        }
        if (!text.endsWith("]") || text.indexOf('[', bracket + 1) >= 0) {
            throw new IllegalArgumentException("Malformed block state '" + text + "'");
        }
        String properties = text.substring(bracket + 1, text.length() - 1);
        if (properties.isBlank()) {
            throw new IllegalArgumentException("Block state property list cannot be empty");
        }
        Set<String> seen = new HashSet<>();
        for (String assignment : properties.split(",", -1)) {
            int equals = assignment.indexOf('=');
            if (equals <= 0 || equals != assignment.lastIndexOf('=')
                    || equals == assignment.length() - 1) {
                throw new IllegalArgumentException(
                        "Malformed block state property '" + assignment + "'");
            }
            String name = assignment.substring(0, equals).trim();
            String value = assignment.substring(equals + 1).trim();
            if (!seen.add(name)) {
                throw new IllegalArgumentException("Duplicate block property '" + name + "'");
            }
            Property<?> property = block.getStateDefinition().getProperty(name);
            if (property == null) {
                throw new IllegalArgumentException(
                        "Unknown property '" + name + "' for block " + blockId);
            }
            state = setValue(state, property, value, blockId);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState setValue(
            BlockState state,
            Property<T> property,
            String value,
            ResourceLocation blockId) {
        Optional<T> parsed = property.getValue(value);
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid value '" + value + "' for property '"
                            + property.getName() + "' on block " + blockId);
        }
        return state.setValue(property, parsed.orElseThrow());
    }
}

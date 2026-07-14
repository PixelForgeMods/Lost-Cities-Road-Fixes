#!/usr/bin/env python3
"""Assert that two road decks are connected near a stacked crossing.

This is a black-box saved-world regression checker. It reads generated chunk
data, identifies exposed Road Fixes surface blocks, and searches for a
driveable block-to-block path whose grade changes by at most one block per
horizontal step.
"""

from __future__ import annotations

import argparse
import importlib.util
from collections import Counter, deque
from pathlib import Path


AIR_BLOCKS = frozenset(
    {
        "minecraft:air",
        "minecraft:cave_air",
        "minecraft:void_air",
    }
)


def load_region_analyzer():
    analyzer_path = Path(__file__).with_name("analyze_road_region.py")
    spec = importlib.util.spec_from_file_location("road_region_analyzer", analyzer_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Could not load {analyzer_path}")
    analyzer = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(analyzer)
    return analyzer


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Check that lower and upper road decks have a driveable interchange path."
    )
    parser.add_argument("world", type=Path, help="saved-world directory")
    parser.add_argument("--chunk-x", type=int, required=True, help="stacked crossing chunk X")
    parser.add_argument("--chunk-z", type=int, required=True, help="stacked crossing chunk Z")
    parser.add_argument("--lower-y", type=int, required=True, help="lower road surface Y")
    parser.add_argument("--upper-y", type=int, required=True, help="upper road surface Y")
    parser.add_argument(
        "--radius",
        type=int,
        default=18,
        help="chunk radius searched for connecting ramps (default: 18)",
    )
    parser.add_argument(
        "--deck-radius",
        type=int,
        default=2,
        help="chunk radius used to identify each deck near the crossing (default: 2)",
    )
    return parser.parse_args()


def exposed_road_surfaces(
    analyzer,
    world: Path,
    center_x: int,
    center_z: int,
    radius: int,
    minimum_y: int,
    maximum_y: int,
) -> set[tuple[int, int, int]]:
    road_blocks = set(analyzer.ROAD_SURFACE_BLOCKS)
    blocks: dict[tuple[int, int, int], str] = {}
    minimum_section = minimum_y // 16
    maximum_section = (maximum_y + 1) // 16

    for chunk_z in range(center_z - radius, center_z + radius + 1):
        for chunk_x in range(center_x - radius, center_x + radius + 1):
            root = analyzer.read_chunk(world, chunk_x, chunk_z)
            if root is None:
                continue
            for section in root["sections"]:
                section_y = int(section["Y"])
                if not minimum_section <= section_y <= maximum_section:
                    continue
                block_states = section["block_states"]
                palette = [analyzer.block_name(entry) for entry in block_states["palette"]]
                indexes = analyzer.palette_indexes(block_states)
                for index, palette_index in enumerate(indexes):
                    block = palette[palette_index]
                    if block not in road_blocks and block not in AIR_BLOCKS:
                        continue
                    local_x = index & 15
                    local_z = (index >> 4) & 15
                    local_y = (index >> 8) & 15
                    position = (
                        chunk_x * 16 + local_x,
                        section_y * 16 + local_y,
                        chunk_z * 16 + local_z,
                    )
                    blocks[position] = block

    return {
        (x, y, z)
        for (x, y, z), block in blocks.items()
        if block in road_blocks and blocks.get((x, y + 1, z), "minecraft:air") in AIR_BLOCKS
    }


def deck_nodes(
    surfaces: set[tuple[int, int, int]],
    center_x: int,
    center_z: int,
    deck_radius: int,
    deck_y: int,
) -> set[tuple[int, int, int]]:
    minimum_x = (center_x - deck_radius) * 16
    maximum_x = (center_x + deck_radius + 1) * 16
    minimum_z = (center_z - deck_radius) * 16
    maximum_z = (center_z + deck_radius + 1) * 16
    return {
        position
        for position in surfaces
        if position[1] == deck_y
        and minimum_x <= position[0] < maximum_x
        and minimum_z <= position[2] < maximum_z
    }


def connected(
    surfaces: set[tuple[int, int, int]],
    starts: set[tuple[int, int, int]],
    goals: set[tuple[int, int, int]],
) -> tuple[bool, int]:
    pending = deque(starts)
    visited = set(starts)
    while pending:
        x, y, z = pending.popleft()
        if (x, y, z) in goals:
            return True, len(visited)
        for delta_x, delta_z in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for delta_y in (0, 1, -1):
                neighbor = (x + delta_x, y + delta_y, z + delta_z)
                if neighbor in surfaces and neighbor not in visited:
                    visited.add(neighbor)
                    pending.append(neighbor)
                    break
    return False, len(visited)


def main() -> int:
    arguments = parse_arguments()
    if arguments.lower_y >= arguments.upper_y:
        raise SystemExit("--lower-y must be below --upper-y")

    analyzer = load_region_analyzer()
    surfaces = exposed_road_surfaces(
        analyzer,
        arguments.world,
        arguments.chunk_x,
        arguments.chunk_z,
        arguments.radius,
        arguments.lower_y - 1,
        arguments.upper_y + 1,
    )
    starts = deck_nodes(
        surfaces,
        arguments.chunk_x,
        arguments.chunk_z,
        arguments.deck_radius,
        arguments.lower_y,
    )
    goals = deck_nodes(
        surfaces,
        arguments.chunk_x,
        arguments.chunk_z,
        arguments.deck_radius,
        arguments.upper_y,
    )
    is_connected, visited_count = connected(surfaces, starts, goals)
    levels = Counter(y for _, y, _ in surfaces)

    print(
        f"crossing=({arguments.chunk_x},{arguments.chunk_z}) "
        f"decks={arguments.lower_y}->{arguments.upper_y} "
        f"starts={len(starts)} goals={len(goals)} visited={visited_count}"
    )
    print(f"surface_levels={dict(sorted(levels.items()))}")
    print("CONNECTED" if is_connected else "DISCONNECTED")
    return 0 if is_connected else 1


if __name__ == "__main__":
    raise SystemExit(main())

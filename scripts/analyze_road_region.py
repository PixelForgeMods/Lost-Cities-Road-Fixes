#!/usr/bin/env python3
"""Inspect generated chunks for the default Lost Cities: Road Fixes surface.

The script intentionally uses only the Python standard library so a saved
NeoForge test world can be checked on a clean workstation. It prints JSON and
never modifies the world.
"""

from __future__ import annotations

import argparse
from collections import Counter, deque
import gzip
import json
from pathlib import Path
import struct
import sys
import zlib


SECTOR_BYTES = 4096
REGION_HEADER_BYTES = SECTOR_BYTES * 2
ROAD_SURFACE_BLOCKS = frozenset({
    "minecraft:cut_sandstone",
    "minecraft:cut_sandstone_slab",
    "minecraft:polished_deepslate",
    "minecraft:polished_deepslate_slab",
    "minecraft:quartz_block",
    "minecraft:smooth_quartz_slab",
})
ROAD_THEME_BLOCKS = ROAD_SURFACE_BLOCKS | {
    "minecraft:smooth_stone",
    "minecraft:smooth_stone_slab",
}


class NbtFormatError(ValueError):
    """Raised when a region contains NBT this inspector cannot decode."""


class NbtReader:
    def __init__(self, data: bytes):
        self._data = memoryview(data)
        self._offset = 0
        self._history: deque[str] = deque(maxlen=12)

    def root_compound(self) -> dict[str, object]:
        tag_type = self._unsigned_byte()
        if tag_type != 10:
            raise NbtFormatError(f"NBT root must be a compound, found tag {tag_type}")
        self._string()
        value = self._payload(tag_type)
        if not isinstance(value, dict):
            raise AssertionError("compound decoder returned a non-compound value")
        return value

    def _take(self, length: int) -> memoryview:
        end = self._offset + length
        if length < 0 or end > len(self._data):
            raise NbtFormatError("unexpected end of NBT payload")
        result = self._data[self._offset:end]
        self._offset = end
        return result

    def _unpack(self, pattern: str) -> int | float:
        size = struct.calcsize(pattern)
        return struct.unpack(pattern, self._take(size))[0]

    def _unsigned_byte(self) -> int:
        return int(self._unpack(">B"))

    def _string(self) -> str:
        length = int(self._unpack(">H"))
        # NBT historically uses Java's modified UTF-8. Mod attachments can
        # therefore contain byte sequences that Python's strict UTF-8 codec
        # rejects even though vanilla accepts the chunk. Replacement keeps
        # standard Minecraft tag names exact and is sufficient for inspection.
        return bytes(self._take(length)).decode("utf-8", errors="replace")

    def _payload(self, tag_type: int) -> object:
        if tag_type == 1:
            return int(self._unpack(">b"))
        if tag_type == 2:
            return int(self._unpack(">h"))
        if tag_type == 3:
            return int(self._unpack(">i"))
        if tag_type == 4:
            return int(self._unpack(">q"))
        if tag_type == 5:
            return float(self._unpack(">f"))
        if tag_type == 6:
            return float(self._unpack(">d"))
        if tag_type == 7:
            length = int(self._unpack(">i"))
            return bytes(self._take(length))
        if tag_type == 8:
            return self._string()
        if tag_type == 9:
            element_type = self._unsigned_byte()
            length = int(self._unpack(">i"))
            if length < 0:
                raise NbtFormatError("negative NBT list length")
            self._history.append(
                f"list(type={element_type}, length={length}, offset={self._offset})")
            return [self._payload(element_type) for _ in range(length)]
        if tag_type == 10:
            compound: dict[str, object] = {}
            while True:
                child_type = self._unsigned_byte()
                if child_type == 0:
                    return compound
                name = self._string()
                self._history.append(
                    f"tag(name={name!r}, type={child_type}, offset={self._offset})")
                compound[name] = self._payload(child_type)
        if tag_type == 11:
            length = int(self._unpack(">i"))
            if length < 0:
                raise NbtFormatError("negative NBT int-array length")
            return [int(self._unpack(">i")) for _ in range(length)]
        if tag_type == 12:
            length = int(self._unpack(">i"))
            if length < 0:
                raise NbtFormatError("negative NBT long-array length")
            return [int(self._unpack(">q")) for _ in range(length)]
        history = "; ".join(self._history)
        raise NbtFormatError(
            f"unsupported NBT tag type {tag_type} at offset {self._offset}; "
            f"recent tags: {history}")


def region_path(world: Path, chunk_x: int, chunk_z: int) -> Path:
    return world / "region" / f"r.{chunk_x // 32}.{chunk_z // 32}.mca"


def read_chunk(world: Path, chunk_x: int, chunk_z: int) -> dict[str, object] | None:
    path = region_path(world, chunk_x, chunk_z)
    if not path.is_file():
        return None
    # Some asynchronous save implementations leave zero-byte placeholders for
    # regions whose chunks were never committed. They represent absent chunks,
    # not a decodable region stream.
    if path.stat().st_size < REGION_HEADER_BYTES:
        return None
    index = (chunk_x % 32) + (chunk_z % 32) * 32
    with path.open("rb") as region:
        if region.seek(index * 4) >= REGION_HEADER_BYTES:
            raise NbtFormatError(f"invalid chunk index for {path}")
        location = region.read(4)
        if len(location) != 4:
            raise NbtFormatError(f"truncated region header in {path}")
        sector_offset = int.from_bytes(location[:3], "big")
        sector_count = location[3]
        if sector_offset == 0 and sector_count == 0:
            return None
        if sector_offset < 2 or sector_count == 0:
            raise NbtFormatError(f"invalid chunk location in {path}")
        region.seek(sector_offset * SECTOR_BYTES)
        stored_length_bytes = region.read(4)
        if len(stored_length_bytes) != 4:
            raise NbtFormatError(f"truncated chunk length in {path}")
        stored_length = int.from_bytes(stored_length_bytes, "big")
        if stored_length <= 1 or stored_length > sector_count * SECTOR_BYTES - 4:
            raise NbtFormatError(f"invalid chunk length in {path}")
        compression = region.read(1)
        payload = region.read(stored_length - 1)
        if len(compression) != 1 or len(payload) != stored_length - 1:
            raise NbtFormatError(f"truncated chunk payload in {path}")
    compression_id = compression[0]
    if compression_id & 0x80:
        raise NbtFormatError(f"external chunk streams are not supported: {path}")
    if compression_id == 1:
        decoded = gzip.decompress(payload)
    elif compression_id == 2:
        decoded = zlib.decompress(payload)
    elif compression_id == 3:
        decoded = payload
    else:
        raise NbtFormatError(
            f"unsupported region compression {compression_id} in {path}")
    return NbtReader(decoded).root_compound()


def block_name(palette_entry: object) -> str:
    if not isinstance(palette_entry, dict):
        raise NbtFormatError("block-state palette entry must be a compound")
    name = palette_entry.get("Name")
    if not isinstance(name, str):
        raise NbtFormatError("block-state palette entry has no Name")
    return name


def palette_indexes(block_states: dict[str, object]) -> list[int]:
    palette = block_states.get("palette")
    if not isinstance(palette, list) or not palette:
        raise NbtFormatError("block_states has no palette")
    if len(palette) == 1:
        return [0] * 4096
    data = block_states.get("data")
    if not isinstance(data, list):
        raise NbtFormatError("multi-entry block-state palette has no packed data")
    bits_per_entry = max(4, (len(palette) - 1).bit_length())
    entries_per_long = 64 // bits_per_entry
    mask = (1 << bits_per_entry) - 1
    indexes: list[int] = []
    for block_index in range(4096):
        packed_index = block_index // entries_per_long
        if packed_index >= len(data):
            raise NbtFormatError("block-state packed data is too short")
        packed = data[packed_index] & ((1 << 64) - 1)
        shift = (block_index % entries_per_long) * bits_per_entry
        palette_index = (packed >> shift) & mask
        if palette_index >= len(palette):
            raise NbtFormatError("block-state data references outside its palette")
        indexes.append(palette_index)
    return indexes


def inspect_chunk(
        root: dict[str, object],
        chunk_x: int,
        chunk_z: int,
        surface_blocks: frozenset[str] = ROAD_SURFACE_BLOCKS,
        theme_blocks: frozenset[str] = ROAD_THEME_BLOCKS) -> dict[str, object]:
    level = root.get("Level", root)
    if not isinstance(level, dict):
        raise NbtFormatError("chunk root is not a compound")
    sections = level.get("sections", level.get("Sections", []))
    if not isinstance(sections, list):
        raise NbtFormatError("chunk sections are not a list")

    road_by_y: Counter[int] = Counter()
    theme_by_y: Counter[int] = Counter()
    road_edges: dict[int, Counter[str]] = {}
    road_names: Counter[str] = Counter()
    for section in sections:
        if not isinstance(section, dict):
            continue
        section_y = section.get("Y")
        block_states = section.get("block_states", section.get("BlockStates"))
        if not isinstance(section_y, int) or not isinstance(block_states, dict):
            continue
        palette = block_states.get("palette")
        if not isinstance(palette, list) or not palette:
            continue
        names = [block_name(entry) for entry in palette]
        if not theme_blocks.intersection(names):
            continue
        indexes = palette_indexes(block_states)
        for block_index, palette_index in enumerate(indexes):
            name = names[palette_index]
            if name not in theme_blocks:
                continue
            local_y = block_index // 256
            remainder = block_index % 256
            local_z = remainder // 16
            local_x = remainder % 16
            world_y = section_y * 16 + local_y
            theme_by_y[world_y] += 1
            if name not in surface_blocks:
                continue
            road_by_y[world_y] += 1
            road_names[name] += 1
            edges = road_edges.setdefault(world_y, Counter())
            if local_z == 0:
                edges["north"] += 1
            if local_z == 15:
                edges["south"] += 1
            if local_x == 0:
                edges["west"] += 1
            if local_x == 15:
                edges["east"] += 1

    dominant_y = None
    if road_by_y:
        dominant_y = max(road_by_y, key=lambda y: (road_by_y[y], y))
    return {
        "chunk": [chunk_x, chunk_z],
        "status": level.get("Status", level.get("status", "unknown")),
        "road_surface_blocks": sum(road_by_y.values()),
        "road_theme_blocks": sum(theme_by_y.values()),
        "road_surface_by_y": dict(sorted(road_by_y.items())),
        "road_theme_by_y": dict(sorted(theme_by_y.items())),
        "road_surface_names": dict(sorted(road_names.items())),
        "dominant_surface_y": dominant_y,
        "dominant_surface_edges": (
            dict(sorted(road_edges[dominant_y].items())) if dominant_y is not None else {}
        ),
    }


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("world", type=Path, help="saved-world directory")
    parser.add_argument("--chunk-x", type=int, required=True, help="center chunk X")
    parser.add_argument("--chunk-z", type=int, required=True, help="center chunk Z")
    parser.add_argument(
        "--radius", type=int, default=1,
        help="inclusive square radius around the center chunk (default: 1)")
    parser.add_argument(
        "--surface-block", action="append", default=[], metavar="RESOURCE_LOCATION",
        help=("surface block to count; repeat for a custom/native palette "
              "(default: built-in Road Fixes palette)"))
    return parser.parse_args()


def main() -> int:
    arguments = parse_arguments()
    if arguments.radius < 0:
        print("--radius must not be negative", file=sys.stderr)
        return 2
    world = arguments.world.resolve()
    custom_surface = frozenset(arguments.surface_block)
    surface_blocks = custom_surface or ROAD_SURFACE_BLOCKS
    theme_blocks = custom_surface or ROAD_THEME_BLOCKS
    chunks: list[dict[str, object]] = []
    missing: list[list[int]] = []
    try:
        for chunk_z in range(
                arguments.chunk_z - arguments.radius,
                arguments.chunk_z + arguments.radius + 1):
            for chunk_x in range(
                    arguments.chunk_x - arguments.radius,
                    arguments.chunk_x + arguments.radius + 1):
                root = read_chunk(world, chunk_x, chunk_z)
                if root is None:
                    missing.append([chunk_x, chunk_z])
                    continue
                chunks.append(inspect_chunk(
                    root, chunk_x, chunk_z, surface_blocks, theme_blocks))
    except (OSError, NbtFormatError, zlib.error, gzip.BadGzipFile) as exception:
        print(f"Unable to inspect world: {exception}", file=sys.stderr)
        return 1

    result = {
        "world": str(world),
        "center_chunk": [arguments.chunk_x, arguments.chunk_z],
        "radius": arguments.radius,
        "road_surface_blocks": sorted(surface_blocks),
        "chunks": chunks,
        "missing_chunks": missing,
    }
    json.dump(result, sys.stdout, indent=2, sort_keys=True)
    print()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

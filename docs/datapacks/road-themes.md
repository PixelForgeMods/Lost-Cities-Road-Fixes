# Custom road themes

Road themes are server-data resources at:

```text
data/<namespace>/lostcities_road_fixes/road_themes/<path>.json
```

The resource location is the theme ID. The runtime currently uses
`lostcitiesroadfixes:default`, so a datapack activates its theme by replacing:

```text
data/lostcitiesroadfixes/lostcities_road_fixes/road_themes/default.json
```

Normal datapack priority determines the winning resource.

## Format 1

```json
{
  "format": 1,
  "full_blocks": {
    "shoulder": "minecraft:stone_bricks",
    "asphalt": "minecraft:black_concrete",
    "white_marking": "minecraft:quartz_block",
    "yellow_marking": "minecraft:yellow_concrete",
    "median": "minecraft:smooth_stone",
    "at_grade_intersection": "minecraft:gray_concrete"
  },
  "bottom_slabs": {
    "shoulder": "minecraft:stone_brick_slab[type=bottom,waterlogged=false]",
    "asphalt": "minecraft:oxidized_cut_copper_slab[type=bottom,waterlogged=false]",
    "white_marking": "minecraft:smooth_quartz_slab[type=bottom,waterlogged=false]",
    "yellow_marking": "minecraft:cut_sandstone_slab[type=bottom,waterlogged=false]",
    "median": "minecraft:smooth_stone_slab[type=bottom,waterlogged=false]",
    "at_grade_intersection": "minecraft:polished_blackstone_brick_slab[type=bottom,waterlogged=false]"
  },
  "foundation": "minecraft:iron_block",
  "support": "minecraft:deepslate_bricks",
  "maximum_support_depth_blocks": 40
}
```

Every semantic role is required in both maps:

- `asphalt` is the normal driving surface;
- `shoulder` is the outside edge of roads and ramps;
- `white_marking` is the lane divider;
- `yellow_marking` separates opposing traffic;
- `median` is the arterial center strip;
- `at_grade_intersection` covers same-height road crossings.

`full_blocks` supplies whole-block surfaces. `bottom_slabs` supplies odd
half-block elevations and must use dry slab states with `type=bottom`.
`foundation` forms a continuous one-block underside beneath every surface cell.
`support` forms the sparse columns beneath that underside. Surface and
structural states cannot be air, fluid, block-entity blocks, or the wrong shape.

Block declarations accept namespaced registry IDs and normal Minecraft state
properties. Unknown blocks, properties, values, roles, and fields reject the
reload with the theme ID and field path. All referenced modded blocks must exist
on the server.

## Landscape-aware supports

The replacement writer follows the active Lost Cities profile's
`highwaySupports` value:

- when disabled, roads keep their continuous one-block foundation but receive
  no deep support columns;
- when enabled, each affected chunk gets up to two deterministic support
  anchors spread toward opposite chunk corners;
- stacked surfaces choose the lowest road at each support column, preventing a
  pillar from being driven through a lower deck;
- support blocks replace air and unobstructed liquid, stop at the first solid
  terrain block or world build minimum, and never exceed
  `maximum_support_depth_blocks`;
- the built-in depth is 40 blocks, matching Lost Cities' native highway limit.

This policy works without special cases for default, ocean, floating, space,
sphere, or cavern landscapes: the profile controls whether supports are wanted,
while terrain and the depth cap control where each column ends.

## Reload behavior

Use `/reload` after changing a theme. Every winning theme is parsed and compiled
against the live block registry before publication. Reload is atomic: an invalid
block state leaves the previous complete compiled theme active. A theme-only
reload changes materials for subsequently generated chunks and does not discard
or recompute road/interchange geometry plans.

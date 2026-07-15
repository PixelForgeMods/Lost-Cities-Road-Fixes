# Custom interchange designs

Interchange designs are server-data resources. Put each design in a datapack at:

```text
data/<namespace>/lostcities_road_fixes/interchanges/<path>.json
```

The file above has the design ID `<namespace>:<path>`. For example,
`data/my_pack/lostcities_road_fixes/interchanges/urban/compact_diamond.json` defines
`my_pack:urban/compact_diamond`.

## Format 2: custom movement geometry

Format 2 lets a design declare every directed movement. The declaration chooses
which calculated route form, control, width, and structural tier each movement
uses. Road coordinates are deliberately not configurable: the mod joins fixed
native-road ports with validated straight and circular geometry so a declaration
cannot leave a disconnected endpoint.

Format 2 retains its exact eight-block native-port behavior for compatibility
with existing datapacks. Automatic auxiliary lanes and staggered professional
terminals apply to built-in templates and format-1 family replacements. A
format-2 author owns the explicit movement widths, forms, and terminal behavior
described here.

This complete two-tier four-way example keeps every authored turn on the upper
core tier. Its large radius leaves enough separated route length for the legal
grade and terminal locks:

```json
{
  "format": 2,
  "family": "diamond",
  "junction_form": "four_way",
  "minimum_radius_blocks": 320,
  "required_quadrants": 2,
  "minimum_approach_run_blocks": 352,
  "structure_levels": 2,
  "uses_loop_ramps": false,
  "all_movements_free_flow": false,
  "capacity": "regional",
  "free_flow_movement_count": 4,
  "construction_complexity": 3,
  "geometry": {
    "movements": [
      {"from":"north","to":"south","form":"mainline","control":"free_flow","width_blocks":8,"structure_level":2},
      {"from":"north","to":"west","form":"direct","control":"yield","width_blocks":8,"structure_level":2},
      {"from":"north","to":"east","form":"direct","control":"signalized","width_blocks":8,"structure_level":2},
      {"from":"east","to":"west","form":"mainline","control":"free_flow","width_blocks":8,"structure_level":1},
      {"from":"east","to":"north","form":"direct","control":"yield","width_blocks":8,"structure_level":2},
      {"from":"east","to":"south","form":"direct","control":"signalized","width_blocks":8,"structure_level":2},
      {"from":"south","to":"north","form":"mainline","control":"free_flow","width_blocks":8,"structure_level":2},
      {"from":"south","to":"east","form":"direct","control":"yield","width_blocks":8,"structure_level":2},
      {"from":"south","to":"west","form":"direct","control":"signalized","width_blocks":8,"structure_level":2},
      {"from":"west","to":"east","form":"mainline","control":"free_flow","width_blocks":8,"structure_level":1},
      {"from":"west","to":"south","form":"direct","control":"yield","width_blocks":8,"structure_level":2},
      {"from":"west","to":"north","form":"direct","control":"signalized","width_blocks":8,"structure_level":2}
    ]
  }
}
```

### Movement fields

- `from` and `to` are `north`, `east`, `south`, or `west`. U-turns are not
  supported.
- `form` is `mainline`, `direct`, or `loop`.
  - Opposite-direction movements must use `mainline`.
  - Left and right turns can use `direct`.
  - Only left turns can use `loop`; it is calculated as a 270-degree ramp.
- `control` is `free_flow`, `yield`, or `signalized`.
- `width_blocks` is the rendered route width from 3 through 16 blocks. A
  `mainline` movement must stay at 8 because its two carriageways are rendered by
  the fixed 32-block arterial cross-section; direct and loop ramp widths are
  customizable.
- `structure_level` is from 1 through the root `structure_levels` value. For a
  turning movement this is a physical core tier: level 1 is the lower planned
  road deck, the highest level is the upper deck, and intermediate levels are
  evenly spaced. The route retains its exact native elevation at both ports,
  changes grade only after a protected terminal lock, reaches the declared tier
  by the core, and returns before its destination merge. Mainline geometry stays
  on the surveyed arterial; its field remains part of the complete blueprint
  metadata.

A four-way design must contain exactly all 12 legal directed movements. A
three-way design uses the canonical west, east, and south approaches, with north
absent, and must contain exactly these six movements:

```text
west -> east    west -> south
east -> west    east -> south
south -> east   south -> west
```

At a generated three-way crossing, the complete canonical blueprint rotates to
whichever surveyed approach is absent. Widths, controls, forms, and tiers rotate
with their movements.

### Geometry and metadata validation

`minimum_radius_blocks` is the nominal radius used to calculate every turn.
With the fixed eight-block carriageway offset and 24-block minimum ramp radius:

- a direct right turn needs at least 32 nominal blocks;
- a direct left turn needs at least 16 nominal blocks;
- a loop needs at least 56 nominal blocks.

The site must also have more approach run than nominal radius. Routes retain the
native elevation at both ports, and the normal half-block grade limit applies to
every native-to-tier and tier-to-native transition. Candidate compilation also
checks every authored route overlap. Two surfaces may join at one elevation or
remain at least seven blocks apart; a site that would create a crushed deck is
rejected for that crossing instead of reaching world generation.

Selector metadata must describe the blueprint exactly:

- `uses_loop_ramps` is true if and only if at least one movement uses `loop`;
- `free_flow_movement_count` equals the number of `free_flow` controls;
- `all_movements_free_flow` is true if and only if every movement is free-flow;
- the largest movement `structure_level` equals root `structure_levels`.

Duplicate, missing, unavailable, contradictory, undersized, and unknown fields
reject the complete datapack reload with the design resource ID in the error.
A structurally valid design can still be infeasible at a particular crossing
when that site's measured run, gap, footprint, or route clearances are too small;
`/lostcitiesroadfixes explain <chunkX> <chunkZ>` reports that rejection.

## Format 1: selector-only compatibility

Existing format 1 files remain supported. They customize selector metadata and
use the built-in route template for their `family`:

```json
{
  "format": 1,
  "family": "diamond",
  "junction_form": "four_way",
  "minimum_radius_blocks": 56,
  "required_quadrants": 2,
  "minimum_approach_run_blocks": 160,
  "structure_levels": 2,
  "uses_loop_ramps": false,
  "all_movements_free_flow": false,
  "capacity": "regional",
  "free_flow_movement_count": 4,
  "construction_complexity": 1
}
```

Root values shared by both formats are:

- `family`: `trumpet`, `three_way_directional`, `spui`,
  `partial_cloverleaf`, `single_quadrant`, `diamond`, `cloverleaf`, or `stack`;
- `junction_form`: `three_way` or `four_way`;
- `capacity`: `local`, `regional`, or `high`.

Dimensions are whole Minecraft blocks. `required_quadrants` must be from 1
through 4. Structure levels and construction complexity must be positive.
Free-flow movement count must be from 0 through 6 for a three-way design or 0
through 12 for a four-way design.

The selector first rejects a design that cannot physically or operationally fit.
It then scores the remaining designs, so declarations should describe the actual
footprint and capabilities rather than values intended to force selection.

## Replacement and reload

A datapack resource with the ID of a built-in design replaces that design. The
built-in IDs are:

```text
lostcitiesroadfixes:trumpet
lostcitiesroadfixes:three_way_directional
lostcitiesroadfixes:spui
lostcitiesroadfixes:partial_cloverleaf
lostcitiesroadfixes:single_quadrant
lostcitiesroadfixes:diamond
lostcitiesroadfixes:cloverleaf
lostcitiesroadfixes:stack
```

Use Minecraft's `/reload` command after changing a datapack. Reload is
all-or-nothing: an invalid file reports its resource ID and field error, and the
previous complete design set remains active. Resource-pack priority follows
normal Minecraft datapack rules. Successful geometry changes invalidate cached
regional roads and interchanges before new chunks are generated.

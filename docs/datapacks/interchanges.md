# Custom interchange designs

Interchange designs are server-data resources. Put each design in a datapack at:

```text
data/<namespace>/lostcities_road_fixes/interchanges/<path>.json
```

The file above has the design ID `<namespace>:<path>`. For example,
`data/my_pack/lostcities_road_fixes/interchanges/urban/compact_diamond.json` defines
`my_pack:urban/compact_diamond`.

## Format 1

```json
{
  "format": 1,
  "family": "diamond",
  "junction_form": "four_way",
  "minimum_radius_blocks": 56,
  "required_quadrants": 2,
  "minimum_approach_run_blocks": 96,
  "structure_levels": 2,
  "uses_loop_ramps": false,
  "all_movements_free_flow": false,
  "capacity": "regional",
  "free_flow_movement_count": 2,
  "construction_complexity": 1
}
```

Allowed values are:

- `family`: `trumpet`, `three_way_directional`, `spui`, `partial_cloverleaf`, `single_quadrant`, `diamond`, `cloverleaf`, or `stack`.
- `junction_form`: `three_way` or `four_way`.
- `capacity`: `local`, `regional`, or `high`.

Dimensions are whole Minecraft blocks. `required_quadrants` must be from 1 through 4. Structure levels and construction complexity must be positive, and free-flow movement count cannot be negative.

The selector first rejects a design that cannot physically or operationally fit. It then scores the remaining designs, so declarations should describe the actual footprint and capabilities rather than values intended to force selection.

## Replacement and reload

A datapack resource with the ID of a built-in design replaces that design. The built-in IDs are:

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

Use Minecraft's `/reload` command after changing a datapack. Reload is all-or-nothing: an invalid file reports its resource ID and field error, and the previous complete design set remains active. Resource-pack priority follows normal Minecraft datapack rules.

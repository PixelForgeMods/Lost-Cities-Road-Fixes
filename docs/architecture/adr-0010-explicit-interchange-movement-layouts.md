# ADR-0010: Represent every interchange as explicit directed movements

Status: Accepted

## Context

An interchange name alone is not renderable or testable. The planner must know which inbound lane reaches which outbound lane, where its geometry begins and ends, whether it is controlled, and which structural tier it occupies. Shared or implied connections make it easy to ship a layout with a missing turn.

## Decision

Every built-in layout contains one connection for each legal directed movement. Three-way designs use west, east, and south as their canonical orientation, rotate to the three surveyed approaches, and contain six movements. Four-way designs contain twelve. Mainline movements are explicit routes; each of the remaining movements is a direct or loop ramp.

Built-in ports use right-hand traffic. Through carriageway centers remain eight
blocks from the arterial centerline, while an outer through edge at 12 blocks
starts a 32-block lane-add taper to a continuous auxiliary-lane center at 20
blocks. Built-in turning movements use staggered terminals on that auxiliary
lane and then shift outward before their controlling curve. Their elevation is
inherited from the X or Z highway deck, and the layout factory verifies every
generated turn ends at its expected terminal.

Format-2 datapack movement blueprints retain their original exact eight-block
native-port contract for backward compatibility. Selector-only format-1 designs
use the built-in professional terminal model.

Family templates differ by footprint, loop placement, controls, free-flow allocation, and structural tiers:

- trumpet: one loop and six free-flow movements;
- three-way directional: no loops and three structural tiers;
- SPUI: one central-control family with only mainlines free-flow;
- partial cloverleaf: two opposite loops;
- single-quadrant and diamond: controlled ramp movements with free-flow mainlines;
- cloverleaf: four loops, four distinct outer ramps, continuous auxiliary
  lanes, and all movements free-flow;
- stack: directional ramps across four declared tiers and all movements free-flow.

## Consequences

Completeness, endpoint continuity, free-flow claims, terminal separation,
auxiliary-lane continuity, loop count, and tier bounds can be checked without
Minecraft. Layout generation is deterministic and all centerlines remain
inside the approach envelope. Regional integration can rasterize the returned
routes directly and can use control/tier metadata for conflict treatment and
vertical separation.

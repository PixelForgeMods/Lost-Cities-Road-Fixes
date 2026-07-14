# ADR-0010: Represent every interchange as explicit directed movements

Status: Accepted

## Context

An interchange name alone is not renderable or testable. The planner must know which inbound lane reaches which outbound lane, where its geometry begins and ends, whether it is controlled, and which structural tier it occupies. Shared or implied connections make it easy to ship a layout with a missing turn.

## Decision

Every built-in layout contains one connection for each legal directed movement. Three-way designs use west, east, and south approaches and contain six movements. Four-way designs contain twelve. Mainline movements are explicit level routes; each of the remaining movements is a direct or loop ramp.

Ports use right-hand traffic and an eight-block offset from the 32-block arterial centerline. Their positions sit at the site's approach envelope, and their elevation is inherited from the X or Z highway deck. The layout factory verifies every generated turn ends at its expected port.

Family templates differ by footprint, loop placement, controls, free-flow allocation, and structural tiers:

- trumpet: one loop and six free-flow movements;
- three-way directional: no loops and three structural tiers;
- SPUI: one central-control family with only mainlines free-flow;
- partial cloverleaf: two opposite loops;
- single-quadrant and diamond: controlled ramp movements with free-flow mainlines;
- cloverleaf: four loops and all movements free-flow;
- stack: directional ramps across four declared tiers and all movements free-flow.

## Consequences

Completeness, endpoint continuity, free-flow claims, loop count, and tier bounds can be checked without Minecraft. Layout generation is deterministic and all centerlines remain inside the approach envelope. Regional integration can rasterize the returned routes directly and can use control/tier metadata for conflict treatment and vertical separation.

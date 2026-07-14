# ADR-0001: Road planning grid and geometric standards

Status: Accepted

## Context

Roads must remain deterministic when chunks generate in different orders, join continuously across planning boundaries, support vehicle-sized clearances, and transition between Lost Cities elevations without abrupt steps. Interchanges also need enough surrounding context to choose ramps before any owning chunk is rendered.

## Decision

- A planner owns a 64 by 64 chunk region.
- It reads a 32 chunk halo on every side, producing a 128 by 128 chunk planning window.
- Region lookup uses floor division, including for negative chunk coordinates.
- Only the owning region may emit geometry for a block; the halo is read-only planning context.
- Elevation is represented in integer half-block units.
- The arterial cross-section is 32 blocks: two 7-block lanes in each direction, a 2-block median, and a 1-block shoulder on each outside edge.
- A grade may rise at most one half-block per eight horizontal blocks.
- Minimum centerline curve radii are 32 blocks for arterials and 24 blocks for ramps.
- Vehicle clearance is at least 7 blocks; planners prefer 10 blocks between stacked deck elevations.

These values live in `RoadDesignStandard`. Planning code must ask that object to validate geometry instead of restating numeric limits.

## Consequences

Planning can be repeated independently for any region and still agree at boundaries. The halo increases sampling work, but region caching can amortize it. Half-block elevation avoids floating-point seams and maps directly to full blocks and slabs during rasterization. Interchanges that cannot satisfy the standard must be rejected or downgraded rather than rendered with inaccessible ramps.

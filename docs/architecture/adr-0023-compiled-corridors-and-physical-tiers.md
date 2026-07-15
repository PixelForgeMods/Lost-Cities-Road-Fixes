# ADR-0023: Reserve compiled corridors and make structure tiers physical

Status: Accepted

## Context

The 1.1.x planner selected each crossing independently and resolved density by
comparing only nominal core squares. A diamond can compile with a 192-block
approach and a stack with a 512-block approach, so neighboring cores could be
disjoint while their graded arterials and ramps occupied the same chunks. Two
crossings 160 blocks apart could therefore plan different elevations for one
shared road. The renderer then received both individually valid plans and
produced decks inside the seven-block vehicle envelope.

The selector also trusted catalogue metadata before compiling geometry. Demand
and family variation came partly from the selection seed rather than measured
road/city facts. `structure_level` on format-2 custom movements was retained as
metadata but did not change a route's physical elevation. Earlier attempts to
remove ramp collisions by flattening profiles erased the intended stack tiers.

## Decision

Selection uses a measured `InterchangeEnvironment`: contiguous arm length,
native and planned road elevations, available radius/quadrants, city density,
and deduplicated single/multi-building footprints. Every candidate compiles at
the shortest safe chunk-aligned approach before scoring. Building displacement
is the first score term, followed by capacity/operational fit, footprint,
vertical work, approach length, and complexity. Selection and conflict ties do
not use the world seed.

Conflict resolution reserves three rectangles for every compiled candidate: a
core plus its X and Z approach corridors. Any component overlap conflicts,
except a connected collinear corridor whose two plans both leave the shared
arterial flat at exactly the same elevation. Incompatible shared grades reject
the stable lower-priority crossing before geometry composition. The survey halo
covers the largest possible pair of 512-block approaches.

A stack requires four physical tiers separated by at least seven blocks. Its
turns use monotonic endpoint-to-core-to-end profiles and common departure and
arrival trunks; it is rejected when the measured deck span cannot contain all
four tiers. Native-to-planned arterials use only the minimum legal grade run,
then remain level through the core.

Format-2 turning `structure_level` values map to physical elevations between the
lower and upper planned decks. The compiler protects terminal locks, requires
the declared tier at the route core, and rejects insufficient run or unsafe
route overlaps. A final composed-surface validator rejects any remaining pair
of distinct decks less than seven blocks apart.

World mutation is ordered in three complete passes: clear every planned vehicle
envelope, place every foundation/deck, then place supports. Operator diagnostics
retain crossing outcomes and expose family counts plus
`/lostcitiesroadfixes explain <chunkX> <chunkZ>`.

## Consequences

Nearby crossings can no longer pass selection independently and collide later.
Compact low-gap sites choose compact families; a stack appears only where its
four real levels fit. Custom tiers affect blocks rather than labels, and unsafe
custom geometry fails as a candidate with an actionable reason. Results remain
deterministic across seeds, generation order, region boundaries, and restarts.
The changes apply only to newly generated chunks.

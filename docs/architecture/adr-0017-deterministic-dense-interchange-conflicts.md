# ADR-0017: Resolve dense interchange cores with bounded stable priority

Status: Accepted

## Context

Lost Cities permits highway lines eight chunks apart when
`highwayDistanceMask = 7`. Their centerlines are then only 128 blocks apart.
The largest selectable interchange site has a 128-block nominal radius, and a
full-width arterial occupies another 16 blocks on either side of its
centerline. Two independently valid cloverleaf or stack plans can therefore
occupy the same blocks, create duplicate decks, or replace only part of one
another. Generation order cannot be allowed to decide which structure survives.

Shrinking a design below its declared radius, steepening its ramps, or reducing
deck clearance would make it non-driveable. An unbounded greedy walk across a
chain of crossings would also make one regional plan depend on arbitrarily
distant chunks.

## Decision

Every feasible selected design reserves an axis-aligned core with half-extent
equal to its declared minimum radius plus half the 32-block arterial width.
Core interiors conflict; cores that only touch are compatible. A candidate is
admitted only if no directly conflicting candidate has a higher stable
priority. Priority compares demand, number of approaches, compactness, the
unsigned world-derived site seed, and finally chunk coordinates.

This local-minimum rule forms a deterministic hard-core set: two accepted cores
cannot overlap, candidate iteration order is irrelevant, and no transitive
conflict search is needed. The maximum selectable 128-block radius plus the
16-block road half-width yields a 144-block core. A regional planner therefore
surveys an 18-chunk halo, enough to see both halves of every possible 288-block
conflict. Each 64×64 owner emits only its own accepted, infeasible, and
conflict-suppressed crossings.

A conflict-suppressed crossing emits no interchange geometry. Its ordinary X
and Z road surfaces remain available, preserving straight-through travel
without a partial ramp or competing deck. The suppression records the
higher-priority crossing and required center separation separately from a site
whose designs were all infeasible. Operator diagnostics expose both counts.

## Consequences

Dense highway grids cannot produce overlapping interchange structures, even
across positive or negative planning boundaries or under parallel generation.
Isolated and exactly tangent sites keep their previous selection and geometry.
Datapack designs participate through their validated declared radius, and the
runtime plan fingerprint prevents old pre-reservation geometry from mixing with
the new policy.

Not every stacked crossing can receive a full interchange when the requested
safe structures physically do not fit. Such sites remain straight flyovers;
drivers use a nearby admitted interchange to transfer between highways. This is
preferable to silently weakening curve, grade, clearance, or continuity rules.

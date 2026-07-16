# ADR-0024: Fail safe at the interchange composition boundary

Status: Accepted

## Context

Road-surface composition is used by both the Lost Cities building reservation
hook and the later replacement-road writer. A clearance exception in either
path could therefore abort generation of the whole Lost Cities chunk. Close
ramp profiles also required a deterministic distinction between one connected
pavement surface and independently stacked decks.

## Decision

Ramp candidates are resolved per block column around the horizontally nearest
route. Every candidate inside that route's seven-block vertical envelope is one
connected surface; candidates at or beyond the clearance remain independent
decks. Resolution repeats from the remaining nearest route, avoiding transitive
clustering through an intermediate elevation.

Built-in planning surveys turning routes, auxiliary lanes, and the complete
32-block arterial footprint. Feasible conflicts are held on physical structure
tiers, SPUI turns share a lower signalized core, and terminal branches include
a two-block pavement-separation margin. Spatial segment indexes bound proximity
queries, and candidate compilation is cached using immutable geometry and deck
facts.

The runtime composition boundary catches a rejected interchange overlay before
it reaches Lost Cities. It then composes only the unaffected native roads and
both graded arterial replacements. Ramps are omitted as one atomic unit, so the
fallback cannot write a partial interchange. The same method serves building
reservation and late rendering.

## Consequences

An interchange defect can no longer delete an entire city chunk. Through
highways remain continuous and vertically safe, while diagnostics retain a
fallback count and the first detailed cause. A rejected overlay may omit ramps
for that chunk, so analytical layout validation and catalogue-envelope tests
remain required; the fallback is a final containment boundary, not a substitute
for valid geometry.

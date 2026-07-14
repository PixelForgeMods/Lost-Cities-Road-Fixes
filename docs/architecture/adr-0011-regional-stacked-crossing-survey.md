# ADR-0011: Survey stacked crossings before selecting interchanges

Status: Accepted

## Context

Lost Cities independently generates X and Z highways. A chunk can therefore contain same-level roads, differing-level roads, a four-arm crossing, or an endpoint that forms a T. Choosing from the current chunk alone cannot establish enough approach length for ramps, and native highway levels are only six blocks apart.

## Decision

The owning 64×64 planning region surveys every chunk through a `RoadTileLookup`. A differing-level X/Z pair becomes a candidate only when at least three contiguous same-level arms exist. Each arm is measured conservatively in complete chunks, capped at 256 blocks. The shortest arm determines approach length; half that length, capped at 128 blocks, determines available radius.

Native deck elevations retain Lost Cities' six-block level spacing. At the interchange center, the upper deck is raised when necessary to provide the preferred ten-block separation. The full ten-block transition therefore requires a 160-block approach under the established half-block grade rule.

Form, footprint, demand, quadrants, loop space, structural tiers, and a world/chunk-derived stable seed become an `InterchangeSite`. The selector records either a complete decision or all per-design rejection reasons. Same-level crossings remain ordinary rasterized intersections and never enter this process.

## Consequences

Crossings on planning boundaries have one owner, including negative coordinates. A site that cannot fit its grade or layout remains a diagnosed stacked crossing rather than receiving unsafe geometry. Runtime integration must regrade the selected center deck back to native elevations at the measured approach ports.

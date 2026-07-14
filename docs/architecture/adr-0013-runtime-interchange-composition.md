# ADR-0013: Compose regional interchanges in the late road runtime

Status: Accepted

## Context

Interchange selection and rasterization do not affect a world until the late Lost Cities generation hook can discover every selected geometry that reaches the current chunk. Planning only the target's owner region misses envelopes crossing a 64×64-region boundary. Removing a whole native endpoint tile also creates a new partial-chunk gap beyond a port.

## Decision

Runtime maintains separate generations for repaired road plans and calculated interchange geometry. Each interchange cache key contains the full, ordered datapack design snapshot as well as the Lost Cities highway rules. A design reload atomically installs the new snapshot and replaces both cache generations.

For each target chunk, runtime visits every owner region within the maximum 256-block interchange influence. Each crossing is still selected once by its owning region, and cached geometry performs a final chunk-envelope filter. Native roads are rasterized first, then only native-elevation cells inside a selected graded arterial footprint are masked. The remaining straight-road surface and all selected interchange surfaces are merged by full `(x, z, half-block elevation)` position and passed to `MinecraftRoadWriter` exactly once.

Planning, selection, rejection, and interchange-render counters remain available for diagnostics. The first selected design is logged with its crossing chunk and dimension.

## Consequences

Interchanges now participate in normal Lost Cities chunk generation, including across positive and negative planning boundaries. Native roads resume on the first block beyond an interchange port even when that port lies halfway through a chunk. Datapack changes cannot mix old road observations with new interchange decisions, and stacked cells are preserved through the one-pass write.

# ADR-0012: Rasterize selected interchanges as regraded stacked surfaces

Status: Accepted

## Context

A selected interchange must replace the native Lost Cities highway tiles without creating a new seam. Its center decks can differ from the native elevations at the edge of the surveyed envelope, and a T-junction can be missing any one of its four arms. Flattening all coverage at a block column would also destroy legitimate stacked roads.

## Decision

Each surveyed arterial becomes an exact 32-block-wide graded surface. Its ports retain the native Lost Cities elevation, while a validated piecewise half-block profile reaches the planned center elevation and returns to native at the opposite port. Three-way layouts rotate their canonical family geometry to the actual surveyed arms; no surface or movement is emitted for the missing arm.

The full-width arterial rasterizer owns mainline coverage. Eight-block turning routes are rasterized separately and merged by `(x, z, half-block elevation)`. Equal-position conflicts use a fixed role priority, while cells at different elevations remain distinct. A selected geometry identifies only same-axis, same-native-elevation road tiles on its surveyed arms as replaced.

## Consequences

Interchange decks retain road markings and width through chunk boundaries, return to native roads at the planning envelope, and preserve vertical separation at crossings. Runtime integration can mask the exact replaced native cells, merge the bounded interchange surface with unaffected roads, and write one deterministic chunk-local result.

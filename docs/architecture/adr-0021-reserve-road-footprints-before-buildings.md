# ADR-0021: Reserve final road footprints before Lost Cities buildings

Status: Accepted

## Context

Post-cleanup road writing guarantees an intact deck, but it also means a ramp
can replace blocks from a building that Lost Cities selected earlier. Avoiding
only a nominal interchange radius is insufficient because approaches, tapers,
auxiliary lanes, and custom road surfaces can extend beyond that radius.

## Decision

A required `BuildingInfo` constructor Mixin asks the same runtime composer used
for final road rendering whether a road surface occupies the candidate
building's footprint. The check uses repaired native roads and complete compiled
interchange geometry, not a centerline or nominal-radius approximation.

The reserved horizontal area is the full single- or multi-chunk building
footprint plus one complete chunk of clearance on every side. If any candidate
chunk contains at least one final road surface cell, `hasBuilding` is cleared
before Lost Cities emits the structure. Every section of a multi-building uses
the same top-left footprint calculation, so suppression is all-or-nothing.
Lost Cities represents a single building as `MultiPos(-1,-1,1,1)`; that sentinel
is normalized to local offset `(0,0)` before validation.

The strict compatibility probe verifies the pinned `BuildingInfo` constructor,
and diagnostics count suppressed building chunks. Invalid multi-building
dimensions fail generation rather than silently reserving the wrong area.

## Consequences

New buildings cannot touch or be cut by generated roads, ramps, auxiliary
lanes, or their one-chunk clearance band. The affected city chunks may become
streets, parks, or open terrain according to Lost Cities. Existing buildings
are not deleted and saved chunks are never rewritten.

# ADR-0005: Chunk-local road surface rasterization

Status: Accepted

## Context

The planner describes roads by chunk segment, axis, and half-block elevation. Minecraft needs exact surface cells, while neighboring chunks must agree on width and paint phase. Crossings also need different treatment depending on whether their elevations match.

## Decision

An axis-aligned road segment is centered between local blocks 7 and 8 of its Lost Cities highway chunk. Its 32-block cross-section extends eight blocks into each neighboring chunk. The layout is:

- one-block outside shoulders;
- two seven-block lanes per direction;
- solid yellow cells next to the median;
- a two-block median; and
- globally phased four-on/four-off white lane-divider cells.

Rasterization is requested one target chunk at a time and emits only cells inside that chunk. Surface positions retain integer half-block elevation. If X and Z coverage share a position and elevation, the cell becomes an open at-grade intersection. Coverage at distinct elevations remains separate.

## Consequences

Chunk borders cannot reset road width or lane dashes. A Minecraft-specific writer can map semantic roles to theme blocks without duplicating geometry. Interchange planning remains responsible for connecting stacked decks; the rasterizer deliberately does not merge them.

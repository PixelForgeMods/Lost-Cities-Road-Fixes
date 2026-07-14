# ADR-0003: Continuity-first Lost Cities road snapshot

Status: Accepted

## Context

Lost Cities exposes separate X- and Z-axis highway levels per chunk. Those answers are deterministic, but rendering can leave a physical hole and addon profiles may create short topology gaps. The replacement planner needs a stable input snapshot before it performs more expensive endpoint routing or interchange selection.

## Decision

For each regional planning window, the continuity planner samples every `(chunk, axis)` pair exactly once and stores the result as immutable `RoadTile` values. It then fills a collinear gap only when:

- both bounding observations use the same axis;
- both have the same Lost Cities level; and
- one to four chunks are missing between them.

The planner reads the complete 32-chunk halo but publishes only tiles owned by its 64 by 64 region. Tiles are sorted by chunk and axis before publication.

## Consequences

A literal missing chunk and other short same-height omissions become continuous without guessing at unsafe slopes. Different-height endpoints remain disconnected at this stage and are delegated to elevation/interchange planning. Long gaps and one-sided dead ends are also left for a later topology pass. Neighboring regions see enough shared context to make compatible decisions at their ownership edge.

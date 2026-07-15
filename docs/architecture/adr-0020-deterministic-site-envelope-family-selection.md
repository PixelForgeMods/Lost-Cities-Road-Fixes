# ADR-0020: Diversify interchange families with deterministic site envelopes

Status: Accepted

## Context

Lost Cities exposes highway topology and elevation but no traffic study,
right-of-way parcel model, or interchange preference. Feeding every long
four-way crossing the same maximal envelope made the score function repeatedly
choose a full cloverleaf, even though all eight requested families were loaded.

## Decision

The survey derives a stable site envelope from world seed, crossing chunk, and
both highway levels. Physical facts remain authoritative: observed arm length,
deck separation, topology, curve radius, grade, and vehicle clearance are never
randomized. The derived envelope supplies only the unavailable planning facts:
traffic class, available quadrants, loop permission, structural levels, and
whether every turn must be free-flow.

Normal long four-way sites use six deterministic envelope classes targeting
SPUI, partial cloverleaf, single-quadrant, diamond, cloverleaf, and stack. The
normal selector still chooses the lowest-cost feasible design inside that
class. A cloverleaf class falls back when less than 320 blocks of approach or
224 blocks of radius are available; a stack class falls back below 512 blocks.
Unusually large deck gaps prefer a stack when its full grade run fits and a
partial cloverleaf otherwise. Three-way sites alternate between trumpet and
directional envelopes.

The choice is independent of chunk-generation order and restart timing. A
regression samples 512 world seeds, requires all six four-way families to be
selected, and compiles the first geometry of every family.

## Consequences

Long roads no longer collapse to cloverleaf-only generation. Selection is
varied but reproducible, and an operator can regenerate the same seed without
moving an interchange. The envelope is a game-world proxy for planning data
that Lost Cities does not expose; it does not claim to be a real traffic model.

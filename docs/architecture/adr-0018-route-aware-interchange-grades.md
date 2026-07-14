# ADR-0018: Evaluate and merge interchanges using their compiled ramp routes

Status: Accepted

## Context

ADR-0011 treated Lost Cities highway crossings as adjacent six-block levels and
tested the full deck difference against the straight approach length. Lost
Cities actually derives highway levels from city levels 0–8. The reported
crossing at chunk `(-80, 104)` uses levels 0 and 3, producing road surfaces at
Y=71 and Y=89. Its 18-block rise needs 288 horizontal blocks under the accepted
half-block-per-eight-block grade.

The site had 256-block approaches, so the selector rejected every family even
though a cloverleaf's shortest right-turn route has 307 usable blocks before it
joins the destination arterial. When selection was allowed in isolation,
independent linear elevation profiles also entered shared merge tangents at
different heights. The upper arterial's foundation then overwrote the final
Y=88 ramp surface and left a two-block step into the Y=89 deck.

## Decision

Grade feasibility is evaluated per design against the shortest compiled
cross-deck run available before the destination merge tangent. A direct right
turn is the shortest required movement in every complete three- and four-way
layout, so its first tangent plus quarter-circle arc is the conservative bound.
Sites remain rejected when that real route cannot satisfy the road standard.

Turning routes use a shared front-loaded elevation profile: they transition at
the maximum accepted grade, reach the destination elevation before the final
merge tangent, and remain level through that tangent. Routes sharing an inbound
or outbound lane therefore share both planar and vertical geometry while they
overlap.

Ramp surfaces take precedence over arterial cells in the same block column when
their separation is inside the seven-block vehicle-clearance envelope. This
carves a driveable ramp lane instead of letting the arterial foundation replace
it. Properly separated stacked surfaces remain unchanged.

A read-only saved-world checker performs a block-level path search between the
two decks and is the end-to-end regression signal.

## Consequences

The reported level-0/level-3 crossing now receives calculated interchange
geometry and has a continuous driveable path between Y=71 and Y=89. Compact
designs whose pre-merge run is genuinely too short remain unavailable, and
larger level differences can still be rejected rather than violating grade or
clearance limits. Existing chunks are not rewritten; validation requires fresh
chunk generation.

This decision amends the grade-feasibility assumptions in ADR-0007, ADR-0009,
ADR-0011, ADR-0012, and ADR-0013 without changing their deterministic ownership
or native-road handoff rules.

# ADR-0014: Compile validated custom movement blueprints

Status: Accepted

## Context

Format 1 datapacks can change interchange selection metadata but still receive
the hard-coded layout for their named family. Pack authors need control over the
actual movement layout. Accepting arbitrary block coordinates or unconstrained
splines would let a resource miss native-road endpoints, violate curve and grade
limits, or produce chunk-order-dependent geometry.

Three-way crossings add another constraint: a reusable declaration cannot assume
which cardinal approach is absent in the surveyed world.

## Decision

Format 2 adds one immutable, complete movement blueprint to an interchange
design. Each canonical directed movement declares a route form (`mainline`,
`direct`, or `loop`), control, width, and structural tier. Four-way declarations
contain all twelve legal movements. Three-way declarations use west, east, and
south as their six-movement canonical orientation; the factory rotates the whole
blueprint to the surveyed missing approach.

Declarations do not contain coordinates. At generation time the layout factory
binds each movement to fixed right-hand-traffic native ports and compiles its
route through the existing deterministic straight/circular path and half-block
grade builders. Mainlines join opposite ports, direct turns use a 90-degree arc,
and left-turn loops use a 270-degree arc.

Parsing rejects unknown nested fields. Domain validation rejects duplicate,
missing, unavailable, contradictory, undersized, and metadata-inconsistent
movements before the atomic repository snapshot is published. Movement lists are
stored in canonical order, so JSON array order cannot affect equality, cache
fingerprints, or generation. Format 1 remains supported and continues to use the
built-in family templates.

The design fingerprint prefix advances to version 2 and includes the immutable
blueprint through `InterchangeDesign` equality and representation.

## Consequences

Datapacks can define genuine custom interchange topology, traffic controls,
route widths, and structural tiers while preserving exact road handoff and legal
driving geometry. The format intentionally cannot express arbitrary artistic
curves or block painting; future geometry primitives must be added as validated
compiler operations. Existing datapacks and all eight built-in layouts remain
compatible.

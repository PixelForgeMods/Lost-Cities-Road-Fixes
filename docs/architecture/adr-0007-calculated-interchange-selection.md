# ADR-0007: Select interchanges by calculated site suitability

Status: Accepted

## Context

Crossing Lost Cities highways can differ in elevation, available approach length, surrounding footprint, and expected traffic. Picking an interchange family randomly can produce an impossible grade, a structure that exceeds its site, or a low-capacity junction on a major route.

The reference set contains eight distinct families: trumpet, three-way directional, SPUI, partial cloverleaf, single-quadrant, diamond, cloverleaf, and stack.

## Decision

Every design declares its junction form, radius, occupied quadrants, approach length, structure levels, loop-ramp requirement, free-flow behavior, capacity, and relative construction complexity.

Selection has two stages:

1. Reject designs that cannot satisfy the site's form, driveable half-block grade, deck clearance, footprint, radius, approach length, capacity, structure-level limit, loop-ramp policy, or free-flow requirement.
2. Score feasible designs by excess capacity and unused site resources. The lowest score is the closest fit. A stable seed-derived key resolves equal scores without making generation order-dependent.

Evaluations retain every score and rejection reason in catalogue order. If no design is feasible, the selector returns no interchange rather than weakening a safety constraint.

## Consequences

All eight requested families are first-class candidates and can be diagnosed independently. Compact junctions favor compact designs, while large high-demand free-flow sites favor cloverleaf or stack designs. Datapack designs can enter the same selector once their declarations are loaded and validated.

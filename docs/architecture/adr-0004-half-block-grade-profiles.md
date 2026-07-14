# ADR-0004: Integer half-block grade profiles

Status: Accepted

## Context

Lost Cities road levels are separated by six full blocks, while stacked interchange decks prefer ten full blocks of separation. Connecting either with a sudden block step is not driveable. Floating-point interpolation can also round differently at chunk boundaries.

## Decision

Elevation profiles contain one integer half-block sample for every horizontal block. The planner distributes half-steps deterministically and permits at most one half-step per eight blocks. It computes feasibility before producing samples:

`minimum run = half-block elevation difference × 8`

With the default standard, a six-block level difference therefore needs 96 blocks and a ten-block deck separation needs 160 blocks. Requests with less space return an infeasible result and the required distance; they never return a truncated ramp.

Ascending and descending requests traverse the same deterministic profile in opposite directions. Flat profiles preserve their exact half-block elevation.

## Consequences

Profiles map directly to full blocks and slabs and do not accumulate numeric drift. Topology and interchange selection must reserve enough approach length before accepting a height-changing connection. Infeasible sites need a different interchange, a longer route, or no connection.

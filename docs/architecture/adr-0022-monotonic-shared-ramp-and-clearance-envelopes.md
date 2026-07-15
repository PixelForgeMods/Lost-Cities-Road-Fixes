# ADR-0022: Use monotonic shared ramps and arched clearance envelopes

Status: Accepted

## Context

Exact-pack inspection of version 1.1.0 found that assigning stack connectors to
alternating tiers above and below both mainlines produced repeated climbs and
dips. Independently planned turns could occupy the same alignment at different
heights, and a ramp could meet its collector one half-block away from the
collector's locally graded surface. Widening a lane without widening its
lateral offset also caused adjacent surfaces to overlap.

The late road writer cleared one seven-block rectangular column per road cell.
That left low terrain fragments over roads, produced square low tunnels, and
did not reconcile unrelated native intersections against the final interchange
vehicle envelope.

## Decision

Built-in ramp and collector surfaces are 10 blocks wide. The collector center
is 21 blocks from the mainline center and a branch shifts a full 10 blocks away
before grading, so adjacent lanes meet without occupying one another. Stack
turns using the same approach share one terminal and one physical collector
trunk before they fork; arrivals use the inverse shared merge.

Every turning elevation profile is monotonic between its exact local collector
heights. A 40-block level zone surrounds outer fork and merge stations. At a
stack crossing, the terminal moves inward until the native-to-planned approach
provides the seven-block vehicle-clearance minimum. Descending ramps complete
their one grade before crossing a higher source road; ascending ramps retain
the lower source level through the crossover and grade afterward. Forced tiers
above or below both endpoints are not generated.

Full cloverleaf loops are the exception to the shared outer terminal rule. A
loop exits after the mainline crossing and rejoins before the next crossing,
using local collector stations in its own quadrant. Four grade-derived inner
loops remain separate from the four outer right-turn ramps. Inner collector
grade locks remove half-block lips at those local terminals.

The runtime gives selected interchange surfaces precedence over incompatible
native intersections within seven blocks. Equal-elevation cells still compose
as an at-grade intersection. Terrain clearing uses a shoulder-derived arch from
8 blocks at an edge to 12 blocks over travel lanes, capped below any planned
upper deck in the same column.

The larger level zones raise compact built-in approach minima to 192 blocks and
the three-way directional minimum to 208 blocks. Cloverleaf and stack remain at
320 and 512 blocks respectively.

## Consequences

Ramps no longer reverse vertical direction, dip at terminals, or stack duplicate
branches on one alignment. Forks, merges, and crossings are driveable at the
same rendered half-block elevation. Tunnel cuts are taller and visibly arched,
and incompatible city intersections cannot overwrite an interchange. The
geometry applies only to newly generated chunks.

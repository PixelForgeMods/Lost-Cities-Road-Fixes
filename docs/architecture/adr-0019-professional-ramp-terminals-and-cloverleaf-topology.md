# ADR-0019: Model professional ramp terminals and full cloverleaf topology

Status: Accepted

## Context

The first full-cloverleaf template placed two turning movements on the same
terminal and let ramps begin in the through carriageway. Its four visible loops
did not make the complete interchange in the client's reference: a full
cloverleaf also needs four separate outer right-turn ramps, speed-change space,
and controlled merge/diverge relationships. Some generated ramps also crossed
one another at the same elevation.

FHWA identifies a full cloverleaf as four loops plus four outer connections.
WSDOT and Caltrans require acceleration/deceleration facilities, lane balance,
and continuous auxiliary or collector-distributor treatment for close loop
weaves. The source translation and test oracles are recorded in
[the research note](../research/2026-07-professional-interchange-geometry.md).

## Decision

Every built-in interchange adds one continuous eight-block auxiliary lane in
each available mainline direction. The lane grows from the outer through edge
with a 32-block reverse-curve taper, remains full width through the interchange,
and returns with the inverse taper. Turning ramps use terminals on this lane,
shift outward before their first controlling curve, and merge back only after
their final controlling curve.

Two turning movements using the same approach receive terminals at least 16
blocks apart. Same-direction overlap is legal only on the declared auxiliary
lane; positive-length accidental centerline sharing is rejected by tests.
Interior encounters must be either a declared merge/diverge/weave or separated
by at least the vehicle-clearance standard.

The full cloverleaf contains exactly four 270-degree loop ramps and four
distinct 90-degree outer ramps. Loop radius grows when needed for grade, while
the outer ramp must remain at least six loop radii away under the project's
conservative separation model. A site that cannot fit the complete topology is
rejected atomically. The built-in cloverleaf consequently advertises a
224-block radius and 320-block approach. Stack turns receive a level central
structure tier above or below both mainlines and are rejected if either grade
cannot reach that tier.

Built-in minimum approaches include terminal tapers and are chunk-aligned:
160 blocks for trumpet, SPUI, partial cloverleaf, single-quadrant, and diamond;
176 for three-way directional; 320 for cloverleaf; and 512 for stack.

Format-2 custom movement blueprints keep their exact native-port geometry for
backward compatibility. Format-1 family replacements use these built-in
terminal rules.

## Consequences

Built-in ramps no longer appear from the middle of a through lane, entrance and
exit identities are not collapsed, and the full cloverleaf matches the required
four-loop/four-outer-ramp topology. The footprint is intentionally much larger,
so compact sites select another family instead of clipping a cloverleaf. Only
newly generated chunks receive the revised geometry.

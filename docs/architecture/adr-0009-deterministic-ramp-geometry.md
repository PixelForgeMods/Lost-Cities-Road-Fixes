# ADR-0009: Build ramps from sampled straight and circular primitives

Status: Accepted

## Context

Interchange families need reusable geometry that can cross chunk boundaries, climb between Lost Cities highway levels, and remain deterministic under parallel generation. Free-form block painting cannot prove a minimum curve radius or driveable grade before modifying a chunk.

## Decision

A ramp recipe starts from an exact planar pose and composes positive-length straight segments with left or right circular arcs. Arc radii are validated against the 24-block ramp standard. `StrictMath` computes poses, and the completed route is sampled at one-block stations plus its exact endpoint.

One linear half-block elevation profile spans the route's cumulative horizontal length. Construction fails unless that length satisfies the one-half-block-per-eight-block grade limit. The exact start and end elevations are retained.

The chunk rasterizer evaluates block centers against sampled centerline chords, keeps an exact declared width, and keys coverage by X, Z, and half-block elevation. Even-width routes use integer center coordinates, placing the centerline between their middle two block columns. Each invocation emits only cells inside its target chunk.

## Consequences

Curves and grades are validated before block placement. Adjacent chunks independently derive matching seam cells, while stacked routes can occupy the same X/Z coordinates without being flattened. One-block chord sampling introduces less than one-hundredth-block radial error at the minimum radius and avoids generation-order state.

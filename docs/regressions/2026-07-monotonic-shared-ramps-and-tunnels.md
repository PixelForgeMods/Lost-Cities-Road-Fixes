# Monotonic shared-ramp and tunnel regression

## Scrum stories

### US-27 — Smooth and widen interchange ramps

As a vehicle driver, I need a ramp to follow one continuous grade and meet its
collector at the same height so I never encounter a dip, step, or fall.

Acceptance criteria:

- built-in ramp and collector surfaces are 10 blocks wide;
- every stack turn remains between its endpoint elevations and never reverses
  vertical direction;
- exact projected fork and merge stations have identical half-block elevation;
- a stack ramp never enters either arterial's seven-block vehicle envelope.

### US-28 — Clear tunnels and incompatible intersections

As a vehicle driver, I need road openings and intersections to preserve the
complete drivable envelope.

Acceptance criteria:

- tunnel clearance rises from 8 shoulder blocks to a 12-block crown;
- low terrain/floating obstructions inside that envelope are removed;
- a planned upper road deck is not erased by a lower road's tunnel clearance;
- same-level intersections compose, while unsafe nearby native decks are
  removed from the interchange footprint.

### US-29 — Share and safely separate branch ramps

As a vehicle driver, I need two destinations to use one collector before a
clear fork, and merging branches to become one level collector afterward.

Acceptance criteria:

- stack turns from one approach share their terminal and first 24 route blocks;
- stack arrivals share their final 24 route blocks;
- adjacent 10-block lanes separate by a full lane width;
- every physical stack overlap is either one joined surface or has at least
  seven blocks of clearance;
- full-cloverleaf loops remain local to their quadrants and do not cross outer
  ramps or unrelated loops at grade.

## Automated evidence

The regression suite covers the reported six-block native to ten-block planned
deck transition, exact centerline projection at terminals, ramp/arterial and
ramp/ramp clearance, cloverleaf topology, protected native intersections, and
the arched world-writing envelope. Unit tests and NeoForge GameTests must pass
before exact-pack validation and release packaging.

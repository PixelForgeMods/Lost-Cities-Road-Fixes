# Changelog

All notable changes to Lost Cities: Road Fixes are documented here.

## 1.2.0+mc1.21.1 — 2026-07-15

### Calculated selection and corridor coordination

- Replaces seed buckets with measured demand, arm length, deck gap, open
  quadrants, and deduplicated Lost Cities building footprints.
- Compiles every candidate at its shortest safe chunk-aligned approach before
  scoring it, prioritizing fewer displaced buildings and the best operational
  fit. Low-gap compact sites prefer diamond/SPUI-class designs.
- Reserves complete compiled approach corridors, not only nominal cores.
  Identical flat shared arterials may coexist; mismatched overlapping grades
  deterministically suppress the lower-priority interchange.
- Makes all selection and conflict tie-breaking independent of the world seed.

### Physical geometry safety

- Restores a true four-tier stack with monotonic endpoint-to-core profiles,
  shared departure/arrival trunks, and seven-block ramp/ramp and ramp/mainline
  clearance. A stack is rejected unless all four physical tiers fit.
- Uses the shortest legal native-to-planned arterial transition and then holds
  the road level through the core, eliminating stretched approach ramps.
- Makes format-2 `structure_level` values physical ramp tiers. Site-dependent
  geometry that cannot reach its tier or maintain clearance is rejected.
- Adds a final composed-surface clearance gate and writes each chunk in three
  ordered passes: clear every vehicle envelope, place every deck, then supports.

### Diagnostics

- Adds per-family selected counts to `/lostcitiesroadfixes status`.
- Adds `/lostcitiesroadfixes explain <chunkX> <chunkZ>` with selected family,
  measured site/elevations, compiled approach, displaced buildings, candidate
  rejection reasons, or the blocking crossing.
- Records the overlapping-envelope root cause and regression evidence in the
  1.2.0 architecture and regression notes.

## 1.1.1+mc1.21.1 — 2026-07-15

### Ramp geometry

- Makes every built-in ramp and collector 10 blocks wide and gives adjacent
  branches a full-width lateral separation.
- Replaces alternating above/below stack tiers with one monotonic half-block
  grade, including level fork/merge locks and exact graded terminal heights.
- Shares one collector trunk before stack forks and after stack merges instead
  of rendering duplicate ramps on top of one another.
- Keeps full-cloverleaf loops local to their quadrants, nested inside separate
  outer right-turn ramps without interior at-grade crossings.
- Moves stack terminals inward only as far as required to establish seven
  blocks of clearance before a ramp crosses either mainline.

### World compatibility

- Protects interchange geometry from incompatible native road intersections;
  same-level crossings compose normally and unsafe nearby decks are omitted.
- Replaces fixed seven-block rectangular carving with an arched tunnel envelope
  that rises from 8 blocks at the shoulders to 12 over the travel lanes.
- Preserves planned upper decks while removing low terrain and floating
  obstructions from the taller vehicle envelope.

## 1.1.0+mc1.21.1 — 2026-07-15

### Interchanges

- Rebuilds full cloverleafs as four independent loop ramps plus four independent
  outer ramps instead of collapsing turns onto shared paths.
- Adds a continuous auxiliary lane in every built-in mainline direction, with
  gradual 32-block lane-add/lane-drop tapers and staggered ramp terminals.
- Rejects at-grade interior ramp conflicts and gives stack connectors a clear
  central structure tier above or below both mainlines.
- Corrects every built-in's advertised minimum approach to include its complete
  terminal geometry.
- Diversifies long four-way sites deterministically across SPUI, partial
  cloverleaf, single-quadrant, diamond, cloverleaf, and stack families.

### Buildings

- Prevents Lost Cities from generating a building or any section of a
  multi-building where it would touch a final road/interchange surface.
- Reserves one complete chunk of clearance around the full road footprint and
  reports suppressed building chunks in operator diagnostics.

### Validation

- Adds source-backed topology, terminal, taper, grade-separation, selection,
  and building-footprint regressions.
- Revalidates the client's exact seed and modpack from empty region files; both
  level-0/level-3 crossings at `(-80, 96)` and `(-80, 104)` are connected.

## 1.0.1+mc1.21.1 — 2026-07-15

### Fixed

- Selects interchanges for non-adjacent Lost Cities highway levels when the
  compiled turning route—not merely the straight approach—has enough length for
  the required vehicle-safe grade.
- Makes converging ramps reach their destination deck before the shared merge
  tangent, preventing independently interpolated routes from meeting at
  different elevations.
- Carves a ramp lane through arterial cells inside the seven-block clearance
  envelope, preventing an upper deck foundation from overwriting the final ramp
  slab.
- Adds a saved-world connectivity checker and regression coverage for the
  reported level-0/level-3 crossing at chunk `(-80, 104)`.

## 1.0.0+mc1.21.1 — 2026-07-15

Initial NeoForge 1.21.1 release.

### Roads

- Replaces Lost Cities highway placement after explosion/floating-block cleanup,
  preventing the reported literal missing-chunk gap.
- Plans deterministic regional continuity, fills bounded one-to-four-chunk
  interruptions, and preserves stable results across generation order/restarts.
- Reconciles road elevations with bounded, vehicle-friendly grades and renders
  chunk-local 32-block road surfaces, markings, shoulders, medians, and supports.

### Interchanges

- Detects stacked road crossings and calculates complete ramp geometry for
  trumpet, three-way directional, SPUI, partial cloverleaf, single quadrant,
  diamond, cloverleaf, and stack designs.
- Selects designs deterministically from topology, clearance, footprint, and
  traffic demand; dense crossings reserve non-overlapping cores consistently.
- Supports format-2 datapack interchange definitions with custom movement
  geometry, controls, widths, and structural tiers.

### Pack author and operator support

- Supports atomic datapack road themes, block-state validation, and configurable
  landscape-aware supports.
- Adds bounded server configuration, theme fallback, diagnostics, cache controls,
  and permission-level-2 operator commands.
- Requires and binary-verifies Lost Cities `1.21-8.3.10` before generation.

### Validation

- Reproduced and fixed seed `-6377442428365110436`, chunk `(-64, -139)`, in the
  client's exact 173-jar NeoForge `21.1.235` environment.
- Includes unit tests and 10 NeoForge GameTests covering generation hooks,
  continuity, themes, supports, custom geometry, and dense conflicts.

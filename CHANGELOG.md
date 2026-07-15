# Changelog

All notable changes to Lost Cities: Road Fixes are documented here.

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

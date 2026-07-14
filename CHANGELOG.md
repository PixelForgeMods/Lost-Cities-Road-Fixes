# Changelog

All notable changes to Lost Cities: Road Fixes are documented here.

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

# Reported road gap: seed `-6377442428365110436`

## Observation

- Dimension: `minecraft:overworld`
- Visible gap chunk: `(-64, -139)`
- Lost Cities debug output: city level `0`, native Z-highway level `0`
- Active profile: `customized`, with mini-explosion chance `0.03`, radius `[5, 12)`, and height `[60, 100)`

## Deterministic cause

Lost Cities seeds each mini-explosion candidate with the world seed plus fixed X/Z chunk salts. For adjacent source chunk `(-64, -140)`, Java's RNG produces:

- chance roll: `0.029846012592`, which is below `0.03`
- radius: `7`
- center: `(-1023, 70, -2225)`

The center is one block north of the visible chunk boundary, so the radius intersects chunk `(-64, -139)`.

After applying explosion damage, Lost Cities scans each affected horizontal layer. A layer with fewer than 16 non-air blocks causes every block above that layer to be cleared. A thin or damaged elevated highway deck is therefore vulnerable even when the deck itself is not the explosion's center.

## Repair invariant

Replacement road surfaces must be rendered after Lost Cities' explosion damage and floating-block cleanup. Merely regenerating the native highway earlier in the same pipeline leaves it vulnerable to the same deletion pass.

This incident is encoded by `ReportedGapRegressionTest` and `ReportedGapGameTests`.

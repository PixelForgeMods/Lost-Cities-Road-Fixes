# Highway roaming chunk-generation regression

## Reported failure

While the player followed a generated highway, Lost Cities repeatedly reported
`There was an error generating a chunk`. The visible result was a fragmented
city and road network even though the player had not entered or edited an
interchange.

The exact-pack log contained 411 Lost Cities chunk-generation failures and 153
late road-render failures. The first reported chunk was `(95,-27)`. A reduced
reproduction needed only one compact diamond and failed at block column
`(-68,-28)`, where two ramp surfaces were emitted at 142 and 143 half-blocks.
Their half-block difference was inside the required seven-block vehicle
clearance.

## Root cause

Lost Cities asks Road Fixes whether a building column overlaps a planned road.
That read path composes the same interchange surface later used by the renderer.
An unsafe ramp pair reached the final clearance validator, which threw an
`IllegalArgumentException`. The exception escaped through the building query and
aborted Lost Cities' entire chunk. Ordinary highway travel merely caused the
nearby chunks—and therefore the interchange footprint—to generate.

The ramp rasterizer also grouped close elevations transitively. Given lower,
transition, and upper candidates, the transition could join both groups even
when the lower and upper decks were safely separated. Resolving that chain to
one candidate could delete a legitimate road deck.

## Scrum story US-34 — Make interchange composition fail-safe

- Resolve one connected pavement surface around the nearest route in each
  block column while retaining every independently clear deck.
- Include complete arterial footprints and shared terminal windows when
  profiling built-in ramp grades.
- Use one lower signalized core for SPUI turning movements.
- Preserve both graded through highways and omit all ramps if a final
  interchange overlay is still rejected.
- Apply the same guarded composition to Lost Cities building suppression and
  late road rendering.
- Count fallbacks in operator diagnostics and log the first rejected overlay.
- Keep analysis bounded with a spatial segment index and a thread-safe
  candidate-compilation cache.

## Automated evidence

`compactDiamondSharedTurnsCannotAbortHighwayChunkComposition` reproduces the
reported close-elevation diamond and verifies that composing the affected chunk
does not throw. `straightThroughFallbackKeepsBothSafelyGradedMainlines` verifies
the atomic fallback contract.

The built-in catalogue test selects each of the eight families, composes every
chunk in its compiled envelope, and checks that each route remains represented
by either its own deck or its connected-surface clearance envelope. A dedicated
three-route regression proves that an intermediate elevation cannot
transitively remove a clear lower or upper deck. The complete JUnit suite must
pass before release packaging.

Only newly generated chunks use the fix. Previously failed or saved chunks must
be regenerated from backup or tested in a fresh world.

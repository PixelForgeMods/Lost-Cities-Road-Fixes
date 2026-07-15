# Lost Cities: Road Fixes

Lost Cities: Road Fixes is a server-side-capable NeoForge 1.21.1 addon that replaces Lost Cities highways with a deterministic road network designed for continuous vehicle travel.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.235 or newer in the 21.1 line
- Lost Cities `1.21-8.3.10` exactly
- Java 21 for development

The mod is intentionally pinned to one Lost Cities build because its compatibility layer targets world-generation internals that are not part of the public Lost Cities API.

## Installation

Back up the world, place the universal JAR in the server and client `mods`
folders, and install it before exploring or pregenerating affected terrain.
Road Fixes only changes newly generated chunks. See the
[1.1.1 release notes](docs/release/1.1.1.md) and [changelog](CHANGELOG.md).

## Generation behavior

The addon suppresses Lost Cities' native highway block placement while retaining its deterministic highway-level decisions for compatibility with buildings and railways. Replacement 32-block roads are written after Lost Cities explosion damage and floating-block cleanup, preventing the reported one-chunk bridge deletion.

Only newly generated chunks are changed. Install the addon before exploring or pregenerating the area that should use replacement roads; it does not rewrite already saved chunks.

Pack authors can add or replace calculated interchange declarations—including
complete movement geometry, controls, widths, and structural tiers—with server
datapacks. See [the custom-interchange format](docs/datapacks/interchanges.md).
Road materials and support blocks can also be replaced with
[datapack road themes](docs/datapacks/road-themes.md).
Server operators can use the bounded [server configuration and diagnostics](docs/configuration.md).

The client's exact 173-jar pack and reported seed were reproduced in an isolated
world. See the [release validation evidence](docs/validation/2026-07-exact-modpack.md).
The reported 18-block stacked crossing was also replayed from a fresh copy of
that seed; see the [stacked-interchange regression](docs/regressions/2026-07-stacked-interchange-level-gap.md).

Closely spaced crossings are coordinated before geometry is emitted. When two
safe interchange cores physically cannot coexist, a stable world-derived winner
receives the complete interchange and the other crossing remains an intact
straight flyover; partial or overlapping ramps are never generated.

Built-in interchanges add 10-block collector lanes with gradual tapers and
level fork/merge zones. Stack branches share one physical trunk and use one
monotonic grade; a full cloverleaf has four local quadrant loops nested inside
four independent outer ramps. Deterministic site envelopes vary long four-way
crossings across SPUI, partial cloverleaf, single-quadrant, diamond, cloverleaf,
and stack families.

Road terrain clearance forms a taller arched envelope—8 blocks at shoulders and
up to 12 over travel lanes—while planned bridge decks remain protected. Native
intersections at the same level compose with an interchange; incompatible decks
inside its vehicle envelope are removed instead of clipping through a ramp.

Before Lost Cities emits a building, the addon reserves the final composed road
surface plus a one-chunk clearance band. A single building or complete
multi-building is omitted whenever that reserved area would be touched.
Implementation and exact-pack evidence are in the
[professional-interchange regression](docs/regressions/2026-07-professional-cloverleaf-and-building-clearance.md).

## Development

```text
gradlew.bat test build
gradlew.bat runGameTestServer
```

The project uses the official NeoForge ModDevGradle template and resolves Lost Cities from Modrinth Maven. Features are delivered as independently tested Scrum increments.

## License

MIT © 2026 Austizz_tds

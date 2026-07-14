# Lost Cities: Road Fixes

Lost Cities: Road Fixes is a server-side-capable NeoForge 1.21.1 addon that replaces Lost Cities highways with a deterministic road network designed for continuous vehicle travel.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.235 or newer in the 21.1 line
- Lost Cities `1.21-8.3.10` exactly
- Java 21 for development

The mod is intentionally pinned to one Lost Cities build because its compatibility layer targets world-generation internals that are not part of the public Lost Cities API.

## Generation behavior

The addon suppresses Lost Cities' native highway block placement while retaining its deterministic highway-level decisions for compatibility with buildings and railways. Replacement 32-block roads are written after Lost Cities explosion damage and floating-block cleanup, preventing the reported one-chunk bridge deletion.

Only newly generated chunks are changed. Install the addon before exploring or pregenerating the area that should use replacement roads; it does not rewrite already saved chunks.

Pack authors can add or replace calculated interchange declarations with server datapacks. See [the custom-interchange format](docs/datapacks/interchanges.md).

## Development

```text
gradlew.bat test build
gradlew.bat runGameTestServer
```

The project uses the official NeoForge ModDevGradle template and resolves Lost Cities from Modrinth Maven. Features are delivered as independently tested Scrum increments.

## License

MIT © 2026 Austizz_tds

# Lost Cities: Road Fixes

Lost Cities: Road Fixes is a server-side-capable NeoForge 1.21.1 addon that replaces Lost Cities highways with a deterministic road network designed for continuous vehicle travel.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.235 or newer in the 21.1 line
- Lost Cities `1.21-8.3.10` exactly
- Java 21 for development

The mod is intentionally pinned to one Lost Cities build because its compatibility layer targets world-generation internals that are not part of the public Lost Cities API.

## Development

```text
gradlew.bat test build
gradlew.bat runGameTestServer
```

The project uses the official NeoForge ModDevGradle template and resolves Lost Cities from Modrinth Maven. Generated roads, interchange manifests, and authoring documentation will be added story by story.

## License

MIT © 2026 Austizz_tds

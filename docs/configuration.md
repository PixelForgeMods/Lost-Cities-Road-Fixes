# Server configuration and diagnostics

Lost Cities: Road Fixes registers a NeoForge server configuration named:

```text
lostcitiesroadfixes-server.toml
```

NeoForge stores server configs with the world/server configuration. The file is
created with these defaults:

```toml
[roads]
maximumGapChunks = 4
maximumCachedRegions = 512
activeRoadTheme = "lostcitiesroadfixes:default"
logFirstInterchangeSelection = true
```

## Settings

### `maximumGapChunks`

Largest collinear run of absent Lost Cities highway chunks repaired between two
observed same-level endpoints. The accepted range is 1 through 4; the default is
the fully verified four-chunk reach. At least the reported literal one-chunk gap
is always repaired. This value participates in regional plan identity, so plans
made with different values cannot be mixed.

### `maximumCachedRegions`

Maximum entries retained independently in the repaired-road and interchange
regional caches. The range is 64 through 4096 and the default is 512. When a
cache crosses the limit, its oldest inserted regions are evicted. A later request
recomputes an identical deterministic plan; eviction cannot change road geometry.

### `activeRoadTheme`

Namespaced ID of a loaded [datapack road theme](datapacks/road-themes.md).
The default is `lostcitiesroadfixes:default`. If the requested ID is not loaded,
new chunks safely use that built-in default and `/lostcitiesroadfixes status`
reports `fallback=true` plus both IDs.

Each chunk captures one resolved compiled theme before writing, so a reload
cannot mix palettes within that chunk. A datapack-only theme reload leaves
geometry caches intact; reloading the server TOML conservatively replaces both
cache generations along with every server-config reload.

### `logFirstInterchangeSelection`

Controls the informative first-selected-interchange log message. It does not
affect selection or generation.

## Locked safety behavior

The server config deliberately has no switches for disabling continuity repair,
calculated interchanges, native-road handoff, grade limits, curve-radius limits,
vehicle clearance, or road width. Those are driveability invariants rather than
operator preferences. Lost Cities' own `highwaySupports` profile option remains
the authority for deep road supports.

Loading or reloading this server config atomically replaces both plan-cache
generations. In-flight work may finish for its original chunk but cannot populate
the new cache generation.

## Operator commands

Both commands require Minecraft permission level 2. `lcroadfixes` is a guarded
alias for `lostcitiesroadfixes`.

```text
/lostcitiesroadfixes status
/lostcitiesroadfixes clear_caches
```

`status` reports:

- Lost Cities binary compatibility state;
- native-suppression and late-render hook counts;
- planned, selected, rejected, and rendered interchange counts;
- current road/interchange cache sizes;
- loaded design/theme counts;
- configured and resolved theme IDs and fallback state;
- active safe configuration bounds.

`clear_caches` atomically replaces both regional cache generations. It does not
modify generated chunks or reset counters; later requests deterministically
rebuild the needed plans.

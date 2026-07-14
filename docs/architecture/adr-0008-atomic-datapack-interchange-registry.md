# ADR-0008: Load interchange declarations as atomic datapack snapshots

Status: Accepted

## Context

Lost Cities pack authors already customize roads through namespaced data assets. Interchange declarations need the same server-managed lifecycle without exposing partially reloaded data to concurrent world-generation workers.

## Decision

Interchange JSON files live under `lostcities_road_fixes/interchanges` in server datapacks. The resource location is the design identity. A strict, versioned parser validates every selector property and rejects unknown fields so misspellings cannot silently change generation.

The repository merges a complete custom map over immutable Java fallbacks, sorts the result by family and namespaced ID, and publishes one immutable list through an atomic reference. A custom declaration with a built-in ID replaces that fallback. The NeoForge server reload listener parses every winning resource before publishing any of them.

The core domain uses `InterchangeDesignId` rather than Minecraft's `ResourceLocation`; conversion occurs in the reload adapter. This keeps the planner independently unit-testable.

## Consequences

`/reload` either publishes the entire validated design set or leaves the previous snapshot intact. Pack priority and namespacing behave like other Minecraft data resources. Any reload that changes designs must invalidate regional road plans before this repository is consumed by world generation; that integration is part of the interchange-planning increment.

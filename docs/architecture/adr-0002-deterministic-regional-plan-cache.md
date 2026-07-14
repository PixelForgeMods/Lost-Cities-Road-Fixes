# ADR-0002: Deterministic regional plan identity and cache

Status: Accepted

## Context

Minecraft may request neighboring chunks in any order and may run world-generation work concurrently. A planner that consumes shared random state or caches only by chunk can produce mismatched road edges. Datapack reloads also need to prevent old template decisions from leaking into new plans.

## Decision

Every regional plan is identified by:

- world seed;
- dimension identifier;
- 64 by 64 owning planning region; and
- a rules fingerprint covering configuration and loaded datapack content.

`PlanningSeed` derives a stable 64-bit seed from those values using fixed UTF-8 hashing and integer mixing. It never consumes process-global randomness.

`RegionalPlanCache` computes one value atomically per identity. Invalidation swaps the complete active map generation. Work already running against the old map may return to its original caller, but it cannot become visible through the new cache generation.

## Consequences

Chunk request order and worker scheduling cannot affect a regional result. Worlds, dimensions, and rulesets are isolated. Reloading deliberately discards all cached plans; the first later query pays the planning cost again.

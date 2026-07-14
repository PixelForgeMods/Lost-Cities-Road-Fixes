# ADR-0016: Bound operational caches and expose immutable diagnostics

Status: Accepted

## Context

Regional plans are deterministic but previously remained cached for the full
server lifetime. Continuous long-distance driving can visit an unbounded number
of 64×64 planning regions. Operators also had counters in Java but no supported
way to inspect them, clear caches, choose a loaded theme, or tune the verified
short-gap repair reach.

Configuration must not become a way to disable the continuity, native-port,
grade, radius, clearance, or width contracts that make roads driveable.

## Decision

A NeoForge server config produces one immutable `RoadOperationalSettings`
snapshot per chunk operation. The exposed values are a 1–4 chunk repair reach, a
64–4096 entry per-cache bound, a namespaced active theme ID, and an informational
selection-log toggle. Only repair reach enters the geometry fingerprint. Config
load/reload swaps both plan-cache generations.

`RegionalPlanCache` retains compute-once concurrency and generation swapping. It
records completed insertions and evicts the oldest entries under a small
eviction lock after crossing the caller's bound. Eviction is operational only:
the plan key and deterministic planner reproduce the same result.

Theme resolution captures one compiled snapshot and falls back to the built-in
default if the configured ID is unavailable. An immutable diagnostics record
captures counters, cache sizes, loaded resources, resolution state, and settings,
then formats stable lines. Permission-level-2 status and cache-clear commands
are registered under `lostcitiesroadfixes`, with an equally guarded
`lcroadfixes` alias.

## Consequences

Planner memory is bounded during long journeys, and operators can inspect or
reset ephemeral state without touching world data. Configuration changes cannot
mix incompatible cached plans. Safety-critical geometry remains non-configurable;
eviction and diagnostics do not affect deterministic generation.

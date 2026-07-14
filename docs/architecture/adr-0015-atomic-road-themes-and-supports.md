# ADR-0015: Compile road themes atomically and honor landscape support policy

Status: Accepted

## Context

Road rasterizers already emit semantic roles independently of Minecraft blocks,
but the writer maps them through one hard-coded palette. Pack authors need the
same datapack workflow used by Lost Cities assets. Theme reload also runs while
world-generation workers may be active, so no worker may observe a partially
compiled block map.

The replacement writer previously placed only one foundation block under every
surface and ignored Lost Cities' `highwaySupports` profile setting. Native Lost
Cities highways use sparse supports, descend through air or liquid, stop at
terrain, and cap the search at 40 blocks.

## Decision

Road-theme JSON remains a Minecraft-independent declaration of semantic block
state strings. Reload parses all winning resources, merges them over the Java
fallbacks, and compiles every declaration against the live block registry before
publishing one immutable compiled snapshot. The active ID is
`lostcitiesroadfixes:default`; datapacks replace it through normal resource-pack
priority. Unknown or unsafe states abort publication, leaving the previous
compiled snapshot active. Theme publication does not invalidate geometry caches.

Each chunk render captures the active compiled theme exactly once. The writer
keeps its continuous one-block foundation and seven-block headroom. A pure
support planner collapses stacked cells to the lowest elevation per column and
chooses at most two deterministic anchors nearest opposite chunk corners. When
the Lost Cities profile enables highway supports, the writer descends from those
anchors through air or collision-free liquid until terrain, build minimum, or
the theme depth limit. Disabled profile supports produce no columns.

## Consequences

Pack authors can replace every road material, including half-block states and
structural blocks, without changing geometry. World generation sees either the
old complete theme or the new complete theme. Supports now match Lost Cities'
profile intent across ordinary, ocean, void, floating, sphere, and cavern
terrain while retaining a hard work bound per chunk.

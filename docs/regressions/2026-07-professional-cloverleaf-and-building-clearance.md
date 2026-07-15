# Professional cloverleaf and building-clearance regression

## Reported failures

The client's exact pack showed three independent problems:

- a purported full cloverleaf merged entrance and exit movements into shared
  paths and started ramps inside the through carriageway;
- generated crossings overwhelmingly used the full-cloverleaf family;
- replacement ramps could cut directly through Lost Cities buildings.

The reference full cloverleaf requires four loop ramps and four separate outer
ramps. Official highway guidance also treats speed-change lanes, tapers, and
auxiliary/collector-distributor continuity as part of the ramp terminal. See
[the primary-source research](../research/2026-07-professional-interchange-geometry.md).

## Reproduction signals

The initial geometry regressions failed because both turns from an approach
shared one terminal, some interior routes shared an undeclared lane, all 512
sampled long-road seeds selected `CLOVERLEAF`, and no pre-building integration
hook existed.

A second minimum-envelope regression found six catalogue entries whose new
terminal geometry could not compile at their advertised approach length. Their
safe chunk-aligned minima were corrected before release.

Fresh exact-pack validation also exposed Lost Cities' special
`MultiPos(-1,-1,1,1)` representation for single buildings. Version 1.1.0
normalizes that sentinel; it is covered by the same policy test as multi-chunk
buildings.

## Fixed invariants

- Full cloverleaf: 4 mainline movements, 4 unique outer ramps, and 4 unique
  loops; no two turning routes share an undeclared centerline.
- Terminals: distinct entrance and exit stations on a declared auxiliary lane,
  with a gradual 32-block lane-add/lane-drop transition.
- Crossings: every close interior encounter is a declared same-direction
  auxiliary weave or has at least seven blocks of vertical clearance.
- Selection: SPUI, partial cloverleaf, single quadrant, diamond, cloverleaf,
  and stack all appear and compile across the deterministic 512-seed sample.
- Buildings: the complete multi-building plus a one-chunk clearance band is
  suppressed if any final composed road surface occupies that area.

## Exact-pack verification

Validation used seed `-6377442428365110436`, NeoForge `21.1.235`, Lost Cities
`1.21-8.3.10`, and the isolated client modpack. All old region, POI, and entity
files were removed before generation. The client loaded version
`1.1.0+mc1.21.1`, joined successfully, rendered replacement roads, and selected
a stack as its first calculated interchange rather than another cloverleaf.
The final runtime diagnostics after nearby generation reported:

```text
hooks: nativeSuppressions=744, buildingChunkSuppressions=222, lateRenders=1509
interchanges: regions=12, selected=3, rejected=1, conflicted=3, renderedChunks=1109
```

The read-only saved-world checker found driveable lower-to-upper paths at both
nearby level-0/level-3 crossings:

```text
crossing=(-80,96) decks=71->89 starts=4768 goals=4768 visited=194598
CONNECTED

crossing=(-80,104) decks=71->89 starts=3642 goals=5154 visited=76073
CONNECTED
```

No Lost Cities Road Fixes exception occurred after the `MultiPos` correction.
Aerial and oblique inspection showed the widened approaches, continuous taper
surfaces, separated ramp tiers, and cleared building footprint. Existing chunks
still retain their old interchange and building geometry.

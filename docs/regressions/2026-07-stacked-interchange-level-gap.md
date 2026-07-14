# Stacked-interchange level-gap regression

## Reported case

- Seed: `-6377442428365110436`
- Dimension: `minecraft:overworld`
- Crossing chunk: `(-80, 104)`
- Approximate block center: `(-1272, 1672)`
- Lower road surface: Y=71 (Lost Cities highway level 0)
- Upper road surface: Y=89 (Lost Cities highway level 3)
- Environment: NeoForge `21.1.235`, Lost Cities `1.21-8.3.10`, and the
  client's isolated pack; incompatible `xmmp 0.2.2` was disabled because it
  crashes against Xaero World Map `1.43.0` before sustained world validation

The screenshots showed intact straight decks crossing at different elevations
but no driveable transfer between them.

## Reproduction signal

The read-only checker searches exposed Road Fixes surface blocks and permits at
most one block of vertical change per horizontal step:

```text
python3 scripts/check_stacked_interchange.py \
  "/path/to/world" \
  --chunk-x -80 --chunk-z 104 \
  --lower-y 71 --upper-y 89 \
  --radius 18 --deck-radius 0
```

Before the fix, it consistently reported:

```text
surface_levels={..., 87: 840, 89: 17671, ...}
DISCONNECTED
```

Y=88 was absent because the upper deck foundation replaced the final ramp
surface. A companion planner regression also showed every design rejected with
`grade requires 288 approach blocks` despite longer compiled turning routes.

## Fix verification

Version `1.0.1+mc1.21.1` was built, installed into a fresh copy of the same
world seed, and run with the exact pack. The shutdown diagnostics reported:

```text
interchanges: regions=8, selected=2, rejected=0, conflicted=1, renderedChunks=664
```

The same checker then reported Y=88 ramp surfaces and:

```text
CONNECTED
```

Unit coverage locks down the level-0/level-3 selection, compiled pre-merge run,
shared merge elevation, and vertical-clearance carving. Existing generated
chunks retain old geometry and must be regenerated or tested in a new world.

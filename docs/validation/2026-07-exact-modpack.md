# Exact modpack validation: July 2026 reported gap

## Result

The production addon passes the client's exact NeoForge 1.21.1 environment.
Native Lost Cities reproduces the reported missing chunk at `(-64, -139)` in a
fresh control world; the addon replaces it with a level, continuous road across
all seven inspected chunks.

The validated production artifact is
`lostcitiesroadfixes-1.0.0+mc1.21.1.jar`, SHA-256
`170fd00218f8cc88faa305b8a63ae64ad99767fc0a6694b4c11745b0aa40751b`.

## Exact environment

- Minecraft `1.21.1`, NeoForge `21.1.235`, Java `21.0.4+7-LTS`
- 173 client jar files; 174 with the production addon
- Lost Cities `1.21-8.3.10`, SHA-256
  `26db73013028ad724af030aa30cbd8dd62da8b951645b8d02f066c8af083a52f`
- seed `-6377442428365110436`, dimension `minecraft:overworld`
- customized default profile: ground Y 71, distance mask 7, main/secondary
  Perlin scales 50/10, factor 2, supports enabled

The complete per-file jar/config fingerprint is in
[the machine manifest](2026-07-exact-modpack-manifest.json). It contains hashes
and relative paths only; no config contents or workstation paths are committed.

## Former gap proof

Both variants were generated fresh from the same copied seed/player metadata in
an isolated game directory. The native control used all 173 client jars. The
production run added only Road Fixes.

| Chunk Z at X=-64 | Native road cells at Y=71 | Replacement road cells at Y=71 | Replacement north/south edges |
|---:|---:|---:|---:|
| -142 | 160 | 224 | 14 / 14 |
| -141 | 160 | 224 | 14 / 14 |
| -140 | 129 | 224 | 14 / 14 |
| -139 | **0** | **224** | **14 / 14** |
| -138 | 160 | 224 | 14 / 14 |
| -137 | 160 | 224 | 14 / 14 |
| -136 | 160 | 224 | 14 / 14 |

The native road is already clipped in source chunk `(-64, -140)` and completely
absent in `(-64, -139)`. Every replacement chunk has the same 224 distinctive
surface cells at Y=71, and both travel-direction edges match. No chunk in the
7-by-7 inspection window was missing.

The reusable inspector is [analyze_road_region.py](../../scripts/analyze_road_region.py).
It reads `.mca`/NBT data without modifying a world and supports custom block
filters for native or datapack road palettes.

## Runtime observations

The final full-pack run recorded:

- compatibility probe passed; no Road Fixes exception or failed replacement render
- post-cleanup replacement rendering and native-highway suppression both activated
- player joined at `(-1015.573, 87.216, -2208.196)`
- calculated `lostcitiesroadfixes:cloverleaf` selected at chunk `(-112, -192)`
- 546 native suppressions and 1,042 late-render invocations
- 8 interchange regions planned; 1 selected, 1 rejected, 0 conflict-suppressed
- road/interchange cache sizes 26/8, each below the configured bound of 512
- all dimensions saved before clean process exit

Minecraft's cold start-region timer measured 5,216 ms without the addon and
6,060 ms with the final production addon: an observed one-run delta of 844 ms
(16.18%). This is a paired operational observation on the client's machine, not
a statistical benchmark; it should not be generalized as a fixed percentage.

## Save safety and pack observations

Validation never launched against the original save. Before and after hashing
found exactly 744 files and the same aggregate SHA-256
`1f4d1d96a10b2a9b924c01dd1f7894151595cf34f177e76e4e49438f84db5a7d`;
zero files were added, removed, resized, or changed.

The original save's target terrain `.mca` files are zero-byte placeholders, so
they cannot provide persisted before-block data. The fresh no-addon control is
therefore the reproducible native baseline.

The pack emits numerous unrelated asset, recipe, and block-entity warnings. One
separate reload also hit a Create/Ponder client payload exception after joining.
The final production run entered the world and shut down cleanly, and none of
these diagnostics implicated Road Fixes.

Structured values and raw-evidence hashes are preserved in
[the machine report](2026-07-exact-modpack-report.json).

# ADR-0006: Replace native highways after Lost Cities cleanup

Status: Accepted

## Context

Lost Cities renders highways before explosion damage and then removes every block above a sparse damaged layer. Replacing a road at the same early point would reproduce the reported gap. Lost Cities has a pre-explosion event but no public post-explosion event.

## Decision

Two required, version-pinned Mixins define the integration seam:

1. `Highways.generateHighways` is canceled at its head, suppressing native highway blocks. Its X/Z highway-level functions remain untouched so buildings, streets, and railways retain their expected topology answers.
2. `LostCityTerrainFeature.generate` invokes the replacement runtime at return, after explosion damage, debris, primer flush, and `ChunkFixer` work.

The runtime builds cached regional continuity plans from the native X/Z observations, converts levels using the profile ground level plus six blocks per level, rasterizes the semantic 32-block surface, and writes only the chunk currently being generated. It clears seven replaceable blocks above each surface cell. Integer half elevations select bottom slabs.

Any runtime exception is rethrown with dimension and chunk context. The addon does not subscribe to chunk-load events and therefore never retrofits saved chunks implicitly.

## Consequences

Explosion cleanup cannot delete a replacement deck. Native asset visuals are replaced by the built-in semantic palette until datapack themes are loaded. The strict Lost Cities binary probe must include both integration targets, and every supported Lost Cities update requires an explicit compatibility review.

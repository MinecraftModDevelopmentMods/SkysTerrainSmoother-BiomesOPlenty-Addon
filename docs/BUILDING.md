# Building from source

Use the Gradle 9.2.1 wrapper and Temurin 25.0.3+9. This branch builds with
NeoGradle 7.1.38 for NeoForge 26.1.2.94. The log checks use Python 3.11 or newer.

Build the four commits in `gradle/content-dependencies.properties` with
`assemble`. Build Grass Slabs and Building Pieces first, then the Building
Pieces BOP add-on and Terrain Smoother. The same jars are used for development
and packaged games. The build checks their commits and hashes and verifies
the pinned BOP, TerraBlender and GlitchCore downloads.

By default the checkouts sit in sibling version folders. To use a different
layout, pass `-PgrassSlabsDirectory`, `-PbuildingPiecesDirectory`,
`-PbuildingPiecesBopDirectory` and `-PterrainDirectory` with their paths.

```text
gradlew check build javadoc verifyReleaseArtifacts writeReleaseChecksums
gradlew verifyEclipseProductionClasspath
```

If Python isn't on your path, pass `-PpythonExecutable=<python>`. Eclipse
launches use the processed resources and keep the tests out of ordinary play.
The Mineralogy launches have their own game folder and use the pinned
NeoForge Mineralogy and OreSpawn jars.

Use `preparePackagedIntegrationProbe` to build the disposable runtime probe.
The tools in `tools/` check native games and saved fixtures without changing
the original worlds. Use a fresh directory under `build/` for each first
load so older logs remain available.

Disposable servers use Java networking (`use-native-transport=false`). This
avoids host-specific Netty availability probes during log auditing.

The NeoGradle BOP check retains the raw logs and audits a separate copy. With
the exact pinned NeoForge and BOP dependencies, it recognises the single allay
spawn-placement warning reproduced in a BOP-only development control. Changed
messages, other entities, repeated notices and other errors still fail. The
packaged-game audit has no such exception.

The same-version upgrade checks also cover older NeoForge saves that still use palettes.
`--native-state-reference` accepts the sealed canonical contract under
`src/test/resources/fixtures`. Its flattened Forge capture was checked against the
qualified stack. The runner checks both capture hashes, matching positions and
shared state properties before comparing all 4,700 blocks. It never edits the source save.

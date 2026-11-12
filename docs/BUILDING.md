# Building from source

Use the Gradle 9.2.1 wrapper and Temurin 21.0.7+6. This branch builds with
NeoGradle 7.1.38 for NeoForge 21.1.247. The log checks use Python 3.11 or newer.

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

Preparing these runs keeps a checksum-verified copy of each optional jar in the
project cache. Rebuilding a sibling mod will not change the version used by this
profile. An explicit jar override must still match the pinned checksum.
Use the same Gradle user home in Eclipse and at the command line.

The pinned BOP build logs an allay spawn-registration error in development
games, including a control with no Sky mods. The log checks recognise that
single startup message only with the pinned vendor jars. Other errors still fail.

Use `preparePackagedIntegrationProbe` to build the disposable runtime probe.
The tools in `tools/` check native games and saved fixtures without changing
the original worlds. Use a fresh directory under `build/` for each first
load so older logs remain available.

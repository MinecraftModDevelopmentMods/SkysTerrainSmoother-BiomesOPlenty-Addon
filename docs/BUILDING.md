# Building from source

Build with the Gradle 9.6.1 wrapper and Temurin 17.0.1+12. Set
STS_JAVA17_HOME to that Java installation. Java 25 is needed by ForgeGradle's
Mavenizer tool; Gradle, compilation and Minecraft use Java 17.

Build the exact four clean source commits in gradle/content-dependencies.properties.
Run assemble for Grass Slabs and Building Pieces, then Terrain Smoother and the
Building Pieces BOP add-on. Also run the latter's bopDevelopmentJar task, which
prepares both BOP and TerraBlender for development. The
build verifies their commits, versions and main/development jar hashes. These
dependencies are checked before compilation and development runs.

The default layout uses sibling version folders. Alternative locations use
-PgrassSlabsDirectory, -PbuildingPiecesDirectory, -PterrainDirectory and
-PbuildingPiecesBopDirectory. Normal client/server launches include only the
production mod and these matching dependencies, not unit tests or runtime probes.

Run clean separately, then check build javadoc genEclipseRuns eclipse
verifyEclipseProductionClasspath verifyReleaseArtifacts writeReleaseChecksums.
Packaged checks can load the sealed 1.12.2 and 1.14.4 through 1.17.1 family
fixtures. They verify first load and reload, including saved biomes, states,
counters and sidecars. Run tools/run_packaged_checks.py --help for the fixture
and client/server options. Archive old disposable runtime evidence before
repeating a check.

Development uses mapped BOP bytes from the pinned Building Pieces BOP build,
including mapped access rules for both BOP and TerraBlender. Packaged runs use
their unchanged public jars. No dependency classes or textures are bundled in
this mod.

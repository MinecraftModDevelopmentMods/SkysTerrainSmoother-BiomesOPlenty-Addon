# Building from source

Use the Gradle 9.6.1 wrapper with Java 17.0.1+12 and Temurin 8.0.502+7 for
production. Set STS_JAVA8_HOME to the Java 8 installation.

Build the exact four clean source commits in gradle/content-dependencies.properties.
Run assemble for Grass Slabs and Building Pieces, then Terrain Smoother and the
Building Pieces BOP add-on. Also run the latter's bopDevelopmentJar task. The
build verifies their commits, versions and main/development jar hashes. These
are local qualified candidates; hosted checks remain pending their later push.

The default layout uses sibling version folders. Alternative locations use
-PgrassSlabsDirectory, -PbuildingPiecesDirectory, -PterrainDirectory and
-PbuildingPiecesBopDirectory. Normal client/server launches include only the
production mod and these matching dependencies, not unit tests or runtime probes.

Run clean separately, then check build javadoc genEclipseRuns eclipse
verifyEclipseProductionClasspath verifyReleaseArtifacts writeReleaseChecksums.
The build-only runtime checks include a genuine sealed 1.12 family upgrade and
reload. Archive old disposable runtime evidence before repeating them.

Development uses mapped BOP bytes from the pinned Building Pieces BOP build,
including its mapped access rules. Packaged runs use the unchanged public BOP
jar. No dependency classes or textures are bundled in this mod.

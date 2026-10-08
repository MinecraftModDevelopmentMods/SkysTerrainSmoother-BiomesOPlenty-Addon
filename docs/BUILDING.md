# Building from source

Use the included Gradle 9.6.1 wrapper with Java 17 and a Java 8 toolchain.
Build the matching Grass Slabs, Building Pieces and Terrain Smoother development
jars first. Their default directories follow the sibling checkout convention;
override grassSlabsDirectory, buildingPiecesDirectory and terrainDirectory if needed.

Run tests, build, verifyReleaseArtifacts and writeReleaseChecksums. Run eclipse
and genEclipseRuns for the Java 8 IDE setup. Production launches exclude tests.
Publication is disabled for this local beta.

CI builds the source commits in `gradle/content-dependencies.properties` before
building this add-on. Each dependency is pinned to a full commit, not a moving
branch. Tag checks do not publish anything. Publication remains disabled until
this mod has its own CurseForge project.

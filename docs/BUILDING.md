# Building from source

Build with the Gradle 9.6.1 wrapper and Temurin 25.0.3+9.
Set `STS_JAVA25_HOME` to that Java installation. Gradle, ForgeGradle and
Minecraft all use it.

Build the exact four clean source commits in gradle/content-dependencies.properties.
Run assemble for Grass Slabs and Building Pieces, then Terrain Smoother and the
Building Pieces BOP add-on. Also run the latter's verifyNativeBopDependencies task to check BOP, TerraBlender and GlitchCore. The
build verifies their commits, versions and main/development jar hashes. These
dependencies are checked before compilation and development runs.

The default layout uses sibling version folders. Alternative locations use
-PgrassSlabsDirectory, -PbuildingPiecesDirectory, -PterrainDirectory and
-PbuildingPiecesBopDirectory. Normal client/server launches include only the
production mod and these matching dependencies, not unit tests or runtime probes.
If the Modrinth Maven mirror is unavailable, pass the downloaded jars with
`-PbiomesOPlentyMainJar`, `-PterraBlenderMainJar` and `-PglitchCoreMainJar`.
The same checksums apply, and Eclipse generation keeps the paths for refreshes.

Run clean separately, then check build javadoc genEclipseRuns eclipse
verifyEclipseProductionClasspath verifyReleaseArtifacts writeReleaseChecksums.
Packaged checks can load the sealed 1.12.2 and 1.14.4 through 1.20.6 family
fixtures. They verify first load and reload, including saved biomes, states,
counters and sidecars. Run tools/run_packaged_checks.py --help for the fixture
and client/server options. Archive old disposable runtime evidence before
repeating a check.

Development and packaged runs use the same public BOP, TerraBlender and GlitchCore jars. No dependency classes or textures are bundled in
this mod.

# Building from source

Use the Gradle 9.6.1 wrapper with Java 17 and Temurin 8.0.502+7 for production.
Set `STS_JAVA8_HOME` to the Java 8 installation. This branch targets
Minecraft 1.10.2 with ForgeGradle 7.0.34.

The dependency versions, CurseForge file IDs and SHA-256 checksums are listed in
`gradle/content-dependencies.properties`. Gradle downloads those public main jars
and maps copies for development. You do not need to clone or build the content
mods, and Eclipse never uses their sibling working trees.

Then run:

```text
gradlew check build javadoc verifyReleaseArtifacts writeReleaseChecksums verifyEclipseProductionClasspath
```

Development dependencies use deobfuscated jars. Packaged games use the normal
main jars; dependency classes and assets are not bundled. CI uses the same
checksum-locked public jars as a local build.

Old source-directory and local-version overrides are rejected. To test another
release, update its file ID, version and checksum together in the lock file.

Use `genEclipseRuns eclipse` to prepare the Java 8 IDE setup. Ordinary launches
exclude unit tests and build-only runtime probes. Publication remains disabled
until this mod has its own CurseForge project.

## Testing with Mineralogy

Run `gradlew genEclipseRuns eclipse`, then refresh the Eclipse project. Select
`runMineralogyClient` under Java Application in Run Configurations. The matching
`runMineralogyServer` profile is available for a dedicated server. From a terminal,
use `gradlew runMineralogyClient` or `gradlew runMineralogyServer`.

These profiles use a separate `run-mineralogy` folder and load Mineralogy plus
its OreSpawn dependency. The BOP project also loads its usual Biomes O Plenty jar.
Ordinary client and server profiles keep their existing mod sets and worlds.
Runtime probes and unit tests are excluded from these manual profiles.

The expected release jars and their checksums are listed in
`gradle/mineralogy-runtime.properties`. Gradle downloads the pinned public
releases automatically. To use an explicit copy of the same release, pass `-PmineralogyRuntimeJar=<jar>` and
`-PorespawnRuntimeJar=<jar>` when generating the profiles. Every supplied jar
must match the recorded checksum. Their access rules are translated for the
mapped development game; the original jars are left alone.

New configurations use smoothing level 3. An existing saved level is kept, so
choose level 3 in Mods > Config and restart if that test folder already has a
different setting. Mineralogy supplies slabs; Building Pieces supplies the
matching rock steps and corners for levels 2 and 3. No separate Mineralogy
add-on is needed.

`gradlew integrationMineralogyTest` checks fresh-world and reload behaviour in
a disposable build folder. `integrationMineralogyClientProcess` checks client
resources there. Neither task opens the manual profile's saved worlds.

## Packaged manual testing

Run `gradlew prepareManualTestMods` to collect the candidate and its published
required dependencies under `build/manual-test/mods`. This folder is separate
from development profiles and contains normal production jars.

The Building Pieces BOP add-on is required in development runs and packaged
games. Its matching dependency is recorded in `gradle/content-dependencies.properties`.

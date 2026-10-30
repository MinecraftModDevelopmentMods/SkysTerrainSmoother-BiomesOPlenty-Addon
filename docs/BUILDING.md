# Building from source

Use the Gradle 9.6.1 wrapper with Java 17 and Temurin 8.0.502+7 for production.
Set `STS_JAVA8_HOME` to the Java 8 installation. This branch targets
Minecraft 1.12.2 with ForgeGradle 7.0.34.

Build the exact Grass Slabs and Building Pieces source commits listed in
`gradle/content-dependencies.properties` with their `jar` tasks first.
Build the pinned Terrain Smoother commit next, using those same content jars.
Build the pinned Building Pieces BOP add-on with that core jar too.

The default layout uses sibling checkouts. For another layout, pass
`-PgrassSlabsDirectory=<checkout>` and
`-PbuildingPiecesDirectory=<checkout>` plus
`-PterrainDirectory=<checkout>` and
`-PbuildingPiecesBopDirectory=<checkout>`.
Then run:

```text
gradlew check build javadoc verifyReleaseArtifacts writeReleaseChecksums verifyEclipseProductionClasspath
```

Development dependencies use deobfuscated jars. Packaged games use the normal
main jars; dependency classes and assets are not bundled. CI builds the exact
pinned commits rather than moving branches.

A local-only `buildingPiecesCoreVersion` override can test another core
candidate. It requires an explicit core directory and is rejected in CI.

Use `genEclipseRuns eclipse` to prepare the Java 8 IDE setup. Ordinary launches
exclude unit tests and build-only runtime probes. See [the release guide](RELEASING.md)
for publication.

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
`gradle/mineralogy-runtime.properties`. The default paths use sibling checkouts;
for a different layout, pass `-PmineralogyRuntimeJar=<jar>` and
`-PorespawnRuntimeJar=<jar>` when generating the profiles. Missing optional jars
do not prevent an ordinary build; `prepareMineralogyDevelopmentRun` explains
what is needed before the combined profile can run. The jars are copied, not
modified. Their access rules are translated for the mapped development game.

New configurations use smoothing level 3. An existing saved level is kept, so
choose level 3 in Mods > Config and restart if that test folder already has a
different setting. Building Pieces supplies Mineralogy steps at level 2 and
corners at level 3 when Mineralogy is installed; no separate add-on is needed.

`gradlew integrationMineralogyTest` checks fresh-world and reload behaviour in
a disposable build folder. `integrationMineralogyClientProcess` checks client
resources there. Neither task opens the manual profile's saved worlds.

The Building Pieces BOP add-on is required in development runs and packaged
games. Its matching dependency is recorded in `gradle/content-dependencies.properties`.

## Recipe-book resources

The build generates unlock advancements from `src/main/recipe-book/recipes.json`.
The runtime checks compare those entries with the registered recipes and exercise
inventory discovery, crafting-table discovery and recipe-book persistence.
Optional catalogue entries load only when their recipe exists.

Recipe unlocks have no shared advancement parent. Minecraft 1.12 walks that
visibility tree recursively when loading completed progress, which can overflow
with a large catalogue. The inventory and crafting unlock criteria are unchanged.


## Local candidates and a fresh Eclipse import

Generating Eclipse runs with explicit local version overrides also writes
`.gradle/local-content.properties`. This ignored file keeps the selected
checkouts and versions when Eclipse's import wizard clears project arguments.
The build reads it automatically; command-line properties take precedence.
CI rejects this file and continues using the tracked source pins.

Remove the local file when returning to the published dependency versions,
or regenerate the launches with another explicit set of overrides.

ForgeGradle's Problems report may still warn about unmerged source sets.
This project prepares its own merged production output and checks Eclipse
classpath isolation; that warning does not mean the sync failed.


The Mineralogy profile reuses checksum-locked retained or installed jars when a
sibling build no longer contains the pinned release. The same selected jars
supply both the running mods and their mapped access rules. Incorrect checksums
or missing access rules fail before launch; installed jars are never copied
onto themselves.

`verifyMineralogyDevelopmentAccess` checks the mapped Minecraft fields required
by OreSpawn and BOP. `integrationMineralogyClientWorldTest` creates and reloads
a disposable BOP world with Mineralogy and OreSpawn, in addition to the fast
model-only check. It does not open the ordinary profile's saved worlds.

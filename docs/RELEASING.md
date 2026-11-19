# Preparing a release

Pushes and pull requests run the build, tests, artifact audit, CodeQL and Gradle
wrapper checks. CI uploads the main, sources and Javadoc jars with their checksums.
Tags only validate a candidate; they never publish it.

This mod publishes to CurseForge project 1770864. Publication is enabled only
in MMD; a fork build never uploads a release.

The MMD release workflow uses the organisation's `CURSEFORGE_TOKEN`,
`MAVEN_UPLOAD_URL`, `MAVEN_UPLOAD_USERNAME` and `MAVEN_UPLOAD_PASSWORD`.
It requires a passing CI build and explicit confirmation of live publication.
Enter the full release version and choose the CurseForge release level.
Select this Minecraft branch when starting the workflow.

The workflow resolves the Minecraft branch from the version, rebuilds the exact
commit, and checks one immutable artifact bundle. It validates or creates the tag,
then publishes the same files to Maven, CurseForge and GitHub, in that order.
A failed step prevents the later publication steps. Check the public downloads
before announcing a release.

Content mods must be released before add-ons that require them. Keep dependency
source pins up to date when preparing a coordinated release.

Declare Terrain Smoother, Grass Slabs, Building Pieces, the Building Pieces BOP
add-on and Biomes O' Plenty as required CurseForge dependencies. The release
workflow supplies these relationships so launchers can install the complete set.

CurseForge uploads have one attempt. If a file is processing or an upload times
out, check the project before retrying. Reuse the same verified bundle and rerun
only unfinished stages; do not submit a second copy.

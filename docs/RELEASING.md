# Preparing a release

Pushes and pull requests run the build, tests, artifact audit, CodeQL and Gradle
wrapper checks. CI uploads the main, sources and Javadoc jars with their checksums.
Tags only validate a candidate; they never publish it.

Publication is currently disabled. Before the first release, create this mod's
CurseForge project, set its own `curseforge_project_id` in `gradle.properties`,
and set `release_enabled=true`. Do not reuse another mod's project ID.

The MMD release workflow uses the organisation's `CURSEFORGE_TOKEN`,
`MAVEN_UPLOAD_URL`, `MAVEN_UPLOAD_USERNAME` and `MAVEN_UPLOAD_PASSWORD`.
It requires a passing CI build and explicit confirmation of live publication.
Enter the full release version and choose the CurseForge release level.

The workflow resolves the Minecraft branch from the version, rebuilds the exact
commit, and checks one immutable artifact bundle. It validates or creates the tag,
then publishes the same files to Maven, CurseForge and GitHub, in that order.
A failed step prevents the later publication steps. Check the public downloads
before announcing a release.

Content mods must be released before add-ons that require them. Keep dependency
release file IDs and checksums up to date when preparing a coordinated release.

CurseForge uploads use one attempt. A timeout or server error may happen after
an upload was accepted. Check the project file list and reconcile the file and
checksum before considering another upload; do not blindly rerun publication.

Declare Terrain Smoother, Grass Slabs, Building Pieces, the Building Pieces BOP
add-on and Biomes O' Plenty as required CurseForge dependencies. The release
workflow supplies these relationships so launchers can install the complete set.

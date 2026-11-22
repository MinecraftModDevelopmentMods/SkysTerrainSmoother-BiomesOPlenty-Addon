package zone.moddev.mc.skysterrainsmootherbop;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PublicationContractTest {
    @Test
    void hostedBuildsPinTheDeclaredProductionToolchainBeforeStartingGradle() throws Exception {
        java.util.Properties metadata = new java.util.Properties();
        metadata.load(new java.io.StringReader(text("gradle.properties")));
        assertEquals("17.0.1+12", metadata.getProperty("java_toolchain_version"));
        assertEquals("17.0.1+12", metadata.getProperty("gradle_java_toolchain_version"));
        for (String name : new String[] { "ci.yml", "codeql-analysis.yml" }) {
            String workflow = text(".github/workflows/" + name);
            int install = workflow.indexOf("- name: Install target Java toolchain");
            int pin = workflow.indexOf("- name: Pin production Java");
            int gradle = workflow.indexOf("- name: Install Java for Gradle", pin);
            assertTrue(install >= 0 && install < pin && pin < gradle, name);
            assertTrue(workflow.substring(install, pin).contains("steps.target.outputs.java_setup_version"));
            assertTrue(workflow.substring(pin, gradle).contains("STS_JAVA17_HOME=$JAVA_HOME"));
        }
        String ci = text(".github/workflows/ci.yml");
        assertTrue(ci.contains("java-version: '17.0.1+12'"));
        assertTrue(ci.contains("Bootstrap Forge from an empty cache"));
        assertTrue(ci.contains("-PterrainDirectory=dependencies/terrain"));
        assertTrue(ci.contains("-PbuildingPiecesBopDirectory=dependencies/bopPieces"));
    }

    @Test
    void publicationNeedsThisModsMetadataAndExplicitApproval() throws Exception {
        String properties = text("gradle.properties");
        java.util.Properties metadata = new java.util.Properties();

        metadata.load(new java.io.StringReader(properties));

        String projectId = metadata.getProperty("curseforge_project_id", "");

        assertTrue(metadata.getProperty("release_enabled").matches("true|false"));

        if (!projectId.matches("[1-9][0-9]*")) {
            assertEquals("false", metadata.getProperty("release_enabled"));
        }

        assertNotEquals("1677588", projectId);
        assertTrue(properties
                .contains("release_repository=MinecraftModDevelopmentMods/SkysTerrainSmoother-BiomesOPlenty-Addon"));

        String workflow = text(".github/workflows/deploy-release.yml");

        assertTrue(workflow.contains("confirm_live_publication"));
        assertTrue(workflow.contains("default: false"));
        assertTrue(workflow.contains("value release_enabled"));
        assertTrue(workflow.contains("Build, test, and audit"));
        assertTrue(workflow.contains("sha256sum --check SHA256SUMS"));
        assertTrue(workflow.contains("publishMavenJavaPublicationToReleaseRepository"));
        assertTrue(workflow.contains("curseforge-id: ${{ needs.preflight.outputs.curseforge_project_id }}"));
        assertTrue(workflow.contains("curseforge-retry-attempts: 1"));
        // Dependency project IDs belong in file relationships, never as our upload destination.
        for (String dependency : new String[] { "1768953", "1677588", "1754162", "1755976", "220318" }) {
            assertNotEquals(dependency, projectId);
            assertTrue(workflow.contains(dependency + "(required)"));
        }
        assertTrue(workflow.indexOf("  publish_maven:") < workflow.indexOf("  publish_curseforge:"));
        assertTrue(workflow.indexOf("  publish_curseforge:") < workflow.indexOf("  publish_github:"));
    }

    @Test
    void mavenUsesTheVerifiedBundleAndCiRunsOnPushAndPullRequest() throws Exception {
        String release = text("gradle/release.gradle");

        assertTrue(release.contains("if (!releaseEnabled"));
        assertTrue(release.contains("Prepared artifact checksum mismatch"));
        assertTrue(release.contains("lines.size() != 3"));
        assertTrue(release.contains("!checked.add"));
        assertTrue(release.contains("verifyPreparedReleaseArtifacts"));
        assertTrue(release.contains("collect { it.toString() }"));
        assertTrue(release.contains("verifyReleaseBundleContract"));

        String ciWorkflow = text(".github/workflows/ci.yml");

        assertTrue(ciWorkflow.contains("  push:"));
        assertTrue(ciWorkflow.contains("  pull_request:"));
        assertTrue(ciWorkflow.contains("if-no-files-found: error"));
        assertTrue(ciWorkflow.contains("integrationTest"));
        assertTrue(ciWorkflow.contains("--offline"));
    }

    private String text(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}

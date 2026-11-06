package zone.moddev.mc.skysterrainsmootherbop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeContractTest {
    private static final Path RESOURCES = Paths.get("src/main/resources");

    @Test
    void permanentPalettesCoverEveryAssignedSlotAndReserveTheRest() {
        assertEquals(21, LegacySoils.PALETTES.size());
        int assigned = 0;
        for (LegacySoils.Palette palette : LegacySoils.PALETTES.values()) {
            assertTrue(palette.id.startsWith("skysterrainsmootherbop:"));
            assertFalse(palette.valid(-1));
            assertFalse(palette.valid(16));
            for (int metadata = 0; metadata < 16; metadata++) {
                assertEquals(metadata < palette.count * palette.orientations, palette.valid(metadata));
                if (palette.valid(metadata)) {
                    assertTrue(palette.material(metadata).startsWith("minecraft:"));
                    assigned++;
                }
            }
        }
        assertEquals(306, assigned);
    }

    @Test
    void playableCoarseDirtSlabsUseTheNativeShovelTag() throws Exception {
        JsonArray values = read(RESOURCES.resolve("data/minecraft/tags/block/mineable/shovel.json"))
                .getAsJsonObject().getAsJsonArray("values");
        assertEquals(1, values.size());
        assertEquals("skysterrainsmootherbop:dirt_slab_00", values.get(0).getAsString());
    }

    @Test
    void everyOriginalRecipeIdentityHasOneNativeRecipeAndUnlock() throws Exception {
        JsonArray manifest = read(Paths.get("src/main/recipe-book/recipes.json")).getAsJsonArray();
        assertEquals(84, manifest.size());
        Set<String> ids = new HashSet<>();
        for (JsonElement row : manifest) {
            String id = row.getAsJsonObject().get("recipe").getAsString();
            assertTrue(ids.add(id));
            Path path = RESOURCES.resolve("data/skysterrainsmootherbop/recipe/"
                    + id.substring(id.indexOf(':') + 1) + ".json");
            JsonObject recipe = read(path).getAsJsonObject();
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
            assertEquals("building", recipe.get("category").getAsString(), id);
            recipe.getAsJsonObject("key").entrySet().forEach(ingredient -> {
                JsonElement value = ingredient.getValue();
                assertTrue(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(),
                    id + " needs a named ingredient for " + ingredient.getKey());
                assertTrue(value.getAsString().matches("#?[a-z0-9_.-]+:[a-z0-9_./-]+"), id);
            });
            assertFalse(recipe.toString().contains("biomesoplenty:"), "Removed native items are not recipe inputs");
            assertFalse(recipe.toString().contains("\"data\""), "Recipe ingredients use flat item identities");
            assertTrue(recipe.getAsJsonObject("result").get("count").getAsInt() > 0);
        }
    }

    @Test
    void eighteenUtf8LanguagesAndEveryHiddenModelRemainAvailable() throws Exception {
        Path assets = RESOURCES.resolve("assets/skysterrainsmootherbop");
        try (Stream<Path> languages = Files.list(assets.resolve("lang"))) {
            Path[] paths = languages.toArray(Path[]::new);
            assertEquals(18, paths.length);
            for (Path path : paths) {
                assertTrue(path.toString().endsWith(".json"));
                assertFalse(read(path).getAsJsonObject().entrySet().isEmpty());
                assertFalse(text(path).contains("\uFFFD"));
            }
        }
        for (String id : LegacySoils.PALETTES.keySet()) {
            String name = id.substring(id.indexOf(':') + 1);
            assertTrue(Files.isRegularFile(assets.resolve("blockstates/" + name + ".json")));
            assertTrue(Files.isRegularFile(assets.resolve("models/item/" + name + ".json")));
            JsonObject definition = read(assets.resolve("items/" + name + ".json")).getAsJsonObject()
                    .getAsJsonObject("model");
            assertEquals("minecraft:model", definition.get("type").getAsString());
            assertEquals("skysterrainsmootherbop:item/" + name, definition.get("model").getAsString());
        }
        JsonObject pack = read(RESOURCES.resolve("pack.mcmeta")).getAsJsonObject().getAsJsonObject("pack");
        assertEquals(101, pack.get("max_format").getAsInt());
        assertEquals(101, pack.getAsJsonArray("min_format").get(0).getAsInt());
        assertEquals(1, pack.getAsJsonArray("min_format").get(1).getAsInt());
    }

    @Test
    void genuineFullFamilyFixtureIsSealedAndContainsAllOldTerrainPieces() throws Exception {
        Path fixture = Paths.get("src/test/resources/fixtures/forward-1.12.2-family");
        String expected = text(fixture.resolve("SHA256SUMS")).split("\\s+")[0];
        StringBuilder actual = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(fixture.resolve("world.zip")))) {
            actual.append(String.format(java.util.Locale.ROOT, "%02X", value & 255));
        }
        assertEquals(expected, actual.toString());
        JsonObject source = read(fixture.resolve("source-family-fixture.json")).getAsJsonObject();
        assertEquals(4688, source.getAsJsonArray("blocks").size());
        assertEquals(4688, source.getAsJsonArray("stacks").size());
        int soils = 0;
        for (JsonElement value : source.getAsJsonArray("blocks")) {
            JsonObject block = value.getAsJsonObject();
            LegacySoils.Palette palette = LegacySoils.PALETTES.get(block.get("id").getAsString());
            if (palette != null) {
                assertTrue(palette.valid(block.get("meta").getAsInt()));
                soils++;
            }
        }
        assertEquals(306, soils);
        assertEquals(5, source.getAsJsonArray("entities").size());
        assertEquals(7, read(fixture.resolve("source-artifacts.json")).getAsJsonArray().size());
    }

    @Test
    void nativeMetadataKeepsAllRequiredDependenciesAndNoExtraTransformer() throws Exception {
        String metadata = text(RESOURCES.resolve("META-INF/mods.toml"));
        for (String mod : new String[] { "forge", "minecraft", "skysgrassslabs", "skysbuildingpieces",
                "skysbuildingpiecesbop", "skysterrainsmoother", "biomesoplenty" }) {
            assertTrue(metadata.contains("modId=\"" + mod + "\""));
        }
        assertTrue(metadata.contains("[26.1.2.0.22]"));
        assertFalse(Files.exists(RESOURCES.resolve("META-INF/coremods.json")));
        assertFalse(Files.exists(RESOURCES.resolve("mcmod.info")));
    }

    @Test
    void developmentLaunchesUseTheSamePinnedBopJarsAsPackagedGames() throws Exception {
        String build = text(Paths.get("build.gradle"));
        assertFalse(build.contains("mixin.env.disableRefMap"));
        assertFalse(build.contains("mappedAccessRules"));
        assertTrue(build.contains("bopPiecesDevelopmentJar, bopDevelopmentJar, terraBlenderDevelopmentJar, glitchCoreDevelopmentJar"));
        String launcher = text(Paths.get("tools/run_packaged_checks.py"));
        assertFalse(launcher.contains("mixin.env.disableRefMap"), "Public jars retain their own refmaps");
        java.util.Properties lock = new java.util.Properties();
        lock.load(new java.io.StringReader(text(Paths.get("gradle/content-dependencies.properties"))));
        for (String name : new String[] { "bop", "terrablender", "glitchcore" }) {
            assertTrue(lock.getProperty(name + ".sha256").matches("[0-9A-F]{64}"));
            assertEquals(lock.getProperty(name + ".sha256"), lock.getProperty(name + ".development.sha256"));
        }
    }


    private static JsonElement read(Path path) throws Exception {
        return new JsonParser().parse(text(path));
    }

    private static String text(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}

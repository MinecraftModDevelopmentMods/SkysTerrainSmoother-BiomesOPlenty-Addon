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
    void everyOriginalRecipeIdentityHasOneNativeRecipeAndUnlock() throws Exception {
        JsonArray manifest = read(Paths.get("src/main/recipe-book/recipes.json")).getAsJsonArray();
        assertEquals(84, manifest.size());
        Set<String> ids = new HashSet<>();
        for (JsonElement row : manifest) {
            String id = row.getAsJsonObject().get("recipe").getAsString();
            assertTrue(ids.add(id));
            Path path = RESOURCES.resolve("data/skysterrainsmootherbop/recipes/"
                    + id.substring(id.indexOf(':') + 1) + ".json");
            JsonObject recipe = read(path).getAsJsonObject();
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
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
        }
        assertEquals(6, read(RESOURCES.resolve("pack.mcmeta")).getAsJsonObject()
                .getAsJsonObject("pack").get("pack_format").getAsInt());
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
        assertTrue(metadata.contains("[1.16.5-13.1.0,1.16.5-14)"));
        assertFalse(Files.exists(RESOURCES.resolve("META-INF/coremods.json")));
        assertFalse(Files.exists(RESOURCES.resolve("mcmod.info")));
    }

    private static JsonElement read(Path path) throws Exception {
        return new JsonParser().parse(text(path));
    }

    private static String text(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}

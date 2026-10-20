package zone.moddev.mc.skysterrainsmootherbop;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceContractTest {
    private static final Path ROOT = Paths.get("src/main/resources/assets/skysterrainsmootherbop");

    @Test
    void fixedPalettesAndAllVisualsAreCovered() throws Exception {
        int palettes = 0;
        int states = 0;

        try (java.util.stream.Stream<Path> files = Files.list(ROOT.resolve("blockstates"))) {
            for (Path path : (Iterable<Path>) files::iterator) {
                JsonObject variants = new JsonParser().parse(text(path)).getAsJsonObject().getAsJsonObject("variants");

                palettes++;

                for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
                    String model = variant.getValue().getAsJsonObject().get("model").getAsString().split(":")[1];
                    JsonObject json = new JsonParser().parse(text(ROOT.resolve("models/block/" + model + ".json")))
                            .getAsJsonObject();
                    boolean snow = variant.getKey().endsWith("true");

                    if (snow) {
                        assertEquals("minecraft:blocks/snow",
                                json.getAsJsonObject("textures").get("top").getAsString());
                        assertFalse(json.get("parent").getAsString().endsWith("_grass"));
                    }

                    JsonArray elements = new JsonParser()
                            .parse(text(
                                    ROOT.resolve("models/" + json.get("parent").getAsString().split(":")[1] + ".json")))
                            .getAsJsonObject().getAsJsonArray("elements");

                    assertFalse(elements.toString().contains("cullface"));
                    states++;
                }
            }
        }

        assertEquals(16, palettes);
        assertTrue(states >= 504);

        Set<String> registryIds = new TreeSet<>();

        for (String group : new String[] { "dirt", "grass" }) {
            registryIds.add(group + "_slab_00.json");

            for (String shape : new String[] { "step", "corner" }) {
                for (int n = 0; n < (group.equals("grass") ? 4 : 3); n++) {
                    registryIds.add(group + "_" + shape + "_0" + n + ".json");
                }
            }
        }
        try (java.util.stream.Stream<Path> files = Files.list(ROOT.resolve("blockstates"))) {
            assertEquals(registryIds, files.map(path -> path.getFileName().toString())
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new)));
        }

        assertTrue(text(ROOT.resolve("models/block/grass_slab_00_4_false.json"))
                .contains("biomesoplenty:blocks/dirt_loamy"));
        assertTrue(text(ROOT.resolve("models/block/grass_slab_00_10_false.json"))
                .contains("biomesoplenty:blocks/grass_origin_top"));
        assertTrue(text(ROOT.resolve("models/block/dirt_slab_00_6_false.json"))
                .contains("biomesoplenty:blocks/coarse_dirt_loamy"));

        try (java.util.stream.Stream<Path> paths = Files.walk(ROOT)) {
            assertFalse(paths.anyMatch(path -> path.toString().endsWith(".png")));
        }
    }

    @Test
    void allEighteenLocalesHaveIdenticalKeysAndPortableEncoding() throws Exception {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("pieces.skysterrainsmootherbop.slab",
                "pieces.skysterrainsmootherbop.step", "pieces.skysterrainsmootherbop.corner"));
        int files = 0;

        try (java.util.stream.Stream<Path> paths = Files.list(ROOT.resolve("lang"))) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                byte[] bytes = Files.readAllBytes(path);
                String content = new String(bytes, StandardCharsets.UTF_8);

                assertFalse(content.startsWith("\uFEFF"));
                assertFalse(content.contains("\r"));
                assertFalse(content.contains("\uFFFD"));
                assertTrue(content.endsWith("\n"));

                Set<String> keys = new LinkedHashSet<>();

                for (String line : content.split("\n")) {
                    assertEquals(line.trim(), line);
                    assertTrue(line.contains("=%s") || line.substring(line.indexOf('=') + 1).contains("%s"));
                    assertTrue(keys.add(line.split("=", 2)[0]));
                }

                assertEquals(new ArrayList<>(expected), new ArrayList<>(keys));
                files++;
            }
        }

        assertEquals(18, files);

        for (String locale : new String[] { "de_at", "de_au", "de_de", "en_ca", "en_en", "en_gb", "en_pt", "en_us",
                "es_es", "es_mx", "fr_ca", "fr_fr", "ja_jp", "ko_kr", "pt_br", "pt_pt", "ru_ru", "zh_cn" }) {
            assertTrue(Files.isRegularFile(ROOT.resolve("lang/" + locale + ".lang")));
        }
    }

    private static String text(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}

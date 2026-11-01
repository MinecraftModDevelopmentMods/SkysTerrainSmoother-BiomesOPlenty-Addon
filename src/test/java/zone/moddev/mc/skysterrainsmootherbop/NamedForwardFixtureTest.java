package zone.moddev.mc.skysterrainsmootherbop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NamedForwardFixtureTest {
    @Test
    void actual115SaveRetainsTheQualifiedFamilyAndItsResolvedEtherealPieces() throws Exception {
        Path root = Paths.get("src/test/resources/fixtures/forward-1.15.2-family");
        JsonObject source = json(root.resolve("provenance.json"));
        String checksum = digest(root.resolve("world.zip"));

        assertEquals("1B7AB7CE8BC9A1F1BA7AB8EAB2F44290B6EE4D3056DBB4475644CBCFE77F22A9", checksum);
        assertEquals(checksum, source.get("world_sha256").getAsString());
        assertEquals("1.15.2", source.get("source_minecraft").getAsString());
        assertEquals(2230, source.get("source_data_version").getAsInt());
        assertEquals(4700, source.get("source_saved_states").getAsInt());
        assertEquals(6, source.get("wet_sand_states").getAsInt());
        assertEquals(24, source.getAsJsonArray("preceding_state_changes").size());
        assertTrue(source.get("source_world_unchanged").getAsBoolean());
        assertEquals(source.get("manifest_sha256").getAsString(), digest(root.resolve("source-family-fixture.json")));
        assertEquals(source.get("native_states_sha256").getAsString(), digest(root.resolve("native-source-states.json")));

        Set<String> artifacts = new HashSet<>();
        source.getAsJsonArray("source_artifacts")
                .forEach(row -> artifacts.add(row.getAsJsonObject().get("sha256").getAsString()));
        assertEquals(6, artifacts.size());
        assertTrue(artifacts.contains("1BA84C1CAACC9006E3272B3BA37FCC9D1DB6B90091E727C366B9969EDD5E74BF"));
        assertTrue(artifacts.contains("A0D741B5D641DE05EFA37DDC0565B5FC3164661A6B03890E71ECF86F7EB56E51"));
        assertTrue(artifacts.contains("94A5BAA4B3DC025A338A60BAC27B3E5BBAD4AE0CE494BA5EC1F852E401A704F7"));
        assertTrue(artifacts.contains("7AD98ED70F0A88ADE5C7048C21811E7B3BAC889593070D54A0790EA339A81E9D"));
        assertTrue(artifacts.contains("CF73DA4A5688D79F5FFBC76FEF83941472A5FBEFF9D84581B57F1B67CAA399EB"));
        assertTrue(artifacts.contains("FA450EB93CCE9CF16F88BCB0DD48FF8ABBD163981EA54DABD27CC0B6E110F99C"));
    }

    @Test
    void precedingVersionSaveUsesQualifiedJarsAndContainsWetSand() throws Exception {
        Path root = Paths.get("src/test/resources/fixtures/forward-1.14.4-family");
        JsonObject source = json(root.resolve("provenance.json"));
        String checksum = digest(root.resolve("world.zip"));

        assertEquals("54791A41A96148B0DBF34BE88185629C8EF6C3133A56C15BFF6136BBDCF5F662", checksum);
        assertEquals(checksum, source.get("world_sha256").getAsString());
        assertEquals("1.14.4", source.get("source_minecraft").getAsString());
        assertEquals(1976, source.get("source_data_version").getAsInt());
        assertEquals(4700, source.get("source_saved_states").getAsInt());
        assertEquals(6, source.get("wet_sand_states").getAsInt());
        assertTrue(source.get("source_world_unchanged").getAsBoolean());
        assertEquals(source.get("manifest_sha256").getAsString(), digest(root.resolve("source-family-fixture.json")));
        assertEquals(source.get("native_states_sha256").getAsString(), digest(root.resolve("native-source-states.json")));
        assertEquals(12, json(root.resolve("source-family-fixture.json")).getAsJsonArray("native_sand_states").size());

        Set<String> artifacts = new HashSet<>();

        source.getAsJsonArray("source_artifacts")
                .forEach(row -> artifacts.add(row.getAsJsonObject().get("sha256").getAsString()));
        assertEquals(6, artifacts.size());
        assertTrue(artifacts.contains("03D2E769EBC5F701DE065BA5002207D9BE399E66F7DABF653B28A3FDC9061DD3"));
        assertTrue(artifacts.contains("CF0BA460D641A4723CEB4E069D5B995C2F3357531DD86CA73CD9157F687D5702"));
        assertTrue(artifacts.contains("65B32AAF4D34B1FFBAC6452041A4411F64357970DE6A736E4D9500346FABCA1B"));
        assertTrue(artifacts.contains("3DCC134F8EC216D8C7E78DC7294BB31F06DAEFA097739C241178E7C1D7E8A357"));
        assertTrue(artifacts.contains("CE66EA52660BC48533387B3065C082CF1C3916F4502BDB95C04B0F761B1BB195"));
        assertTrue(artifacts.contains("8A0A1DEA2A4A6C069519C7B1BCF28331186FF2DF6284DB72C2FFDB8178FC84D4"));
    }

    private JsonObject json(Path path) throws Exception {
        return new JsonParser().parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private String digest(Path path) throws Exception {
        StringBuilder result = new StringBuilder();

        for (byte value : MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))) {
            result.append(String.format(Locale.ROOT, "%02X", value));
        }
        return result.toString();
    }
}

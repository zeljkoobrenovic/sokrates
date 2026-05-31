package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VirtualLandscapesConfigTest {

    @Test
    void parsesVirtualLandscapesFromConfigJson() throws Exception {
        String json = "{\n" +
                "  \"metadata\": { \"name\": \"Parent\" },\n" +
                "  \"virtualLandscapes\": {\n" +
                "    \"remainderLandscapeMetadata\": { \"name\": \"Remainder\" },\n" +
                "    \"landscapes\": [\n" +
                "      { \"metadata\": { \"name\": \"Datadog\" },\n" +
                "        \"includeRepoNamePatterns\": [\".*datadog.*\"],\n" +
                "        \"excludeRepoNamePatterns\": [] }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        LandscapeConfiguration config = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);

        assertNotNull(config.getVirtualLandscapes());
        assertEquals("Remainder", config.getVirtualLandscapes().getRemainderLandscapeMetadata().getName());
        assertEquals(1, config.getVirtualLandscapes().getLandscapes().size());
        VirtualLandscapeConfig vl = config.getVirtualLandscapes().getLandscapes().get(0);
        assertEquals("Datadog", vl.getMetadata().getName());
        assertEquals(java.util.List.of(".*datadog.*"), vl.getIncludeRepoNamePatterns());
        assertTrue(vl.getExcludeRepoNamePatterns().isEmpty());
    }

    @Test
    void configWithoutVirtualLandscapesGivesEmptyDefault() throws Exception {
        String json = "{ \"metadata\": { \"name\": \"Parent\" } }";
        LandscapeConfiguration config = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);

        assertNotNull(config.getVirtualLandscapes());
        assertTrue(config.getVirtualLandscapes().getLandscapes().isEmpty());
    }

    @Test
    void roundTripsThroughJson() throws Exception {
        LandscapeConfiguration config = new LandscapeConfiguration();
        VirtualLandscapeConfig vl = new VirtualLandscapeConfig();
        vl.getMetadata().setName("Datadog");
        vl.setIncludeRepoNamePatterns(java.util.List.of(".*datadog.*"));
        config.getVirtualLandscapes().getLandscapes().add(vl);

        String json = new JsonGenerator().generate(config);
        LandscapeConfiguration restored = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);

        assertEquals(1, restored.getVirtualLandscapes().getLandscapes().size());
        assertEquals("Datadog", restored.getVirtualLandscapes().getLandscapes().get(0).getMetadata().getName());
    }
}

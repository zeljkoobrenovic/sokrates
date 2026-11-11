package nl.obren.sokrates.reports.dataexporters.duplication;

import nl.obren.sokrates.common.io.JsonGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateFragmentExportTest {

    // The JSON field names are a contract with src/viewer.html's renderDuplicate
    // (reads ext and blocks[].file/from/to/code).
    @Test
    void serializesToViewerExpectedKeys() throws Exception {
        DuplicateFragmentExport fragment = new DuplicateFragmentExport("java");
        fragment.addBlock("com/x/Foo.java", 10, 20, "a();\nb();");
        fragment.addBlock("com/x/Bar.java", 30, 40, "a();\nb();");

        String json = new JsonGenerator().generate(fragment);

        assertTrue(json.contains("\"ext\" : \"java\""), json);
        assertTrue(json.contains("\"blocks\""), json);
        assertTrue(json.contains("\"file\" : \"com/x/Foo.java\""), json);
        assertTrue(json.contains("\"from\" : 10"), json);
        assertTrue(json.contains("\"to\" : 20"), json);
        assertTrue(json.contains("\"file\" : \"com/x/Bar.java\""), json);
        assertTrue(json.contains("\"from\" : 30"), json);
        assertTrue(json.contains("\"code\" : \"a();\\nb();\""), json);
    }
}

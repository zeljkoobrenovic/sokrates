package nl.obren.sokrates.reports.dataexporters.units;

import nl.obren.sokrates.common.io.JsonGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FragmentExportTest {

    // The JSON field names are a contract with src/viewer.html's render logic
    // (renderUnit reads name/file/from/to/loc/mccabe/ext/code).
    @Test
    void serializesToViewerExpectedKeys() throws Exception {
        FragmentExport fragment = new FragmentExport(
                "doWork", "com/x/Foo.java", 42, 58, 12, 4, "java", "void doWork() {}");

        String json = new JsonGenerator().generate(fragment);

        assertTrue(json.contains("\"name\" : \"doWork\""), json);
        assertTrue(json.contains("\"file\" : \"com/x/Foo.java\""), json);
        assertTrue(json.contains("\"from\" : 42"), json);
        assertTrue(json.contains("\"to\" : 58"), json);
        assertTrue(json.contains("\"loc\" : 12"), json);
        assertTrue(json.contains("\"mccabe\" : 4"), json);
        assertTrue(json.contains("\"ext\" : \"java\""), json);
        assertTrue(json.contains("\"code\" : \"void doWork() {}\""), json);
    }
}

package nl.obren.sokrates.common.renderingutils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.InflaterInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualizationTemplateTest {

    // deflateBase64 produces a zlib stream (java.util.zip.Deflater default) that round-trips via
    // Inflater — the same stream the browser decodes with fflate.unzlibSync.
    @Test
    void deflateBase64RoundTrips() throws Exception {
        String json = "[{\"name\":\"a\",\"size\":10},{\"name\":\"b\",\"size\":20}]";
        String b64 = VisualizationTemplate.deflateBase64(json);

        byte[] compressed = Base64.getDecoder().decode(b64);
        InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(compressed));
        String decoded = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(json, decoded);
    }

    @Test
    void compressesLargePayload() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 2000; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"component_").append(i).append("\",\"size\":").append(i).append("}");
        }
        sb.append("]");
        String json = sb.toString();

        String b64 = VisualizationTemplate.deflateBase64(json);
        // Repetitive JSON should compress well even after base64 (~33% inflation).
        assertTrue(b64.length() < json.length() / 2,
                "expected compressed+base64 (" + b64.length() + ") < half of raw (" + json.length() + ")");
    }
}

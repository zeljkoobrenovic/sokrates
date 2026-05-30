package nl.obren.sokrates.reports.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DataImageUtilsTest {

    @Test
    void getLangDataImageReturnsDataUriForKnownLang() {
        String image = DataImageUtils.getLangDataImage("java");
        assertNotNull(image);
        assertTrue(image.startsWith("data:image/png;base64,"));
    }

    @Test
    void getLangDataImageIsCaseAndWhitespaceInsensitive() {
        assertNotNull(DataImageUtils.getLangDataImage("  JAVA  "));
    }

    @Test
    void getLangDataImageReturnsNullForUnknownLang() {
        assertNull(DataImageUtils.getLangDataImage("definitelynotalanguage"));
    }

    @Test
    void getLangDataImageHandlesNull() {
        assertNull(DataImageUtils.getLangDataImage(null));
    }

    @Test
    void getLangDataImageMapJsonContainsKnownLangsOnly() {
        String json = DataImageUtils.getLangDataImageMapJson(Arrays.asList("java", "definitelynotalanguage"));
        assertTrue(json.startsWith("{") && json.endsWith("}"));
        assertTrue(json.contains("\"java\":\"data:image/png;base64,"));
        assertFalse(json.contains("definitelynotalanguage"));
    }

    @Test
    void getLangDataImageMapJsonDeduplicatesAndIgnoresBlanksAndNulls() {
        String json = DataImageUtils.getLangDataImageMapJson(Arrays.asList("java", "JAVA", " ", null));
        // "java" key appears exactly once despite being requested twice (different case).
        int first = json.indexOf("\"java\"");
        assertTrue(first >= 0);
        assertEquals(first, json.lastIndexOf("\"java\""));
    }

    @Test
    void getLangDataImageMapJsonReturnsEmptyObjectForNoMatches() {
        assertEquals("{}", DataImageUtils.getLangDataImageMapJson(Collections.emptyList()));
        assertEquals("{}", DataImageUtils.getLangDataImageMapJson(Collections.singletonList("nope")));
    }
}

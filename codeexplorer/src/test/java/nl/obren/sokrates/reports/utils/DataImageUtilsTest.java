package nl.obren.sokrates.reports.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataImageUtilsTest {

    @Test
    void getLangDataImage() {
        assertNotEquals(DataImageUtils.getLangDataImage("java"), DataImageUtils.DEVELOPER);
        assertNotEquals(DataImageUtils.getLangDataImage("c"), DataImageUtils.DEVELOPER);
        assertEquals(DataImageUtils.getLangDataImage("java"), DataImageUtils.getLangDataImage("Java"));
    }
}
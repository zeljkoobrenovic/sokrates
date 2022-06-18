package nl.obren.sokrates.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilsTest {

    @Test
    void getSafeFileName() {
        assertEquals(SystemUtils.getSafeFileName(""), "");
        assertEquals(SystemUtils.getSafeFileName("_"), "_");
        assertEquals(SystemUtils.getSafeFileName("abc"), "abc");
        assertEquals(SystemUtils.getSafeFileName("abc.def"), "abc.def");
        assertEquals(SystemUtils.getSafeFileName("a/b/c.def"), "a_b_c.def");
        assertEquals(SystemUtils.getSafeFileName("\"abc\".def"), "_abc_.def");
        assertEquals(SystemUtils.getSafeFileName("!_#456fdsE.html"), "___456fdsE.html");
    }

}
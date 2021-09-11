package nl.obren.sokrates.reports.landscape.statichtml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LandscapeIndividualContributorsReportsTest {

    @Test
    void getSafeFileName() {
        assertEquals(LandscapeIndividualContributorsReports.getSafeFileName(""), "");
        assertEquals(LandscapeIndividualContributorsReports.getSafeFileName("_"), "_");
        assertEquals(LandscapeIndividualContributorsReports.getSafeFileName("abc"), "abc");
        assertEquals(LandscapeIndividualContributorsReports.getSafeFileName("abc.def"), "abc.def");
        assertEquals(LandscapeIndividualContributorsReports.getSafeFileName("a/b/c.def"), "a_b_c.def");
        assertEquals(LandscapeIndividualContributorsReports.getSafeFileName("\"abc\".def"), "_abc_.def");
        assertEquals(LandscapeIndividualContributorsReports.getSafeFileName("!_#456fdsE.html"), "___456fdsE.html");
    }
}
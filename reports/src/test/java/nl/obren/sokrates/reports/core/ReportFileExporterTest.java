package nl.obren.sokrates.reports.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportFileExporterTest {

    @Test
    void extractTitle() {
        assertEquals(ReportFileExporter.extractTitle("ABC"), "ABC");
        assertEquals(ReportFileExporter.extractTitle("<div>ABC</div>"), "ABC");
        assertEquals(ReportFileExporter.extractTitle("<div>ABC</div> <div><img></div>"), "ABC");
        assertEquals(ReportFileExporter.extractTitle(" <div>ABC </div> <div><img>  </div>"), "ABC");
    }
}
/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalyzer;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LandscapeReportGeneratorTest {

    @Ignore
    @Test
    public void report() throws IOException {
        LandscapeAnalyzer analyzer = new LandscapeAnalyzer();

        File landscapeConfigFile = new File("");

        File reportsFolder = new File("");


        LandscapeAnalysisResults landscapeAnalysisResults = analyzer.analyze(landscapeConfigFile);

        LandscapeReportGenerator reportGenerator = new LandscapeReportGenerator(landscapeAnalysisResults);
        List<RichTextReport> reports = reportGenerator.report();

        try {
            FileUtils.deleteDirectory(reportsFolder);
            reportsFolder.mkdirs();

            reports.forEach(report -> {
                ReportFileExporter.exportHtml(reportsFolder, "", report);
            });

            LandscapeVisualsGenerator visualsGenerator = new LandscapeVisualsGenerator(reportsFolder);
            visualsGenerator.exportVisuals(landscapeAnalysisResults);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

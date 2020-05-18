/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalyzer;
import nl.obren.sokrates.sourcecode.landscape.init.LandscapeAnalysisInitiator;
import nl.obren.sokrates.sourcecode.landscape.init.LandscapeAnalysisUpdater;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class LandscapeAnalysisCommands {
    public static void init(File analysisRoot, File landscapeConfigFile) {
        landscapeConfigFile = getConfigFile(analysisRoot, landscapeConfigFile);
        LandscapeAnalysisInitiator initiator = new LandscapeAnalysisInitiator();
        initiator.initConfiguration(analysisRoot, landscapeConfigFile, true);
        generateReport(landscapeConfigFile);

        System.out.println("Configuration file: " + landscapeConfigFile.getPath());
    }

    public static void update(File analysisRoot, File landscapeConfigFile) {
        landscapeConfigFile = getConfigFile(analysisRoot, landscapeConfigFile);
        LandscapeAnalysisUpdater updater = new LandscapeAnalysisUpdater();
        updater.updateConfiguration(analysisRoot, landscapeConfigFile);
        System.out.println("Configuration file: " + landscapeConfigFile.getPath());
        generateReport(landscapeConfigFile);
    }

    private static File getConfigFile(File analysisRoot, File landscapeConfigFile) {
        if (landscapeConfigFile == null) {
            File landscapeAnalysisRoot = new File(analysisRoot, "_sokrates_landscape");
            landscapeConfigFile = new File(landscapeAnalysisRoot, "config.json");
        }
        return landscapeConfigFile;
    }

    public static void generateReport(File landscapeConfigFile) {
        File reportsFolder = Paths.get(landscapeConfigFile.getParent(), "").toFile();

        LandscapeAnalyzer analyzer = new LandscapeAnalyzer();

        LandscapeAnalysisResults landscapeAnalysisResults = analyzer.analyze(landscapeConfigFile);

        LandscapeReportGenerator reportGenerator = new LandscapeReportGenerator(landscapeAnalysisResults);
        List<RichTextReport> reports = reportGenerator.report();

        try {
            FileUtils.deleteDirectory(reportsFolder);
            reportsFolder.mkdirs();

            File finalReportsFolder = reportsFolder;
            reports.forEach(report -> {
                ReportFileExporter.exportHtml(finalReportsFolder, "", report);
            });

            LandscapeVisualsGenerator visualsGenerator = new LandscapeVisualsGenerator(reportsFolder);
            visualsGenerator.exportVisuals(landscapeAnalysisResults);

            System.out.println("Report file: " + finalReportsFolder.getPath() + "/index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

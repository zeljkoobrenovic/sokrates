/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.LandscapeVisualsGenerator;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.landscape.DefaultTags;
import nl.obren.sokrates.sourcecode.landscape.TagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalyzer;
import nl.obren.sokrates.sourcecode.landscape.init.LandscapeAnalysisUpdater;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LandscapeAnalysisCommands {
    private static final Log LOG = LogFactory.getLog(LandscapeAnalysisCommands.class);

    public static File update(File analysisRoot, File landscapeConfigFile, Metadata metadata) {
        landscapeConfigFile = getConfigFile(analysisRoot, landscapeConfigFile);
        LandscapeAnalysisUpdater updater = new LandscapeAnalysisUpdater();
        updater.updateConfiguration(analysisRoot, landscapeConfigFile, metadata);
        LOG.info("Configuration file: " + landscapeConfigFile.getPath());
        generateReport(analysisRoot, landscapeConfigFile);

        return landscapeConfigFile.getParentFile();
    }

    private static File getConfigFile(File analysisRoot, File landscapeConfigFile) {
        if (landscapeConfigFile == null) {
            File landscapeAnalysisRoot = new File(analysisRoot, "_sokrates_landscape");
            landscapeConfigFile = new File(landscapeAnalysisRoot, "config.json");
        }
        return landscapeConfigFile;
    }

    public static void generateReport(File analysisRoot, File landscapeConfigFile) {
        File reportsFolder = Paths.get(landscapeConfigFile.getParent(), "").toFile();
        reportsFolder.mkdirs();
        File individualReportsFolder = new File(reportsFolder, "contributors");
        try {
            FileUtils.deleteDirectory(individualReportsFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        individualReportsFolder.mkdirs();

        LandscapeAnalyzer analyzer = new LandscapeAnalyzer();

        ProcessingStopwatch.start("analyzing");
        LandscapeAnalysisResults landscapeAnalysisResults = analyzer.analyze(landscapeConfigFile);
        List<TagGroup> tagGroups = getTagGroups(analysisRoot, landscapeConfigFile);
        ProcessingStopwatch.end("analyzing");

        ProcessingStopwatch.start("reporting");
        LandscapeReportGenerator reportGenerator = new LandscapeReportGenerator(landscapeAnalysisResults, tagGroups, landscapeConfigFile.getParentFile(), reportsFolder);
        List<RichTextReport> reports = reportGenerator.report();

        try {
            ProcessingStopwatch.start("reporting/saving");
            ProcessingStopwatch.start("reporting/saving/reports");
            File finalReportsFolder = reportsFolder;
            String customHtmlReportHeaderFragment = landscapeAnalysisResults.getConfiguration().getCustomHtmlReportHeaderFragment();
            reports.forEach(report -> {
                LOG.info("Exporting " + report.getFileName() + ".");
                ReportFileExporter.exportHtml(finalReportsFolder, "", report, customHtmlReportHeaderFragment);
            });
            ProcessingStopwatch.end("reporting/saving/reports");

            ProcessingStopwatch.start("reporting/saving/contributors");
            File finalIndividualReportsFolder = individualReportsFolder;
            reportGenerator.getIndividualContributorReports().forEach(individualReport -> {
                LOG.info("Exporting person " + individualReport.getFileName() + ".");
                ReportFileExporter.exportHtml(finalIndividualReportsFolder, "", individualReport, customHtmlReportHeaderFragment);
            });
            reportGenerator.getIndividualTeamReports().forEach(individualReport -> {
                LOG.info("Exporting team " + individualReport.getFileName() + ".");
                ReportFileExporter.exportHtml(finalIndividualReportsFolder, "", individualReport, customHtmlReportHeaderFragment);
            });
            reportGenerator.getIndividualBotReports().forEach(individualReport -> {
                LOG.info("Exporting bot " + individualReport.getFileName() + ".");
                ReportFileExporter.exportHtml(finalIndividualReportsFolder, "", individualReport, customHtmlReportHeaderFragment);
            });
            ProcessingStopwatch.end("reporting/saving/contributors");

            ProcessingStopwatch.start("reporting/saving/generating visuals");
            LandscapeVisualsGenerator visualsGenerator = new LandscapeVisualsGenerator(reportsFolder, landscapeAnalysisResults.getTeamsConfig());
            visualsGenerator.exportVisuals(landscapeAnalysisResults);
            ProcessingStopwatch.end("reporting/saving/generating visuals");

            LOG.info("Report file: " + finalReportsFolder.getPath() + "/index.html");
            ProcessingStopwatch.end("reporting/saving");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ProcessingStopwatch.end("reporting");
    }

    private static List<TagGroup> getTagGroups(File analysisRoot, File landscapeConfigFile) {
        File landscapeTagsConfigFile = new File(landscapeConfigFile.getParentFile(), "config-tags.json");
        List<TagGroup> tagGroups = new ArrayList<>();
        if (!landscapeTagsConfigFile.exists()) {
            try {
                tagGroups = new DefaultTags().defaultTagGroups();
                FileUtils.write(landscapeTagsConfigFile, new JsonGenerator().generate(tagGroups), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.error(e);
            }
        } else {
            try {
                tagGroups = new JsonMapper().getObject(FileUtils.readFileToString(landscapeTagsConfigFile, StandardCharsets.UTF_8), new TypeReference<>() {
                });
                if (tagGroups != null) {
                    FileUtils.write(landscapeTagsConfigFile, new JsonGenerator().generate(tagGroups), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                LOG.error(e);
            }
        }

        tagGroups.forEach(group -> group.getRepositoryTags().forEach(tag -> tag.setGroup(group)));

        return tagGroups;
    }
}

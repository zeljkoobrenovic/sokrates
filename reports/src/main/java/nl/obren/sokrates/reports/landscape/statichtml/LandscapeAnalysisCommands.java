/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.dataexporters.DataExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.LandscapeVisualsGenerator;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.landscape.DefaultTags;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.SubLandscapeLink;
import nl.obren.sokrates.sourcecode.landscape.TagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalyzer;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.init.LandscapeAnalysisUpdater;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        // Virtual landscapes: generate a full report per virtual landscape (and a Remainder), then
        // register them as sub-landscapes so the parent's Sub-landscapes tab lists them.
        generateVirtualLandscapes(landscapeAnalysisResults, tagGroups, reportsFolder);

        ProcessingStopwatch.start("reporting");
        LandscapeReportGenerator reportGenerator = new LandscapeReportGenerator(landscapeAnalysisResults, tagGroups, landscapeConfigFile.getParentFile(), reportsFolder);
        exportLandscape(reportGenerator, landscapeAnalysisResults, reportsFolder, individualReportsFolder);
        ProcessingStopwatch.end("reporting");
    }

    /**
     * Renders and writes a single landscape (main reports + individual contributor/team/bot reports
     * + visuals) into {@code reportsFolder}. Individual contributor reports go into
     * {@code individualReportsFolder} (the landscape's {@code contributors/} folder).
     */
    private static void exportLandscape(LandscapeReportGenerator reportGenerator,
                                        LandscapeAnalysisResults landscapeAnalysisResults,
                                        File reportsFolder, File individualReportsFolder) {
        List<RichTextReport> reports = reportGenerator.report();
        try {
            ProcessingStopwatch.start("reporting/saving");
            ProcessingStopwatch.start("reporting/saving/reports");
            String customHtmlReportHeaderFragment = landscapeAnalysisResults.getConfiguration().getCustomHtmlReportHeaderFragment();
            reports.forEach(report -> {
                LOG.info("Exporting " + report.getFileName() + ".");
                ReportFileExporter.exportHtml(reportsFolder, "", report, customHtmlReportHeaderFragment);
            });
            ProcessingStopwatch.end("reporting/saving/reports");

            ProcessingStopwatch.start("reporting/saving/contributors");
            reportGenerator.getIndividualContributorReports().forEach(individualReport -> {
                LOG.info("Exporting person " + individualReport.getFileName() + ".");
                ReportFileExporter.exportHtml(individualReportsFolder, "", individualReport, customHtmlReportHeaderFragment);
            });
            reportGenerator.getIndividualTeamReports().forEach(individualReport -> {
                LOG.info("Exporting team " + individualReport.getFileName() + ".");
                ReportFileExporter.exportHtml(individualReportsFolder, "", individualReport, customHtmlReportHeaderFragment);
            });
            reportGenerator.getIndividualBotReports().forEach(individualReport -> {
                LOG.info("Exporting bot " + individualReport.getFileName() + ".");
                ReportFileExporter.exportHtml(individualReportsFolder, "", individualReport, customHtmlReportHeaderFragment);
            });
            ProcessingStopwatch.end("reporting/saving/contributors");

            ProcessingStopwatch.start("reporting/saving/generating visuals");
            LandscapeVisualsGenerator visualsGenerator = new LandscapeVisualsGenerator(reportsFolder, landscapeAnalysisResults.getTeamsConfig());
            visualsGenerator.exportVisuals(landscapeAnalysisResults);
            ProcessingStopwatch.end("reporting/saving/generating visuals");

            // Final step: package this landscape's data/ folder into data/data.zip and drop the
            // loose files. Done LAST so that any sub-landscape reads earlier in this run (which read
            // children's loose data) and the just-written report links still resolved; the parent
            // landscape and the data downloader read this zip (with a loose-file fallback).
            zipLandscapeDataFolder(reportsFolder);

            LOG.info("Report file: " + reportsFolder.getPath() + "/index.html");
            ProcessingStopwatch.end("reporting/saving");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Collapses the landscape's data/ folder into data/data.zip and removes the loose files
    // (mirrors DataExporter.zipDataFolder for repositories). Only landscapeAnalysisResults.json is
    // machine-read (by a parent landscape's Sub-landscapes tab, zip-or-loose); the rest are
    // download-only and served via the in-browser downloader. It MERGES: if data.zip already exists,
    // its entries are kept and any newly-written loose files (e.g. executionTimes written by the CLI
    // after the first packaging) are folded in. Safe to call more than once. Streamed byte-by-byte
    // (no entry held as a String), so it is safe even when a data file approaches the ~2 GB limit.
    public static void zipLandscapeDataFolder(File reportsFolder) {
        File dataFolder = new File(reportsFolder, "data");
        if (!dataFolder.exists()) {
            return;
        }
        try {
            File zipFile = new File(dataFolder, "data.zip");
            // Drop any preview from a prior call before merging so it is never folded into data.zip
            // (this method may run more than once); it is rewritten from the fresh zip below.
            FileUtils.deleteQuietly(new File(dataFolder, DataExporter.DATA_PREVIEW_FILE_NAME));
            ZipUtils.zipFolderMergingExistingZip(dataFolder, zipFile);

            File[] children = dataFolder.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.equals(zipFile)) {
                        continue;
                    }
                    if (child.isDirectory()) {
                        FileUtils.deleteDirectory(child);
                    } else {
                        FileUtils.deleteQuietly(child);
                    }
                }
            }

            DataExporter.writeDataPreview(dataFolder, zipFile);
        } catch (Exception e) {
            LOG.warn("Could not package landscape data folder: " + e.getMessage());
        }
    }

    /**
     * For each configured virtual landscape (and the Remainder), build a child analysis from the
     * parent's repositories, generate its full report under
     * {@code reportsFolder/landscapes/<safe-name>/_sokrates_landscape/}, write its config.json, and
     * register it as a (virtual) sub-landscape on the parent configuration. Virtual landscapes may
     * themselves define nested virtual landscapes (unlimited depth); these are generated recursively
     * and surfaced in each child's own Sub-landscapes tab.
     */
    private static void generateVirtualLandscapes(LandscapeAnalysisResults parentResults,
                                                  List<TagGroup> tagGroups, File reportsFolder) {
        VirtualLandscapeBuilder builder = new VirtualLandscapeBuilder(parentResults);
        if (!builder.hasVirtualLandscapes()) {
            return;
        }
        ProcessingStopwatch.start("reporting/virtual-landscapes");
        generateVirtualLandscapes(builder, parentResults.getConfiguration().getVirtualLandscapes(),
                parentResults.getRepositoryAnalysisResults(), parentResults.getConfiguration(),
                parentResults.getConfiguration(), tagGroups, reportsFolder, 1);
        ProcessingStopwatch.end("reporting/virtual-landscapes");
    }

    /**
     * Recursive worker for the public {@code generateVirtualLandscapes(...)} entry point.
     * Partitions {@code repositories} according to {@code virtualConfig}, generates a full report per
     * partition into {@code parentFolder/landscapes/<safe-name>/_sokrates_landscape/}, then recurses
     * into each partition's own nested virtual landscapes. {@code rootConfiguration} stays the
     * top-level config so relative path climbs are computed consistently; {@code depth} is the
     * current nesting level (1-based). Sub-landscape links are registered on {@code parentConfiguration}.
     */
    private static void generateVirtualLandscapes(VirtualLandscapeBuilder builder,
                                                  nl.obren.sokrates.sourcecode.landscape.VirtualLandscapesConfig virtualConfig,
                                                  List<RepositoryAnalysisResults> repositories,
                                                  LandscapeConfiguration rootConfiguration,
                                                  LandscapeConfiguration parentConfiguration,
                                                  List<TagGroup> tagGroups, File parentFolder, int depth) {
        // Start fresh so removed virtual landscapes do not linger.
        File landscapesFolder = new File(parentFolder, "landscapes");
        try {
            FileUtils.deleteDirectory(landscapesFolder);
        } catch (IOException e) {
            LOG.error(e);
        }
        landscapesFolder.mkdirs();

        builder.build(virtualConfig, repositories).forEach(virtualLandscape -> {
            String safeName = nl.obren.sokrates.common.utils.SystemUtils.getSafeFileName(virtualLandscape.getName());
            File childLandscapeFolder = new File(new File(landscapesFolder, safeName), "_sokrates_landscape");
            File childContributorsFolder = new File(childLandscapeFolder, "contributors");
            childContributorsFolder.mkdirs();

            LandscapeAnalysisResults childResults = virtualLandscape.getResults();
            nl.obren.sokrates.sourcecode.landscape.VirtualLandscapesConfig nested =
                    virtualLandscape.getConfig() != null ? virtualLandscape.getConfig().getVirtualLandscapes() : null;
            childResults.setConfiguration(VirtualLandscapeBuilder.childConfiguration(
                    rootConfiguration, virtualLandscape.getMetadata(), nested, depth));

            // Compute the contributor/team topology data (people dependencies, connections, history)
            // from the child's repositories, with its configuration now set so thresholds/anonymize
            // match. Without this the topology graphs are empty for virtual landscapes.
            LandscapeAnalyzer.updatePeopleDependencies(childResults);

            // Recurse into this virtual landscape's own nested virtual landscapes first, so their
            // reports + config.json exist (and links are registered on the child) before it renders.
            if (VirtualLandscapeBuilder.hasVirtualLandscapes(nested)) {
                generateVirtualLandscapes(builder, nested,
                        childResults.getRepositoryAnalysisResults(), rootConfiguration,
                        childResults.getConfiguration(), tagGroups, childLandscapeFolder, depth + 1);
            }

            try {
                FileUtils.write(new File(childLandscapeFolder, "config.json"),
                        new JsonGenerator().generate(childResults.getConfiguration()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.error(e);
            }

            LandscapeReportGenerator childGenerator = new LandscapeReportGenerator(childResults, tagGroups, childLandscapeFolder, childLandscapeFolder);
            exportLandscape(childGenerator, childResults, childLandscapeFolder, childContributorsFolder);

            // Register as a virtual sub-landscape of the parent (resolved relative to the parent's
            // _sokrates_landscape folder, without the repository-reports prefix).
            SubLandscapeLink link = new SubLandscapeLink(virtualLandscape.getName(),
                    "landscapes/" + safeName + "/_sokrates_landscape/index.html");
            link.setVirtual(true);
            link.setLabel(virtualLandscape.getName());
            parentConfiguration.getSubLandscapes().add(link);
        });
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

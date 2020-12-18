/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.dataexporters.DataExportUtils;
import nl.obren.sokrates.reports.utils.ScopesRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ConcernsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ConcernsReportGenerator {
    private static final Log LOG = LogFactory.getLog(ConcernsReportGenerator.class);
    private CodeAnalysisResults codeAnalysisResults;
    private int groupCounter = 0;
    private int concernCounter = 0;
    private RichTextReport report;

    public ConcernsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addConcernsToReport(RichTextReport report) {
        this.report = report;
        addIntro(report);

        addSummary(report);

        addConcernsGroup(report);
    }

    private void addSummary(RichTextReport report) {
        report.startSection("Overview", "");

        codeAnalysisResults.getConcernsAnalysisResults().forEach(concernsAnalysisResults -> {
            String group = concernsAnalysisResults.getKey();
            report.addLevel2Header(group.toUpperCase());
            report.startDiv("width: 100%; overflow-x: auto");
            if (concernsAnalysisResults.getConcerns().size() > 1) {
                concernsAnalysisResults.getConcerns().forEach(concern -> {
                    if (!isDerivedConcern(concern.getName())) {
                        int mainLoc = codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode();
                        int concernLoc = concern.getLinesOfCode();
                        double relativeConcernSizeInPerc = 100.0 * concernLoc / mainLoc;
                        String fileListPath = concern.getAspect().getFileSystemFriendlyName(DataExportUtils.getConcernFilePrefix(group));
                        String svg = getOverviewCodePercentageSvg(relativeConcernSizeInPerc,
                                concern.getFilesCount(), concernLoc, 400, 20, fileListPath, isDerivedConcern(concern.getName()));
                        report.startDiv("");
                        report.addContentInDiv(concern.getName());
                        report.addHtmlContent(svg);
                        report.endDiv();
                    }
                });
            }
            report.endDiv();
        });

        report.endSection();
    }

    private boolean isDerivedConcern(String name) {
        return name.trim().startsWith("- ")
                || name.equalsIgnoreCase("Unclassified")
                || name.equalsIgnoreCase("Multiple Classifications");
    }

    private void addConcernsGroup(RichTextReport report) {
        codeAnalysisResults.getConcernsAnalysisResults().forEach(concernsAnalysisResults -> {
            if (concernsAnalysisResults.getConcerns().size() > 1) {
                renderConcern(report, concernsAnalysisResults);
            } else {
                report.addParagraph("No concerns defined.");
            }
        });
    }

    private void addIntro(RichTextReport report) {
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("Features of interest are any aspects of a software system that can be identified through patterns in code.");
        report.addListItem("Features of interest provide you with a way to focus your attention on relevant parts of the codebase. Typical examples include, security, TODOs, logging.");
        report.addListItem("A feature of interest may be present in multiple files. Any source code file may be in zero or multiple features of interest.");
        report.endUnorderedList();
        report.endSection();
    }

    private void renderConcern(RichTextReport report, ConcernsAnalysisResults concernsAnalysisResults) {
        String key = concernsAnalysisResults.getKey();
        if (key.equalsIgnoreCase("Unclassified")) {
            return;
        }
        groupCounter++;
        report.startSection("" + groupCounter + " " + key.toUpperCase() + " Concerns", "");
        report.startUnorderedList();
        int count = concernsAnalysisResults.getConcerns().size();
        report.addListItem("The \"" + key + "\" group contains <b>"
                + count + "</b> concern" + (count > 1 ? "s" : "") + ".");

        report.startUnorderedList();
        concernsAnalysisResults.getConcerns().forEach(c -> report.addListItem(c.getName()));
        report.endUnorderedList();

        report.endUnorderedList();
        concernsAnalysisResults.getConcerns().forEach(aspectAnalysisResults -> {
            renderScopes(key, aspectAnalysisResults);
        });
        report.endSection();
    }

    private void renderScopes(String key, AspectAnalysisResults aspectAnalysisResults) {
        String name = aspectAnalysisResults.getName();
        if (name.equalsIgnoreCase("Multiple Classifications")) {
            return;
        }
        if (name.equalsIgnoreCase("Unclassified")) {
            return;
        }
        concernCounter++;
        List<NumericMetric> fileCountPerExtension = aspectAnalysisResults.getFileCountPerExtension();
        List<NumericMetric> linesOfCodePerExtension = aspectAnalysisResults.getLinesOfCodePerExtension();

        ScopesRenderer renderer = new ScopesRenderer();
        renderer.setLinesOfCodeInMain(codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode());

        String title = "<span style='color: grey; font-size: 90%'>" + key + "</span><br>";
        title += groupCounter + "." + concernCounter + " " + name.replace(" - ", " Multiple Classifications");
        renderer.setTitle(title);
        renderer.setDescription("");
        if (name.equalsIgnoreCase("Unclassified")) {
            renderer.setDescription("This concern include all files that are not included in any of the previously described concerns in this group.");
        }
        if (name.equalsIgnoreCase("Multiple Classifications")) {
            renderer.setDescription("This concern include all files that are included in two or more concerns in this group.");
        }

        renderer.setFileCountPerComponent(fileCountPerExtension);
        renderer.setAspect(aspectAnalysisResults.getAspect());
        renderer.setLinesOfCode(linesOfCodePerExtension);
        renderer.setMaxFileCount(codeAnalysisResults.getMainAspectAnalysisResults().getFilesCount());
        int mainLoc = codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        renderer.setMaxLinesOfCode(mainLoc);
        renderer.setTotalNumberOfRegexMatches(aspectAnalysisResults.getNumberOfRegexLineMatches());

        double relativeSizeInPerc = 100.0 * aspectAnalysisResults.getLinesOfCode() / mainLoc;
        int numberOfFiles = aspectAnalysisResults.getFilesCount();
        int linesOfCode = aspectAnalysisResults.getLinesOfCode();

        report.startSubSection(renderer.getTitle(), "");
        String fileListPath = aspectAnalysisResults.getAspect().getFileSystemFriendlyName(DataExportUtils.getConcernFilePrefix(key));
        report.addContentInDiv(getOverviewCodePercentageSvg(relativeSizeInPerc, numberOfFiles, linesOfCode,
                200, 20, fileListPath, isDerivedConcern(name)), fileListPath);

        if (name.contains(" - ") && name.contains(" AND ")) {
            List<Double> percentages = extractPercentages(name);
            if (percentages.size() == 2) {
                String svg = getOverlapSvg(percentages);
                report.addParagraph(svg);
            }
        } else {
            renderer.renderDetails(report, false);
        }

        report.startShowMoreBlock("details...");
        codeAnalysisResults.getLogicalDecompositionsAnalysisResults()
                .forEach(logicalDecompositionAnalysisResults -> {
                    renderConcernPerComponent(aspectAnalysisResults, mainLoc, logicalDecompositionAnalysisResults);
                });
        report.endShowMoreBlock();

        report.endSection();
    }

    private void renderConcernPerComponent(AspectAnalysisResults aspectAnalysisResults, int mainLoc, LogicalDecompositionAnalysisResults logicalDecompositionAnalysisResults) {
        report.addHorizontalLine();
        report.addLevel3Header("per component - " + logicalDecompositionAnalysisResults.getKey() + " logical decomposition");
        report.startDiv("width: 100%; overflow-x: auto");
        logicalDecompositionAnalysisResults.getComponents()
                .stream().sorted(Comparator.comparingInt(AspectAnalysisResults::getLinesOfCode).reversed())
                .forEach(component -> {
                    int componentLoc = component.getLinesOfCode();
                    int[] loc = {0};
                    int[] filesCount = {0};
                    aspectAnalysisResults.getAspect().getSourceFiles()
                            .forEach(sourceFile -> {
                                if (sourceFile.getLogicalComponents().contains(component.getAspect())) {
                                    loc[0] += sourceFile.getLinesOfCode();
                                    filesCount[0] += 1;
                                }
                            });
                    if (filesCount[0] > 0) {
                        double relativeComponentSizeInPerc = 100.0 * loc[0] / componentLoc;
                        String svg = getCodePercentageSvg(relativeComponentSizeInPerc,
                                component.getName(),
                                filesCount[0],
                                loc[0],
                                (int) ((double) 400 * componentLoc / mainLoc), 20);
                        report.startDiv("");
                        report.addHtmlContent(svg);
                        report.endDiv();
                    }
                });
        report.endDiv();
    }

    private String getCodePercentageSvg(double percentage, String aspectName, int numberOfFiles, int linesOfCode, int maxSize, int barHeight) {
        String displayText = "in " + numberOfFiles + (numberOfFiles == 1 ? " file " : " files, ")
                + " " + FormattingUtils.formatCount(linesOfCode) + " LOC ("
                + FormattingUtils.getFormattedPercentage(percentage) + "%)";

        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(900);
        chart.setMaxBarWidth(maxSize);
        chart.setBarHeight(barHeight);

        return chart.getPercentageSvg(percentage, aspectName, displayText);
    }

    private String getOverviewCodePercentageSvg(double percentage, int numberOfFiles, int linesOfCode, int maxSize, int barHeight, String fileListPath, boolean derivedConcern) {
        String filesFragment = numberOfFiles + (numberOfFiles == 1 ? " file " : " files");
        if (StringUtils.isNotBlank(fileListPath)) {
            filesFragment = "<u><a href='../data/text/aspect_" + fileListPath + ".txt'>" + filesFragment + "</a></u>";
            if (!derivedConcern) {
                filesFragment += " | <u><a href='../data/text/aspect_" + fileListPath + "_found_text.txt'>found text</a></u>";
                filesFragment += " | <u><a href='../data/text/aspect_" + fileListPath + "_found_text_per_file.txt'>found text per file</a></u>";
            }
        }
        String displayText = FormattingUtils.formatCount(linesOfCode) + " LOC ("
                + FormattingUtils.getFormattedPercentage(percentage) + "%) "
                + filesFragment;

        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(800);
        chart.setMaxBarWidth(maxSize);
        chart.setBarHeight(barHeight);
        chart.setBarStartXOffset(2);

        return chart.getPercentageSvg(percentage, "", displayText);
    }

    private String getOverlapSvg(List<Double> percentages) {
        Double perc1 = percentages.get(0);
        Double perc2 = percentages.get(1);
        double size1 = perc1 <= perc2 ? 100 : 100 * (perc2 / perc1);
        double size2 = perc1 > perc2 ? 100 : 100 * (perc1 / perc2);
        String svg = "<svg width='220' height='40'>";
        svg += "<rect width='"
                + (int) (size1) + "' x='2' y='2' height='20' style='fill-opacity:0.4;fill:#c0deed;stroke:#898989;stroke-width:2;' />";
        svg += "<rect x='" + (2 + (int) (size1 * (100 - perc1) / 100.0)) + "' y='6' width='"
                + (int) (size2) + "' height='20' style='fill-opacity:0.4;fill:#c0deed;fill:#c0deed;stroke:#898989;stroke-width:2;' />";
        svg += "</svg>";
        return svg;
    }

    private List<Double> extractPercentages(String name) {
        List<Double> percentages = new ArrayList<>();
        try {
            Pattern soe = Pattern.compile("\\(.*?\\%\\)");
            Matcher matcher = soe.matcher(name);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String strValue = name.substring(start + 1, end - 2);
                if (NumberUtils.isParsable(strValue)) {
                    percentages.add(Double.parseDouble(strValue));
                }
            }
        } catch (PatternSyntaxException e) {
            LOG.debug(e);
        } catch (StackOverflowError e) {
            LOG.error(e);
        }

        return percentages;
    }
}

/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.data.LandscapeDataExport;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.*;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LandscapeReportGenerator {
    public static final int RECENT_THRESHOLD_DAYS = 31;
    private static final Log LOG = LogFactory.getLog(LandscapeReportGenerator.class);
    private RichTextReport landscapeReport = new RichTextReport("Landscape Report", "index.html");
    private RichTextReport landscapeProjectsReport = new RichTextReport("", "projects.html");
    private RichTextReport landscapeRecentContributorsReport = new RichTextReport("", "contributors-recent.html");
    private RichTextReport landscapeContributorsReport = new RichTextReport("", "contributors.html");
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private int dependencyVisualCounter = 1;
    private File folder;
    private File reportsFolder;

    public LandscapeReportGenerator(LandscapeAnalysisResults landscapeAnalysisResults, File folder, File reportsFolder) {
        this.folder = folder;
        this.reportsFolder = reportsFolder;
        landscapeProjectsReport.setEmbedded(true);
        landscapeContributorsReport.setEmbedded(true);
        landscapeRecentContributorsReport.setEmbedded(true);
        LandscapeDataExport dataExport = new LandscapeDataExport(landscapeAnalysisResults, folder);
        dataExport.exportProjects();
        dataExport.exportContributors();
        dataExport.exportAnalysisResults();

        Metadata metadata = landscapeAnalysisResults.getConfiguration().getMetadata();
        String landscapeName = metadata.getName();
        if (StringUtils.isNotBlank(landscapeName)) {
            landscapeReport.setDisplayName(landscapeName);
        }
        landscapeReport.setParentUrl(landscapeAnalysisResults.getConfiguration().getParentUrl());
        landscapeReport.setLogoLink(metadata.getLogoLink());
        String description = metadata.getDescription();
        String tooltip = metadata.getTooltip();
        if (StringUtils.isNotBlank(description)) {
            if (StringUtils.isBlank(tooltip)) {
                landscapeReport.addParagraph(description, "font-size: 90%; color: #787878; margin-top: 8px; margin-bottom: 5px;");
            }
            if (StringUtils.isNotBlank(tooltip)) {
                landscapeReport.addParagraphWithTooltip(description, tooltip, "font-size: 90%; color: #787878; margin-top: 8px;");
            }
        }
        if (metadata.getLinks().size() > 0) {
            landscapeReport.startDiv("font-size: 80%; margin-top: 2px;");
            boolean first[] = {true};
            metadata.getLinks().forEach(link -> {
                if (!first[0]) {
                    landscapeReport.addHtmlContent(" | ");
                }
                landscapeReport.addNewTabLink(link.getLabel(), link.getHref());
                first[0] = false;
            });
            landscapeReport.endDiv();
        }
        this.landscapeAnalysisResults = landscapeAnalysisResults;

        landscapeReport.addLineBreak();
        landscapeReport.startTabGroup();
        landscapeReport.addTab("overview", "Overview", true);
        landscapeReport.addTab("source code", "Projects", false);
        landscapeReport.addTab("commits", "Contributors", false);
        landscapeReport.endTabGroup();

        landscapeReport.startTabContentSection("overview", true);
        addBigSummary(landscapeAnalysisResults);
        addSubLandscapeSection(landscapeAnalysisResults.getConfiguration().getSubLandscapes());
        landscapeReport.endTabContentSection();
        landscapeReport.startTabContentSection("source code", false);
        addBigProjectsSummary(landscapeAnalysisResults);
        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesProjectsAtStart());
        addExtensions();
        addProjectsSection(getProjects());
        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesProjects());
        landscapeReport.endTabContentSection();

        landscapeReport.startTabContentSection("commits", false);
        addBigContributorsSummary(landscapeAnalysisResults);
        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesContributorsAtStart());
        addContributors();
        addContributorsPerExtension();
        addPeopleDependencies();
        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesContributors());
        landscapeReport.endTabContentSection();
        landscapeReport.addParagraph("<span style='color: grey; font-size: 90%'>updated: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "</span>");
    }

    private void addPeopleDependencies() {
        List<ContributorProjects> contributors = landscapeAnalysisResults.getContributors();

        landscapeReport.startSubSection("People Dependencies", "");

        List<ComponentDependency> peopleDependencies30Days = landscapeAnalysisResults.getPeopleDependencies30Days();
        List<ContributorConnections> connectionsViaProjects30Days = landscapeAnalysisResults.getConnectionsViaProjects30Days();
        this.renderPeopleDependencies(peopleDependencies30Days, connectionsViaProjects30Days,
                landscapeAnalysisResults.getcIndex30Days(), landscapeAnalysisResults.getpIndex30Days(),
                landscapeAnalysisResults.getcMean30Days(), landscapeAnalysisResults.getpMean30Days(),
                landscapeAnalysisResults.getcMedian30Days(), landscapeAnalysisResults.getpMedian30Days(),
                30);

        List<ComponentDependency> peopleDependencies90Days = landscapeAnalysisResults.getPeopleDependencies90Days();
        List<ContributorConnections> connectionsViaProjects90Days = landscapeAnalysisResults.getConnectionsViaProjects90Days();
        this.renderPeopleDependencies(peopleDependencies90Days, connectionsViaProjects90Days,
                landscapeAnalysisResults.getcIndex90Days(), landscapeAnalysisResults.getpIndex90Days(),
                landscapeAnalysisResults.getcMean90Days(), landscapeAnalysisResults.getpMean90Days(),
                landscapeAnalysisResults.getcMedian90Days(), landscapeAnalysisResults.getpMedian90Days(),
                90);

        List<ComponentDependency> peopleDependencies180Days = landscapeAnalysisResults.getPeopleDependencies180Days();
        List<ContributorConnections> connectionsViaProjects180Days = landscapeAnalysisResults.getConnectionsViaProjects180Days();
        this.renderPeopleDependencies(peopleDependencies180Days, connectionsViaProjects180Days,
                landscapeAnalysisResults.getcIndex180Days(), landscapeAnalysisResults.getpIndex180Days(),
                landscapeAnalysisResults.getcMean180Days(), landscapeAnalysisResults.getpMean180Days(),
                landscapeAnalysisResults.getcMedian180Days(), landscapeAnalysisResults.getpMedian180Days(),
                180);

        landscapeReport.endSection();
    }

    private List<ProjectAnalysisResults> getProjects() {
        return landscapeAnalysisResults.getFilteredProjectAnalysisResults();
    }

    private void addSubLandscapeSection(List<SubLandscapeLink> subLandscapes) {
        List<SubLandscapeLink> links = new ArrayList<>(subLandscapes);
        if (links.size() > 0) {
            Collections.sort(links, Comparator.comparing(a -> getLabel(a).toLowerCase()));
            landscapeReport.startSubSection("Sub-Landscapes (" + links.size() + ")", "");

            landscapeReport.startTable();
            landscapeReport.addTableHeader("", "main loc", "test loc", "other loc", "projects", "recent contributors", "commits (30 days)");
            String prevRoot[] = {""};
            links.forEach(subLandscape -> {
                String label = StringUtils.removeEnd(getLabel(subLandscape), "/");
                String style = "";
                String root = label.replaceAll("/.*", "");
                if (!prevRoot[0].equals(root)) {
                    label = "<b>" + label + "</b>";
                    style = "color: black; font-weight: bold;";
                } else {
                    int lastIndex = label.lastIndexOf("/");
                    label = "<span style='color: lightgrey'>" + label.substring(0, lastIndex + 1) + "</span>" + label.substring(lastIndex + 1) + "";
                    style = "color: grey; font-size: 90%";
                }
                landscapeReport.startTableRow(style);
                landscapeReport.startTableCell();
                String href = landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + subLandscape.getIndexFilePath();
                landscapeReport.addNewTabLink(label, href);
                LandscapeAnalysisResultsReadData subLandscapeAnalysisResults = getSubLandscapeAnalysisResults(subLandscape);
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getMainLoc()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getTestLoc()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    int other = subLandscapeAnalysisResults.getBuildAndDeploymentLoc()
                            + subLandscapeAnalysisResults.getGeneratedLoc() + subLandscapeAnalysisResults.getOtherLoc();
                    landscapeReport.addHtmlContent("<span style='color: lightgrey'>" + FormattingUtils.formatCount(other) + "</span>");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getProjectsCount()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getRecentContributorsCount()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.startTableCell("text-align: right;");
                if (subLandscapeAnalysisResults != null) {
                    landscapeReport.addHtmlContent(FormattingUtils.formatCount(subLandscapeAnalysisResults.getCommitsCount30Days()) + "");
                }
                landscapeReport.endTableCell();
                landscapeReport.endTableRow();

                prevRoot[0] = root;
            });
            landscapeReport.endTable();

            landscapeReport.endSection();
        }

    }

    private LandscapeAnalysisResultsReadData getSubLandscapeAnalysisResults(SubLandscapeLink subLandscape) {
        try {
            String prefix = landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix();
            File resultsFile = new File(new File(folder, prefix + subLandscape.getIndexFilePath()).getParentFile(), "data/landscapeAnalysisResults.json");
            System.out.println(resultsFile.getPath());
            String json = FileUtils.readFileToString(resultsFile, StandardCharsets.UTF_8);
            return (LandscapeAnalysisResultsReadData) new JsonMapper().getObject(json, LandscapeAnalysisResultsReadData.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getLabel(SubLandscapeLink subLandscape) {
        return subLandscape.getIndexFilePath()
                .replaceAll("(/|\\\\)_sokrates_landscape(/|\\\\).*", "");
    }

    private void addBigSummary(LandscapeAnalysisResults landscapeAnalysisResults) {
        landscapeReport.startDiv("margin-top: 0px;");
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int thresholdContributors = configuration.getProjectThresholdContributors();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(getProjects().size()), "projects",
                "", "active project with " + (thresholdContributors > 1 ? "(" + thresholdContributors + "+&nbsp;contributors)" : ""));
        int extensionsCount = getLinesOfCodePerExtension().size();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getMainLoc()), "lines of code (main)", "", getExtraLocInfo());
        int mainLocActive = landscapeAnalysisResults.getMainLocActive();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(mainLocActive), "lines of code (active)", "", "files updated in past year");
        int mainLocNew = landscapeAnalysisResults.getMainLocNew();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(mainLocNew), "lines of code (new)", "", "files created in past year");

        List<ContributorProjects> contributors = landscapeAnalysisResults.getContributors();
        long contributorsCount = contributors.size();
        if (contributorsCount > 0) {
            int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount();
            int locPerRecentContributor = 0;
            int locNewPerRecentContributor = 0;
            if (recentContributorsCount > 0) {
                locPerRecentContributor = (int) Math.round((double) mainLocActive / recentContributorsCount);
                locNewPerRecentContributor = (int) Math.round((double) mainLocNew / recentContributorsCount);
            }
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(recentContributorsCount), "recent contributors",
                    "(past 30 days)", getExtraPeopleInfo(contributors, contributorsCount) + "\n" + FormattingUtils.formatCount(locPerRecentContributor) + " active lines of code per recent contributor");
            int rookiesContributorsCount = landscapeAnalysisResults.getRookiesContributorsCount();
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(rookiesContributorsCount),
                    rookiesContributorsCount == 1 ? "active rookie" : "active rookies",
                    "(started in past year)", "active contributors with the first commit in past year");
        }

        addContributorsPerYear(false);

        landscapeReport.addLineBreak();

        if (configuration.getCustomMetrics().size() > 0) {
            configuration.getCustomMetrics().forEach(customMetric -> addCustomInfoBlock(customMetric));
            landscapeReport.addLineBreak();
        }

        if (configuration.getCustomMetricsSmall().size() > 0) {
            configuration.getCustomMetricsSmall().forEach(customMetric -> {
                addSmallInfoBlock(customMetric.getValue(), customMetric.getTitle(), customMetric.getColor(), customMetric.getLink());
            });
        }

        landscapeReport.endDiv();
        landscapeReport.addLineBreak();

        addIFrames(configuration.getiFrames());
        addCustomTags(configuration);
    }

    private void addBigProjectsSummary(LandscapeAnalysisResults landscapeAnalysisResults) {
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int thresholdContributors = configuration.getProjectThresholdContributors();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(getProjects().size()), "projects",
                "", "active project with " + (thresholdContributors > 1 ? "(" + thresholdContributors + "+&nbsp;contributors)" : ""));
        int extensionsCount = getLinesOfCodePerExtension().size();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getMainLoc()), "lines of code (main)", "", getExtraLocInfo());
        int mainLocActive = landscapeAnalysisResults.getMainLocActive();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(mainLocActive), "lines of code (active)", "", "files updated in past year");
        int mainLocNew = landscapeAnalysisResults.getMainLocNew();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(mainLocNew), "lines of code (new)", "", "files created in past year");
    }

    private void addBigContributorsSummary(LandscapeAnalysisResults landscapeAnalysisResults) {
        List<ContributorProjects> contributors = landscapeAnalysisResults.getContributors();
        long contributorsCount = contributors.size();
        int mainLocActive = landscapeAnalysisResults.getMainLocActive();
        int mainLocNew = landscapeAnalysisResults.getMainLocNew();
        if (contributorsCount > 0) {
            int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount();
            int locPerRecentContributor = 0;
            int locNewPerRecentContributor = 0;
            if (recentContributorsCount > 0) {
                locPerRecentContributor = (int) Math.round((double) mainLocActive / recentContributorsCount);
                locNewPerRecentContributor = (int) Math.round((double) mainLocNew / recentContributorsCount);
            }
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(recentContributorsCount), "recent contributors",
                    "(past 30 days)", getExtraPeopleInfo(contributors, contributorsCount) + "\n" + FormattingUtils.formatCount(locPerRecentContributor) + " active lines of code per recent contributor");
            int rookiesContributorsCount = landscapeAnalysisResults.getRookiesContributorsCount();
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(rookiesContributorsCount),
                    rookiesContributorsCount == 1 ? "active rookie" : "active rookies",
                    "(started in past year)", "active contributors with the first commit in past year");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(locPerRecentContributor), "contributor load",
                    "(active LOC/contributor)", "active lines of code per recent contributor\n\n" + FormattingUtils.getPlainTextForNumber(locNewPerRecentContributor) + " new LOC/recent contributor");
            List<ComponentDependency> peopleDependencies = ContributorConnectionUtils.getPeopleDependencies(contributors, 0, 30);
            peopleDependencies.sort((a, b) -> b.getCount() - a.getCount());

            double cIndex = landscapeAnalysisResults.getcIndex30Days();
            double pIndex = landscapeAnalysisResults.getpIndex30Days();
            double cMean = landscapeAnalysisResults.getcMean30Days();
            double pMean = landscapeAnalysisResults.getpMean30Days();
            double cMedian = landscapeAnalysisResults.getcMedian30Days();
            double pMedian = landscapeAnalysisResults.getpMedian30Days();

            int connectionSum = landscapeAnalysisResults.getConnectionsViaProjects30Days().stream().mapToInt(c -> c.getConnectionsCount()).sum();
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(peopleDependencies.size()), "C2C connections", "30 days", "");
            // addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(connectionSum), "all C2C connections", "30 days", "");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) Math.round(cMedian)), "C-Median", "30 days", "");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) Math.round(cMean)), "C-Mean", "30 days", "");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) Math.round(cIndex)), "C-Index",
                    "30 days", "" + (int) Math.round(cIndex) + " active contributes connected to " + (int) Math.round(cIndex) + " or more of other contributers via commits to shared projects in past 30 days.");
        }

        addContributorsPerYear(true);
        addContributorsPerWeek();
        landscapeReport.addParagraph("latest commit date: <b>" + landscapeAnalysisResults.getLatestCommitDate() + "</b>", "color: grey");
    }

    private void addIFrames(List<WebFrameLink> iframes) {
        if (iframes.size() > 0) {
            iframes.forEach(iframe -> {
                addIFrame(iframe);
            });
        }
    }

    private void addIFrame(WebFrameLink iframe) {
        if (StringUtils.isNotBlank(iframe.getTitle())) {
            String title;
            if (StringUtils.isNotBlank(iframe.getMoreInfoLink())) {
                title = "<a href='" + iframe.getMoreInfoLink() + "' target='_blank' style='text-decoration: none'>" + iframe.getTitle() + "</a>";
            } else {
                title = iframe.getTitle();
            }
            landscapeReport.startSubSection(title, "");
        }
        String style = StringUtils.defaultIfBlank(iframe.getStyle(), "width: 100%; height: 200px; border: 1px solid lightgrey;");
        landscapeReport.addHtmlContent("<iframe src='" + iframe.getSrc()
                + "' frameborder='0' style='" + style + "'"
                + (iframe.getScrolling() ? "" : " scrolling='no' ")
                + "></iframe>");
        if (StringUtils.isNotBlank(iframe.getTitle())) {
            landscapeReport.endSection();
        }
    }

    private void addCustomTags(LandscapeConfiguration configuration) {
        if (configuration.getTags().getGroups().size() > 0) {
            landscapeReport.startDiv("margin-bottom: 20px");
            configuration.getTags().getGroups().forEach(tagGroup -> {
                renderTagGroup(configuration, tagGroup);
            });
            landscapeReport.endDiv();
        }
    }

    private void renderTagGroup(LandscapeConfiguration configuration, CustomTagGroup tagGroup) {
        if (anyTagsPresent(tagGroup)) {
            landscapeReport.startDiv("display: inline-block; border: 1px solid lightgrey; padding: 6px; border-radius: 6px;");
            landscapeReport.addParagraph(tagGroup.getName(), "font-size: 70%; color: grey;");
            tagGroup.getTags().forEach(tag -> {
                renderTag(configuration, tag);
            });
            tagGroup.getSubGroups().forEach(subGroup -> {
                renderTagGroup(configuration, subGroup);
            });
            landscapeReport.endDiv();
        }
    }

    private boolean anyTagsPresent(CustomTagGroup tagGroup) {
        if (tagGroup.getTags().size() > 0) {
            return true;
        }

        for (CustomTagGroup subGroup : tagGroup.getSubGroups()) {
            if (anyTagsPresent(subGroup)) {
                return true;
            }
        }

        return false;
    }

    private void renderTag(LandscapeConfiguration configuration, CustomTag tag) {
        String logoSrc = configuration.getTags().getLogosRoot() + tag.getIcon();
        landscapeReport.startSpan("position: relative;");
        String imageStyle = "width: 80px; height: 60px; object-fit: contain;";
        String title = tag.getTitle();
        if (StringUtils.isNotBlank(tag.getDescription())) {
            title += "\n\n" + tag.getDescription();
        }
        if (StringUtils.isNotBlank(tag.getLink())) {
            landscapeReport.addHtmlContent("<a target='_blank' href='" + tag.getLink() + "'>");
        }
        landscapeReport.addHtmlContent("<img src='" + logoSrc + "' title='" + title + "' style='" + imageStyle + "'>");
        if (StringUtils.isNotBlank(tag.getMark())) {
            landscapeReport.addHtmlContent("<span style='border: 1px solid lightgrey; font-size: 80%; background-color: yellow; position: absolute; top: -44px; left: 0px;'>&nbsp;" + tag.getMark() + "&nbsp;</span>");
        }
        if (StringUtils.isNotBlank(tag.getLink())) {
            landscapeReport.addHtmlContent("</a>");
        }
        landscapeReport.endSpan();
    }

    private void addExtensions() {
        int threshold = landscapeAnalysisResults.getConfiguration().getExtensionThresholdLoc();

        List<NumericMetric> linesOfCodePerExtension = getLinesOfCodePerExtension();
        landscapeReport.startSubSection("File Extensions in Main Code (" + linesOfCodePerExtension.size() + ")",
                threshold >= 1 ? threshold + "+ lines of code" : "");
        landscapeReport.startDiv("");
        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_extensions.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_extensions.html");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
        landscapeReport.endDiv();
        landscapeReport.startDiv("");
        boolean tooLong = linesOfCodePerExtension.size() > 25;
        List<NumericMetric> linesOfCodePerExtensionDisplay = tooLong ? linesOfCodePerExtension.subList(0, 25) : linesOfCodePerExtension;
        List<NumericMetric> linesOfCodePerExtensionHide = tooLong ? linesOfCodePerExtension.subList(25, linesOfCodePerExtension.size()) : new ArrayList<>();
        linesOfCodePerExtensionDisplay.forEach(extension -> {
            String smallTextForNumber = FormattingUtils.getSmallTextForNumber(extension.getValue().intValue());
            addSmallInfoBlockLoc(smallTextForNumber, extension.getName().replace("*.", ""), null);
        });
        if (linesOfCodePerExtensionHide.size() > 0) {
            landscapeReport.startShowMoreBlockDisappear("", "show all...");
            linesOfCodePerExtensionHide.forEach(extension -> {
                String smallTextForNumber = FormattingUtils.getSmallTextForNumber(extension.getValue().intValue());
                addSmallInfoBlockLoc(smallTextForNumber, extension.getName().replace("*.", ""), null);
            });
            landscapeReport.endShowMoreBlock();
        }
        landscapeReport.endDiv();
        landscapeReport.endSection();
    }

    private List<NumericMetric> getLinesOfCodePerExtension() {
        int threshold = landscapeAnalysisResults.getConfiguration().getExtensionThresholdLoc();
        return landscapeAnalysisResults.getLinesOfCodePerExtension().stream()
                .filter(e -> !e.getName().endsWith("="))
                .filter(e -> !e.getName().startsWith("h-"))
                .filter(e -> e.getValue().intValue() >= threshold)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void addContributors() {
        int contributorsCount = landscapeAnalysisResults.getContributorsCount();

        if (contributorsCount > 0) {
            List<ContributorProjects> contributors = landscapeAnalysisResults.getContributors();
            List<ContributorProjects> recentContributors = landscapeAnalysisResults.getContributors().stream()
                    .filter(c -> c.getContributor().getCommitsCount30Days() > 0)
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.sort(recentContributors, (a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days());
            int totalCommits = contributors.stream().mapToInt(c -> c.getContributor().getCommitsCount()).sum();
            int totalRecentCommits = recentContributors.stream().mapToInt(c -> c.getContributor().getCommitsCount30Days()).sum();
            final String[] latestCommit = {""};
            contributors.forEach(c -> {
                if (c.getContributor().getLatestCommitDate().compareTo(latestCommit[0]) > 0) {
                    latestCommit[0] = c.getContributor().getLatestCommitDate();
                }
            });

            landscapeReport.startSubSection("<a href='contributors-recent.html' target='_blank' style='text-decoration: none'>" +
                            "Recent Contributors (" + recentContributors.size() + ")</a>",
                    "latest commit " + latestCommit[0]);
            addRecentContributorLinks();

            landscapeReport.addHtmlContent("<iframe src='contributors-recent.html' frameborder=0 style='height: 450px; width: 100%; margin-bottom: 0px; padding: 0;'></iframe>");

            landscapeReport.endSection();

            landscapeReport.startSubSection("<a href='contributors.html' target='_blank' style='text-decoration: none'>" +
                            "All Contributors (" + contributorsCount + ")</a>",
                    "latest commit " + latestCommit[0]);

            landscapeReport.startShowMoreBlock("show details...");
            addContributorLinks();

            landscapeReport.addHtmlContent("<iframe src='contributors.html' frameborder=0 style='height: 450px; width: 100%; margin-bottom: 0px; padding: 0;'></iframe>");

            landscapeReport.endShowMoreBlock();
            landscapeReport.endSection();

            new LandscapeContributorsReport(landscapeAnalysisResults, landscapeRecentContributorsReport).saveContributorsTable(recentContributors, totalRecentCommits, true);
            new LandscapeContributorsReport(landscapeAnalysisResults, landscapeContributorsReport).saveContributorsTable(contributors, totalCommits, false);
        }

    }

    private void addContributorsPerExtension() {
        int commitsCount = landscapeAnalysisResults.getCommitsCount();
        if (commitsCount > 0) {
            List<CommitsPerExtension> perExtension = landscapeAnalysisResults.getContributorsPerExtension();

            if (perExtension.size() > 0) {
                landscapeReport.startSubSection("Commits & File Extensions (" + perExtension.size() + ")", "");

                landscapeReport.startShowMoreBlock("show details...");

                landscapeReport.startTable("width: 100%");
                landscapeReport.addTableHeader("Extension",
                        "# contributors<br>30 days", "# commits<br>30 days", "# files<br>30 days",
                        "# contributors<br>90 days", "# commits<br>90 days", "# files<br>90 days",
                        "# contributors", "# commits", "# files");

                perExtension.forEach(commitsPerExtension -> {
                    addCommitExtension(commitsPerExtension);
                });
                landscapeReport.endTable();

                landscapeReport.endShowMoreBlock();

                landscapeReport.endSection();
            }
        }
    }

    private void addContributorLinks() {
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_contributors.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_contributors.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/contributors.txt");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
    }

    private void addRecentContributorLinks() {
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_contributors_30_days.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_contributors_30_days.html");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
    }

    private void addCommitExtension(CommitsPerExtension commitsPerExtension) {
        landscapeReport.startTableRow(commitsPerExtension.getCommitters30Days().size() > 0 ? "font-weight: bold;"
                : "color: " + (commitsPerExtension.getCommitters90Days().size() > 0 ? "grey" : "lightgrey"));
        landscapeReport.addTableCell("" + commitsPerExtension.getExtension(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters30Days().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount30Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount30Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters90Days().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount90Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount90Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount(), "text-align: center;");
        landscapeReport.endTableCell();
        landscapeReport.endTableRow();
    }

    private void addProjectsSection(List<ProjectAnalysisResults> projectsAnalysisResults) {
        Collections.sort(projectsAnalysisResults, (a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
        landscapeReport.startSubSection("<a href='projects.html' target='_blank' style='text-decoration: none'>Projects (" + projectsAnalysisResults.size() + ")</a>", "");
        if (projectsAnalysisResults.size() > 0) {
            List<NumericMetric> projectSizes = new ArrayList<>();
            projectsAnalysisResults.forEach(projectAnalysisResults -> {
                CodeAnalysisResults analysisResults = projectAnalysisResults.getAnalysisResults();
                projectSizes.add(new NumericMetric(analysisResults.getMetadata().getName(), analysisResults.getMainAspectAnalysisResults().getLinesOfCode()));
            });
            landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
            landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_projects.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("tree map", "visuals/tree_map_projects.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("data", "data/projects.txt");
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();

            landscapeReport.addHtmlContent("<iframe src='projects.html' frameborder=0 style='height: 450px; width: 100%; margin-bottom: 0px; padding: 0;'></iframe>");

            new LandscapeProjectsReport(landscapeAnalysisResults).saveProjectsReport(landscapeProjectsReport, projectsAnalysisResults);
        }

        landscapeReport.endSection();
    }

    private void addCustomInfoBlock(CustomMetric customMetric) {
        String subtitle = customMetric.getTitle();
        if (StringUtils.isNotBlank(customMetric.getSubTitle())) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + customMetric.getSubTitle() + "</span>";
        }
        String color = StringUtils.isNotBlank(customMetric.getColor()) ? customMetric.getColor() : "lightgrey";
        addInfoBlockWithColor(customMetric.getValue(), subtitle, color, "");
    }

    private void addInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, "skyblue", tooltip);
    }

    private String getExtraLocInfo() {
        String info = "";

        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getMainLoc()) + " LOC (main)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getTestLoc()) + " LOC (test)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getGeneratedLoc()) + " LOC (generated)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getBuildAndDeploymentLoc()) + " LOC (build and deployment)\n";
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getOtherLoc()) + " LOC (other)";

        return info;
    }

    private String getExtraPeopleInfo(List<ContributorProjects> contributors, long contributorsCount) {
        String info = "";

        int recentContributorsCount6Months = landscapeAnalysisResults.getRecentContributorsCount6Months();
        int recentContributorsCount3Months = landscapeAnalysisResults.getRecentContributorsCount3Months();
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getRecentContributorsCount()) + " contributors (30 days)\n";
        info += FormattingUtils.getPlainTextForNumber(recentContributorsCount3Months) + " contributors (3 months)\n";
        info += FormattingUtils.getPlainTextForNumber(recentContributorsCount6Months) + " contributors (6 months)\n";

        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int thresholdCommits = configuration.getContributorThresholdCommits();
        info += FormattingUtils.getPlainTextForNumber((int) contributorsCount) + " contributors (all time)\n";
        info += "\nOnly the contributors with " + (thresholdCommits > 1 ? "(" + thresholdCommits + "+&nbsp;commits)" : "") + " included";

        return info;
    }

    private void addPeopleInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, "lavender", tooltip);
    }

    private void addCommitsInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, "#fefefe", tooltip);
    }

    private void addInfoBlockWithColor(String mainValue, String subtitle, String color, String tooltip) {
        String style = "border-radius: 12px;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 160px; height: 120px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 36px;";

        landscapeReport.startDiv(style, tooltip);
        landscapeReport.addHtmlContent("<div style='font-size: 50px; margin-top: 20px'>" + mainValue + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 16px'>" + subtitle + "</div>");
        landscapeReport.endDiv();
    }

    private void addSmallInfoBlockLoc(String value, String subtitle, String link) {
        addSmallInfoBlock(value, subtitle, "skyblue", link);
    }

    private void addSmallInfoBlockPeople(String value, String subtitle, String link) {
        addSmallInfoBlock(value, subtitle, "lavender", link);
    }

    private void addSmallInfoBlock(String value, String subtitle, String color, String link) {
        String style = "border-radius: 8px;";

        style += "margin: 4px 4px 4px 0px;";
        style += "display: inline-block; width: 80px; height: 76px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 16px;";

        landscapeReport.startDiv(style);
        if (StringUtils.isNotBlank(link)) {
            landscapeReport.startNewTabLink(link, "text-decoration: none");
        }
        landscapeReport.addHtmlContent("<div style='font-size: 24px; margin-top: 8px;'>" + value + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 13px'>" + subtitle + "</div>");
        if (StringUtils.isNotBlank(link)) {
            landscapeReport.endNewTabLink();
        }
        landscapeReport.endDiv();
    }

    public List<RichTextReport> report() {
        List<RichTextReport> reports = new ArrayList<>();

        reports.add(this.landscapeReport);
        reports.add(this.landscapeProjectsReport);
        reports.add(this.landscapeContributorsReport);
        reports.add(this.landscapeRecentContributorsReport);

        return reports;
    }

    private void addContributorsPerYear(boolean showContributorsCount) {
        List<ContributionTimeSlot> contributorsPerYear = landscapeAnalysisResults.getContributorsPerYear();
        if (contributorsPerYear.size() > 0) {
            int limit = landscapeAnalysisResults.getConfiguration().getCommitsMaxYears();
            if (contributorsPerYear.size() > limit) {
                contributorsPerYear = contributorsPerYear.subList(0, limit);
            }

            int maxCommits = contributorsPerYear.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);

            landscapeReport.startTable();

            landscapeReport.startTableRow();
            landscapeReport.startTableCell("border: none; height: 100px");
            int commitsCount = landscapeAnalysisResults.getCommitsCount();
            if (commitsCount > 0) {
                landscapeReport.startDiv("max-height: 105px");
                addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(commitsCount), "commits", "white", "");
                landscapeReport.endDiv();
            }
            landscapeReport.endTableCell();
            String style = "border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px";
            int thisYear = Calendar.getInstance().get(Calendar.YEAR);
            contributorsPerYear.forEach(year -> {
                landscapeReport.startTableCell(style);
                int count = year.getCommitsCount();
                String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
                landscapeReport.addParagraph(count + "", "margin: 2px; color: " + color);
                int height = 1 + (int) (64.0 * count / maxCommits);
                String bgColor = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "lightgrey";
                landscapeReport.addHtmlContent("<div style='width: 100%; background-color: " + bgColor + "; height:" + height + "px'></div>");
                landscapeReport.endTableCell();
            });
            landscapeReport.endTableRow();

            if (showContributorsCount) {
                int maxContributors[] = {1};
                contributorsPerYear.forEach(year -> {
                    int count = getContributorsCountPerYear(year.getTimeSlot());
                    maxContributors[0] = Math.max(maxContributors[0], count);
                });
                landscapeReport.startTableRow();
                landscapeReport.startTableCell("border: none; height: 100px");
                int contributorsCount = landscapeAnalysisResults.getContributors().size();
                if (contributorsCount > 0) {
                    landscapeReport.startDiv("max-height: 105px");
                    addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(contributorsCount), "contributors", "white", "");
                    landscapeReport.endDiv();
                }
                landscapeReport.endTableCell();
                contributorsPerYear.forEach(year -> {
                    landscapeReport.startTableCell(style);
                    int count = getContributorsCountPerYear(year.getTimeSlot());
                    String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
                    landscapeReport.addParagraph(count + "", "margin: 2px; color: " + color + ";");
                    int height = 1 + (int) (64.0 * count / maxContributors[0]);
                    landscapeReport.addHtmlContent("<div style='width: 100%; background-color: skyblue; height:" + height + "px'></div>");
                    landscapeReport.endTableCell();
                });
                landscapeReport.endTableRow();
            }

            landscapeReport.startTableRow();
            landscapeReport.addTableCell("", "border: none; ");
            contributorsPerYear.forEach(year -> {
                String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
                landscapeReport.addTableCell(year.getTimeSlot(), "border: none; text-align: center; font-size: 90%; color: " + color);
            });
            landscapeReport.endTableRow();

            landscapeReport.endTable();

            landscapeReport.addLineBreak();
        }
    }

    private List<ContributionTimeSlot> getContributionWeeks(List<ContributionTimeSlot> contributorsPerWeekOriginal, int pastWeeks) {
        List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>(contributorsPerWeekOriginal);
        List<String> slots = contributorsPerWeek.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastWeeks(pastWeeks);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerWeek.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerWeek;
    }

    private void addContributorsPerWeek() {
        landscapeReport.addLevel2Header("Commits Per Week (past two years)");
        int limit = 104;
        List<ContributionTimeSlot> contributorsPerWeek = getContributionWeeks(landscapeAnalysisResults.getContributorsPerWeek(), limit);
        contributorsPerWeek.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());
        if (contributorsPerWeek.size() > 0) {
            if (contributorsPerWeek.size() > limit) {
                contributorsPerWeek = contributorsPerWeek.subList(0, limit);
            }

            int maxCommits = contributorsPerWeek.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);

            landscapeReport.startTable();

            landscapeReport.startTableRow();
            String style = "max-width: 20px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px";
            contributorsPerWeek.forEach(week -> {
                landscapeReport.startTableCell(style);
                int count = week.getCommitsCount();
                landscapeReport.addParagraph("&nbsp;", "margin: 1px");
                int height = 1 + (int) (64.0 * count / maxCommits);
                String title = "week of " + week.getTimeSlot() + " = " + count + " commits";
                String yearString = week.getTimeSlot().split("[-]")[0];

                String color = "darkgrey";

                if (StringUtils.isNumeric(yearString)) {
                    int year = Integer.parseInt(yearString);
                    color = year % 2 == 0 ? "#89CFF0" : "#588BAE";
                }

                landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + color + "; height:" + height + "px; margin: 1px'></div>");
                landscapeReport.endTableCell();
            });
            landscapeReport.endTableRow();

            landscapeReport.endTable();

            landscapeReport.addLineBreak();
        }
    }

    private int getContributorsCountPerYear(String year) {
        int count[] = {0};

        landscapeAnalysisResults.getContributors().forEach(contributorProjects -> {
            if (contributorProjects.getContributor().getActiveYears().contains(year)) {
                count[0] += 1;
            }
        });

        return count[0];
    }

    private void renderPeopleDependencies(List<ComponentDependency> peopleDependencies, List<ContributorConnections> contributorConnections,
                                          double cIndex, double pIndex,
                                          double cMean, double pMean,
                                          double cMedian, double pMedian,
                                          int daysAgo) {
        landscapeReport.addLevel2Header("People Dependencies (" + daysAgo + " days)", "margin-top: 40px");

        if (daysAgo > 60) {
            landscapeReport.startShowMoreBlock("show details...");
        }
        landscapeReport.addParagraph("Based on the number of same repositories that two persons committed to in the past " + daysAgo + " days.", "color: grey");
        int connectionSum = contributorConnections.stream().mapToInt(c -> c.getConnectionsCount()).sum();
        landscapeReport.addParagraph("In total there are <b>" + FormattingUtils.formatCount(peopleDependencies.size()) + "</b> " +
                "contributor-to-contributor (C2C) connections via <b>" + FormattingUtils.formatCount(connectionSum) + " project dependencies.</b>.");

        List<Double> activeContributors30DaysHistory = landscapeAnalysisResults.getActiveContributors30DaysHistory();
        if (activeContributors30DaysHistory.size() > 0) {
            addDataSection("Active Contributors", activeContributors30DaysHistory.get(0), daysAgo, activeContributors30DaysHistory, "");
        }
        List<Double> peopleDependenciesCount30DaysHistory = landscapeAnalysisResults.getPeopleDependenciesCount30DaysHistory();
        if (peopleDependenciesCount30DaysHistory.size() > 0) {
            addDataSection("Unique Contributor-to-Contributor (C2C) Connections", peopleDependenciesCount30DaysHistory.get(0), daysAgo, peopleDependenciesCount30DaysHistory, "");
        }

        /*List<Double> connectionsViaProjects30DaysCountHistory = landscapeAnalysisResults.getConnectionsViaProjects30DaysCountHistory();
        if (connectionsViaProjects30DaysCountHistory.size() > 0) {
            addDataSection("All Contributor-to-Contributor (C2C) Connections", connectionsViaProjects30DaysCountHistory.get(0), daysAgo, connectionsViaProjects30DaysCountHistory, "");
        }*/
        addDataSection("C-median", cMedian, daysAgo, landscapeAnalysisResults.getcMedian30DaysHistory(), "");
        addDataSection("C-mean", cMean, daysAgo, landscapeAnalysisResults.getcMean30DaysHistory(), "");
        addDataSection("C-index", cIndex, daysAgo, landscapeAnalysisResults.getcIndex30DaysHistory(),
                "you have people with " + cIndex + " or more project connections with other people");

        addDataSection("P-median", pMedian, daysAgo, landscapeAnalysisResults.getpMedian30DaysHistory(), "");
        addDataSection("P-mean", pMean, daysAgo, landscapeAnalysisResults.getpMean30DaysHistory(), "");
        addDataSection("P-index", pIndex, daysAgo, landscapeAnalysisResults.getpIndex30DaysHistory(),
                "you have " + pIndex + " people committing to " + pIndex + " or more projects");

        peopleDependencies.sort((a, b) -> b.getCount() - a.getCount());
        List<ContributorProjects> contributors = landscapeAnalysisResults.getContributors();

        addMostConnectedPeopleSection(contributorConnections, daysAgo);
        addMostProjectsPeopleSection(contributorConnections, daysAgo);
        addTopConnectionsSection(peopleDependencies, daysAgo, contributors);
        addPeopleGraph(peopleDependencies, daysAgo);
        addProjectContributors(contributors, daysAgo);
        List<ComponentDependency> projectDependenciesViaPeople = addProjectDependenciesViaPeople(daysAgo, contributors);

        landscapeReport.startShowMoreBlock("show project dependencies graph...<br>");
        StringBuilder builder = new StringBuilder();
        builder.append("Project 1\tProject 2\t# people\n");
        projectDependenciesViaPeople.subList(0, Math.min(10000, projectDependenciesViaPeople.size())).forEach(d -> builder
                .append(d.getFromComponent()).append("\t")
                .append(d.getToComponent()).append("\t")
                .append(d.getCount()).append("\n"));
        String fileName = "projects_dependencies_via_people_" + daysAgo + "_days.txt";
        saveData(fileName, builder.toString());

        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("See data...", "data/" + fileName);
        addDependencyGraphVisuals(projectDependenciesViaPeople, new ArrayList<>(), "project_dependencies_" + daysAgo + "_");
        landscapeReport.endShowMoreBlock();
        if (daysAgo > 60) {
            landscapeReport.endShowMoreBlock();
        }

    }

    private void addDataSection(String type, double value, int daysAgo, List<Double> history, String info) {
        if (StringUtils.isNotBlank(info)) {
            landscapeReport.addParagraph(type + ": <b>" + ((int) Math.round(value)) + "</b> <span style='color: grey'>(" + info + ")</span>");
        } else {
            landscapeReport.addParagraph(type + ": <b>" + ((int) Math.round(value)) + "</b>");
        }

        if (daysAgo == 30 && history.size() > 0) {
            landscapeReport.startTable("border: none");
            landscapeReport.startTableRow("font-size: 70%;");
            double max = history.stream().max(Double::compare).get();
            history.forEach(historicalValue -> {
                landscapeReport.startTableCell("text-align: center; vertical-align: bottom;border: none");
                landscapeReport.addContentInDiv((int) Math.round(historicalValue) + "", "width: 20px;border: none");
                landscapeReport.addContentInDiv("", "width: 20px; background-color: skyblue; border: none; height:"
                        + (int) (1 + Math.round(40.0 * historicalValue / max)) + "px;");
                landscapeReport.endTableCell();
            });
            landscapeReport.endTableRow();
            landscapeReport.startTableRow("font-size: 70%;");
            landscapeReport.addTableCell("<b>now</b>", "border: none");
            landscapeReport.addTableCell("1m<br>ago", "text-align: center; border: none");
            for (int i = 0; i < history.size() - 2; i++) {
                landscapeReport.addTableCell((i + 2) + "m<br>ago", "text-align: center; border: none");
            }
            landscapeReport.endTableRow();
            landscapeReport.endTable();
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
        }
    }

    private List<ComponentDependency> addProjectDependenciesViaPeople(int daysAgo, List<ContributorProjects> contributors) {
        List<ComponentDependency> projectDependenciesViaPeople = ContributorConnectionUtils.getProjectDependenciesViaPeople(contributors, 0, daysAgo);

        landscapeReport.startShowMoreBlock("show project dependencies via people...<br>");
        landscapeReport.startTable();
        int maxListSize = Math.min(100, projectDependenciesViaPeople.size());
        if (maxListSize < projectDependenciesViaPeople.size()) {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + maxListSize + " items (out of " + projectDependenciesViaPeople.size() + ").");
        } else {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + maxListSize + (maxListSize == 1 ? " item" : " items") + ".");
        }
        projectDependenciesViaPeople.subList(0, maxListSize).forEach(dependency -> {
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(dependency.getFromComponent());
            landscapeReport.addTableCell(dependency.getToComponent());
            landscapeReport.addTableCell(dependency.getCount() + (dependency.getCount() == 1 ? " person" : " people"));
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
        return projectDependenciesViaPeople;
    }

    private void addProjectContributors(List<ContributorProjects> contributors, int daysAgo) {
        Map<String, Pair<String, Integer>> map = new HashMap<>();
        final List<String> list = new ArrayList<>();

        contributors.forEach(contributorProjects -> {
            contributorProjects.getProjects().stream().filter(project -> DateUtils.isAnyDateCommittedBetween(project.getCommitDates(), 0, daysAgo)).forEach(project -> {
                String key = project.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName();
                if (map.containsKey(key)) {
                    Integer currentValue = map.get(key).getRight();
                    map.put(key, Pair.of(key, currentValue + 1));
                } else {
                    Pair<String, Integer> pair = Pair.of(key, 1);
                    map.put(key, pair);
                    list.add(key);
                }
            });
        });

        Collections.sort(list, (a, b) -> map.get(b).getRight() - map.get(a).getRight());

        List<String> displayList = list;
        if (list.size() > 100) {
            displayList = list.subList(0, 100);
        }

        landscapeReport.startShowMoreBlock("show projects with most people...<br>");
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor\t# people\n");
        list.forEach(project -> builder.append(map.get(project).getLeft()).append("\t")
                .append(map.get(project).getRight()).append("\n"));
        String prefix = "projects_with_most_people_" + daysAgo + "_days";
        String fileName = prefix + ".txt";
        saveData(fileName, builder.toString());

        if (displayList.size() < list.size()) {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing top 100 items (out of " + list.size() + ").");
        }
        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/" + fileName);
        landscapeReport.addHtmlContent("</p>");
        List<VisualizationItem> visualizationItems = new ArrayList<>();
        list.forEach(project -> visualizationItems.add(new VisualizationItem(project, map.get(project).getRight())));
        exportVisuals(prefix, visualizationItems);
        landscapeReport.startTable();
        displayList.forEach(project -> {
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(map.get(project).getLeft());
            Integer count = map.get(project).getRight();
            landscapeReport.addTableCell(count + (count == 1 ? " person" : " people"));
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void addPeopleGraph(List<ComponentDependency> peopleDependencies, int daysAgo) {
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor 1\tContributor 2\t# shared projects\n");
        peopleDependencies.forEach(d -> builder
                .append(d.getFromComponent()).append("\t")
                .append(d.getToComponent()).append("\t")
                .append(d.getCount()).append("\n"));
        String fileName = "projects_shared_projects_" + daysAgo + "_days.txt";
        saveData(fileName, builder.toString());

        landscapeReport.startShowMoreBlock("show people graph...<br>");
        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("See data...", "data/" + fileName);

        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setMaxNumberOfDependencies(100);
        graphvizDependencyRenderer.setType("graph");
        graphvizDependencyRenderer.setArrow("--");
        addDependencyGraphVisuals(peopleDependencies, new ArrayList<>(), "people_dependencies_" + daysAgo + "_");
        landscapeReport.endShowMoreBlock();
    }

    private void addTopConnectionsSection(List<ComponentDependency> peopleDependencies, int daysAgo, List<ContributorProjects> contributors) {
        landscapeReport.startShowMoreBlock("show top connections...<br>");
        landscapeReport.startTable();
        List<ComponentDependency> displayListConnections = peopleDependencies.subList(0, Math.min(100, peopleDependencies.size()));
        if (displayListConnections.size() < peopleDependencies.size()) {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + displayListConnections.size() + " items (out of " + peopleDependencies.size() + ").");
        } else {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + displayListConnections.size() + (displayListConnections.size() == 1 ? " item" : " items") + ".");
        }
        int index[] = {0};
        displayListConnections.forEach(dependency -> {
            index[0] += 1;
            landscapeReport.startTableRow();
            String from = dependency.getFromComponent();
            String to = dependency.getToComponent();
            int dependencyCount = dependency.getCount();
            landscapeReport.addTableCell(index[0] + ".");
            int projectCount1 = ContributorConnectionUtils.getProjectCount(contributors, from, 0, daysAgo);
            int projectCount2 = ContributorConnectionUtils.getProjectCount(contributors, to, 0, daysAgo);
            double perc1 = 0;
            double perc2 = 0;
            if (projectCount1 > 0) {
                perc1 = 100.0 * dependencyCount / projectCount1;
            }
            if (projectCount2 > 0) {
                perc2 = 100.0 * dependencyCount / projectCount2;
            }
            landscapeReport.addTableCell(from + "<br><span style='color: grey'>" + projectCount1 + " projects (" + FormattingUtils.getFormattedPercentage(perc1) + "%)</span>", "");
            landscapeReport.addTableCell(to + "<br><span style='color: grey'>" + projectCount2 + " projects (" + FormattingUtils.getFormattedPercentage(perc2) + "%)</span>", "");
            landscapeReport.addTableCell(dependencyCount + " shared projects", "");
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void addMostConnectedPeopleSection(List<ContributorConnections> contributorConnections, int daysAgo) {
        landscapeReport.startShowMoreBlock("show most connected people...<br>");
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor\t# projects\t# connections\n");
        contributorConnections.forEach(c -> builder.append(c.getEmail()).append("\t")
                .append(c.getProjectsCount()).append("\t")
                .append(c.getConnectionsCount()).append("\n"));
        String prefix = "most_connected_people_" + daysAgo + "_days";
        String fileName = prefix + ".txt";

        saveData(fileName, builder.toString());

        List<ContributorConnections> displayListPeople = contributorConnections.subList(0, Math.min(100, contributorConnections.size()));
        if (displayListPeople.size() < contributorConnections.size()) {
            landscapeReport.addHtmlContent("<p>&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + displayListPeople.size() + " items (out of " + contributorConnections.size() + "). ");
        } else {
            landscapeReport.addHtmlContent("<p>&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + displayListPeople.size() + (displayListPeople.size() == 1 ? " item" : " items") + ". ");
        }
        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/" + fileName);
        landscapeReport.addHtmlContent("</p>");
        List<VisualizationItem> visualizationItems = new ArrayList<>();
        contributorConnections.forEach(c -> visualizationItems.add(new VisualizationItem(c.getEmail(), c.getConnectionsCount())));
        exportVisuals(prefix, visualizationItems);
        landscapeReport.addHtmlContent("</p>");
        int index[] = {0};
        landscapeReport.startTable();
        displayListPeople.forEach(name -> {
            index[0] += 1;
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(index[0] + ".", "");
            landscapeReport.addTableCell(name.getEmail(), "");
            landscapeReport.addTableCell(name.getProjectsCount() + "&nbsp;projects");
            landscapeReport.addTableCell(name.getConnectionsCount() + " connections", "");
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();

    }

    private void addMostProjectsPeopleSection(List<ContributorConnections> contributorConnections, int daysAgo) {
        landscapeReport.startShowMoreBlock("show people with most projects...<br>");
        List<ContributorConnections> sorted = new ArrayList<>(contributorConnections);
        sorted.sort((a, b) -> b.getProjectsCount() - a.getProjectsCount());
        List<ContributorConnections> displayListPeople = sorted.subList(0, Math.min(100, sorted.size()));
        if (displayListPeople.size() < contributorConnections.size()) {
            landscapeReport.addHtmlContent("<p>&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + displayListPeople.size() + " items (out of " + contributorConnections.size() + "). ");
        } else {
            landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + displayListPeople.size() + (displayListPeople.size() == 1 ? " item" : " items") + ". ");
        }
        String prefix = "most_projects_people_" + daysAgo + "_days";
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor\t# projects\t# connections\n");
        contributorConnections.forEach(c -> builder.append(c.getEmail()).append("\t")
                .append(c.getProjectsCount()).append("\t")
                .append(c.getConnectionsCount()).append("\n"));
        String fileName = prefix + ".txt";
        saveData(fileName, builder.toString());

        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/" + fileName);
        landscapeReport.addHtmlContent("</p>");
        List<VisualizationItem> visualizationItems = new ArrayList<>();
        contributorConnections.forEach(c -> visualizationItems.add(new VisualizationItem(c.getEmail(), c.getProjectsCount())));
        int index[] = {0};
        landscapeReport.startTable();
        displayListPeople.forEach(name -> {
            index[0] += 1;
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(index[0] + ".", "");
            landscapeReport.addTableCell(name.getEmail(), "");
            landscapeReport.addTableCell(name.getProjectsCount() + "&nbsp;projects");
            landscapeReport.addTableCell(name.getConnectionsCount() + " connections", "");
            landscapeReport.endTableRow();
        });

        exportVisuals(prefix, visualizationItems);
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void exportVisuals(String prefix, List<VisualizationItem> visualizationItems) {
        try {
            new LandscapeVisualsGenerator(reportsFolder).exportVisuals(prefix, visualizationItems);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addDependencyGraphVisuals(List<ComponentDependency> componentDependencies, List<String> componentNames, String prefix) {
        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setMaxNumberOfDependencies(100);
        graphvizDependencyRenderer.setType("graph");
        graphvizDependencyRenderer.setArrow("--");

        if (100 < componentDependencies.size()) {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing top " + 100 + " items (out of " + componentDependencies.size() + ").");
        } else {
            landscapeReport.addParagraph("&nbsp;&nbsp;&nbsp;&nbsp;Showing all " + componentDependencies.size() + (componentDependencies.size() == 1 ? " item" : " items") + ".");
        }
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(componentNames, componentDependencies);
        String graphId = prefix + dependencyVisualCounter++;
        landscapeReport.addGraphvizFigure(graphId, "", graphvizContent);
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();

        addDownloadLinks(graphId);
    }

    private void addDownloadLinks(String graphId) {
        landscapeReport.startDiv("");
        landscapeReport.addHtmlContent("Download: ");
        landscapeReport.addNewTabLink("SVG", "visuals/" + graphId + ".svg");
        landscapeReport.addHtmlContent(" ");
        landscapeReport.addNewTabLink("DOT", "visuals/" + graphId + ".dot.txt");
        landscapeReport.addHtmlContent(" ");
        landscapeReport.addNewTabLink("(open online Graphviz editor)", "https://obren.io/tools/graphviz/");
        landscapeReport.endDiv();
    }

    private void saveData(String fileName, String content) {
        File reportsFolder = Paths.get(this.folder.getParent(), "").toFile();
        File folder = Paths.get(reportsFolder.getPath(), "_sokrates_landscape/data").toFile();
        folder.mkdirs();

        try {
            File file = new File(folder, fileName);
            FileUtils.writeStringToFile(file, content, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.data.LandscapeDataExport;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionYear;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorConnections;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjects;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class LandscapeReportGenerator {
    public static final int RECENT_THRESHOLD_DAYS = 40;
    private static final Log LOG = LogFactory.getLog(LandscapeReportGenerator.class);
    private RichTextReport landscapeReport = new RichTextReport("Landscape Report", "index.html");
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private int dependencyVisualCounter = 1;

    public LandscapeReportGenerator(LandscapeAnalysisResults landscapeAnalysisResults, File folder) {
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
        if (StringUtils.isNotBlank(description)) {
            landscapeReport.addParagraph(description);
        }
        if (metadata.getLinks().size() > 0) {
            landscapeReport.startDiv("");
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
        addExtensions();
        addProjectsSection(getProjects());
        landscapeReport.endTabContentSection();

        landscapeReport.startTabContentSection("commits", false);
        addBigContributorsSummary(landscapeAnalysisResults);
        addContributors();
        addContributorsPerExtension();
        addPeopleDependencies();
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
            Collections.sort(links, (a, b) -> getLabel(a).compareTo(getLabel(b)));
            landscapeReport.startSubSection("Sub-Landscapes (" + links.size() + ")", "");

            landscapeReport.startUnorderedList();

            links.forEach(subLandscape -> {
                landscapeReport.startListItem();
                String href = landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + subLandscape.getIndexFilePath();
                String label = getLabel(subLandscape);
                landscapeReport.addNewTabLink(label, href);
                landscapeReport.endListItem();
            });

            landscapeReport.endUnorderedList();

            landscapeReport.endSection();
        }

    }

    private String getLabel(SubLandscapeLink subLandscape) {
        return subLandscape.getIndexFilePath().replaceAll("(/|\\\\)_sokrates_landscape(/|\\\\).*", "");
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
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getRookiesContributorsCount()), "active rookies",
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

        addIFrames(configuration);
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
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getRookiesContributorsCount()), "active rookies",
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

            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) Math.round(cMedian)), "C-Median", "30 days", "");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) Math.round(cMean)), "C-Mean", "30 days", "");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) Math.round(cIndex)), "C-Index",
                    "30 days", "" + (int) Math.round(cIndex) + " active contributes connected to " + (int) Math.round(cIndex) + " or more of other contributers via commits to shared projects in past 30 days.");

            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) Math.round(pMedian)), "P-Median", "30 days", "");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) Math.round(pMean)), "P-Mean", "30 days", "");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) Math.round(pIndex)), "P-Index",
                    "30 days", "" + (int) Math.round(pIndex) + " active contributes committing to " + (int) Math.round(pIndex) + " or more of projects in past 30 days.");
        }

        addContributorsPerYear(true);
    }

    private void addIFrames(LandscapeConfiguration configuration) {
        if (configuration.getiFrames().size() > 0) {
            configuration.getiFrames().forEach(iframe -> {
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
            });
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
        List<ContributorProjects> contributors = landscapeAnalysisResults.getContributors();
        int contributorsCount = landscapeAnalysisResults.getContributorsCount();

        if (contributorsCount > 0) {
            int thresholdCommits = landscapeAnalysisResults.getConfiguration().getContributorThresholdCommits();
            int totalCommits = contributors.stream().mapToInt(c -> c.getContributor().getCommitsCount()).sum();
            final String[] latestCommit = {""};
            contributors.forEach(c -> {
                if (c.getContributor().getLatestCommitDate().compareTo(latestCommit[0]) > 0) {
                    latestCommit[0] = c.getContributor().getLatestCommitDate();
                }
            });

            landscapeReport.startSubSection("Contributors (" + contributorsCount + ")",
                    (thresholdCommits > 1 ? thresholdCommits + "+&nbsp;commits, " : "") + "latest commit " + latestCommit[0]);

            addContributorLinks();

            if (contributorsCount > 100) {
                landscapeReport.startShowMoreBlock("show details...");
            }
            landscapeReport.startTable("width: 100%");
            landscapeReport.addTableHeader("", "Contributor", "# commits", "# commits<br>30 days", "# commits<br>90 days", "first", "latest", "projects");

            int counter[] = {0};

            contributors.forEach(contributor -> {
                addContributor(totalCommits, counter, contributor);
            });
            landscapeReport.endTable();
            if (contributorsCount > 100) {
                landscapeReport.endShowMoreBlock();
            }
            landscapeReport.endSection();
        }

    }

    private void addContributorsPerExtension() {
        int commitsCount = landscapeAnalysisResults.getCommitsCount();
        if (commitsCount > 0) {
            List<CommitsPerExtension> perExtension = landscapeAnalysisResults.getContributorsPerExtension();

            if (perExtension.size() > 0) {
                landscapeReport.startSubSection("Commits & File Extensions (" + perExtension.size() + ")", "");

                if (perExtension.size() > 100) {
                    landscapeReport.startShowMoreBlock("show details...");
                }
                landscapeReport.startTable("width: 100%");
                landscapeReport.addTableHeader("Extension",
                        "# contributors<br>30 days", "# commits<br>30 days", "# files<br>30 days",
                        "# contributors<br>90 days", "# commits<br>90 days", "# files<br>90 days",
                        "# contributors", "# commits", "# files");

                perExtension.forEach(commitsPerExtension -> {
                    addCommitExtension(commitsPerExtension);
                });
                landscapeReport.endTable();
                if (perExtension.size() > 100) {
                    landscapeReport.endShowMoreBlock();
                }

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

    private void addContributor(int totalCommits, int[] counter, ContributorProjects contributor) {
        landscapeReport.startTableRow(contributor.getContributor().isActive(RECENT_THRESHOLD_DAYS) ? "font-weight: bold;"
                : "color: " + (contributor.getContributor().isActive(90) ? "grey" : "lightgrey"));
        counter[0] += 1;
        landscapeReport.addTableCell("" + counter[0], "text-align: center; vertical-align: top; padding-top: 13px;");
        landscapeReport.addTableCell(StringEscapeUtils.escapeHtml4(contributor.getContributor().getEmail()), "vertical-align: top; padding-top: 13px;");
        int contributerCommits = contributor.getContributor().getCommitsCount();
        double percentage = 100.0 * contributerCommits / totalCommits;
        landscapeReport.addTableCell(contributerCommits + " (" + FormattingUtils.getFormattedPercentage(percentage) + "%)", "vertical-align: top; padding-top: 13px;");
        landscapeReport.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount30Days()), "vertical-align: top; padding-top: 13px;");
        landscapeReport.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount90Days()), "vertical-align: top; padding-top: 13px;");
        landscapeReport.addTableCell(contributor.getContributor().getFirstCommitDate(), "vertical-align: top; padding-top: 13px;");
        landscapeReport.addTableCell(contributor.getContributor().getLatestCommitDate(), "vertical-align: top; padding-top: 13px;");
        StringBuilder projectInfo = new StringBuilder();
        landscapeReport.startTableCell();
        int projectsCount = contributor.getProjects().size();
        landscapeReport.startShowMoreBlock(projectsCount + (projectsCount == 1 ? " project" : " projects"));
        contributor.getProjects().forEach(contributorProjectInfo -> {
            String projectName = contributorProjectInfo.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName();
            int commits = contributorProjectInfo.getCommitsCount();
            if (projectInfo.length() > 0) {
                projectInfo.append("<br/>");
            }
            projectInfo.append(projectName + " <span style='color: grey'>(" + commits + (commits == 1 ? " commit" : " commit") + ")</span>");
        });
        landscapeReport.addHtmlContent(projectInfo.toString());
        landscapeReport.endTableCell();
        landscapeReport.endTableRow();
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
        landscapeReport.startSubSection("Projects (" + projectsAnalysisResults.size() + ")", "");

        if (projectsAnalysisResults.size() > 0) {
            List<NumericMetric> projectSizes = new ArrayList<>();
            projectsAnalysisResults.forEach(projectAnalysisResults -> {
                CodeAnalysisResults analysisResults = projectAnalysisResults.getAnalysisResults();
                projectSizes.add(new NumericMetric(analysisResults.getMetadata().getName(), analysisResults.getMainAspectAnalysisResults().getLinesOfCode()));
            });
            landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_projects.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("tree map", "visuals/tree_map_projects.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("data", "data/projects.txt");
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            if (projectsAnalysisResults.size() > 100) {
                landscapeReport.startShowMoreBlock("show details...");
            }
            landscapeReport.startTable("width: 100%");
            int thresholdCommits = landscapeAnalysisResults.getConfiguration().getContributorThresholdCommits();
            int thresholdContributors = landscapeAnalysisResults.getConfiguration().getProjectThresholdContributors();
            landscapeReport.addTableHeader("",
                    "Project" + (thresholdContributors > 1 ? "<br/>(" + thresholdContributors + "+&nbsp;contributors)" : ""),
                    "Main<br/>Language", "LOC<br/>(main)",
                    "LOC<br/>(test)", "LOC<br/>(other)",
                    "Age", "Contributors" + (thresholdCommits > 1 ? "<br/>(" + thresholdCommits + "+&nbsp;commits)" : ""),
                    "Recent<br>Contributors<br>(30d)", "Rookies", "Commits<br>this year", "Report");
            Collections.sort(projectsAnalysisResults,
                    (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsThisYear()
                            - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsThisYear());
            projectsAnalysisResults.forEach(projectAnalysis -> {
                addProjectRow(projectAnalysis);
            });
            landscapeReport.endTable();
            if (projectsAnalysisResults.size() > 100) {
                landscapeReport.endShowMoreBlock();
            }
        }

        landscapeReport.endSection();
    }

    private void addProjectRow(ProjectAnalysisResults projectAnalysis) {
        CodeAnalysisResults analysisResults = projectAnalysis.getAnalysisResults();
        Metadata metadata = analysisResults.getMetadata();
        String logoLink = metadata.getLogoLink();

        landscapeReport.startTableRow();
        landscapeReport.addTableCell(StringUtils.isNotBlank(logoLink) ? "<img src='" + logoLink + "' style='width: 20px'>" : "", "text-align: center");
        landscapeReport.addTableCell(metadata.getName());
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysisResults.getTestAspectAnalysisResults();
        AspectAnalysisResults generated = analysisResults.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults build = analysisResults.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults other = analysisResults.getOtherAspectAnalysisResults();

        int thresholdCommits = landscapeAnalysisResults.getConfiguration().getContributorThresholdCommits();
        List<Contributor> contributors = analysisResults.getContributorsAnalysisResults().getContributors()
                .stream().filter(c -> c.getCommitsCount() >= thresholdCommits).collect(Collectors.toCollection(ArrayList::new));

        int contributorsCount = contributors.size();
        int recentContributorsCount = (int) contributors.stream().filter(c -> c.isActive(RECENT_THRESHOLD_DAYS)).count();
        int rookiesCount = (int) contributors.stream().filter(c -> c.isRookie(RECENT_THRESHOLD_DAYS)).count();

        List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
        StringBuilder locSummary = new StringBuilder();
        if (linesOfCodePerExtension.size() > 0) {
            locSummary.append(linesOfCodePerExtension.get(0).getName().replace("*.", "").trim().toUpperCase());
        } else {
            locSummary.append("-");
        }
        landscapeReport.addTableCell(locSummary.toString().replace("> = ", ">"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.formatCount(main.getLinesOfCode(), "-"), "text-align: center");

        landscapeReport.addTableCell(FormattingUtils.formatCount(test.getLinesOfCode(), "-"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.formatCount(generated.getLinesOfCode() + build.getLinesOfCode() + other.getLinesOfCode(), "-"), "text-align: center");
        int projectAgeYears = (int) Math.round(analysisResults.getFilesHistoryAnalysisResults().getAgeInDays() / 365.0);
        String age = projectAgeYears == 0 ? "<1y" : projectAgeYears + "y";
        landscapeReport.addTableCell(age, "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.formatCount(contributorsCount, "-"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.formatCount(recentContributorsCount, "-"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.formatCount(rookiesCount, "-"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.formatCount(analysisResults.getContributorsAnalysisResults().getCommitsThisYear(), "-"), "text-align: center");
        String projectReportUrl = landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/index.html";
        landscapeReport.addTableCell("<a href='" + projectReportUrl + "' target='_blank'>"
                + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 40) + "</div></a>", "text-align: center");
        landscapeReport.endTableRow();
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

        return reports;
    }

    private void addContributorsPerYear(boolean showContributorsCount) {
        List<ContributionYear> contributorsPerYear = landscapeAnalysisResults.getContributorsPerYear();
        if (contributorsPerYear.size() > 0) {
            int limit = landscapeAnalysisResults.getConfiguration().getCommitsMaxYears();
            if (contributorsPerYear.size() > limit) {
                contributorsPerYear = contributorsPerYear.subList(contributorsPerYear.size() - limit, contributorsPerYear.size());
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
            contributorsPerYear.forEach(year -> {
                landscapeReport.startTableCell(style);
                int count = year.getCommitsCount();
                landscapeReport.addParagraph(count + "", "margin: 2px");
                int height = 1 + (int) (64.0 * count / maxCommits);
                landscapeReport.addHtmlContent("<div style='width: 100%; background-color: darkgrey; height:" + height + "px'></div>");
                landscapeReport.endTableCell();
            });
            landscapeReport.endTableRow();

            if (showContributorsCount) {
                int maxContributors[] = {1};
                contributorsPerYear.forEach(year -> {
                    int count = getContributorsCountPerYear(year.getYear());
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
                    int count = getContributorsCountPerYear(year.getYear());
                    landscapeReport.addParagraph(count + "", "margin: 2px");
                    int height = 1 + (int) (64.0 * count / maxContributors[0]);
                    landscapeReport.addHtmlContent("<div style='width: 100%; background-color: skyblue; height:" + height + "px'></div>");
                    landscapeReport.endTableCell();
                });
                landscapeReport.endTableRow();
            }

            landscapeReport.startTableRow();
            landscapeReport.addTableCell("", "border: none; ");
            contributorsPerYear.forEach(year -> {
                landscapeReport.addTableCell(year.getYear(), "border: none; text-align: center; font-size: 90%");
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
        landscapeReport.addParagraph("Based on the number of same repositories that two persons committed to in the past " + daysAgo + " days.", "color: grey");
        landscapeReport.addParagraph("In total there are <b>" + FormattingUtils.formatCount(peopleDependencies.size()) +  "</b> peer-to-peer connections between contributors.");

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

        addMostConnectedPeopleSection(contributorConnections);
        addMostProjectsPeopleSection(contributorConnections);
        addTopConnectionsSection(peopleDependencies, daysAgo, contributors);
        addPeopleGraph(peopleDependencies, daysAgo);
        List<ComponentDependency> projectDependenciesViaPeople = addProjectDependenciesViaPeople(daysAgo, contributors);

        landscapeReport.startShowMoreBlock("show project dependencies graph...<br>");
        addDependencyGraphVisuals(projectDependenciesViaPeople, new ArrayList<>(), "project_dependencies_" + daysAgo + "_");
        landscapeReport.endShowMoreBlock();

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
            landscapeReport.addTableCell("now", "border: none");
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
            landscapeReport.addParagraph("Showing top " + maxListSize + " items (out of " + projectDependenciesViaPeople.size() + ").");
        } else {
            landscapeReport.addParagraph("Showing all " + maxListSize + (maxListSize == 1 ? " item" : " items") + ".");
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

    private void addPeopleGraph(List<ComponentDependency> peopleDependencies, int daysAgo) {
        landscapeReport.startShowMoreBlock("show people graph...<br>");
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
            landscapeReport.addParagraph("Showing top " + displayListConnections.size() + " items (out of " + peopleDependencies.size() + ").");
        } else {
            landscapeReport.addParagraph("Showing all " + displayListConnections.size() + (displayListConnections.size() == 1 ? " item" : " items") + ".");
        }
        int index[] = {0};
        displayListConnections.forEach(dependency -> {
            index[0] += 1;
            landscapeReport.startTableRow();
            String from = dependency.getFromComponent();
            String to = dependency.getToComponent();
            landscapeReport.addTableCell(index[0] + ".");
            landscapeReport.addTableCell(from + "<br><span style='color: grey'>" + ContributorConnectionUtils.getProjectCount(contributors, from, 0, daysAgo) + " projects</span>", "");
            landscapeReport.addTableCell(to + "<br><span style='color: grey'>" + ContributorConnectionUtils.getProjectCount(contributors, to, 0, daysAgo) + " projects</span>", "");
            landscapeReport.addTableCell(dependency.getCount() + " shared projects", "");
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void addMostConnectedPeopleSection(List<ContributorConnections> contributorConnections) {
        landscapeReport.startShowMoreBlock("show most connected people...<br>");
        landscapeReport.startTable();
        List<ContributorConnections> displayListPeople = contributorConnections.subList(0, Math.min(100, contributorConnections.size()));
        if (displayListPeople.size() < contributorConnections.size()) {
            landscapeReport.addParagraph("Showing top " + displayListPeople.size() + " items (out of " + contributorConnections.size() + ").");
        } else {
            landscapeReport.addParagraph("Showing all " + displayListPeople.size() + (displayListPeople.size() == 1 ? " item" : " items") + ".");
        }
        int index[] = {0};
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

    private void addMostProjectsPeopleSection(List<ContributorConnections> contributorConnections) {
        landscapeReport.startShowMoreBlock("show people with most projects...<br>");
        landscapeReport.startTable();
        List<ContributorConnections> sorted = new ArrayList<>(contributorConnections);
        sorted.sort((a, b) -> b.getProjectsCount() - a.getProjectsCount());
        List<ContributorConnections> displayListPeople = sorted.subList(0, Math.min(100, sorted.size()));
        if (displayListPeople.size() < sorted.size()) {
            landscapeReport.addParagraph("Showing top " + displayListPeople.size() + " items (out of " + sorted.size() + ").");
        } else {
            landscapeReport.addParagraph("Showing all " + displayListPeople.size() + (displayListPeople.size() == 1 ? " item" : " items") + ".");
        }
        int index[] = {0};
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

    private void addDependencyGraphVisuals(List<ComponentDependency> componentDependencies, List<String> componentNames, String prefix) {
        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setMaxNumberOfDependencies(100);
        graphvizDependencyRenderer.setType("graph");
        graphvizDependencyRenderer.setArrow("--");

        if (100 < componentDependencies.size()) {
            landscapeReport.addParagraph("Showing top " + 100 + " items (out of " + componentDependencies.size() + ").");
        } else {
            landscapeReport.addParagraph("Showing all " + componentDependencies.size() + (componentDependencies.size() == 1 ? " item" : " items") + ".");
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


}


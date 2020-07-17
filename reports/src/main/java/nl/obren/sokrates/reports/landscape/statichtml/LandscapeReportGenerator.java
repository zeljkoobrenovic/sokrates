/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.data.LandscapeDataExport;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionYear;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProject;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LandscapeReportGenerator {
    public static final int RECENT_THRESHOLD_DAYS = 30;
    private static final Log LOG = LogFactory.getLog(LandscapeReportGenerator.class);
    private RichTextReport landscapeReport = new RichTextReport("Landscape Report", "index.html");
    private LandscapeAnalysisResults landscapeAnalysisResults;

    public LandscapeReportGenerator(LandscapeAnalysisResults landscapeAnalysisResults, File folder) {
        LandscapeDataExport dataExport = new LandscapeDataExport(landscapeAnalysisResults, folder);
        dataExport.exportProjects();
        dataExport.exportContributors();

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

        addBigSummary(landscapeAnalysisResults);
        addContributorsPerYear();
        addExtensions();

        addSubLandscapeSection(landscapeAnalysisResults.getConfiguration().getSubLandscapes());

        addProjectsSection(getProjects());

        addContributors();
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
        landscapeReport.startDiv("margin-top: 32px;");
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int thresholdContributors = configuration.getProjectThresholdContributors();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(getProjects().size()), "projects",
                thresholdContributors > 1 ? "(" + thresholdContributors + "+&nbsp;contributors)" : "");
        int extensionsCount = getLinesOfCodePerExtension().size();
        addInfoBlock(FormattingUtils.getSmallTextForNumber(extensionsCount), extensionsCount == 1 ? "file extension" : "file extensions", "");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getMainLoc()), "lines of code (main)", "");
        int commitsCount = landscapeAnalysisResults.getCommitsCount();
        if (commitsCount > 0) {
            addInfoBlock(FormattingUtils.getSmallTextForNumber(commitsCount), "commits", "");
        }

        List<ContributorProject> contributors = landscapeAnalysisResults.getContributors();
        long contributorsCount = contributors.size();
        if (contributorsCount > 0) {
            int thresholdCommits = configuration.getContributorThresholdCommits();
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber((int) contributorsCount), "contributors",
                    (thresholdCommits > 1 ? "(" + thresholdCommits + "+&nbsp;commits)" : ""));
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(getRecentContributorsCount(contributors)), "recent contributors",
                    "(past 30 days)");
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(getRookiesContributorsCount(contributors)), "active rookies",
                    "(started in past year)");
        }
        landscapeReport.addLineBreak();
        if (configuration.getCustomMetrics().size() > 0) {
            configuration.getCustomMetrics().forEach(customMetric -> addCustomInfoBlock(customMetric));
            landscapeReport.addLineBreak();
        }
        addSmallInfoBlockLoc(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getTestLoc()), "(test)");
        addSmallInfoBlockLoc(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getGeneratedLoc()
                + landscapeAnalysisResults.getBuildAndDeploymentLoc()
                + landscapeAnalysisResults.getOtherLoc()), "(other)");

        int recentContributorsCount6Months = getRecentContributorsCount6Months(contributors);
        int recentContributorsCount3Months = getRecentContributorsCount3Months(contributors);
        if (recentContributorsCount3Months > 0 || recentContributorsCount6Months > 0) {
            addSmallInfoBlockPeople(FormattingUtils.getSmallTextForNumber(recentContributorsCount6Months), "(6 months)");
            addSmallInfoBlockPeople(FormattingUtils.getSmallTextForNumber(recentContributorsCount3Months), "(3 months)");
        }

        landscapeReport.endDiv();
        landscapeReport.addLineBreak();

        addCustomTags(configuration);
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

    private int getContributorsCount(List<ContributorProject> contributors) {
        return contributors.size();
    }

    private int getRecentContributorsCount(List<ContributorProject> contributors) {
        return (int) contributors.stream().filter(c -> c.getContributor().isActive(RECENT_THRESHOLD_DAYS)).count();
    }

    private int getRecentContributorsCount6Months(List<ContributorProject> contributors) {
        return (int) contributors.stream().filter(c -> c.getContributor().isActive(180)).count();
    }

    private int getRecentContributorsCount3Months(List<ContributorProject> contributors) {
        return (int) contributors.stream().filter(c -> c.getContributor().isActive(90)).count();
    }

    private int getRookiesContributorsCount(List<ContributorProject> contributors) {
        return (int) contributors.stream().filter(c -> c.getContributor().isRookie(RECENT_THRESHOLD_DAYS)).count();
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
        linesOfCodePerExtension.forEach(extension -> {
            addSmallInfoBlockLoc(FormattingUtils.getSmallTextForNumber(extension.getValue().intValue()), extension.getName().replace("*.", ""));
        });
        landscapeReport.endDiv();
        landscapeReport.endSection();
    }

    private List<NumericMetric> getLinesOfCodePerExtension() {
        int threshold = landscapeAnalysisResults.getConfiguration().getExtensionThresholdLoc();
        return landscapeAnalysisResults.getLinesOfCodePerExtension().stream()
                .filter(e -> e.getValue().intValue() >= threshold)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void addContributors() {
        List<ContributorProject> contributors = landscapeAnalysisResults.getContributors();
        int contributorsCount = getContributorsCount(contributors);

        if (contributorsCount > 0) {
            int thresholdCommits = landscapeAnalysisResults.getConfiguration().getContributorThresholdCommits();
            int totalCommits = contributors.stream().mapToInt(c -> c.getContributor().getCommitsCount()).sum();

            landscapeReport.startSubSection("Contributors (" + contributorsCount + ")",
                    thresholdCommits > 1 ? thresholdCommits + "+&nbsp;commits" : "");

            addContributorLinks();

            if (contributorsCount > 100) {
                landscapeReport.startShowMoreBlock("show details...");
            }
            landscapeReport.startTable("width: 100%");
            landscapeReport.addTableHeader("", "Contributor", "# commits", "first", "latest", "projects");

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

    private void addContributorLinks() {
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_contributors.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_contributors.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/contributors.txt");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
    }

    private void addContributor(int totalCommits, int[] counter, ContributorProject contributor) {
        landscapeReport.startTableRow(contributor.getContributor().isActive(RECENT_THRESHOLD_DAYS) ? "font-weight: bold;"
                : "color: " + (contributor.getContributor().isActive(90) ? "grey" : "lightgrey"));
        counter[0] += 1;
        landscapeReport.addTableCell("" + counter[0], "text-align: center; vertical-align: top; padding-top: 13px;");
        landscapeReport.addTableCell(StringEscapeUtils.escapeHtml4(contributor.getContributor().getEmail()), "vertical-align: top; padding-top: 13px;");
        int contributerCommits = contributor.getContributor().getCommitsCount();
        double percentage = 100.0 * contributerCommits / totalCommits;
        landscapeReport.addTableCell(contributerCommits + " (" + FormattingUtils.getFormattedPercentage(percentage) + "%)", "vertical-align: top; padding-top: 13px;");
        landscapeReport.addTableCell(contributor.getContributor().getFirstCommitDate(), "vertical-align: top; padding-top: 13px;");
        landscapeReport.addTableCell(contributor.getContributor().getLatestCommitDate(), "vertical-align: top; padding-top: 13px;");
        StringBuilder projectInfo = new StringBuilder();
        landscapeReport.startTableCell();
        int projectsCount = contributor.getProjects().size();
        landscapeReport.startShowMoreBlock(projectsCount + (projectsCount == 1 ? " project" : " projects"));
        for (int i = 0; i < projectsCount; i++) {
            String projectName = contributor.getProjects().get(i).getAnalysisResults().getMetadata().getName();
            int commits = contributor.getProjectsCommits().get(i);
            if (projectInfo.length() > 0) {
                projectInfo.append("<br/>");
            }
            projectInfo.append(projectName + " <span style='color: grey'>(" + commits + (commits == 1 ? " commit" : " commit") + ")</span>");
        }
        landscapeReport.addHtmlContent(projectInfo.toString());
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
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(main.getLinesOfCode(), "-"), "text-align: center");

        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(test.getLinesOfCode(), "-"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(generated.getLinesOfCode() + build.getLinesOfCode() + other.getLinesOfCode(), "-"), "text-align: center");
        int projectAgeYears = (int) Math.round(analysisResults.getFilesHistoryAnalysisResults().getAgeInDays() / 365.0);
        String age = projectAgeYears == 0 ? "<1y" : projectAgeYears + "y";
        landscapeReport.addTableCell(age, "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(contributorsCount, "-"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(recentContributorsCount, "-"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(rookiesCount, "-"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(analysisResults.getContributorsAnalysisResults().getCommitsThisYear(), "-"), "text-align: center");
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
        addInfoBlockWithColor(customMetric.getValue(), subtitle, color);
    }

    private void addInfoBlock(String mainValue, String subtitle, String description) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, "skyblue");
    }

    private void addPeopleInfoBlock(String mainValue, String subtitle, String description) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, "lavender");
    }

    private void addInfoBlockWithColor(String mainValue, String subtitle, String color) {
        String style = "border-radius: 12px;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 160px; height: 120px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 36px;";

        landscapeReport.startDiv(style);
        landscapeReport.addHtmlContent("<div style='font-size: 50px; margin-top: 20px'>" + mainValue + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 16px'>" + subtitle + "</div>");
        landscapeReport.endDiv();
    }

    private void addSmallCustomInfoBlockLoc(CustomMetric customMetric) {
        String color = StringUtils.isNotBlank(customMetric.getColor()) ? customMetric.getColor() : "lightgrey";
        addSmallInfoBlock(customMetric.getValue(), customMetric.getTitle(), color);
    }

    private void addSmallInfoBlockLoc(String value, String subtitle) {
        addSmallInfoBlock(value, subtitle, "skyblue");
    }

    private void addSmallInfoBlockPeople(String value, String subtitle) {
        addSmallInfoBlock(value, subtitle, "lavender");
    }

    private void addSmallInfoBlock(String value, String subtitle, String color) {
        String style = "border-radius: 8px;";

        style += "margin: 4px 4px 4px 0px;";
        style += "display: inline-block; width: 80px; height: 64px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 16px;";

        landscapeReport.startDiv(style);
        landscapeReport.addHtmlContent("<div style='font-size: 26px; margin-top: 8px'>" + value + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 14px'>" + subtitle + "</div>");
        landscapeReport.endDiv();
    }

    public List<RichTextReport> report() {
        List<RichTextReport> reports = new ArrayList<>();

        reports.add(this.landscapeReport);

        return reports;
    }

    private void addContributorsPerYear() {
        List<ContributionYear> contributorsPerYear = landscapeAnalysisResults.getContributorsPerYear();
        if (contributorsPerYear.size() > 0) {
            int limit = 20;
            if (contributorsPerYear.size() > limit) {
                contributorsPerYear = contributorsPerYear.subList(contributorsPerYear.size() - limit, contributorsPerYear.size());
            }

            landscapeReport.startSubSection("Commits Trend", "");
            int maxContributors = contributorsPerYear.stream().mapToInt(c -> c.getContributorsCount()).max().orElse(1);
            int maxCommits = contributorsPerYear.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);

            landscapeReport.startTable();

            landscapeReport.startTableRow();
            landscapeReport.addTableCell("Commits", "border: none;");
            String style = "border: none; text-align: center; vertical-align: bottom; font-size: 80%";
            contributorsPerYear.forEach(year -> {
                landscapeReport.startTableCell(style);
                int count = year.getCommitsCount();
                landscapeReport.addParagraph(count + "", "margin: 2px");
                int height = 1 + (int) (64.0 * count / maxCommits);
                landscapeReport.addHtmlContent("<div style='width: 100%; background-color: darkgrey; height:" + height + "px'></div>");
                landscapeReport.endTableCell();
            });
            landscapeReport.endTableRow();

            /*
            landscapeReport.startTableRow();
            landscapeReport.addTableCell("Contributors", "border: none;");
            contributorsPerYear.forEach(year -> {
                landscapeReport.startTableCell(style);
                int count = year.getContributorsCount();
                landscapeReport.addParagraph(count + "", "margin: 2px");
                int height = 1 + (int) (64.0 * count / maxContributors);
                landscapeReport.addHtmlContent("<div style='width: 100%; background-color: skyblue; height:" + height + "px'></div>");
                landscapeReport.endTableCell();
            });
            landscapeReport.endTableRow();
             */

            landscapeReport.startTableRow();
            landscapeReport.addTableCell("", "border: none; ");
            contributorsPerYear.forEach(year -> {
                landscapeReport.addTableCell(year.getYear(), "border: none; text-align: center; font-size: 90%");
            });
            landscapeReport.endTableRow();

            landscapeReport.endTable();

            landscapeReport.endSection();
        }
    }

}


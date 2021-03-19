/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.ContributionCounter;
import nl.obren.sokrates.sourcecode.landscape.ContributorConnection;
import nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorConnections;

import java.util.*;
import java.util.stream.Collectors;

public class ContributorsReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private int dependencyVisualCounter = 1;
    private RichTextReport report;

    public ContributorsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public static List<ContributorConnections> contributorConnections(List<ComponentDependency> peopleDependencies) {
        Map<String, ContributorConnections> map = new HashMap<>();

        peopleDependencies.forEach(dependency -> {
            String from = dependency.getFromComponent();
            String to = dependency.getToComponent();

            ContributorConnections contributorConnections1 = map.get(from);
            ContributorConnections contributorConnections2 = map.get(to);

            if (contributorConnections1 == null) {
                contributorConnections1 = new ContributorConnections();
                contributorConnections1.setEmail(from);
                contributorConnections1.setConnectionsCount(1);
                map.put(from, contributorConnections1);
            } else {
                contributorConnections1.setConnectionsCount(contributorConnections1.getConnectionsCount() + 1);
            }

            if (contributorConnections2 == null) {
                contributorConnections2 = new ContributorConnections();
                contributorConnections2.setEmail(to);
                contributorConnections2.setConnectionsCount(1);
                map.put(to, contributorConnections2);
            } else {
                contributorConnections2.setConnectionsCount(contributorConnections2.getConnectionsCount() + 1);
            }
        });

        List<ContributorConnections> names = new ArrayList<>(map.values());
        names.sort((a, b) -> b.getConnectionsCount() - a.getConnectionsCount());

        return names;
    }

    public void addContributorsAnalysisToReport(RichTextReport report) {
        this.report = report;

        report.startTabGroup();
        report.addTab("visuals", "Overview", true);
        report.addTab("30_days", "Past 30 Days", false);
        report.addTab("90_days", "Past 3 Months", false);
        report.addTab("180_days", "Past 6 Months", false);
        report.addTab("365_days", "Past Year", false);
        report.addTab("all_time", "All Time", false);
        report.addTab("data", "Data", false);
        report.endTabGroup();

        ContributorsAnalysisResults analysis = codeAnalysisResults.getContributorsAnalysisResults();
        List<Contributor> contributors = analysis.getContributors();

        report.startTabContentSection("visuals", true);
        report.addLevel2Header("Per Year");
        ContributorsReportUtils.addContributorsPerTimeSlot(report, analysis.getContributorsPerYear(), 20, true, 4);
        report.addLevel2Header("Per Month");
        ContributorsReportUtils.addContributorsPerTimeSlot(report, analysis.getContributorsPerMonth(), 24, true, 2);
        report.addLevel2Header("Per Week");
        int pastWeeks = 104;
        List<ContributionTimeSlot> contributorsPerWeek = getContributionWeeks(analysis, pastWeeks);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, contributorsPerWeek, pastWeeks, false, 2);
        report.addLevel2Header("Per Day");
        int pastDays = 365;
        List<ContributionTimeSlot> contributorsPerDay = getContributionDays(analysis, pastDays);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, contributorsPerDay, pastDays, false, 1);
        report.addLineBreak();
        ContributorsReportUtils.addContributorsSection(codeAnalysisResults, report);
        report.endTabContentSection();

        report.startTabContentSection("all_time", false);
        addContributorsPanel(report, contributors, c -> c.getCommitsCount());
        renderPeopleDependencies(analysis.getPeopleDependenciesAllTime(), 35600, c -> c.getCommitsCount(), contributors);
        report.endTabContentSection();

        report.startTabContentSection("30_days", false);
        List<Contributor> commits30Days = contributors.stream().filter(c -> c.getCommitsCount30Days() > 0).collect(Collectors.toList());
        if (commits30Days.size() > 0) {
            commits30Days.sort((a, b) -> b.getCommitsCount30Days() - a.getCommitsCount30Days());
            addContributorsPanel(report, commits30Days, c -> c.getCommitsCount30Days());
            renderPeopleDependencies(analysis.getPeopleDependencies30Days(), 30, c -> c.getCommitsCount30Days(), commits30Days);
        } else {
            report.addParagraph("No commits in past 30 days.", "margin-top: 16px");
        }
        report.endTabContentSection();

        report.startTabContentSection("90_days", false);
        List<Contributor> commits90Days = contributors.stream().filter(c -> c.getCommitsCount90Days() > 0).collect(Collectors.toList());
        if (commits90Days.size() > 0) {
            commits90Days.sort((a, b) -> b.getCommitsCount90Days() - a.getCommitsCount90Days());
            addContributorsPanel(report, commits90Days, c -> c.getCommitsCount90Days());
            renderPeopleDependencies(analysis.getPeopleDependencies90Days(), 90, c -> c.getCommitsCount90Days(), commits90Days);
        } else {
            report.addParagraph("No commits in past 90 days.", "margin-top: 16px");
        }
        report.endTabContentSection();

        report.startTabContentSection("180_days", false);
        List<Contributor> commits180Days = contributors.stream().filter(c -> c.getCommitsCount180Days() > 0).collect(Collectors.toList());
        if (commits180Days.size() > 0) {
            commits180Days.sort((a, b) -> b.getCommitsCount180Days() - a.getCommitsCount180Days());
            addContributorsPanel(report, commits180Days, c -> c.getCommitsCount180Days());
            renderPeopleDependencies(analysis.getPeopleDependencies180Days(), 180, c -> c.getCommitsCount180Days(), commits180Days);
        } else {
            report.addParagraph("No commits in past 180 days.", "margin-top: 16px");
        }
        report.endTabContentSection();

        report.startTabContentSection("365_days", false);
        List<Contributor> commits365Days = contributors.stream().filter(c -> c.getCommitsCount365Days() > 0).collect(Collectors.toList());
        commits365Days.sort((a, b) -> b.getCommitsCount365Days() - a.getCommitsCount365Days());
        addContributorsPanel(report, commits365Days, c -> c.getCommitsCount365Days());
        renderPeopleDependencies(analysis.getPeopleDependencies365Days(), 365, c -> c.getCommitsCount365Days(), commits365Days);
        report.endTabContentSection();

        report.startTabContentSection("data", false);
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("Contributors' details...", "../data/text/contributors.txt");
        report.endListItem();
        report.endUnorderedList();
        report.endTabContentSection();
    }

    private List<ContributionTimeSlot> getContributionWeeks(ContributorsAnalysisResults analysis, int pastWeeks) {
        List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>(analysis.getContributorsPerWeek());
        List<String> slots = contributorsPerWeek.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastWeeks(pastWeeks);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerWeek.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerWeek;
    }

    private List<ContributionTimeSlot> getContributionDays(ContributorsAnalysisResults analysis, int pastDays) {
        List<ContributionTimeSlot> contributorsPerDay = new ArrayList<>(analysis.getContributorsPerDay());
        List<String> slots = contributorsPerDay.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastDays(pastDays);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerDay.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerDay;
    }

    private void renderPeopleDependencies(List<ComponentDependency> peopleDependencies, int daysAgo,
                                          ContributionCounter contributionCounter,
                                          List<Contributor> contributors) {
        if (peopleDependencies.size() > 0) {
            report.addLevel2Header("People Dependencies", "margin-top: 40px");

            report.startShowMoreBlock("show graph...");
            report.addParagraph("The number on lines shows the number of same files that both persons changed in past <b>" + daysAgo + "</b> days.", "color: grey");
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setMaxNumberOfDependencies(100);
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");

            Set<String> emails = new HashSet<>();
            peopleDependencies.forEach(peopleDependency -> {
                emails.add(peopleDependency.getFromComponent());
                emails.add(peopleDependency.getToComponent());
            });

            String prefix = "people_dependencies_" + daysAgo + "_";
            addDependencyGraphVisuals(peopleDependencies, new ArrayList<>(), graphvizDependencyRenderer, prefix);
            report.endShowMoreBlock();
            List<ContributorConnection> contributorConnections = ContributorConnectionUtils.getContributorConnections(peopleDependencies, contributors, contributionCounter);
            report.addParagraph("C-median: " + ContributorConnectionUtils.getCMedian(contributorConnections(peopleDependencies)));
            report.addParagraph("C-mean: " + ContributorConnectionUtils.getCMean(contributorConnections(peopleDependencies)));
            report.addParagraph("C-index: " + ContributorConnectionUtils.getCIndex(contributorConnections(peopleDependencies)));
            report.startTable();
            report.addTableHeader("", "Contributor", "# connections", "# commits");
            int index[] = {0};
            contributorConnections.forEach(contributorConnection -> {
                index[0]++;
                report.startTableRow();
                report.addTableCell(index[0] + ".");
                report.addTableCell(contributorConnection.getEmail());
                report.addTableCell(contributorConnection.getCount() + "");
                report.addTableCell(contributorConnection.getCommits() + "");
                report.endTableRow();
            });
            report.endTable();
        }
    }

    private void addDependencyGraphVisuals(List<ComponentDependency> componentDependencies, List<String> componentNames, GraphvizDependencyRenderer graphvizDependencyRenderer, String prefix) {
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(
                componentNames,
                componentDependencies);
        String graphId = prefix + dependencyVisualCounter++;
        report.addGraphvizFigure(graphId, "", graphvizContent);
        report.addLineBreak();
        report.addLineBreak();
        addDownloadLinks(graphId);
    }

    private void addDownloadLinks(String graphId) {
        report.startDiv("");
        report.addHtmlContent("Download: ");
        report.addNewTabLink("SVG", "visuals/" + graphId + ".svg");
        report.addHtmlContent(" ");
        report.addNewTabLink("DOT", "visuals/" + graphId + ".dot.txt");
        report.addHtmlContent(" ");
        report.addNewTabLink("(open online Graphviz editor)", "https://obren.io/tools/graphviz/");
        report.endDiv();
    }

    public void addContributorsPanel(RichTextReport report, List<Contributor> contributors, ContributionCounter contributionCounter) {
        int count = contributors.size();
        if (count == 0) {
            return;
        }
        report.addLineBreak();
        int total[] = {0};
        contributors.forEach(contributor -> total[0] += contributionCounter.count(contributor));
        if (total[0] > 0) {
            report.addParagraph("<b>" + FormattingUtils.formatCount(count) + "</b> " + (count == 1 ? "contributor" : "contributors") + " (" + "<b>" + FormattingUtils.formatCount(total[0]) + "</b> " + (count == 1 ? "commit" : "commits") + "):");
            StringBuilder map = new StringBuilder("");
            Palette palette = Palette.getDefaultPalette();
            int index[] = {0};
            int cumulative[] = {0};
            contributors.forEach(contributor -> {
                int contributorCommitsCount = contributionCounter.count(contributor);
                cumulative[0] += contributorCommitsCount;
                int w = (int) Math.round(600 * (double) contributorCommitsCount / total[0]);
                int x = 620 - w;
                index[0]++;
                String cumulativeText = "";
                if (index[0] > 1) {
                    cumulativeText = "\n\ntop " + index[0]
                            + " contributors together ("
                            + FormattingUtils.getFormattedPercentage(100.0 * index[0] / contributors.size())
                            + "% of contributors) = "
                            + FormattingUtils.getFormattedPercentage(100.0 * cumulative[0] / total[0])
                            + "% of all commits";
                }
                map.append("<div style='background-color: " + palette.nextColor() + "; display: inline-block; height: 20px; width: " + w + "px' title='" + contributor.getEmail() + "\n" + contributorCommitsCount + " commits (" + (Math.round(100.0 * contributorCommitsCount / total[0])) + "%)" + cumulativeText + "'>&nbsp;</div>");
            });
            report.addHtmlContent(map.toString());
            report.addLineBreak();
            report.addLineBreak();
        }
        report.startTable();
        report.addTableHeader("#", "Contributor", "First Commit", "Latest Commit", "# commits");
        int index[] = {0};
        contributors.forEach(contributor -> {
            index[0]++;
            String style = "";
            if (contributor.getCommitsCount90Days() == 0) {
                style = "color: lightgrey";
            } else if (contributor.getCommitsCount30Days() == 0) {
                style = "color: grey";
            }
            report.startTableRow(style);
            report.addTableCell(index[0] + ".");
            report.addTableCell(contributor.getEmail());
            report.addTableCell(contributor.getFirstCommitDate());
            report.addTableCell(contributor.getLatestCommitDate());
            int contributorCommitsCount = contributionCounter.count(contributor);
            String formattedCount = FormattingUtils.formatCount(contributorCommitsCount);
            String formattedPercentage = FormattingUtils.getFormattedPercentage(100.0 * contributorCommitsCount / total[0]);
            report.addTableCell(formattedCount + " (" + formattedPercentage + "%)");
            report.endTableRow();
        });

        report.endTable();
    }
}

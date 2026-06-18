/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DLink;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DNode;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DObject;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.ContributorPerExtensionStats;
import nl.obren.sokrates.sourcecode.landscape.ContributionCounter;
import nl.obren.sokrates.sourcecode.landscape.ContributorConnection;
import nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorConnections;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ContributorsReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private int dependencyVisualCounter = 1;
    private File reportsFolder;
    private RichTextReport report;
    private Map<String, Contributor> emailContributorMap = new HashMap<>();
    private Map<String, List<Pair<String, ContributorPerExtensionStats>>> emailStatsMap = new HashMap<>();

    public ContributorsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
        codeAnalysisResults.getContributorsAnalysisResults().getContributors().forEach(contributor -> {
            emailContributorMap.put(contributor.getEmail(), contributor);
        });
        codeAnalysisResults.getContributorsAnalysisResults().getCommitsPerExtensions().forEach(commitsPerExtension -> {
            commitsPerExtension.getContributorPerExtensionStats().forEach(contributorPerExtensionStats -> {
                String email = contributorPerExtensionStats.getContributor();
                if (!emailStatsMap.containsKey(email)) {
                    emailStatsMap.put(email, new ArrayList<>());
                }
                emailStatsMap.get(email).add(Pair.of(commitsPerExtension.getExtension(), contributorPerExtensionStats));
            });
        });
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

    public void addContributorsAnalysisToReport(File reportsFolder, RichTextReport report) {
        this.reportsFolder = reportsFolder;
        this.report = report;


        report.addParagraph("An overview of contributor trends.", "margin-top: 12px; color: grey; font-size: 94%");

        report.startTabGroup();
        report.addTab("matrix", "Contributors Matrix", true);
        report.addTab("30_days", "Past 30 Days", false);
        report.addTab("90_days", "Past 3 Months", false);
        report.addTab("180_days", "Past 6 Months", false);
        report.addTab("365_days", "Past Year", false);
        report.addTab("contributors", "Overview", false);
        report.addTab("data", "Data", false);
        report.endTabGroup();

        ContributorsAnalysisResults analysis = codeAnalysisResults.getContributorsAnalysisResults();
        List<Contributor> contributors = analysis.getContributors();

        List<Contributor> people = contributors.stream().filter(c -> !c.isBot()).collect(Collectors.toList());
        List<Contributor> bots = contributors.stream().filter(c -> c.isBot()).collect(Collectors.toList());

        report.startTabContentSection("contributors", false);
        addZoomableCircleLinks(report);
        ContributorsReportUtils.addContributorsSection(codeAnalysisResults, report);
        report.endTabContentSection();

        report.startTabContentSection("matrix", true);
        addMatrix(new ArrayList<>(people), "Contributors");
        addMatrix(new ArrayList<>(bots), "Bots");
        report.endTabContentSection();

        report.startTabContentSection("30_days", false);
        List<Contributor> commits30Days = contributors.stream().filter(c -> c.getCommitsCount30Days() > 0).collect(Collectors.toList());
        if (commits30Days.size() > 0) {
            List<Contributor> peopleCommits30Days = people.stream().filter(c -> c.getCommitsCount30Days() > 0).collect(Collectors.toList());
            List<Contributor> botCommits30Days = bots.stream().filter(c -> c.getCommitsCount30Days() > 0).collect(Collectors.toList());
            commits30Days.sort((a, b) -> b.getCommitsCount30Days() - a.getCommitsCount30Days());
            addContributorsPanel(report, peopleCommits30Days, c -> c.getCommitsCount30Days(), true, e -> e.getFileUpdates30Days(), "Contributor", Contributor::getLinesAdded30Days, Contributor::getLinesDeleted30Days);
            addContributorsPanel(report, botCommits30Days, c -> c.getCommitsCount30Days(), true, e -> e.getFileUpdates30Days(), "Bot", Contributor::getLinesAdded30Days, Contributor::getLinesDeleted30Days);
            renderPeopleDependencies(analysis.getPeopleDependencies30Days(), analysis.getPeopleFileDependencies30Days(), 30, c -> c.getCommitsCount30Days(), commits30Days);
        } else {
            report.addParagraph("No commits in past 30 days.", "margin-top: 16px");
        }
        report.endTabContentSection();

        report.startTabContentSection("90_days", false);
        List<Contributor> commits90Days = contributors.stream().filter(c -> c.getCommitsCount90Days() > 0).collect(Collectors.toList());
        if (commits90Days.size() > 0) {
            List<Contributor> peopleCommits90Days = people.stream().filter(c -> c.getCommitsCount90Days() > 0).collect(Collectors.toList());
            List<Contributor> botCommits90Days = bots.stream().filter(c -> c.getCommitsCount90Days() > 0).collect(Collectors.toList());
            commits90Days.sort((a, b) -> b.getCommitsCount90Days() - a.getCommitsCount90Days());
            addContributorsPanel(report, peopleCommits90Days, c -> c.getCommitsCount90Days(), true, e -> e.getFileUpdates90Days(), "Contributor", Contributor::getLinesAdded90Days, Contributor::getLinesDeleted90Days);
            addContributorsPanel(report, botCommits90Days, c -> c.getCommitsCount90Days(), true, e -> e.getFileUpdates90Days(), "Bot", Contributor::getLinesAdded90Days, Contributor::getLinesDeleted90Days);
            renderPeopleDependencies(analysis.getPeopleDependencies90Days(), analysis.getPeopleFileDependencies90Days(), 90, c -> c.getCommitsCount90Days(), commits90Days);
        } else {
            report.addParagraph("No commits in past 90 days.", "margin-top: 16px");
        }
        report.endTabContentSection();

        report.startTabContentSection("180_days", false);
        List<Contributor> commits180Days = contributors.stream().filter(c -> c.getCommitsCount180Days() > 0).collect(Collectors.toList());
        if (commits180Days.size() > 0) {
            List<Contributor> peopleCommits180Days = people.stream().filter(c -> c.getCommitsCount180Days() > 0).collect(Collectors.toList());
            List<Contributor> botCommits180Days = bots.stream().filter(c -> c.getCommitsCount180Days() > 0).collect(Collectors.toList());
            commits180Days.sort((a, b) -> b.getCommitsCount180Days() - a.getCommitsCount180Days());
            addContributorsPanel(report, peopleCommits180Days, c -> c.getCommitsCount180Days(), true, null, "Contributor", Contributor::getLinesAdded180Days, Contributor::getLinesDeleted180Days);
            addContributorsPanel(report, botCommits180Days, c -> c.getCommitsCount180Days(), true, null, "Bot", Contributor::getLinesAdded180Days, Contributor::getLinesDeleted180Days);
            renderPeopleDependencies(analysis.getPeopleDependencies180Days(), analysis.getPeopleFileDependencies180Days(), 180, c -> c.getCommitsCount180Days(), commits180Days);
        } else {
            report.addParagraph("No commits in past 180 days.", "margin-top: 16px");
        }
        report.endTabContentSection();

        report.startTabContentSection("365_days", false);
        List<Contributor> commits365Days = contributors.stream().filter(c -> c.getCommitsCount365Days() > 0).collect(Collectors.toList());
        commits365Days.sort((a, b) -> b.getCommitsCount365Days() - a.getCommitsCount365Days());
        List<Contributor> peopleCommits365Days = people.stream().filter(c -> c.getCommitsCount365Days() > 0).collect(Collectors.toList());
        List<Contributor> botCommits365Days = bots.stream().filter(c -> c.getCommitsCount365Days() > 0).collect(Collectors.toList());
        commits365Days.sort((a, b) -> b.getCommitsCount365Days() - a.getCommitsCount365Days());
        addContributorsPanel(report, peopleCommits365Days, c -> c.getCommitsCount365Days(), true, null, "Contributor", Contributor::getLinesAdded365Days, Contributor::getLinesDeleted365Days);
        addContributorsPanel(report, botCommits365Days, c -> c.getCommitsCount365Days(), true, null, "Bot", Contributor::getLinesAdded365Days, Contributor::getLinesDeleted365Days);
        renderPeopleDependencies(analysis.getPeopleDependencies365Days(), analysis.getPeopleFileDependencies365Days(), 365, c -> c.getCommitsCount365Days(), commits365Days);
        report.endTabContentSection();

        report.startTabContentSection("data", false);
        report.startUnorderedList();
        report.startListItem();
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/contributors.txt')\">" + "Contributors' details..." + "</a>");
        report.endListItem();
        report.endUnorderedList();
        report.endTabContentSection();
    }

    private void addZoomableCircleLinks(RichTextReport report) {
        report.startDiv("margin-top: 10px");
        report.addHtmlContent("Zoomable circles (number of contributors per file): ");
        report.addNewTabLink("30 days", "visuals/zoomable_circles.html#contributors_30_main");
        report.addHtmlContent(" | ");
        report.addNewTabLink("90 days", "visuals/zoomable_circles.html#contributors_90_main");
        report.addHtmlContent(" | ");
        report.addNewTabLink("6 months", "visuals/zoomable_circles.html#contributors_180_main");
        report.addHtmlContent(" | ");
        report.addNewTabLink("past year", "visuals/zoomable_circles.html#contributors_365_main");
        report.addHtmlContent(" | ");
        report.addNewTabLink("all time", "visuals/zoomable_circles.html#contributors_main");
        report.addContentInDiv("Files with only one contributor are shown as grey.", "color: grey; font-size: 80%");
        report.endDiv();
    }


    // A matrix commits-count cell: the count ("-" when zero), with that window's line churn shown
    // underneath (+added / -deleted) when the history carried churn data.
    private String commitsCountWithChurnCell(int commitsCount, int linesAdded, int linesDeleted) {
        String cell = commitsCount > 0 ? commitsCount + "" : "-";
        if (linesAdded > 0 || linesDeleted > 0) {
            cell += "<div style='font-size: 80%; white-space: nowrap;'>"
                    + "<span style='color: #2e7d32;'>+" + FormattingUtils.getSmallTextForNumber(linesAdded) + "</span> / "
                    + "<span style='color: #c62828;'>-" + FormattingUtils.getSmallTextForNumber(linesDeleted) + "</span></div>";
        }
        return cell;
    }

    // Two summary rows under the matrix header: total commits per month and total line churn per
    // month, across the given contributors. Aligned with the four commits columns (left blank) and
    // the 24 month columns. The churn row is only emitted when there is churn data.
    private void addMatrixPerMonthTotals(List<Contributor> contributors, List<String> pastMonths) {
        Map<String, Integer> commitsPerMonth = new HashMap<>();
        Map<String, Integer> addedPerMonth = new HashMap<>();
        Map<String, Integer> deletedPerMonth = new HashMap<>();
        boolean hasChurn[] = {false};
        contributors.forEach(contributor -> {
            contributor.getCommitsPerDate().forEach((date, count) ->
                    commitsPerMonth.merge(DateUtils.getMonth(date), count, Integer::sum));
            contributor.getLinesAddedPerDate().forEach((date, count) -> {
                addedPerMonth.merge(DateUtils.getMonth(date), count, Integer::sum);
                if (count != 0) hasChurn[0] = true;
            });
            contributor.getLinesDeletedPerDate().forEach((date, count) -> {
                deletedPerMonth.merge(DateUtils.getMonth(date), count, Integer::sum);
                if (count != 0) hasChurn[0] = true;
            });
        });

        // Commits-per-month row.
        report.startTableRow();
        report.addTableCell("commits", "min-width: 200px; border: none; text-align: right; font-size: 80%; color: grey");
        for (int i = 0; i < 4; i++) {
            report.addTableCell("", "border: none");
        }
        pastMonths.forEach(pastMonth -> {
            int c = commitsPerMonth.getOrDefault(pastMonth, 0);
            report.startTableCell("font-size: 70%; border: none; text-align: center; color: grey");
            report.addContentInDivWithTooltip(c == 0 ? "-" : (c + ""), "Month " + pastMonth + ": " + c + (c == 1 ? " commit" : " commits"), "text-align: center");
            report.endTableCell();
        });
        report.endTableRow();

        if (!hasChurn[0]) {
            return;
        }

        // Line-churn-per-month row (+added / -deleted).
        report.startTableRow();
        report.addTableCell("line churn", "min-width: 200px; border: none; text-align: right; font-size: 80%; color: grey");
        for (int i = 0; i < 4; i++) {
            report.addTableCell("", "border: none");
        }
        pastMonths.forEach(pastMonth -> {
            int added = addedPerMonth.getOrDefault(pastMonth, 0);
            int deleted = deletedPerMonth.getOrDefault(pastMonth, 0);
            report.startTableCell("font-size: 65%; border: none; text-align: center; white-space: nowrap");
            if (added == 0 && deleted == 0) {
                report.addContentInDiv("-", "color: lightgrey; text-align: center");
            } else {
                String tooltip = "Month " + pastMonth + ": +" + added + " / -" + deleted + " lines";
                report.addContentInDivWithTooltip(
                        "<span style='color: #2e7d32;'>+" + FormattingUtils.getSmallTextForNumber(added) + "</span>"
                                + "<br><span style='color: #c62828;'>-" + FormattingUtils.getSmallTextForNumber(deleted) + "</span>",
                        tooltip, "text-align: center");
            }
            report.endTableCell();
        });
        report.endTableRow();
    }

    private void addMatrix(List<Contributor> contributors, String type) {
        report.addContentInDiv("&nbsp;", "height: 20px");
        report.startSubSection(type + " Matrix (Per Month)", "");
        report.startDiv("width: 100%; overflow-x: scroll; overflow-y: scroll; max-height: 600px");
        report.startTable();

        final List<String> pastMonths = DateUtils.getPastMonths(24, DateUtils.getAnalysisDate());
        report.startTableRow("font-size: 80%");
        report.addTableCell("", "min-width: 200px; border: none; border: none");
        report.addTableCell("", "border: none; border: none");
        report.addTableCell("", "border: none; border: none");
        report.addTableCell("", "border: none; border: none");
        report.addTableCell("", "border: none; border: none");
        pastMonths.forEach(pastMonth -> {
            report.startTableCell("font-size: 70%; border: none; color: lightgrey; text-align: center");
            int count[] = {0};
            contributors.forEach(contributor -> {
                boolean active[] = {false};
                contributor.getCommitDates().forEach(date -> {
                    String month = DateUtils.getMonth(date);
                    if (month.equals(pastMonth)) {
                        active[0] = true;
                        return;
                    }
                });
                if (active[0]) {
                    count[0] += 1;
                }
            });
            String tooltip = "Month " + pastMonth + ": " + (count[0] + (count[0] == 1 ? " contributor" : " contributors "));
            report.addContentInDivWithTooltip(count[0] == 0 ? "-" : (count[0] + ""), tooltip, "text-align: center");
            report.endTableCell();
        });
        report.endTableRow();

        addMatrixPerMonthTotals(contributors, pastMonths);


        report.startTableRow("font-size: 80%");
        report.addTableCell("", "min-width: 200px; border: none; border: none");
        report.addTableCell("30d", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("3m", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("1y", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("all time", "max-width: 100px; text-align: center; border: none");
        report.endTableRow();

        Collections.sort(contributors, (a, b) -> {
            if (b.getLatestCommitDate().equals(a.getLatestCommitDate())) {
                if (b.getCommitsCount30Days() == a.getCommitsCount30Days()) {
                    return b.getCommitsCount() - a.getCommitsCount();
                } else {
                    return b.getCommitsCount30Days() - a.getCommitsCount30Days();
                }
            } else {
                return b.getLatestCommitDate().compareTo(a.getLatestCommitDate());
            }
        });

        int listLimit = 500;
        contributors.subList(0, contributors.size() > listLimit ? listLimit : contributors.size()).forEach(contributor -> {
            report.startTableRow();
            String textOpacity = contributor.getCommitsCount90Days() > 0 ? "font-weight: bold;" : "opacity: 0.4";
            report.startTableCell("border: none; " + textOpacity);
            if (StringUtils.isNotBlank(contributor.getEmail()) && StringUtils.isNotBlank(contributor.getUserName())) {
                report.addHtmlContent(contributor.getUserName() + " <div style='color: grey; font-size: 80%; margin-bottom: 6px;'>&lt;" + contributor.getEmail() + "&gt;</div>");
            } else {
                report.addHtmlContent((contributor.getUserName() + contributor.getEmail()).trim());
            }
            report.endTableCell();
            // Each commits-count window shows the count with that window's line churn underneath in
            // the same cell (+added / -deleted; omitted when the history carried no churn data).
            String cellStyle = "text-align: center; border: none; " + textOpacity;
            report.addTableCell(commitsCountWithChurnCell(contributor.getCommitsCount30Days(),
                    contributor.getLinesAdded30Days(), contributor.getLinesDeleted30Days()), cellStyle);
            report.addTableCell(commitsCountWithChurnCell(contributor.getCommitsCount90Days(),
                    contributor.getLinesAdded90Days(), contributor.getLinesDeleted90Days()), cellStyle);
            report.addTableCell(commitsCountWithChurnCell(contributor.getCommitsCount365Days(),
                    contributor.getLinesAdded365Days(), contributor.getLinesDeleted365Days()), cellStyle);
            report.addTableCell(commitsCountWithChurnCell(contributor.getCommitsCount(),
                    contributor.getLinesAdded(), contributor.getLinesDeleted()), cellStyle);
            int index[] = {0};
            pastMonths.forEach(pastMonth -> {
                int count[] = {0};
                contributor.getCommitDates().forEach(date -> {
                    String month = DateUtils.getMonth(date);
                    if (month.equals(pastMonth)) {
                        count[0] += 1;
                    }
                });
                index[0] += 1;
                report.startTableCell("text-align: center; padding: 0; border: none; vertical-align: middle;");
                if (count[0] > 0) {
                    int size = 10 + (count[0] / 4) * 4;
                    String tooltip = "Month " + pastMonth + ": " + count[0] + (count[0] == 1 ? " commit day" : " commit days");
                    String opacity = "" + Math.max(0.9 - (index[0] - 1) * 0.2, 0.2);
                    report.addContentInDivWithTooltip("", tooltip,
                            "padding: 0; margin: 0; display: inline-block; background-color: #483D8B; opacity: " + opacity + "; border-radius: 50%; width: " + size + "px; height: " + size + "px;");
                } else {
                    report.addContentInDiv("-", "color: lightgrey; font-size: 80%");
                }
                report.endTableCell();
            });
            report.endTableRow();
        });
        report.endTable();
        if (contributors.size() > 500) {
            report.addParagraph("Top 500 (out of " + contributors.size() + ") items shows.");
        }
        report.endDiv();
        report.endSection();
    }

    private void renderPeopleDependencies(List<ComponentDependency> peopleDependencies,
                                          List<ComponentDependency> peopleFileDependencies,
                                          int daysAgo,
                                          ContributionCounter contributionCounter,
                                          List<Contributor> contributors) {
        if (peopleDependencies.size() > 0) {
            report.addLevel2Header("Contributor Dependencies", "margin-top: 40px; margin-bottom: 0;");
            report.addParagraph("A contributor dependency is detected if two contributors have changed the same files in the past " + daysAgo + " days.",
                    "color: grey; font-size: 80%; margin-bottom: 12px;");
            addDependenciesViaSharedFiles(peopleDependencies, peopleFileDependencies, daysAgo);

            List<ContributorConnection> contributorConnections = ContributorConnectionUtils.getContributorConnections(peopleDependencies, contributors, contributionCounter);
            addContributors(contributorConnections);
            // Built once: contributorConnections(...) rebuilds and re-sorts the same map each call.
            List<ContributorConnections> connections = contributorConnections(peopleDependencies);
            String cMedian = getRoundedValueOf(ContributorConnectionUtils.getCMedian(connections));
            report.addParagraph("C-median: " + cMedian, "margin-bottom: 0; margin-top: 10px;");
            report.addParagraph("A half of the contributors has more than " + cMedian + " connections, and a half has less than this number.",
                    "color: grey; font-size: 80%; margin-bottom: 20px");
            report.addParagraph("C-mean: " + getRoundedValueOf(ContributorConnectionUtils.getCMean(connections)), "margin-bottom: 0;");
            report.addParagraph("An average number of connections a contributor has with other contributors.", "color: grey; font-size: 80%; margin-bottom: 20px");
            String cIndex = getRoundedValueOf(ContributorConnectionUtils.getCIndex(connections));
            report.addParagraph("C-index: " + cIndex, "margin-bottom: 0;");
            report.addParagraph("There are " + cIndex + " contributors with " + cIndex + " or more connections.", "color: grey; font-size: 80%; margin-bottom: 40px");

        }
    }

    private void addDependenciesViaSharedFiles(List<ComponentDependency> peopleDependencies, List<ComponentDependency> peopleFileDependencies, int daysAgo) {
        report.addLevel3Header("Contributor Dependencies via Shared Files", "margin-top: 20px");
        report.startDetailsBlock("contributor dependencies 2D graph");
        report.addParagraph("The number on lines shows the number of same files that both persons changed in past <b>" + daysAgo + "</b> days.", "color: grey");
        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setMaxNumberOfDependencies(100);
        graphvizDependencyRenderer.setTypeGraph();

        Set<String> emails = new HashSet<>();
        peopleDependencies.forEach(peopleDependency -> {
            emails.add(peopleDependency.getFromComponent());
            emails.add(peopleDependency.getToComponent());
        });

        String prefix = "people_dependencies_" + daysAgo + "_";
        String graphId = addDependencyGraphVisuals(peopleDependencies, new ArrayList<>(), graphvizDependencyRenderer, prefix);
        report.endDetailsBlock();
        report.addLineBreak();
        report.addNewTabLink("- open 2D force graph", "visuals/" + graphId + "_force_2d.html");
        report.addLineBreak();
        report.addNewTabLink("- open 3D force graph", "visuals/" + graphId + "_force_3d.html");
        report.addLineBreak();
        if (peopleFileDependencies != null) {
            String prefixFile = "people_dependencies_via_files_" + daysAgo + "_";
            String graphIdFile = add3DDependencyGraphVisuals(peopleFileDependencies, prefixFile);
            report.addNewTabLink("- open 2D force graph (including all files)", "visuals/" + graphIdFile + "_force_2d.html");
            report.addLineBreak();
            report.addNewTabLink("- open 2D force graph (including only shared files)", "visuals/" + graphIdFile + "_force_2d_only_shared_file.html");
            report.addLineBreak();
            report.addNewTabLink("- open 3D force graph (including all files)", "visuals/" + graphIdFile + "_force_3d.html");
            report.addLineBreak();
            report.addNewTabLink("- open 3D force graph (including only shared files)", "visuals/" + graphIdFile + "_force_3d_only_shared_file.html");
            report.addLineBreak();
        }
        report.addLineBreak();
        report.addLineBreak();
        addPeopleDependenciesTable(peopleDependencies);
    }

    private void addContributors(List<ContributorConnection> contributorConnections) {
        report.addLevel3Header("Most Connected Contributors", "margin-top: 20px");
        report.startScrollingDiv();
        report.startTable();
        report.addTableHeader("", "Contributor", "# connections", "# commits");
        int index[] = {0};
        contributorConnections.forEach(contributorConnection -> {
            index[0]++;
            report.startTableRow();
            report.addTableCell(index[0] + ".");
            if (StringUtils.isNotBlank(contributorConnection.getEmail()) && StringUtils.isNotBlank(contributorConnection.getUserName())) {
                report.addTableCell(contributorConnection.getUserName() + " <div style='color: grey; font-size: 80%; margin-bottom: 6px;'>&lt;" + contributorConnection.getEmail() + "&gt;</div>");
            } else {
                report.addTableCell((contributorConnection.getUserName() + contributorConnection.getEmail()).trim());
            }
            report.addTableCell(contributorConnection.getCount() + "");
            report.addTableCell(contributorConnection.getCommits() + "");

            report.endTableRow();
        });
        report.endTable();
        report.endDiv();
    }

    private void addPeopleDependenciesTable(List<ComponentDependency> peopleDependencies) {
        report.startScrollingDiv();
        report.startTable();
        report.addTableHeader("", "Contributor 1", "Contributor 2", "# shared files");
        int index[] = {0};
        if (peopleDependencies.size() > 100) {
            peopleDependencies = peopleDependencies.subList(0, 100);
        }
        peopleDependencies.forEach(dependency -> {
            index[0]++;
            int count = dependency.getCount();

            report.startTableRow();
            report.addTableCell(index[0] + ".");

            String from = dependency.getFromComponent() + "";
            String to = dependency.getToComponent() + "";

            if (emailContributorMap.containsKey(from) && StringUtils.isNotBlank(emailContributorMap.get(from).getUserName())) {
                report.addTableCell(emailContributorMap.get(from).getUserName() + " <div style='color: grey; font-size: 80%; margin-bottom: 6px;'>&lt;" + from + "&gt;</div>");
            } else {
                report.addTableCell(from);
            }

            if (emailContributorMap.containsKey(to) && StringUtils.isNotBlank(emailContributorMap.get(to).getUserName())) {
                report.addTableCell(emailContributorMap.get(to).getUserName() + " <div style='color: grey; font-size: 80%; margin-bottom: 6px;'>&lt;" + to + "&gt;</div>");
            } else {
                report.addTableCell(to);
            }


            report.startTableCell();
            report.startDetailsBlock(count + " shared " + (count == 1 ? "file" : "files"));
            addSharedFiles(dependency);
            report.endDetailsBlock();
            report.endTableCell();
            report.endTableRow();
        });
        report.endTable();
        report.endDiv();
    }

    private void addSharedFiles(ComponentDependency dependency) {
        List<String> data = dependency.getData();
        boolean tooLong = data.size() > 100;
        if (tooLong) {
            data = data.subList(0, 100);
        }
        data.forEach(path -> report.addHtmlContent("<br>" + path));
        if (tooLong) {
            report.addHtmlContent("<br>...");
        }
    }

    private String getRoundedValueOf(double value) {
        return "" + (((int) (10 * value)) / 10.0);
    }

    private String addDependencyGraphVisuals(List<ComponentDependency> componentDependencies, List<String> componentNames, GraphvizDependencyRenderer graphvizDependencyRenderer, String prefix) {
        String graphvizContent = graphvizDependencyRenderer.getMermaidContent(
                componentNames,
                componentDependencies);
        String graphId = prefix + dependencyVisualCounter++;
        report.addGraphvizFigure(graphId, "", graphvizContent);
        report.addLineBreak();
        report.addLineBreak();
        VisualizationTools.addDownloadLinks(report, graphId);

        export3DForceGraph(componentDependencies, graphId);

        return graphId;
    }

    private String add3DDependencyGraphVisuals(List<ComponentDependency> componentDependencies, String prefix) {
        String graphId = prefix + dependencyVisualCounter++;
        export3DForceGraph(componentDependencies, graphId);

        return graphId;
    }

    private void export3DForceGraph(List<ComponentDependency> componentDependencies, String graphId) {
        Force3DObject force3DObject = new Force3DObject();
        Force3DObject force3DObjectOnlyLinked = new Force3DObject();
        Map<String, Integer> names = new HashMap<>();
        componentDependencies.forEach(dependency -> {
            String from = dependency.getFromComponent();
            String to = dependency.getToComponent();
            if (names.containsKey(from)) {
                names.put(from, names.get(from) + 1);
            } else {
                names.put(from, 1);
            }
            if (names.containsKey(to)) {
                names.put(to, names.get(to) + 1);
            } else {
                names.put(to, 1);
            }
            force3DObject.getLinks().add(new Force3DLink(from, to, dependency.getCount()));
            force3DObject.getLinks().add(new Force3DLink(to, from, dependency.getCount()));
        });
        names.keySet().forEach(key -> {
            force3DObject.getNodes().add(new Force3DNode(key, names.get(key)));
        });

        force3DObjectOnlyLinked.setNodes(force3DObject.getNodes().stream().filter(n -> n.getSize() > 1).collect(Collectors.toList()));
        Map<String, Force3DNode> force3DNodeMapLinked = new HashMap<>();
        force3DObjectOnlyLinked.getNodes().forEach(node -> force3DNodeMapLinked.put(node.getId(), node));
        force3DObjectOnlyLinked.setLinks(force3DObject.getLinks().stream()
                .filter(l -> force3DNodeMapLinked.containsKey(l.getSource()) && force3DNodeMapLinked.containsKey(l.getTarget()))
                .collect(Collectors.toList()));

        File folder = new File(reportsFolder, "html/visuals");
        folder.mkdirs();

        try {
            FileUtils.write(new File(folder, graphId + "_force_2d.html"), new VisualizationTemplate().render2DForceGraph(force3DObject), UTF_8);
            FileUtils.write(new File(folder, graphId + "_force_2d_only_shared_file.html"), new VisualizationTemplate().render2DForceGraph(force3DObjectOnlyLinked), UTF_8);
            FileUtils.write(new File(folder, graphId + "_force_3d.html"), new VisualizationTemplate().render3DForceGraph(force3DObject), UTF_8);
            FileUtils.write(new File(folder, graphId + "_force_3d_only_shared_file.html"), new VisualizationTemplate().render3DForceGraph(force3DObjectOnlyLinked), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addContributorsPanel(RichTextReport report, List<Contributor> contributors
            , ContributionCounter contributionCounter, boolean showPerExtension, PerExtensionCounter perExtensionCounter, String type) {
        // Default churn accessors: all-time (used by callers that don't scope to a period).
        addContributorsPanel(report, contributors, contributionCounter, showPerExtension, perExtensionCounter, type,
                Contributor::getLinesAdded, Contributor::getLinesDeleted);
    }

    // churnAdded/churnDeleted return the line churn for the SAME period the contributionCounter
    // counts, so the Line Churn column matches the tab (30d / 3m / 6m / 1y) instead of all-time.
    public void addContributorsPanel(RichTextReport report, List<Contributor> contributors
            , ContributionCounter contributionCounter, boolean showPerExtension, PerExtensionCounter perExtensionCounter, String type
            , ContributionCounter churnAdded, ContributionCounter churnDeleted) {
        int count = contributors.size();
        if (count == 0) {
            return;
        }
        report.addLineBreak();
        int total[] = {0};
        contributors.forEach(contributor -> total[0] += contributionCounter.count(contributor));
        if (total[0] > 0) {
            report.addParagraph("<b>" + FormattingUtils.formatCount(count) + "</b> " + (count == 1 ? type.toLowerCase() : type.toLowerCase() + "s") + " (" + "<b>" + FormattingUtils.formatCount(total[0]) + "</b> " + (count == 1 ? "commit" : "commits") + "):");
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
                            + type.toLowerCase() + "s together ("
                            + FormattingUtils.getFormattedPercentage(100.0 * index[0] / contributors.size())
                            + "% of " + type.toLowerCase() + "s) = "
                            + FormattingUtils.getFormattedPercentage(100.0 * cumulative[0] / total[0])
                            + "% of all commits";
                }
                map.append("<div style='background-color: " + palette.nextColor() + "; display: inline-block; height: 20px; width: " + w + "px' title='" + contributor.getEmail() + "\n" + contributorCommitsCount + " commits (" + (Math.round(100.0 * contributorCommitsCount / total[0])) + "%)" + cumulativeText + "'>&nbsp;</div>");
            });
            report.addHtmlContent(map.toString());
            report.addLineBreak();
            report.addLineBreak();
        }
        report.startScrollingDiv();
        report.startTable();
        if (showPerExtension && perExtensionCounter != null) {
            report.addTableHeader("#", type + "<br>", "First<br>Commit", "Latest<br>Commit", "Commits<br>Count", "Line<br>Churn", "File Updates<br>(per extension)");
        } else {
            report.addTableHeader("#", type + "<br>", "First<br>Commit", "Latest<br>Commit", "Commits<br>Count", "Line<br>Churn");
        }
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
            if (StringUtils.isNotBlank(contributor.getEmail()) && StringUtils.isNotBlank(contributor.getUserName())) {
                report.addTableCell(contributor.getUserName() + " <div style='color: grey; font-size: 80%; margin-bottom: 6px;'>&lt;" + contributor.getEmail() + "&gt;</div>");
            } else {
                report.addTableCell((contributor.getUserName() + contributor.getEmail()).trim());
            }

            report.addTableCell(contributor.getFirstCommitDate());
            report.addTableCell(contributor.getLatestCommitDate());
            int contributorCommitsCount = contributionCounter.count(contributor);
            String formattedCount = FormattingUtils.formatCount(contributorCommitsCount);
            String formattedPercentage = FormattingUtils.getFormattedPercentage(100.0 * contributorCommitsCount / total[0]);
            report.addTableCell(formattedCount + " (" + formattedPercentage + "%)");

            // Line churn for this tab's period (added/deleted lines). 0 for histories without churn
            // columns, shown as a dim placeholder.
            int linesAdded = churnAdded.count(contributor);
            int linesDeleted = churnDeleted.count(contributor);
            if (linesAdded > 0 || linesDeleted > 0) {
                report.addTableCell("<span style='color: #2e7d32;'>+" + FormattingUtils.getSmallTextForNumber(linesAdded) + "</span> / "
                                + "<span style='color: #c62828;'>-" + FormattingUtils.getSmallTextForNumber(linesDeleted) + "</span>",
                        "white-space: nowrap;");
            } else {
                report.addTableCell("<span style='color: lightgrey;'>-</span>");
            }

            if (showPerExtension && perExtensionCounter != null) {
                // A contributor present in the contributors list may have no per-extension stats
                // (e.g. their commits only touched files excluded from the per-extension aggregation),
                // so emailStatsMap.get(...) can be null - default to an empty list rather than NPE.
                List<Pair<String, ContributorPerExtensionStats>> stats =
                        emailStatsMap.getOrDefault(contributor.getEmail(), Collections.emptyList());
                String perExtension = stats.stream()
                        .filter(e -> perExtensionCounter.count(e.getRight()) > 0)
                        .sorted((a, b) -> perExtensionCounter.count(b.getRight()) - perExtensionCounter.count(a.getRight()))
                        .limit(5)
                        .map(s -> s.getLeft() + " (" + perExtensionCounter.count(s.getRight()) + ")")
                        .collect(Collectors.joining(", "));

                report.addTableCell(perExtension + "");
            }

            report.endTableRow();
        });

        report.endTable();
        report.endDiv();
    }

    static interface PerExtensionCounter {
        int count(ContributorPerExtensionStats perExtensionStats);
    }
}

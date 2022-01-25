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
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.ContributorPerExtensionStats;
import nl.obren.sokrates.sourcecode.landscape.ContributionCounter;
import nl.obren.sokrates.sourcecode.landscape.ContributorConnection;
import nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorConnections;
import org.apache.commons.io.FileUtils;
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
    private Map<String, List<Pair<String, ContributorPerExtensionStats>>> emailStatsMap = new HashMap<>();

    public ContributorsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
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

        report.addParagraph("An overview of commit and contributor trends.", "color: grey;");

        report.startTabGroup();
        report.addTab("visuals", "Overview", true);
        report.addTab("per_language", "Overview Per Language", false);
        report.addTab("contributors", "Contributors", false);
        report.addTab("matrix", "Contributors Matrix", false);
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
        report.addLineBreak();
        report.startSubSection("Timeline", "");
        report.addLevel2Header("Per Year", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        ContributorsReportUtils.addContributorsPerTimeSlot(report, analysis.getContributorsPerYear(), 20, true, 4);
        report.addLevel2Header("Per Month", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        List<ContributionTimeSlot> contributorsPerMonth = getContributionMonths(analysis, 25);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, contributorsPerMonth, 24, true, 2);
        report.addLevel2Header("Per Week", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        int pastWeeks = 104;
        List<ContributionTimeSlot> contributorsPerWeek = getContributionWeeks(analysis, pastWeeks);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, contributorsPerWeek, pastWeeks, true, 1);
        report.addLevel2Header("Per Day", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        int pastDays = 365;
        List<ContributionTimeSlot> contributorsPerDay = getContributionDays(analysis, pastDays);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, contributorsPerDay, pastDays, true, 1);
        report.endSection();
        report.endTabContentSection();

        report.startTabContentSection("contributors", false);
        ContributorsReportUtils.addContributorsSection(codeAnalysisResults, report);
        report.endTabContentSection();

        report.startTabContentSection("matrix", false);
        addMatrix(new ArrayList<>(contributors));
        report.endTabContentSection();

        addPerLanguageTabContent(report);

        report.startTabContentSection("all_time", false);
        addContributorsPanel(report, contributors, c -> c.getCommitsCount(), true, e -> e.getFileUpdates());
        renderPeopleDependencies(analysis.getPeopleDependenciesAllTime(), 35600, c -> c.getCommitsCount(), contributors);
        report.endTabContentSection();


        report.startTabContentSection("30_days", false);
        List<Contributor> commits30Days = contributors.stream().filter(c -> c.getCommitsCount30Days() > 0).collect(Collectors.toList());
        if (commits30Days.size() > 0) {
            commits30Days.sort((a, b) -> b.getCommitsCount30Days() - a.getCommitsCount30Days());
            addContributorsPanel(report, commits30Days, c -> c.getCommitsCount30Days(), true, e -> e.getFileUpdates30Days());
            renderPeopleDependencies(analysis.getPeopleDependencies30Days(), 30, c -> c.getCommitsCount30Days(), commits30Days);
        } else {
            report.addParagraph("No commits in past 30 days.", "margin-top: 16px");
        }
        report.endTabContentSection();

        report.startTabContentSection("90_days", false);
        List<Contributor> commits90Days = contributors.stream().filter(c -> c.getCommitsCount90Days() > 0).collect(Collectors.toList());
        if (commits90Days.size() > 0) {
            commits90Days.sort((a, b) -> b.getCommitsCount90Days() - a.getCommitsCount90Days());
            addContributorsPanel(report, commits90Days, c -> c.getCommitsCount90Days(), true, e -> e.getFileUpdates90Days());
            renderPeopleDependencies(analysis.getPeopleDependencies90Days(), 90, c -> c.getCommitsCount90Days(), commits90Days);
        } else {
            report.addParagraph("No commits in past 90 days.", "margin-top: 16px");
        }
        report.endTabContentSection();

        report.startTabContentSection("180_days", false);
        List<Contributor> commits180Days = contributors.stream().filter(c -> c.getCommitsCount180Days() > 0).collect(Collectors.toList());
        if (commits180Days.size() > 0) {
            commits180Days.sort((a, b) -> b.getCommitsCount180Days() - a.getCommitsCount180Days());
            addContributorsPanel(report, commits180Days, c -> c.getCommitsCount180Days(), false, null);
            renderPeopleDependencies(analysis.getPeopleDependencies180Days(), 180, c -> c.getCommitsCount180Days(), commits180Days);
        } else {
            report.addParagraph("No commits in past 180 days.", "margin-top: 16px");
        }
        report.endTabContentSection();

        report.startTabContentSection("365_days", false);
        List<Contributor> commits365Days = contributors.stream().filter(c -> c.getCommitsCount365Days() > 0).collect(Collectors.toList());
        commits365Days.sort((a, b) -> b.getCommitsCount365Days() - a.getCommitsCount365Days());
        addContributorsPanel(report, commits365Days, c -> c.getCommitsCount365Days(), false, null);
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

    public void addPerLanguageTabContent(RichTextReport report) {
        report.startTabContentSection("per_language", false);
        report.startTable();
        report.startTableRow();
        report.addTableCell("Commits", "border: none");
        report.startTableCell("border: none");
        List<HistoryPerExtension> historyPerExtensionPerYear = codeAnalysisResults.getFilesHistoryAnalysisResults().getHistoryPerExtensionPerYear();
        List<String> extensions = codeAnalysisResults.getMainAspectAnalysisResults().getExtensions();
        HistoryPerLanguageGenerator.getInstanceCommits(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(report);
        report.endTableCell();
        report.endTableRow();
        report.startTableRow();
        report.addTableCell("&nbsp;", "border: none");
        report.addTableCell("&nbsp;", "border: none");
        report.endTableRow();
        report.startTableRow();
        report.addTableCell("Contributors", "border: none");
        report.startTableCell("border: none");
        HistoryPerLanguageGenerator.getInstanceContributors(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(report);
        report.endTableCell();
        report.endTableRow();
        report.endTable();
        report.endTabContentSection();
    }

    private void addMatrix(List<Contributor> contributors) {
        report.addContentInDiv("&nbsp;", "height: 20px");
        report.startSubSection("Contributors Matrix (Per Month)", "");
        report.startDiv("width: 100%; overflow-x: scroll; overflow-y: scroll; max-height: 600px");
        report.startTable();

        final List<String> pastMonths = DateUtils.getPastMonths(24, DateUtils.getAnalysisDate());
        report.startTableRow();
        report.addTableCell("", "min-width: 200px; border: none; border: none");
        report.addTableCell("Commits<br>(3m)", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("Commit<br>Days", "max-width: 100px; text-align: center; border: none");
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
            report.addHtmlContent(contributor.getEmail());
            report.endTableCell();
            report.addTableCell(contributor.getCommitsCount90Days() > 0 ? contributor.getCommitsCount90Days() + "" : "-", "text-align: center; border: none; " + textOpacity);
            report.addTableCell(contributor.getCommitDates().size() + "", "text-align: center; border: none; " + textOpacity);
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

    private List<ContributionTimeSlot> getContributionWeeks(ContributorsAnalysisResults analysis, int pastWeeks) {
        List<ContributionTimeSlot> activeWeeks = analysis.getContributorsPerWeek();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        activeWeeks.forEach(week -> map.put(week.getTimeSlot(), week));

        List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>();
        List<String> pastDates = DateUtils.getPastWeeks(pastWeeks, analysis.getLatestCommitDate());
        pastDates.forEach(pastDate -> {
            ContributionTimeSlot contributionTimeSlot = map.get(pastDate);
            if (contributionTimeSlot != null) {
                contributorsPerWeek.add(contributionTimeSlot);
            } else {
                contributorsPerWeek.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerWeek;
    }

    private List<ContributionTimeSlot> getContributionMonths(ContributorsAnalysisResults analysis, int pastMonths) {
        List<ContributionTimeSlot> activeMonth = analysis.getContributorsPerMonth();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        activeMonth.forEach(month -> map.put(month.getTimeSlot(), month));

        List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>();
        List<String> pastDates = DateUtils.getPastMonths(pastMonths, analysis.getLatestCommitDate());
        pastDates.forEach(pastDate -> {
            ContributionTimeSlot contributionTimeSlot = map.get(pastDate);
            if (contributionTimeSlot != null) {
                contributorsPerMonth.add(contributionTimeSlot);
            } else {
                contributorsPerMonth.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerMonth;
    }

    private List<ContributionTimeSlot> getContributionDays(ContributorsAnalysisResults analysis, int pastDays) {
        List<ContributionTimeSlot> activeDays = analysis.getContributorsPerDay();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        activeDays.forEach(activeDay -> map.put(activeDay.getTimeSlot(), activeDay));

        List<ContributionTimeSlot> contributorsPerDay = new ArrayList<>();
        List<String> pastDates = DateUtils.getPastDays(pastDays, analysis.getLatestCommitDate());
        pastDates.forEach(pastDate -> {
            ContributionTimeSlot contributionTimeSlot = map.get(pastDate);
            if (contributionTimeSlot != null) {
                contributorsPerDay.add(contributionTimeSlot);
            } else {
                contributorsPerDay.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerDay;
    }

    private void renderPeopleDependencies(List<ComponentDependency> peopleDependencies, int daysAgo,
                                          ContributionCounter contributionCounter,
                                          List<Contributor> contributors) {
        if (peopleDependencies.size() > 0) {
            report.addLevel2Header("Contributor Dependencies", "margin-top: 40px; margin-bottom: 0;");
            report.addParagraph("A contributor dependency is detected if two contributors have changed the same files in the past " + daysAgo + " days.",
                    "color: grey; font-size: 80%; margin-bottom: 12px;");

            List<ContributorConnection> contributorConnections = ContributorConnectionUtils.getContributorConnections(peopleDependencies, contributors, contributionCounter);
            String cMedian = getRoundedValueOf(ContributorConnectionUtils.getCMedian(contributorConnections(peopleDependencies)));
            report.addParagraph("C-median: " + cMedian, "margin-bottom: 0;");
            report.addParagraph("A half of the contributors has more than " + cMedian + " connections, and a half has less than this number.",
                    "color: grey; font-size: 80%; margin-bottom: 20px");
            report.addParagraph("C-mean: " + getRoundedValueOf(ContributorConnectionUtils.getCMean(contributorConnections(peopleDependencies))), "margin-bottom: 0;");
            report.addParagraph("An average number of connections a contributor has with other contributors.", "color: grey; font-size: 80%; margin-bottom: 20px");
            String cIndex = getRoundedValueOf(ContributorConnectionUtils.getCIndex(contributorConnections(peopleDependencies)));
            report.addParagraph("C-index: " + cIndex, "margin-bottom: 0;");
            report.addParagraph("There are " + cIndex + " contributors with " + cIndex + " or more connections.", "color: grey; font-size: 80%; margin-bottom: 40px");
            addContributors(contributorConnections);

            report.addLevel3Header("Contributor Dependencies via Shared Files", "margin-top: 42px");
            report.startShowMoreBlock("show graph...");
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
            report.endShowMoreBlock();
            report.addLineBreak();
            report.addNewTabLink("- open 3D force graph", "visuals/" + graphId + "_force_3d.html");
            report.addLineBreak();
            report.addLineBreak();
            addPeopleDependenciesTable(peopleDependencies);
        }
    }

    private void addContributors(List<ContributorConnection> contributorConnections) {
        report.addLevel3Header("Most Connected Contributors");
        report.startScrollingDiv();
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
            report.addTableCell(dependency.getFromComponent());
            report.addTableCell(dependency.getToComponent() + "");

            report.startTableCell();
            report.startShowMoreBlock(count + " shared " + (count == 1 ? "file" : "files"));
            addSharedFiles(dependency);
            report.endShowMoreBlock();
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
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(
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

    private void export3DForceGraph(List<ComponentDependency> componentDependencies, String graphId) {
        Force3DObject force3DObject = new Force3DObject();
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
        File folder = new File(reportsFolder, "html/visuals");
        folder.mkdirs();
        try {
            FileUtils.write(new File(folder, graphId + "_force_3d.html"), new VisualizationTemplate().render3DForceGraph(force3DObject), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addContributorsPanel(RichTextReport report, List<Contributor> contributors
            , ContributionCounter contributionCounter, boolean showPerExtension, PerExtensionCounter perExtensionCounter) {
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
        report.startScrollingDiv();
        report.startTable();
        if (showPerExtension && perExtensionCounter != null) {
            report.addTableHeader("#", "Contributor<br>", "First<br>Commit", "Latest<br>Commit", "Commits<br>Count", "File Updates<br>(per extension)");
        } else {
            report.addTableHeader("#", "Contributor<br>", "First<br>Commit", "Latest<br>Commit", "Commits<br>Count");
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
            report.addTableCell(contributor.getEmail());
            report.addTableCell(contributor.getFirstCommitDate());
            report.addTableCell(contributor.getLatestCommitDate());
            int contributorCommitsCount = contributionCounter.count(contributor);
            String formattedCount = FormattingUtils.formatCount(contributorCommitsCount);
            String formattedPercentage = FormattingUtils.getFormattedPercentage(100.0 * contributorCommitsCount / total[0]);
            report.addTableCell(formattedCount + " (" + formattedPercentage + "%)");

            if (showPerExtension && perExtensionCounter != null) {
                String perExtension = emailStatsMap.get(contributor.getEmail()).stream()
                        .filter(e -> perExtensionCounter.count(e.getRight()) > 0)
                        .sorted((a, b) -> perExtensionCounter.count(b.getRight()) - perExtensionCounter.count(a.getRight()))
                        .limit(5)
                        .map(stats -> stats.getLeft() + " (" + perExtensionCounter.count(stats.getRight()) + ")")
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

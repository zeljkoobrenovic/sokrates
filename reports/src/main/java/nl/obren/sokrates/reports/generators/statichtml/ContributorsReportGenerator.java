/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ContributorsReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private int dependencyVisualCounter = 1;
    private RichTextReport report;


    public ContributorsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addContributorsAnalysisToReport(RichTextReport report) {
        this.report = report;
        ContributorsReportUtils.addContributorsPerYear(report, codeAnalysisResults.getContributorsAnalysisResults().getContributorsPerYear());
        ;
        report.addLineBreak();

        report.startTabGroup();
        report.addTab("visuals", "Overview", true);
        report.addTab("30_days", "Past 30 Days", false);
        report.addTab("90_days", "Past 3 Months", false);
        report.addTab("180_days", "Past 6 Months", false);
        report.addTab("365_days", "Past Year", false);
        report.addTab("all_time", "All Time", false);
        report.addTab("data", "Data", false);
        report.endTabGroup();

        List<Contributor> contributors = codeAnalysisResults.getContributorsAnalysisResults().getContributors();

        report.startTabContentSection("visuals", true);
        ContributorsReportUtils.addContributorsSection(codeAnalysisResults, report);
        report.endTabContentSection();

        report.startTabContentSection("all_time", false);
        addContributorsPanel(report, contributors, c -> c.getCommitsCount());
        renderPeopleDependencies(35600);
        report.endTabContentSection();

        report.startTabContentSection("30_days", false);
        List<Contributor> commits30Days = contributors.stream().filter(c -> c.getCommitsCount30Days() > 0).collect(Collectors.toList());
        commits30Days.sort((a, b) -> b.getCommitsCount30Days() - a.getCommitsCount30Days());
        addContributorsPanel(report, commits30Days, c -> c.getCommitsCount30Days());
        renderPeopleDependencies(30);
        report.endTabContentSection();

        report.startTabContentSection("90_days", false);
        List<Contributor> commits90Days = contributors.stream().filter(c -> c.getCommitsCount90Days() > 0).collect(Collectors.toList());
        commits90Days.sort((a, b) -> b.getCommitsCount90Days() - a.getCommitsCount90Days());
        addContributorsPanel(report, commits90Days, c -> c.getCommitsCount90Days());
        renderPeopleDependencies(90);
        report.endTabContentSection();

        report.startTabContentSection("180_days", false);
        List<Contributor> commits180Days = contributors.stream().filter(c -> c.getCommitsCount180Days() > 0).collect(Collectors.toList());
        commits180Days.sort((a, b) -> b.getCommitsCount180Days() - a.getCommitsCount180Days());
        addContributorsPanel(report, commits180Days, c -> c.getCommitsCount180Days());
        renderPeopleDependencies(180);
        report.endTabContentSection();

        report.startTabContentSection("365_days", false);
        List<Contributor> commits365Days = contributors.stream().filter(c -> c.getCommitsCount365Days() > 0).collect(Collectors.toList());
        commits365Days.sort((a, b) -> b.getCommitsCount365Days() - a.getCommitsCount365Days());
        addContributorsPanel(report, commits365Days, c -> c.getCommitsCount365Days());
        renderPeopleDependencies(365);
        report.endTabContentSection();

        report.startTabContentSection("data", false);
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("Contributors' details...", "../data/text/contributors.txt");
        report.endListItem();
        report.endUnorderedList();
        report.endTabContentSection();
    }

    private void renderPeopleDependencies(int daysAgo) {
        report.addLevel2Header("People Dependencies", "margin-top: 40px");
        report.startShowMoreBlock("show graph...");
        report.addParagraph("The number on lines shows the number of same files that both persons changed in past <b>" + daysAgo + "</b> days.", "color: grey");
        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setMaxNumberOfDependencies(100);
        graphvizDependencyRenderer.setType("graph");
        graphvizDependencyRenderer.setArrow("--");

        List<ComponentDependency> peopleDependencies = this.getPeopleDependencies(daysAgo);
        Set<String> emails = new HashSet<>();
        peopleDependencies.forEach(peopleDependency -> {
            emails.add(peopleDependency.getFromComponent());
            emails.add(peopleDependency.getToComponent());
        });

        String prefix = "people_dependencies_" + daysAgo + "_";
        addDependencyGraphVisuals(peopleDependencies, new ArrayList<>(), graphvizDependencyRenderer, prefix);
        report.endShowMoreBlock();

        List<ContributorConnection> contributorConnections = getContributorConnections(peopleDependencies);
        report.addParagraph("C-index: " + getCIndex(contributorConnections));
        report.startTable();
        report.addTableHeader("", "Contributor", "# connections");
        int index[] = {0};
        contributorConnections.forEach(contributorConnection -> {
            index[0]++;
            report.startTableRow();
            report.addTableCell(index[0] + ".");
            report.addTableCell(contributorConnection.getEmail());
            report.addTableCell(contributorConnection.getCount() + "");
            report.endTableRow();
        });
        report.endTable();
    }

    private int getCIndex(List<ContributorConnection> contributorConnections) {
        int index[] = {0};

        contributorConnections.sort((a, b) -> b.getCount() - a.getCount());

        contributorConnections.forEach(contributorConnection -> {
            if (contributorConnection.getCount() > index[0]) {
                index[0]++;
            }
        });

        return index[0];
    }

    private List<ContributorConnection> getContributorConnections(List<ComponentDependency> peopleDependencies) {
        List<ContributorConnection> contributorConnections = new ArrayList<>();
        Map<String, ContributorConnection> map = new HashMap<>();
        peopleDependencies.forEach(dependency -> {
            String email1 = dependency.getFromComponent();
            String email2 = dependency.getToComponent();
            if (!email1.equalsIgnoreCase(email2)) {
                ContributorConnection contributorConnection1 = map.get(email1);
                if (contributorConnection1 == null) {
                    contributorConnection1 = new ContributorConnection(email1, 1);
                    map.put(email1, contributorConnection1);
                    contributorConnections.add(contributorConnection1);
                } else {
                    contributorConnection1.setCount(contributorConnection1.getCount() + 1);
                }
                ContributorConnection contributorConnection2 = map.get(email2);
                if (contributorConnection2 == null) {
                    contributorConnection2 = new ContributorConnection(email2, 1);
                    map.put(email2, contributorConnection2);
                    contributorConnections.add(contributorConnection2);
                } else {
                    contributorConnection2.setCount(contributorConnection2.getCount() + 1);
                }
            }
        });
        contributorConnections.sort((a, b) -> b.getCount() - a.getCount());
        return contributorConnections;
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
        report.addNewTabLink("(open online Graphviz editor)", "https://www.zeljkoobrenovic.com/tools/graphviz/");
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
            report.addParagraph("<b>" + FormattingUtils.getFormattedCount(count) + "</b> " + (count == 1 ? "contributor" : "contributors") + " (" + "<b>" + FormattingUtils.getFormattedCount(total[0]) + "</b> " + (count == 1 ? "commit" : "commits") + "):");
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
            String formattedCount = FormattingUtils.getFormattedCount(contributorCommitsCount);
            String formattedPercentage = FormattingUtils.getFormattedPercentage(100.0 * contributorCommitsCount / total[0]);
            report.addTableCell(formattedCount + " (" + formattedPercentage + "%)");
            report.endTableRow();
        });

        report.endTable();
    }

    private List<ComponentDependency> getPeopleDependencies(int daysAgo) {
        Map<String, List<String>> contributionMap = new HashMap<>();

        codeAnalysisResults.getFilesHistoryAnalysisResults().getHistory().forEach(fileModificationHistory -> {
            fileModificationHistory.getCommits().stream()
                    .filter(commit -> DateUtils.isCommittedLessThanDaysAgo(commit.getDate(), daysAgo))
                    .forEach(commit -> {
                        String path = fileModificationHistory.getPath();
                        String email = commit.getEmail();
                        if (contributionMap.containsKey(path)) {
                            List<String> emails = contributionMap.get(path);
                            if (!emails.contains(email)) {
                                emails.add(email);
                            }
                        } else {
                            contributionMap.put(path, new ArrayList<>(Arrays.asList(email)));
                        }
                    });

        });

        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();

        contributionMap.keySet().forEach(path -> {
            List<String> emails = contributionMap.get(path);
            emails.forEach(email1 -> {
                emails.forEach(email2 -> {
                    if (email1.equalsIgnoreCase(email2)) return;

                    String key1 = email1 + "::" + email2;
                    String key2 = email2 + "::" + email1;

                    if (dependenciesMap.containsKey(key1)) {
                        dependenciesMap.get(key1).increment(1);
                    } else if (dependenciesMap.containsKey(key2)) {
                        dependenciesMap.get(key2).increment(1);
                    } else {
                        ComponentDependency dependency = new ComponentDependency(email1, email2);
                        dependenciesMap.put(key1, dependency);
                        dependencies.add(dependency);
                    }
                });
            });
        });
        dependencies.sort((a, b) -> b.getCount() - a.getCount());
        return dependencies;
    }

    interface ContributionCounter {
        int count(Contributor contributor);
    }

    class ContributorConnection {
        private String email;
        private int count;

        public ContributorConnection() {
        }

        public ContributorConnection(String email, int count) {
            this.email = email;
            this.count = count;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}

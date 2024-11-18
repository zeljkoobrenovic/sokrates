/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorConnections;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

public class LandscapeReportPeopleTopologyTab {

    public static final String REMAINDER = "Undefined Team";

    private static final Log LOG = LogFactory.getLog(LandscapeReportPeopleTopologyTab.class);
    public static final String PEOPLE_COLOR = "#ADD8E6";
    private final List<ContributorRepositories> contributors;
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private int dependencyVisualCounter = 1;
    private File folder;
    private File reportsFolder;
    private Map<String, List<String>> contributorsPerWeekMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerWeekMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerDayMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerDayMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerMonthMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerMonthMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerYearMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerYearMap = new HashMap<>();
    private RichTextReport landscapeReport;
    private final LandscapeReportContributorsTab.Type type;
    private final TeamsConfig teamsConfig;

    public LandscapeReportPeopleTopologyTab(LandscapeAnalysisResults landscapeAnalysisResults, List<ContributorRepositories> contributors, RichTextReport landscapeReport, File folder, File reportsFolder, LandscapeReportContributorsTab.Type type, TeamsConfig teamsConfig) {
        this.contributors = contributors;
        this.folder = folder;
        this.reportsFolder = reportsFolder;
        this.landscapeReport = landscapeReport;
        this.type = type;
        this.teamsConfig = teamsConfig;

        this.landscapeAnalysisResults = landscapeAnalysisResults;

        populateTimeSlotMaps();
    }

    void render30DaysTopology() {
        landscapeReport.startSubSection(StringUtils.capitalize(type.singular()) + " Topology (past 30 days)", "");

        boolean recentlyActive = landscapeAnalysisResults.getRecentContributorsCount(contributors) > 0;
        String prefix = isContributorReport() ? "people" : "teams";
        if (recentlyActive) {
            landscapeReport.addParagraph("The diagram shows contributor collaborations defined as working on " +
                    "the same repositories in the past 30 days. The lines display the number of shared repositories " +
                    "between two contributors.\n", "color: grey");
            landscapeReport.addNewTabLink("<div style='font-weight: bold; font-size: 110%; margin-bottom: 8px;'>2D force graph (including repositories)&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON + "</div>", "visuals/" + prefix + "_dependencies_including_repositories_30_2_force_2d.html");
            landscapeReport.addNewTabLink("<div style='font-weight: bold; font-size: 110%; margin-bottom: 8px;'>3D force graph (including repositories)&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON + "</div>", "visuals/" + prefix + "_dependencies_including_repositories_30_2_force_3d.html");
            landscapeReport.startDiv("font-size: 90%");
            landscapeReport.addHtmlContent("direct links: ");
            landscapeReport.addNewTabLink("graphviz", "visuals/" + prefix + "_dependencies_30_1.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("graphviz (including contributors)", "visuals/" + prefix + "_dependencies_including_repositories_30_2.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("2D force graph", "visuals/" + prefix + "_dependencies_30_1_force_2d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("3D force graph", "visuals/" + prefix + "_dependencies_30_1_force_3d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("data", "data/repository_shared_repositories_30_days.txt");
            landscapeReport.addHtmlContent("&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON);
            landscapeReport.endDiv();
            landscapeReport.addLineBreak();
            landscapeReport.addHtmlContent("<iframe src=\"visuals/" + prefix + "_dependencies_30_1.svg\" " +
                    "style=\"border: 1px solid lightgrey; width: 100%; height: 600px\"></iframe>");
        } else {
            landscapeReport.addParagraph("No commits in past 30 days.", "color: grey");
        }
        landscapeReport.endSection();
    }

    void renderDetails() {
        if (!isContributorReport()) {
            landscapeReport.startDiv("display: none");
        }
        landscapeReport.startSubSection(StringUtils.capitalize(type.singular()) + " Dependencies Details", "");

        List<ComponentDependency> peopleDependencies30Days = landscapeAnalysisResults.getPeopleDependencies30Days();
        List<ComponentDependency> peopleRepositoryDependencies30Days = landscapeAnalysisResults.getPeopleRepositoryDependencies30Days();
        List<ContributorConnections> connectionsViaRepositories30Days = landscapeAnalysisResults.getConnectionsViaRepositories30Days();
        this.renderPeopleDependencies(peopleDependencies30Days, peopleRepositoryDependencies30Days, connectionsViaRepositories30Days, 30);

        List<ComponentDependency> peopleDependencies90Days = landscapeAnalysisResults.getPeopleDependencies90Days();
        List<ContributorConnections> connectionsViaRepositories90Days = landscapeAnalysisResults.getConnectionsViaRepositories90Days();
        this.renderPeopleDependencies(peopleDependencies90Days, null, connectionsViaRepositories90Days, 90);

        List<ComponentDependency> peopleDependencies180Days = landscapeAnalysisResults.getPeopleDependencies180Days();
        List<ContributorConnections> connectionsViaRepositories180Days = landscapeAnalysisResults.getConnectionsViaRepositories180Days();
        this.renderPeopleDependencies(peopleDependencies180Days, null, connectionsViaRepositories180Days, 180);

        landscapeReport.endSection();
        if (!isContributorReport()) {
            landscapeReport.endDiv();
        }
    }

    void renderRepoAndKnowlegeTopologies() {
        boolean recentlyActive = landscapeAnalysisResults.getRecentContributorsCount(contributors) > 0;
        String prefix = isContributorReport() ? "people" : "teams";
        landscapeReport.startSubSection("Repository Topology (past 30 days)", "");
        if (recentlyActive) {
            landscapeReport.addParagraph("The diagram shows repository dependencies defined as having the same " +
                    "contributors working on the same repositories in the past 30 days. " +
                    "The lines between repositories display the number of contributors working on both repositories.", "color: grey");
            landscapeReport.addNewTabLink("graphviz", "visuals/repository_dependencies_30_3.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("graphviz (including contributors)", "visuals/" + prefix + "_dependencies_including_repositories_30_2.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("2D force graph", "visuals/repository_dependencies_30_3_force_2d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("2D force graph (including contributors)", "visuals/" + prefix + "_dependencies_including_repositories_30_2_force_2d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("3D force graph", "visuals/repository_dependencies_30_3_force_3d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("3D force graph (including contributors)", "visuals/" + prefix + "_dependencies_including_repositories_30_2_force_3d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("data", "data/repository_shared_repositories_30_days.txt");
            landscapeReport.addHtmlContent("&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON);
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.addHtmlContent("<iframe src=\"visuals/repository_dependencies_30_3.svg\" " +
                    "style=\"border: 1px solid lightgrey; width: 100%; height: 600px\"></iframe>");
        } else {
            landscapeReport.addParagraph("No commits in past 30 days.", "color: grey");
        }
        landscapeReport.endSection();
        landscapeReport.startSubSection("Knowledge Topology (past 30 days)", "");
        if (recentlyActive) {
            landscapeReport.addParagraph("The diagram shows dependencies between programming languages (file extensions) defined as having the same contributors committing to files with these extensions in the past 30 days. " +
                    "The lines between repositories display the number of contributors committing to files with both extensions in ht past 30 days.", "color: grey");
            landscapeReport.addNewTabLink("graphviz", "visuals/extension_dependencies_30d.svg");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("2D force graph", "visuals/extension_dependencies_30d_force_2d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("3D force graph", "visuals/extension_dependencies_30d_force_3d.html");
            landscapeReport.addHtmlContent("&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON);
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.addHtmlContent("<iframe src=\"visuals/extension_dependencies_30d.svg\" " +
                    "style=\"border: 1px solid lightgrey; width: 100%; height: 600px\"></iframe>");
        } else {
            landscapeReport.addParagraph("No commits in past 30 days.", "color: grey");
        }
        landscapeReport.endSection();
        landscapeReport.addLineBreak();
    }

    private String getTeamOf(String email) {
        if (email.startsWith("[")) return email;

        for (TeamConfig team : teamsConfig.getTeams()) {
            if (RegexUtils.matchesAnyPattern(email, team.getEmailPatterns())) {
                return team.getName();
            }
        }

        return REMAINDER;
    }

    private List<ComponentDependency> getTeamDependencies(List<ComponentDependency> peopleDependencies) {
        List<ComponentDependency> teamDependencies = new ArrayList<>();
        Map<String, ComponentDependency> map = new HashMap<>();

        for (ComponentDependency dependency : peopleDependencies) {
            String team1 = getTeamOf(dependency.getFromComponent());
            String team2 = getTeamOf(dependency.getToComponent());

            if (!team1.equals(team2)) {
                String key1 = team1 + "::" + team2;
                String key2 = team2 + "::" + team1;

                if (map.containsKey(key1)) {
                    if (dependency.getFromComponent().startsWith("[") || dependency.getToComponent().startsWith("[")) {
                        map.get(key1).increment(1);
                    }
                } else if (map.containsKey(key2)) {
                    if (dependency.getFromComponent().startsWith("[") || dependency.getToComponent().startsWith("[")) {
                        map.get(key2).increment(1);
                    }
                } else {
                    ComponentDependency teamDependency = new ComponentDependency(team1, team2);
                    teamDependency.setCount(1);
                    map.put(key1, teamDependency);
                    teamDependencies.add(teamDependency);
                }
            }
        }

        return teamDependencies;
    }

    void addPeopleInfoBlock() {
        int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount(contributors);

        addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(recentContributorsCount), recentContributorsCount == 1 ? type.singular() : type.plural(), "30 days", "");
    }

    private void addPeopleInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        addPeopleInfoBlockWithColor(mainValue, subtitle, description, tooltip, PEOPLE_COLOR);
    }

    private void addPeopleInfoBlockWithColor(String mainValue, String subtitle, String description, String tooltip, String color) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: #707070; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color, tooltip, isContributorReport() ? "contributors" : "teams");
    }

    private void addInfoBlockWithColor(String mainValue, String subtitle, String color, String tooltip, String icon) {
        String style = "border-radius: 12px;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 160px; height: 120px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 36px;";
        style += "box-shadow: rgb(0 0 0 / 12%) 0px 1px 3px, rgb(0 0 0 / 24%) 0px 1px 2px;";

        landscapeReport.startDiv("display: inline-block; text-align: center", tooltip);
        landscapeReport.addContentInDiv(ReportFileExporter.getIconSvg(icon, 48), "margin-top: 18px; margin-bottom: -12px");
        landscapeReport.startDiv(style, tooltip);
        String specialColor = mainValue.equals("<b>0</b>") ? " color: grey;" : "";
        landscapeReport.addHtmlContent("<div style='font-size: 50px; margin-top: 20px;" + specialColor + "'>" + mainValue + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 15px;" + specialColor + "'>" + subtitle + "</div>");
        landscapeReport.endDiv();
        landscapeReport.endDiv();
    }

    private void populateTimeSlotMaps() {
        contributors.forEach(contributorRepositories -> {
            List<String> commitDates = contributorRepositories.getContributor().getCommitDates();
            commitDates.forEach(day -> {
                String week = DateUtils.getWeekMonday(day);
                String month = DateUtils.getMonth(day);
                String year = DateUtils.getYear(day);

                updateTimeSlotMap(contributorRepositories, contributorsPerDayMap, rookiesPerDayMap, day, day);
                updateTimeSlotMap(contributorRepositories, contributorsPerWeekMap, rookiesPerWeekMap, week, week);
                updateTimeSlotMap(contributorRepositories, contributorsPerMonthMap, rookiesPerMonthMap, month, month + "-01");
                updateTimeSlotMap(contributorRepositories, contributorsPerYearMap, rookiesPerYearMap, year, year + "-01-01");
            });
        });

    }

    private void updateTimeSlotMap(ContributorRepositories contributorRepositories,
                                   Map<String, List<String>> map, Map<String, List<String>> rookiesMap, String key, String rookieDate) {
        boolean rookie = contributorRepositories.getContributor().isRookieAtDate(rookieDate);

        String email = contributorRepositories.getContributor().getEmail();
        if (map.containsKey(key)) {
            if (!map.get(key).contains(email)) {
                map.get(key).add(email);
            }
        } else {
            map.put(key, new ArrayList<>(Arrays.asList(email)));
        }
        if (rookie) {
            if (rookiesMap.containsKey(key)) {
                if (!rookiesMap.get(key).contains(email)) {
                    rookiesMap.get(key).add(email);
                }
            } else {
                rookiesMap.put(key, new ArrayList<>(Arrays.asList(email)));
            }
        }
    }

    private void renderPeopleDependencies(List<ComponentDependency> peopleDependencies,
                                          List<ComponentDependency> peopleRepositoryDependencies,
                                          List<ContributorConnections> contributorConnections,
                                          int daysAgo) {
        List<ComponentDependency> repositoryDependenciesViaPeople = ContributorConnectionUtils.getRepositoryDependenciesViaPeople(contributors, 0, daysAgo);

        landscapeReport.addLevel2Header("Contributor Dependencies (past " + daysAgo + " days)", "margin-top: 40px");
        List<Double> activeContributors30DaysHistory = landscapeAnalysisResults.getActiveContributors30DaysHistory();
        if (activeContributors30DaysHistory.size() > 0 && daysAgo == 30) {
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            addDataSection("Active Contributors", activeContributors30DaysHistory.get(0), daysAgo, activeContributors30DaysHistory,
                    "An active contributor is anyone who has committed code changes in past " + daysAgo + " days.");
        }

        landscapeReport.startTable();
        landscapeReport.startTableRow();
        landscapeReport.startTableCell("border: none; vertical-align: top");
        landscapeReport.addHtmlContent(DEPENDENCIES_ICON);
        landscapeReport.endTableCell();
        landscapeReport.startTableCell("border: none");
        addPeopleGraph(peopleDependencies, daysAgo, "", "");
        if (peopleRepositoryDependencies != null) {
            addPeopleGraph(peopleRepositoryDependencies, daysAgo, "including_repositories_", " (including repositories)");
        }
        addRepositoriesGraph(daysAgo, repositoryDependenciesViaPeople);
        landscapeReport.endTableCell();
        landscapeReport.endTableRow();
        landscapeReport.endTable();

        landscapeReport.startDiv("margin-left: 124px; padding-left: 4px; border-left: 1px dashed lightgrey; margin-top: 10px;");

        peopleDependencies.sort((a, b) -> b.getCount() - a.getCount());

        addMostConnectedPeopleSection(contributorConnections, daysAgo);
        addMostRepositoriesPeopleSection(contributorConnections, daysAgo);
        addTopConnectionsSection(peopleDependencies, daysAgo, contributors);
        addRepositoryContributors(contributors, daysAgo);
        addRepositoryDependenciesViaPeople(repositoryDependenciesViaPeople);

        landscapeReport.endDiv();
    }

    private boolean isContributorReport() {
        return type == LandscapeReportContributorsTab.Type.CONTRIBUTORS;
    }

    private void addRepositoriesGraph(int daysAgo, List<ComponentDependency> repositoryDependenciesViaPeople) {
        landscapeReport.startShowMoreBlock("show repository dependencies graph...<br>");
        StringBuilder builder = new StringBuilder();
        builder.append("Repository 1\tRepository 2\t# people\n");
        repositoryDependenciesViaPeople.subList(0, Math.min(10000, repositoryDependenciesViaPeople.size())).forEach(d -> builder
                .append(d.getFromComponent()).append("\t")
                .append(d.getToComponent()).append("\t")
                .append(d.getCount()).append("\n"));
        String fileName = "repository_dependencies_via_" + (isContributorReport() ? "people" : "teams") + "_" + daysAgo + "_days.txt";
        saveData(fileName, builder.toString());

        landscapeReport.addNewTabLink("See data...", "data/" + fileName);

        List<String> repositoryNames = landscapeAnalysisResults.getRepositoryAnalysisResults().stream()
                .filter(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days() > 0)
                .map(p -> "[" + p.getAnalysisResults().getMetadata().getName() + "]")
                .collect(Collectors.toList());

        String graphId = addDependencyGraphVisuals(repositoryDependenciesViaPeople, new ArrayList<>(repositoryNames), "repository_dependencies_" + daysAgo + "_", "TB");

        landscapeReport.endShowMoreBlock();
        landscapeReport.addNewTabLink(" - 2D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/" + graphId + "_force_2d.html");
        landscapeReport.addLineBreak();
        landscapeReport.addNewTabLink(" - 3D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/" + graphId + "_force_3d.html");
        landscapeReport.addLineBreak();
    }

    private void addDataSection(String type, double value, int daysAgo, List<Double> history, String info) {
        if (StringUtils.isNotBlank(info)) {
            landscapeReport.addParagraph(type + ": <b>" + ((int) Math.round(value)) + "</b>");
            landscapeReport.addParagraph("<span style='color: #a2a2a2; font-size: 90%;'>" + info + "</span>", "margin-top: -12px;");
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

    private void addRepositoryDependenciesViaPeople(List<ComponentDependency> repositoryDependenciesViaPeople) {
        landscapeReport.startShowMoreBlock("show repository dependencies via people...<br>");
        landscapeReport.startTable();
        int maxListSize = Math.min(100, repositoryDependenciesViaPeople.size());
        if (maxListSize < repositoryDependenciesViaPeople.size()) {
            landscapeReport.addLineBreak();
            landscapeReport.addParagraph("Showing top " + maxListSize + " items (out of " + repositoryDependenciesViaPeople.size() + ").");
        } else {
            landscapeReport.addParagraph("Showing all " + maxListSize + (maxListSize == 1 ? " item" : " items") + ".");
        }
        repositoryDependenciesViaPeople.subList(0, maxListSize).forEach(dependency -> {
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(dependency.getFromComponent());
            landscapeReport.addTableCell(dependency.getToComponent());
            landscapeReport.addTableCell(dependency.getCount() + (dependency.getCount() == 1 ? " person" : " people"));
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void addRepositoryContributors(List<ContributorRepositories> contributors, int daysAgo) {
        Map<String, Pair<String, Integer>> map = new HashMap<>();
        final List<String> list = new ArrayList<>();

        contributors.forEach(contributorRepositories -> {
            contributorRepositories.getRepositories().stream().filter(repository -> DateUtils.isAnyDateCommittedBetween(repository.getCommitDates(), 0, daysAgo)).forEach(repository -> {
                String key = repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName();
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

        landscapeReport.startShowMoreBlock("show repositories with most " + (isContributorReport() ? "people" : "teams") + "...<br>");
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor\t# people\n");
        list.forEach(repository -> builder.append(map.get(repository).getLeft()).append("\t")
                .append(map.get(repository).getRight()).append("\n"));
        String prefix = "repository_with_most_" + (isContributorReport() ? "people" : "teams") + "_" + daysAgo + "_days";
        String fileName = prefix + ".txt";
        saveData(fileName, builder.toString());

        if (displayList.size() < list.size()) {
            landscapeReport.addLineBreak();
            landscapeReport.addParagraph("Showing top 100 items (out of " + list.size() + ").");
        }
        landscapeReport.addLineBreak();
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/" + fileName);
        landscapeReport.addHtmlContent("</p>");
        List<VisualizationItem> visualizationItems = new ArrayList<>();
        list.forEach(repository -> visualizationItems.add(new VisualizationItem(repository, map.get(repository).getRight())));
        exportVisuals(prefix, visualizationItems);
        landscapeReport.startTable();
        displayList.forEach(repository -> {
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(map.get(repository).getLeft());
            Integer count = map.get(repository).getRight();
            landscapeReport.addTableCell(count + (count == 1 ? " person" : " people"));
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void addPeopleGraph(List<ComponentDependency> peopleDependencies, int daysAgo, String suffix, String extraLabel) {
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor 1\tContributor 2\t# shared repositories\n");
        peopleDependencies.forEach(d -> builder
                .append(d.getFromComponent()).append("\t")
                .append(d.getToComponent()).append("\t")
                .append(d.getCount()).append("\n"));
        String fileName = "repository_shared_repositories_" + suffix + daysAgo + "_days.txt";
        saveData(fileName, builder.toString());

        landscapeReport.startShowMoreBlock("show contributor dependencies graph..." + extraLabel + "<br>");
        landscapeReport.startDiv("border-left: 6px solid lightgrey; padding-left: 4px; margin-left: 4px; overflow-x: auto");
        landscapeReport.addHtmlContent("&nbsp;&nbsp;&nbsp;");
        landscapeReport.addNewTabLink("See data...", "data/" + fileName);

        String orientation = suffix.length() > 0 ? "LR" : "TB";
        String graphId = addDependencyGraphVisuals(peopleDependencies, new ArrayList<>(),
                (isContributorReport() ? "people" : "teams") + "_dependencies_" + suffix + daysAgo + "_", orientation);
        landscapeReport.endDiv();
        landscapeReport.endShowMoreBlock();

        landscapeReport.addNewTabLink(" - 2D force graph" + extraLabel + "&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON,
                "visuals/" + graphId + "_force_2d.html");
        landscapeReport.addLineBreak();
        landscapeReport.addNewTabLink(" - 3D force graph" + extraLabel + "&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON,
                "visuals/" + graphId + "_force_3d.html");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
    }

    private void addTopConnectionsSection(List<ComponentDependency> peopleDependencies, int daysAgo, List<ContributorRepositories> contributors) {
        landscapeReport.startShowMoreBlock("show top connections...<br>");
        landscapeReport.startTable();
        List<ComponentDependency> displayListConnections = peopleDependencies.subList(0, Math.min(100, peopleDependencies.size()));
        if (displayListConnections.size() < peopleDependencies.size()) {
            landscapeReport.addLineBreak();
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
            int dependencyCount = dependency.getCount();
            landscapeReport.addTableCell(index[0] + ".");
            int repositoryCount1 = ContributorConnectionUtils.getRepositoryCount(contributors, from, 0, daysAgo);
            int repositoryCount2 = ContributorConnectionUtils.getRepositoryCount(contributors, to, 0, daysAgo);
            double perc1 = 0;
            double perc2 = 0;
            if (repositoryCount1 > 0) {
                perc1 = 100.0 * dependencyCount / repositoryCount1;
            }
            if (repositoryCount2 > 0) {
                perc2 = 100.0 * dependencyCount / repositoryCount2;
            }
            landscapeReport.addTableCell(from + "<br><span style='color: grey'>" + repositoryCount1 + " repositories (" + FormattingUtils.getFormattedPercentage(perc1) + "%)</span>", "");
            landscapeReport.addTableCell(to + "<br><span style='color: grey'>" + repositoryCount2 + " repositories (" + FormattingUtils.getFormattedPercentage(perc2) + "%)</span>", "");
            landscapeReport.addTableCell(dependencyCount + " shared repositories", "");
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void addMostConnectedPeopleSection(List<ContributorConnections> contributorConnections, int daysAgo) {
        landscapeReport.startShowMoreBlock("show most connected people...<br>");
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor\t# repositories\t# connections\n");
        contributorConnections.forEach(c -> builder.append(c.getEmail()).append("\t")
                .append(c.getRepositoriesCount()).append("\t")
                .append(c.getConnectionsCount()).append("\n"));
        String prefix = "most_connected_" + (isContributorReport() ? "people" : "teams") + "_" + daysAgo + "_days";
        String fileName = prefix + ".txt";

        saveData(fileName, builder.toString());

        List<ContributorConnections> displayListPeople = contributorConnections.subList(0, Math.min(100, contributorConnections.size()));
        if (displayListPeople.size() < contributorConnections.size()) {
            landscapeReport.addLineBreak();
            landscapeReport.addParagraph("Showing top " + displayListPeople.size() + " items (out of " + contributorConnections.size() + "). ");
        } else {
            landscapeReport.addParagraph("Showing all " + displayListPeople.size() + (displayListPeople.size() == 1 ? " item" : " items") + ". ");
        }
        landscapeReport.addLineBreak();
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
            landscapeReport.addTableCell(name.getRepositoriesCount() + "&nbsp;repositories");
            landscapeReport.addTableCell(name.getConnectionsCount() + " connections", "");
            landscapeReport.endTableRow();
        });
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();

    }

    private void addMostRepositoriesPeopleSection(List<ContributorConnections> contributorConnections, int daysAgo) {
        landscapeReport.startShowMoreBlock("show people with most repositories...<br>");
        List<ContributorConnections> sorted = new ArrayList<>(contributorConnections);
        sorted.sort((a, b) -> b.getRepositoriesCount() - a.getRepositoriesCount());
        List<ContributorConnections> displayListPeople = sorted.subList(0, Math.min(100, sorted.size()));
        if (displayListPeople.size() < contributorConnections.size()) {
            landscapeReport.addLineBreak();
            landscapeReport.addParagraph("Showing top " + displayListPeople.size() + " items (out of " + contributorConnections.size() + "). ");
        } else {
            landscapeReport.addParagraph("Showing all " + displayListPeople.size() + (displayListPeople.size() == 1 ? " item" : " items") + ". ");
        }
        String prefix = "most_repositories_" + (isContributorReport() ? "people" : "teams") + "_" + daysAgo + "_days";
        StringBuilder builder = new StringBuilder();
        builder.append("Contributor\t# repositories\t# connections\n");
        contributorConnections.forEach(c -> builder.append(c.getEmail()).append("\t")
                .append(c.getRepositoriesCount()).append("\t")
                .append(c.getConnectionsCount()).append("\n"));
        String fileName = prefix + ".txt";
        saveData(fileName, builder.toString());

        landscapeReport.addLineBreak();
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + prefix + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("data", "data/" + fileName);
        landscapeReport.addHtmlContent("</p>");
        List<VisualizationItem> visualizationItems = new ArrayList<>();
        contributorConnections.forEach(c -> visualizationItems.add(new VisualizationItem(c.getEmail(), c.getRepositoriesCount())));
        int index[] = {0};
        landscapeReport.startTable();
        displayListPeople.forEach(name -> {
            index[0] += 1;
            landscapeReport.startTableRow();
            landscapeReport.addTableCell(index[0] + ".", "");
            landscapeReport.addTableCell(name.getEmail(), "");
            landscapeReport.addTableCell(name.getRepositoriesCount() + "&nbsp;repositories");
            landscapeReport.addTableCell(name.getConnectionsCount() + " connections", "");
            landscapeReport.endTableRow();
        });

        exportVisuals(prefix, visualizationItems);
        landscapeReport.endTable();
        landscapeReport.endShowMoreBlock();
    }

    private void exportVisuals(String prefix, List<VisualizationItem> visualizationItems) {
        try {
            new LandscapeVisualsGenerator(reportsFolder, teamsConfig).exportVisuals(prefix, visualizationItems);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String addDependencyGraphVisuals(List<ComponentDependency> componentDependencies, List<String> componentNames,
                                             String prefix, String orientation) {
        if (type == LandscapeReportContributorsTab.Type.TEAMS) {
            componentDependencies = getTeamDependencies(componentDependencies);
        }
        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
        graphvizDependencyRenderer.setMaxNumberOfDependencies(100);
        graphvizDependencyRenderer.setType("graph");
        graphvizDependencyRenderer.setOrientation(orientation);
        graphvizDependencyRenderer.setArrow("--");

        if (100 < componentDependencies.size()) {
            landscapeReport.addLineBreak();
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
        new Force3DGraphExporter().export2D3DForceGraph(componentDependencies, reportsFolder, graphId);

        return graphId;
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

    public void setLandscapeReport(RichTextReport report) {
        this.landscapeReport = report;
    }
}

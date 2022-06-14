package nl.obren.sokrates.reports.landscape.statichtml.projects;

import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator;
import nl.obren.sokrates.reports.landscape.utils.Force3DGraphExporter;
import nl.obren.sokrates.reports.landscape.utils.LandscapeGeneratorUtils;
import nl.obren.sokrates.reports.landscape.utils.TagStats;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.landscape.ProjectTag;
import nl.obren.sokrates.sourcecode.landscape.ProjectTagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class LandscapeProjectsTagsReport {
    private static final Log LOG = LogFactory.getLog(LandscapeProjectsTagsReport.class);

    private LandscapeAnalysisResults landscapeAnalysisResults;
    private File reportsFolder;

    private List<ProjectTagGroup> projectTagGroups = new ArrayList<>();
    private TagMap tagsMap;
    private String type;
    private String matrixReportFileName;
    private boolean renderLangIcons;

    public LandscapeProjectsTagsReport(LandscapeAnalysisResults landscapeAnalysisResults, List<ProjectTagGroup> projectTagGroups, TagMap tagsMap, String type, String matrixReportFileName, boolean renderLangIcons) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.projectTagGroups = projectTagGroups;
        this.tagsMap = tagsMap;
        this.type = type;
        this.matrixReportFileName = matrixReportFileName;
        this.renderLangIcons = renderLangIcons;
    }

    public void saveProjectsReport(RichTextReport report, File reportsFolder, List<ProjectAnalysisResults> projectsAnalysisResults) {
        this.reportsFolder = reportsFolder;

        report.startDiv("position: absolute; left: 1px");
        addTagStats(report);
        report.endDiv();
    }

    private void addTagStats(RichTextReport report) {
        renderTagDependencies();

        report.startTable();
        report.addTableHeader("Tag", "# repositories", "LOC<br>(main)", "LOC<br>(test)", "LOC<br>(active)", "LOC<br>(new)", "# commits<br>(30 days)", "# contributors<br>(30 days)");
        int index[] = {0};
        projectTagGroups.stream().filter(tagGroup -> tagGroup.getProjectTags().size() > 0).forEach(tagGroup -> {
            int count[] = {0};
            tagGroup.getProjectTags().stream().forEach(projectTag -> {
                if (tagsMap.getTagStats(projectTag.getKey()) != null) count[0] += 1;
            });
            if (count[0] == 0) {
                return;
            }
            index[0] += 1;
            report.startTableRow();
            report.startMultiColumnTableCell(8, "");
            report.startDiv("border-radius: 9px; padding: 6px; margin-top: 16px; border: 1px solid lightgrey; background-color: " + tagGroup.getColor());
            report.addHtmlContent(tagGroup.getName() + " (" + count[0] + ")");
            if (StringUtils.isNotBlank(tagGroup.getDescription())) {
                report.addHtmlContent("<span style='color: grey;'>: " + tagGroup.getDescription() + "</span>");
            }
            addTagGroupSummary(tagGroup, report);
            addDependencyLinks(report, index);

            report.endTableCell();
            report.endTableRow();
            tagGroup.getProjectTags().stream()
                    .filter(t -> (tagsMap.getTagStats(t.getKey()) != null))
                    .sorted((a, b) -> tagsMap.getTagStats(b.getKey()).getProjectsAnalysisResults().size() - tagsMap.getTagStats(a.getKey()).getProjectsAnalysisResults().size())
                    .forEach(projectTag -> addTagRow(report, projectTag.getTag(), projectTag, tagGroup.getColor()));
        });
        if (tagsMap.containsKey("")) {
            report.addMultiColumnTableCell("&nbsp;", 8);
            addTagRow(report, "", new ProjectTag(), "lightgrey");
        }
        report.endTable();


        visualizeTagProjects(report);
    }

    private void addDependencyLinks(RichTextReport report, int[] index) {
        report.startDiv("margin: 5px; font-size: 80%");
        report.addHtmlContent("tag dependencies: ");
        report.addNewTabLink("3D graph (via repositories)", "visuals/" + type + "_tags_graph_" + index[0] + "_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (excluding repositories)", "visuals/" + type + "_tags_graph_" + index[0] + "_direct_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (via repositories)", "visuals/" + type + "_tags_graph_" + index[0] + ".svg");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (excluding repositories)", "visuals/" + type + "_tags_graph_" + index[0] + "_direct.svg");
        report.addNewTabLink("2D graph (excluding repositories)", "visuals/" + type + "_tags_graph_" + index[0] + "_direct.svg");
        report.endDiv();
    }

    private void addTagGroupSummary(ProjectTagGroup tagGroup, RichTextReport report) {
        Map<String, ProjectAnalysisResults> projects = new HashMap<>();

        tagGroup.getProjectTags().forEach(tag -> {
            TagStats stats = tagsMap.getTagStats(tag.getKey());
            if (stats == null) {
                return;
            }
            stats.getProjectsAnalysisResults().forEach(project -> {
                projects.put(project.getSokratesProjectLink().getAnalysisResultsPath(), project);
            });
        });

        int count = projects.size();
        int locMain = projects.values().stream().mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);
        int locTest = projects.values().stream().mapToInt(p -> p.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);
        int locGenerated = projects.values().stream().mapToInt(p -> p.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);
        int locBuildAndDeployment = projects.values().stream().mapToInt(p -> p.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);
        int locOther = projects.values().stream().mapToInt(p -> p.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCode()).reduce(0, (a, b) -> a + b);

        int locSecondary = locTest + locGenerated + locBuildAndDeployment + locOther;

        report.startDiv("margin: 5px; font-size: 80%");
        report.addContentInDiv("<b>" + FormattingUtils.formatCountPlural(count, "repository", "repositories") + "</b>, " +
                "<b>" + FormattingUtils.getSmallTextForNumber(locMain) + "</b> lines of main code, " +
                "<b>" + FormattingUtils.getSmallTextForNumber(locSecondary) + "</b> lines of other code");
        report.endDiv();
    }

    private void renderTagDependencies() {
        int index[] = {0};
        projectTagGroups.forEach(tagGroup -> {
            index[0] += 1;
            String prefix = type + "_tags_graph_" + index[0];
            List<ProjectTag> groupTags = tagGroup.getProjectTags();
            exportTagGraphs(prefix, groupTags);
        });

        List<ProjectTag> allTags = new ArrayList<>();
        projectTagGroups.forEach(tagGroup -> {
            allTags.addAll(tagGroup.getProjectTags());
        });
        String prefix = type + "_tags_graph";
        exportTagGraphs(prefix, allTags);
    }

    private void exportTagGraphs(String prefix, List<ProjectTag> groupTags) {
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, Set<String>> projectTagsMap = new HashMap<>();
        groupTags.stream().filter(tag -> tagsMap.getTagStats(tag.getKey()) != null)
                .forEach(tag -> {
                    TagStats stats = tagsMap.getTagStats(tag.getKey());
                    stats.getProjectsAnalysisResults().forEach(project -> {
                        String name = project.getAnalysisResults().getMetadata().getName();
                        dependencies.add(new ComponentDependency("[" + name + "]", tag.getTag()));
                        if (!projectTagsMap.containsKey(name)) {
                            projectTagsMap.put(name, new HashSet<>());
                        }
                        projectTagsMap.get(name).add(tag.getKey());
                    });
                });
        new Force3DGraphExporter().export3DForceGraph(dependencies, reportsFolder, prefix);

        List<ComponentDependency> directDependencies = new ArrayList<>();
        Map<String, ComponentDependency> directDependenciesMap = new HashMap<>();
        projectTagsMap.values().forEach(projectTags -> {
            projectTags.forEach(tag1 -> {
                projectTags.stream().filter(tag2 -> !tag1.equals(tag2)).forEach(tag2 -> {
                    String key1 = tag1 + "::" + tag2;
                    String key2 = tag2 + "::" + tag1;
                    if (directDependenciesMap.containsKey(key1)) {
                        directDependenciesMap.get(key1).increment(1);
                    } else if (directDependenciesMap.containsKey(key2)) {
                        directDependenciesMap.get(key2).increment(1);
                    } else {
                        ComponentDependency directDependency = new ComponentDependency(
                                tag1 + " (" + tagsMap.getTagStats(tag1).getProjectsAnalysisResults().size() + ")",
                                tag2 + " (" + tagsMap.getTagStats(tag2).getProjectsAnalysisResults().size() + ")");
                        directDependencies.add(directDependency);
                        directDependenciesMap.put(key1, directDependency);
                    }
                });
            });
        });

        directDependencies.forEach(d -> d.setCount(d.getCount() / 2));
        new Force3DGraphExporter().export3DForceGraph(directDependencies, reportsFolder, prefix + "_direct");

        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setMaxNumberOfDependencies(100);
        graphvizDependencyRenderer.setTypeGraph();
        graphvizDependencyRenderer.setOrientation("RL");
        List<String> keys = tagsMap.keySet().stream().filter(t -> tagsMap.getTagStats(t) != null).collect(Collectors.toList());
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(keys), dependencies);
        String graphvizContentDirect = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), directDependencies);
        try {
            FileUtils.write(new File(reportsFolder, "visuals/" + prefix + ".svg"), GraphvizUtil.getSvgFromDot(graphvizContent), StandardCharsets.UTF_8);
            FileUtils.write(new File(reportsFolder, "visuals/" + prefix + "_direct.svg"), GraphvizUtil.getSvgFromDot(graphvizContentDirect), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.info(e);
        }
    }

    private void visualizeTagProjects(RichTextReport report) {
        report.startDiv("margin: 2px; margin-top: 18px; margin-bottom: 42px;");
        report.startShowMoreBlock("show visuals...");
        int maxLoc = this.landscapeAnalysisResults.getProjectAnalysisResults().stream()
                .map(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .reduce((a, b) -> Math.max(a, b)).get();
        int maxCommits = this.landscapeAnalysisResults.getProjectAnalysisResults().stream()
                .map(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .reduce((a, b) -> Math.max(a, b)).get();
        projectTagGroups.forEach(tagGroup -> {
            tagGroup.getProjectTags().forEach(tag -> {
                visualizeTag(report, maxLoc, maxCommits, tag);
            });
        });
        if (tagsMap.containsKey("")) {
            visualizeTag(report, maxLoc, maxCommits, new ProjectTag());
        }
        report.endShowMoreBlock();
        report.endDiv();
    }

    private void visualizeTag(RichTextReport report, int maxLoc, int maxCommits, ProjectTag tag) {
        TagStats stats = tagsMap.getTagStats(tag.getKey());
        if (stats == null) {
            return;
        }

        String tagName = tag.getKey();
        report.startDiv("margin: 18px;");
        report.addContentInDiv("<b>" + (tagName.isBlank() ? "Untagged" : tagName) + "</b> (" + stats.getProjectsAnalysisResults().size() + ")", "margin-bottom: 5px");
        List<ProjectAnalysisResults> projects = stats.getProjectsAnalysisResults();
        projects.sort((a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days() - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days());
        projects.forEach(project -> {
            int loc = project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
            int barSize = 3 + (int) Math.round(Math.sqrt(4900 * ((double) loc / maxLoc)));
            int commitsCount30Days = project.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days();
            double opacity = commitsCount30Days > 0 ? 0.4 + 0.6 * commitsCount30Days / maxCommits : 0.0;
            report.startNewTabLink(getProjectReportUrl(project), "");
            report.startDivWithLabel(tooltip(project),
                    "border: 1px solid grey; border-radius: 50%;" +
                            "display: inline-block; " +
                            "padding: 0;" +
                            "vertical-align: middle; " +
                            "overflow: none; " +
                            "width: " + (barSize + 2) + "px; " +
                            "height: " + (barSize + 2) + "px; ");
            report.startDiv(" margin: 0;border-radius: 50%;" +
                    "opacity: " + opacity + ";" +
                    "background-color: " + getTabColor(stats.getTag()) + "; " +
                    "border: 1px solid lightgrey; cursor: pointer;" +
                    "width: " + barSize + "px; " +
                    "height: " + barSize + "px; ");
            report.endDiv();
            report.endDiv();
            report.endNewTabLink();
        });
        report.endDiv();
    }

    private void addTagRow(RichTextReport report, String tagName, ProjectTag tag, String color) {
        TagStats stats = tagsMap.getTagStats(tag.getKey());
        if (stats == null) {
            return;
        }
        report.startTableRow("text-align: center");
        report.startTableCell("vertical-align: top; white-space: nowrap;");
        if (StringUtils.isNotBlank(tagName)) {
            String tooltip = getTagTooltip(tag);

            String htmlFragment = "";
            String style = "vertical-align: top; cursor: help; padding: 4px; border-radius: 6px; border: 1px solid lightgrey; background-color: " + color;

            if (renderLangIcons) {
                String imageHtml = DataImageUtils.getLangDataImageDiv30(tagName);
                htmlFragment = imageHtml + "<div style='margin: 6px; display: inline-block;'>" + tagName + "</div>";
            } else if (StringUtils.isNoneBlank(tag.getImageLink())) {
                int size = 36;
                String imgStyle = "border: 1px solid grey; border-radius: 50%; padding: 1px; background-color: #fcfcfc; vertical-align: middle; margin-right: 5px; width: " + size + "px; height: " + size + "px; object-fit: contain;";
                String imageHtml = "<img title='" + tag.getTag() + "' style=\"" + imgStyle + "\" src=\"" +
                        tag.getImageLink() + "\">";
                htmlFragment = imageHtml + "<div style='vertical-align: middle; display: inline-block;'>" + tagName + "</div>";
            } else {
                htmlFragment = tagName;
            }

            report.addContentInDivWithTooltip(htmlFragment, tooltip, style);
        } else {
            report.addContentInDiv("Untagged");
        }
        report.endTableCell();
        if (stats != null) {
            int totalProjectsCount = landscapeAnalysisResults.getProjectsCount();
            List<ProjectAnalysisResults> projectsAnalysisResults = new ArrayList<>(stats.getProjectsAnalysisResults());
            projectsAnalysisResults.sort((a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
            int count = projectsAnalysisResults.size();
            report.startTableCell("text-align: left");
            String projectPercText = FormattingUtils.getFormattedPercentage(totalProjectsCount > 0 ? (100.0 * count / totalProjectsCount) : 0);
            report.startShowMoreBlock("<b>" + count + "</b>" + (count == 1 ? " repository" : " repositories")
                    + (count == 0 ? "" : " <span style='color: grey; font-size: 90%'>(" + projectPercText + "%)</span>"));
            report.startDiv("border-left: 2px solid lightgrey; margin-left: 5px; font-size: 80%");
            int maxListSize = 100;
            projectsAnalysisResults.stream().limit(maxListSize).forEach(project -> {
                CodeAnalysisResults projectAnalysisResults = project.getAnalysisResults();
                String projectReportUrl = getProjectReportUrl(project);
                report.addContentInDiv(
                        "<a href='" + projectReportUrl + "' target='_blank' style='margin-left: 6px'>" + projectAnalysisResults.getMetadata().getName() + "</a> "
                                + "<span color='lightgrey'>(<b>"
                                + FormattingUtils.formatCount(projectAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode(), "-") + "</b> LOC)</span>");
            });
            if (projectsAnalysisResults.size() > maxListSize) {
                report.addContentInDiv("...", "margin-bottom: 10px; margin-left: 7px; font-size: 160%");
            }
            report.endDiv();
            report.endShowMoreBlock();
            report.endTableCell();
            int mainLoc = landscapeAnalysisResults.getMainLoc();
            int tagMainLoc = projectsAnalysisResults.stream()
                    .mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                    .sum();
            String mainLocPercText = FormattingUtils.getFormattedPercentage(totalProjectsCount > 0 ? (100.0 * tagMainLoc / mainLoc) : 0);
            report.addTableCell(FormattingUtils.formatCount(tagMainLoc) + " <span style='color: grey; font-size: 90%'>(" + mainLocPercText + "%)</span>", "");
            report.addTableCell(FormattingUtils.formatCount(projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode())
                    .sum(), "-"), "");
            report.addTableCell(FormattingUtils.formatCount(LandscapeAnalysisResults.getLoc1YearActive(projectsAnalysisResults), "-"), "r");
            report.addTableCell(FormattingUtils.formatCount(LandscapeAnalysisResults.getLocNew(projectsAnalysisResults), "-"), "");
            int commitsCount30Days = landscapeAnalysisResults.getCommitsCount30Days();
            int tagCommitsCount30Days = projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                    .sum();
            String commits30DaysPercText = FormattingUtils.getFormattedPercentage(commitsCount30Days > 0 ? (100.0 * tagCommitsCount30Days / commitsCount30Days) : 0);
            report.addTableCell(FormattingUtils.formatCount(tagCommitsCount30Days, "-") + (tagCommitsCount30Days == 0 ? "" : " <span style='color: grey; font-size: 90%'>(" + commits30DaysPercText + "%)</span>"), "");
            int totalRecentContributorCount = landscapeAnalysisResults.getRecentContributorsCount();
            int recentContributorCount = getRecentContributorCount(projectsAnalysisResults);
            String recentContributorsPercText = FormattingUtils.getFormattedPercentage(totalRecentContributorCount > 0 ? (100.0 * recentContributorCount / totalRecentContributorCount) : 0);
            if (recentContributorCount > 0) {
                report.addTableCell("<div style='vertical-align: middle; display: inline-block'>"
                                + FormattingUtils.formatCount(recentContributorCount, "-")
                                + "</div><div style='vertical-align: middle; display: inline-block'>"
                                + LandscapeReportGenerator.DEVELOPER_SVG_ICON
                                + "</div>"
                                + (recentContributorCount == 0 ? "" : " <span style='color: grey; font-size: 90%'>(" + recentContributorsPercText + "%)</span>"),
                        "vertical-align: middle");
            } else {
                report.addTableCell("-", "vertical-align: middle");
            }
        } else {
            report.addTableCell("");
            report.addTableCell("");
            report.addTableCell("");
            report.addTableCell("");
            report.addTableCell("");
        }

        report.endTableRow();
    }

    private String getTagTooltip(ProjectTag tag) {
        String tooltip = "";

        if (tag.getPatterns().size() > 0) {
            tooltip += "includes repositories with names like:\n  - " + tag.getPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getExcludePatterns().size() > 0) {
            tooltip += "excludes repositories with names like:\n  - " + tag.getExcludePatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getPathPatterns().size() > 0) {
            tooltip += "includes repositories with at least one file matching:\n  - " + tag.getPathPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getExcludePathPatterns().size() > 0) {
            tooltip += "excludes repositories with at least one file matching:\n  - " + tag.getExcludePathPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getMainExtensions().size() > 0) {
            tooltip += "includes repositories with main extensions:\n  - " + tag.getMainExtensions().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getAnyExtensions().size() > 0) {
            tooltip += "includes repositories with any file with extensions:\n  - " + tag.getMainExtensions().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        return tooltip;
    }

    private boolean isTagged(ProjectAnalysisResults project, String mainTech, ProjectTag tag) {
        String name = project.getAnalysisResults().getMetadata().getName();
        return !tag.excludesMainTechnology(mainTech) &&
                ((tag.matchesName(name) && !tag.excludeName(name)) || tag.matchesMainTechnology(mainTech) || tag.matchesAnyTechnology(LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension())) || tag.matchesPath(project.getFiles()));
    }

    private String getTabColor(ProjectTag tag) {
        return tag.getGroup() != null && StringUtils.isNotBlank(tag.getGroup().getColor()) ? tag.getGroup().getColor() : "#99badd";
    }

    private String getProjectReportFolderUrl(ProjectAnalysisResults projectAnalysis) {
        return landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/";
    }

    private String getProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "index.html";
    }

    private String tooltip(ProjectAnalysisResults project) {
        CodeAnalysisResults analysis = project.getAnalysisResults();
        return analysis.getMetadata().getName() + "\n\n" +
                analysis.getContributorsAnalysisResults().getCommitsCount30Days() + " commits (30 days)" + "\n" +
                analysis.getContributorsAnalysisResults().getContributors()
                        .stream().filter(contributor -> contributor.getCommitsCount30Days() > 0).count() + " contributors (30 days)" + "\n" +
                FormattingUtils.formatCount(analysis.getMainAspectAnalysisResults().getLinesOfCode()) + " LOC";
    }

    private int getRecentContributorCount(List<ProjectAnalysisResults> projectsAnalysisResults) {
        Set<String> ids = new HashSet<>();
        projectsAnalysisResults.forEach(project -> {
            project.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream()
                    .filter(c -> c.getCommitsCount30Days() > 0).forEach(c -> ids.add(c.getEmail()));
        });
        return ids.size();
    }

}

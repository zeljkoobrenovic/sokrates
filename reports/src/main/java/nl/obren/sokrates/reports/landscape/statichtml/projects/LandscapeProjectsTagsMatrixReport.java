package nl.obren.sokrates.reports.landscape.statichtml.projects;

import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
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
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class LandscapeProjectsTagsMatrixReport {
    private static final Log LOG = LogFactory.getLog(LandscapeProjectsTagsMatrixReport.class);

    private LandscapeAnalysisResults landscapeAnalysisResults;
    private Map<String, TagStats> tagStatsMap = new HashMap<>();
    private File reportsFolder;

    private List<ProjectTagGroup> projectTagGroups = new ArrayList<>();
    private String type;
    private boolean renderLangIcons;

    public LandscapeProjectsTagsMatrixReport(LandscapeAnalysisResults landscapeAnalysisResults, List<ProjectTagGroup> projectTagGroups, String type, boolean renderLangIcons) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.projectTagGroups = projectTagGroups;
        this.type = type;
        this.renderLangIcons = renderLangIcons;
    }

    public void saveProjectsReport(RichTextReport report, File reportsFolder, List<ProjectAnalysisResults> projectsAnalysisResults) {
        this.reportsFolder = reportsFolder;

        updateTagMap(projectsAnalysisResults);

        report.startDiv("position: absolute; left: 1px");
        addTagStats(report);
        report.endDiv();
    }

    private void addTagStats(RichTextReport report) {
        int index[] = {0};
        projectTagGroups.stream().filter(tagGroup -> tagGroup.getProjectTags().size() > 0).forEach(tagGroup -> {
            int count[] = {0};
            tagGroup.getProjectTags().stream().forEach(projectTag -> {
                if (tagStatsMap.get(projectTag.getKey()) != null) count[0] += 1;
            });
            if (count[0] == 0) {
                return;
            }
            List<Pair<String, Integer>> roots = getRoots(tagGroup);
            index[0] += 1;

            report.startDiv("border-radius: 9px; padding: 6px; margin-top: 16px; border: 1px solid lightgrey; background-color: " + tagGroup.getColor());
            report.addHtmlContent(tagGroup.getName() + " (" + count[0] + ")");
            if (StringUtils.isNotBlank(tagGroup.getDescription())) {
                report.addHtmlContent("<span style='color: grey;'>: " + tagGroup.getDescription() + "</span>");
            }
            report.endDiv();

            report.startTable();

            addHeaderRow(report, roots);

            tagGroup.getProjectTags().stream()
                    .filter(t -> (tagStatsMap.get(t.getKey()) != null))
                    .sorted((a, b) -> tagStatsMap.get(b.getKey()).getProjectsAnalysisResults().size() - tagStatsMap.get(a.getKey()).getProjectsAnalysisResults().size())
                    .forEach(projectTag -> addTagRow(report, roots, projectTag.getTag(), projectTag, tagGroup.getColor()));
            report.endTable();
        });
    }

    private void addHeaderRow(RichTextReport report, List<Pair<String, Integer>> roots) {
        report.startTableRow("font-size: 80%");
        report.addTableCell("");
        report.addTableCell("all projects", "text-align: center; border-right: 4px solid lightgrey");
        roots.forEach(root -> {
            String text = StringUtils.abbreviate(root.getKey(), 20) + "<br>(" + root.getValue() + ")";
            report.addTableCell(text, "text-align: center");
        });
        report.endTableRow();
    }

    private List<Pair<String, Integer>> getRoots(ProjectTagGroup tagGroup) {
        Map<String, List<String>> roots = new HashMap<>();
        tagGroup.getProjectTags().stream().filter(t -> tagStatsMap.get(t.getKey()) != null).forEach(projectTag -> {
            tagStatsMap.get(projectTag.getKey()).getProjectsAnalysisResults().forEach(project -> {
                String root = project.getSokratesProjectLink().getAnalysisResultsPath().split("(\\/|\\\\)")[0];
                if (!roots.containsKey(root)) {
                    roots.put(root, new ArrayList<>());
                }
                roots.get(root).add(project.getSokratesProjectLink().getAnalysisResultsPath());
            });
        });

        List<Pair<String, Integer>> rootsList = new ArrayList<>();

        roots.keySet().forEach(key -> rootsList.add(Pair.of(key, roots.get(key).size())));

        Collections.sort(rootsList, (a, b) -> b.getValue() - a.getValue());

        return rootsList;
    }

    private void addTagRow(RichTextReport report, List<Pair<String, Integer>> roots, String tagName, ProjectTag tag, String color) {
        TagStats stats = tagStatsMap.get(tag.getKey());
        if (stats == null) {
            return;
        }
        report.startTableRow("text-align: center");
        addTagCell(report, tagName, tag, color);

        addProjectsTagCell(report, stats, "");

        roots.forEach(root -> addProjectsTagCell(report, stats, root.getKey()));

        report.endTableRow();
    }

    private void addProjectsTagCell(RichTextReport report, TagStats stats, String root) {
        List<ProjectAnalysisResults> projectsAnalysisResults = new ArrayList<>(stats.getProjectsAnalysisResults()).stream()
                .filter(project -> StringUtils.isBlank(root) || project.getSokratesProjectLink().getAnalysisResultsPath().startsWith(root + "/"))
                .collect(Collectors.toList());
        projectsAnalysisResults.sort((a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
        int count = projectsAnalysisResults.size();
        int tagMainLoc = projectsAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .sum();
        int tagCommitsCount30Days = projectsAnalysisResults
                .stream()
                .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .sum();
        int recentContributorCount = getRecentContributorCount(projectsAnalysisResults);
        if (count > 0) {
            if (StringUtils.isNoneBlank(root)) {
                report.startTableCell("background-color: " + stats.getTag().getGroup().getColor() + "; text-align: center; vertical-align: top");
            } else {
                report.startTableCell("text-align: center; border-right: 4px solid lightgrey; vertical-align: top");
            }
            String bgColor = StringUtils.isNoneBlank(root) ? "#f0f0f0" : "white";
            int margin = renderLangIcons ? 7 : 2;
            report.startDiv("box-shadow: 0 1px 2px 0 rgb(0 0 0 / 20%), 0 2px 5px 0 rgb(0 0 0 / 19%); margin: " + margin + "px; display: inline-block; border-radius: 4px; padding: 4px; background-color: " +
                    bgColor);
            report.startShowMoreBlock("<b>" + count + "</b>");
            report.startDiv("background-color: " + bgColor + "; a margin-left: 5px; font-size: 80%; text-align: left");
            report.addContentInDiv("<b>" + FormattingUtils.formatCount(tagMainLoc) + "</b> LOC (main)<br>"
                            + "<b>" + FormattingUtils.formatCount(tagCommitsCount30Days) + "</b> commits (30d)<br>"
                            + "<b>" + FormattingUtils.formatCount(recentContributorCount) + "</b> contributors (30d)",
                    "border-top: 2px solid lightgrey; border-bottom: 2px solid lightgrey; padding: 2px");
            projectsAnalysisResults.stream().limit(100).forEach(project -> {
                CodeAnalysisResults projectAnalysisResults = project.getAnalysisResults();
                String projectReportUrl = getProjectReportUrl(project);
                report.addContentInDiv(
                        "<a href='" + projectReportUrl + "' target='_blank' style='margin-left: 6px'>" + projectAnalysisResults.getMetadata().getName() + "</a> "
                                + "<span color='lightgrey'>(<b>"
                                + FormattingUtils.formatCount(projectAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode(), "-") + "</b> LOC)</span>");
            });
            report.endDiv();
            report.endShowMoreBlock();
            report.endDiv();
            report.endTableCell();
        } else {
            report.addTableCell("", "text-align: center");
        }
    }

    private void addTagCell(RichTextReport report, String tagName, ProjectTag tag, String color) {
        report.startTableCell("vertical-align: top;");
        if (StringUtils.isNotBlank(tagName)) {
            String tooltip = getTagTooltip(tag);

            String htmlFragment = "";
            String style = "vertical-align: top; cursor: help; padding: 4px; border-radius: 6px; border: 1px solid lightgrey; background-color: " + color;

            if (renderLangIcons) {
                String imageHtml = DataImageUtils.getLangDataImageDiv30(tagName);
                htmlFragment = imageHtml + "<div style='margin: 6px; display: inline-block;'>" + tagName + "</div>";
            } else {
                htmlFragment = tagName;
            }

            report.addContentInDivWithTooltip(htmlFragment, tooltip, style);
        } else {
            report.addContentInDiv("Untagged");
        }
        report.endTableCell();
    }

    private String getTagTooltip(ProjectTag tag) {
        String tooltip = "";

        if (tag.getPatterns().size() > 0) {
            tooltip += "includes projects with names like:\n  - " + tag.getPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getExcludePatterns().size() > 0) {
            tooltip += "excludes projects with names like:\n  - " + tag.getExcludePatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getPathPatterns().size() > 0) {
            tooltip += "includes projects with at least one file matching:\n  - " + tag.getPathPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getExcludePathPatterns().size() > 0) {
            tooltip += "excludes projects with at least one file matching:\n  - " + tag.getExcludePathPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getMainExtensions().size() > 0) {
            tooltip += "includes projects with main extensions:\n  - " + tag.getMainExtensions().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        if (tag.getAnyExtensions().size() > 0) {
            tooltip += "includes projects with any file with extensions:\n  - " + tag.getMainExtensions().stream().collect(Collectors.joining("\n  - ")) + "\n";
        }
        return tooltip;
    }

    private void updateTagMap(List<ProjectAnalysisResults> projects) {
        projects.forEach(project -> {
            List<NumericMetric> linesOfCodePerExtension = project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension();
            linesOfCodePerExtension.sort((a, b) -> b.getValue().intValue() - a.getValue().intValue());
            String mainTech = linesOfCodePerExtension.size() > 0 ? linesOfCodePerExtension.get(0).getName().replaceAll(".*[.]", "") : "";
            List<ProjectTag> tags = new ArrayList<>();
            projectTagGroups.forEach(tagGroup -> tags.addAll(tagGroup.getProjectTags()));

            boolean tagged[] = {false};

            tags.forEach(tag -> {
                if (isTagged(project, mainTech, tag)) {
                    String key = tag.getKey();
                    if (!tagStatsMap.containsKey(key)) {
                        tagStatsMap.put(key, new TagStats(tag));
                    }
                    tagStatsMap.get(key).getProjectsAnalysisResults().add(project);
                    tagged[0] = true;
                }
            });

            if (!tagged[0]) {
                if (!tagStatsMap.containsKey("")) {
                    tagStatsMap.put("", new TagStats(new ProjectTag()));
                }
                tagStatsMap.get("").getProjectsAnalysisResults().add(project);
            }

        });
    }

    private boolean isTagged(ProjectAnalysisResults project, String mainTech, ProjectTag tag) {
        String name = project.getAnalysisResults().getMetadata().getName();
        return !tag.excludesMainTechnology(mainTech) &&
                ((tag.matchesName(name) && !tag.excludeName(name)) || tag.matchesMainTechnology(mainTech) || tag.matchesAnyTechnology(LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension())) || tag.matchesPath(project.getFiles()));
    }

    private String getProjectReportFolderUrl(ProjectAnalysisResults projectAnalysis) {
        return landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/";
    }

    private String getProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "index.html";
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

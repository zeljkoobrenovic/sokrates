package nl.obren.sokrates.reports.landscape.statichtml.repositories;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.TagStats;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.RepositoryTag;
import nl.obren.sokrates.sourcecode.landscape.TagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.stream.Collectors;

public class LandscapeRepositoriesTagsMatrixReport {
    private static final Log LOG = LogFactory.getLog(LandscapeRepositoriesTagsMatrixReport.class);

    private LandscapeAnalysisResults landscapeAnalysisResults;

    private List<TagGroup> tagGroups;
    private TagMap tagsMap;
    private String type;
    private boolean renderLangIcons;

    public LandscapeRepositoriesTagsMatrixReport(LandscapeAnalysisResults landscapeAnalysisResults, List<TagGroup> tagGroups, TagMap tagsMap, String type, boolean renderLangIcons) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.tagGroups = tagGroups;
        this.tagsMap = tagsMap;
        this.type = type;
        this.renderLangIcons = renderLangIcons;
    }

    public void saveRepositoriesReport(RichTextReport report, String title) {
        report.startDiv("margin-bottom: 42px");
        report.addLevel1Header(title);
        report.addParagraph("Tags per sub-folder", "color: grey");
        ProcessingStopwatch.start("reporting/repositories/tags/" + type + "/adding report sections");
        addTagStats(report);
        ProcessingStopwatch.end("reporting/repositories/tags/" + type + "/adding report sections");
        report.endDiv();
    }

    private void addTagStats(RichTextReport report) {
        int index[] = {0};
        tagGroups.stream().filter(tagGroup -> tagGroup.getRepositoryTags().size() > 0).forEach(tagGroup -> {
            int count[] = {0};
            tagGroup.getRepositoryTags().stream().forEach(repositoryTag -> {
                if (tagsMap.getTagStats(repositoryTag.getKey()) != null) count[0] += 1;
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

            tagGroup.getRepositoryTags().stream()
                    .filter(t -> (tagsMap.getTagStats(t.getKey()) != null))
                    .sorted((a, b) -> tagsMap.getTagStats(b.getKey()).getRepositoryAnalysisResults().size() - tagsMap.getTagStats(a.getKey()).getRepositoryAnalysisResults().size())
                    .forEach(repositoryTag -> addTagRow(report, roots, repositoryTag.getTag(), repositoryTag, tagGroup.getColor()));
            report.endTable();
        });
    }

    private void addHeaderRow(RichTextReport report, List<Pair<String, Integer>> roots) {
        report.startTableRow("font-size: 80%");
        report.addTableCell("");
        report.addTableCell("all repositories", "text-align: center; border-right: 4px solid lightgrey");
        roots.forEach(root -> {
            String text = StringUtils.abbreviate(root.getKey(), 20) + "<br>(" + root.getValue() + ")";
            report.addTableCell(text, "text-align: center");
        });
        report.endTableRow();
    }

    private List<Pair<String, Integer>> getRoots(TagGroup tagGroup) {
        Map<String, List<String>> roots = new HashMap<>();
        tagGroup.getRepositoryTags().stream().filter(t -> tagsMap.getTagStats(t.getKey()) != null).forEach(repositoryTag -> {
            tagsMap.getTagStats(repositoryTag.getKey()).getRepositoryAnalysisResults().forEach(repository -> {
                String root = repository.getSokratesRepositoryLink().getAnalysisResultsPath().split("(\\/|\\\\)")[0];
                if (!roots.containsKey(root)) {
                    roots.put(root, new ArrayList<>());
                }
                roots.get(root).add(repository.getSokratesRepositoryLink().getAnalysisResultsPath());
            });
        });

        List<Pair<String, Integer>> rootsList = new ArrayList<>();

        roots.keySet().forEach(key -> rootsList.add(Pair.of(key, roots.get(key).size())));

        Collections.sort(rootsList, (a, b) -> b.getValue() - a.getValue());

        return rootsList;
    }

    private void addTagRow(RichTextReport report, List<Pair<String, Integer>> roots, String tagName, RepositoryTag tag, String color) {
        TagStats stats = tagsMap.getTagStats(tag.getKey());
        if (stats == null) {
            return;
        }
        report.startTableRow("text-align: center");
        addTagCell(report, tagName, tag, color);

        addRepositoriesTagCell(report, stats, "");

        roots.forEach(root -> addRepositoriesTagCell(report, stats, root.getKey()));

        report.endTableRow();
    }

    private void addRepositoriesTagCell(RichTextReport report, TagStats stats, String root) {
        List<RepositoryAnalysisResults> repositoriesAnalysisResults = new ArrayList<>(stats.getRepositoryAnalysisResults()).stream()
                .filter(repository -> StringUtils.isBlank(root) || repository.getSokratesRepositoryLink().getAnalysisResultsPath().startsWith(root + "/"))
                .collect(Collectors.toList());
        repositoriesAnalysisResults.sort((a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
        int count = repositoriesAnalysisResults.size();
        int tagMainLoc = repositoriesAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .sum();
        int tagCommitsCount30Days = repositoriesAnalysisResults
                .stream()
                .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .sum();
        int recentContributorCount = getRecentContributorCount(repositoriesAnalysisResults);
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
            int maxListSize = 100;
            repositoriesAnalysisResults.stream().limit(maxListSize).forEach(repository -> {
                CodeAnalysisResults repositoryAnalysisResults = repository.getAnalysisResults();
                String repositoryReportUrl = getrepositoryReportUrl(repository);
                report.addContentInDiv(
                        "<a href='" + repositoryReportUrl + "' target='_blank' style='margin-left: 6px'>" + repositoryAnalysisResults.getMetadata().getName() + "</a> "
                                + "<span color='lightgrey'>(<b>"
                                + FormattingUtils.formatCount(repositoryAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode(), "-") + "</b> LOC)</span>");
            });
            if (repositoriesAnalysisResults.size() > maxListSize) {
                report.addContentInDiv("...", "margin-bottom: 10px; margin-left: 7px; font-size: 160%");
            }
            report.endDiv();
            report.endShowMoreBlock();
            report.endDiv();
            report.endTableCell();
        } else {
            report.addTableCell("", "text-align: center");
        }
    }

    private void addTagCell(RichTextReport report, String tagName, RepositoryTag tag, String color) {
        report.startTableCell("vertical-align: top; white-space: nowrap;");
        if (StringUtils.isNotBlank(tagName)) {
            String tooltip = getTagTooltip(tag);

            String htmlFragment = "";
            String style = "vertical-align: top; cursor: help; padding: 4px; border-radius: 6px; border: 1px solid lightgrey; background-color: " + color;

            if (renderLangIcons) {
                String imageHtml = DataImageUtils.getLangDataImageDiv30(tagName);
                htmlFragment = imageHtml + "<div style='margin: 6px; display: inline-block;'>" + tagName + "</div>";
            } else if (StringUtils.isNoneBlank(tag.getImageLink())) {
                int size = 26;
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
    }

    private String getTagTooltip(RepositoryTag tag) {
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


    private String getRepositoryReportFolderUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return landscapeAnalysisResults.getConfiguration().getRepositoryReportsUrlPrefix() + repositoryAnalysis.getSokratesRepositoryLink().getHtmlReportsRoot() + "/";
    }

    private String getrepositoryReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "index.html";
    }

    private int getRecentContributorCount(List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        Set<String> ids = new HashSet<>();
        repositoryAnalysisResults.forEach(repository -> {
            repository.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream()
                    .filter(c -> c.getCommitsCount30Days() > 0).forEach(c -> ids.add(c.getEmail()));
        });
        return ids.size();
    }

}

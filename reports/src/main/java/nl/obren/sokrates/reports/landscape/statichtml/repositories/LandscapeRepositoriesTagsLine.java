package nl.obren.sokrates.reports.landscape.statichtml.repositories;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.TagStats;
import nl.obren.sokrates.sourcecode.landscape.RepositoryTag;
import nl.obren.sokrates.sourcecode.landscape.TagGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.OPEN_IN_NEW_TAB_SVG_ICON;

public class LandscapeRepositoriesTagsLine {
    private static final Log LOG = LogFactory.getLog(LandscapeRepositoriesTagsLine.class);

    private List<TagGroup> tagGroups = new ArrayList<>();
    private final TagMap tagsMap;

    public LandscapeRepositoriesTagsLine(List<TagGroup> tagGroups, TagMap tagsMap) {
        this.tagGroups = tagGroups;
        this.tagsMap = tagsMap;
    }

    public void addTagsLine(RichTextReport report) {
        tagGroups.stream().filter(tagGroup -> tagGroup.getRepositoryTags().size() > 0).forEach(tagGroup -> {
            int[] count = {0};
            tagGroup.getRepositoryTags().stream().forEach(repositoryTag -> {
                if (tagsMap.getTagStats(repositoryTag.getKey()) != null) count[0] += 1;
            });
            if (count[0] == 0) {
                return;
            }
            report.startDiv("border: 1px solid " + tagGroup.getColor() + "; background-color: #fcfcfc; border-radius: 5px; display: inline-block; vertical-align: top; margin-right: 10px; margin-bottom: 5px;");
            report.addContentInDiv(tagGroup.getName(), "width: 100%; margin: 4px; color: grey; font-size: 70%; white-space: nowrap; overflow: hidden;");
            tagGroup.getRepositoryTags().stream()
                    .filter(t -> (tagsMap.getTagStats(t.getKey()) != null))
                    .sorted((a, b) -> tagsMap.getTagStats(b.getKey()).getRepositoryAnalysisResults().size() - tagsMap.getTagStats(a.getKey()).getRepositoryAnalysisResults().size())
                    .forEach(tag -> addTagDiv(report, tag.getTag(), tag, tagGroup.getColor(), tagsMap.getTagStats(tag.getKey()).getRepositoryAnalysisResults().size()));
            report.endDiv();
        });

        report.startDiv("margin-bottom: 14px; margin-top: 18px; margin-left: 5px;");
        report.addNewTabLink("<b>see details</b>&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "repositories-tags.html");
        report.addHtmlContent("&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;");
        report.addNewTabLink("<b>see expanded view</b> (tag stats per sub-folder)&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "repositories-tags-matrix.html");
        report.endDiv();

    }

    private void addTagDiv(RichTextReport report, String tagName, RepositoryTag tag, String color, int count) {
        TagStats stats = tagsMap.getTagStats(tag.getKey());
        if (stats == null) {
            return;
        }

        report.startDiv("display: inline-block; margin: 3px; text-align: center");

        if (StringUtils.isNotBlank(tagName)) {
            String tooltip = getTagTooltip(tag, count);

            String htmlFragment = "";
            String style = "vertical-align: top; cursor: help; padding: 4px; #f0f0f0";

            int size = 48;
            String imgStyle = "margin: 4px; padding: 1px; vertical-align: middle; width: " + size + "px; height: " + size + "px; object-fit: contain;";
            if (StringUtils.isNoneBlank(tag.getImageLink())) {
                htmlFragment = "<img title='" + tag.getTag() + "' style=\"" + imgStyle + "\" src=\"" + tag.getImageLink() + "\">";
            } else {
                String textStyle = "padding: 1px; background-color: #fcfcfc; vertical-align: middle; border: 3px solid grey; border-radius: 50%; " +
                        "width: " + size + "px; height: " + size + "px;";
                htmlFragment = "<div title='" + tag.getTag() + "' style=\"" + textStyle + "\"><div style='overflow: hidden; white-space: nowrap; margin-top: 16px; font-size: 90%;'>" + tagName + "</div></div>";
            }
            htmlFragment += "<div style='vertical-align: middle; font-size: 120%; color: black; margin: 5px;'><b>" + FormattingUtils.getSmallTextForNumber(count) + "</b></div>";
            htmlFragment += "<div style='vertical-align: middle; font-size: 70%; color: grey; '>" + tagName + "</div>";

            report.addContentInDivWithTooltip(htmlFragment, tooltip, style);
        }

        report.endDiv();
    }

    private String getTagTooltip(RepositoryTag tag, int count) {
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

        tooltip += "\n\nFound " + FormattingUtils.formatCountPlural(count, "repositories", "repositories (see the Tags tab for details)");

        return tooltip;
    }
}

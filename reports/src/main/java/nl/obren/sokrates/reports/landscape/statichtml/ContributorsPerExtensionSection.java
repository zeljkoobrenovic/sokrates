package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

/**
 * The per-extension sections of the landscape contributors tab: the contributors per file extension with their
 * language cards, the extension dependencies graph (people active in two extensions in 30 days) and the per-extension
 * commit details. Moved out of {@link LandscapeReportContributorsTab}, which owns the tab's layout.
 */
class ContributorsPerExtensionSection {
    private final RichTextReport landscapeReport;
    private final LandscapeAnalysisResults landscapeAnalysisResults;
    private final List<ContributorRepositories> contributors;
    private final File reportsFolder;
    private final LandscapeReportContributorsTab.Type type;
    private final TeamsConfig teamsConfig;

    ContributorsPerExtensionSection(RichTextReport landscapeReport, LandscapeAnalysisResults landscapeAnalysisResults, List<ContributorRepositories> contributors,
                                    File reportsFolder, LandscapeReportContributorsTab.Type type, TeamsConfig teamsConfig) {
        this.landscapeReport = landscapeReport;
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.contributors = contributors;
        this.reportsFolder = reportsFolder;
        this.type = type;
        this.teamsConfig = teamsConfig;
    }

    private boolean isContributorReport() {
        return type == LandscapeReportContributorsTab.Type.CONTRIBUTORS;
    }

    void addContributorsPerExtension(boolean linkCharts) {
        landscapeReport.startSubSection(StringUtils.capitalize(type.plural()) + " Per File Extension", "past 30 days");
        if (linkCharts) {
            landscapeReport.startDiv("");
            landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_extensions_" + type.plural() + "_30d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("tree map", "visuals/tree_map_extensions_" + type.plural() + "_30d.html");
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.endDiv();
        }

        landscapeReport.startDiv("");
        List<String> mainExtensions = getMainExtensions();
        List<CommitsPerExtension> contributorsPerExtension = landscapeAnalysisResults.getContributorsPerExtension()
                .stream().filter(c -> mainExtensions.contains(c.getExtension())).collect(Collectors.toList());
        Collections.sort(contributorsPerExtension, (a, b) -> b.getCommitters30Days().size() - a.getCommitters30Days().size());
        boolean tooLong = contributorsPerExtension.size() > 25;
        List<CommitsPerExtension> contributorsPerExtensionDisplay = tooLong ? contributorsPerExtension.subList(0, 25) : contributorsPerExtension;
        List<CommitsPerExtension> linesOfCodePerExtensionHide = tooLong ? contributorsPerExtension.subList(25, contributorsPerExtension.size()) : new ArrayList<>();

        ExtractStringListValue<CommitsPerExtension> valueFunction;

        if (isContributorReport()) {
            valueFunction = (e) -> e.getCommitters30Days();
        } else {
            valueFunction = (e) -> e.getTeams30Days(teamsConfig);
        }

        contributorsPerExtensionDisplay.stream()
                .filter(e -> e.getCommitters30Days().size() > 0)
                .sorted((a, b) -> b.getCommitsCount30Days() - a.getCommitsCount30Days())
                .sorted((a, b) -> valueFunction.getValue(b).size() - valueFunction.getValue(a).size())
                .forEach(extension -> {
                    addLangInfo(extension, valueFunction, extension.getCommitsCount30Days(), getSvgIcon());
                });

        if (linesOfCodePerExtensionHide.stream().filter(e -> e.getCommitters30Days().size() > 0).count() > 0) {
            landscapeReport.startShowMoreBlockDisappear("", "show all...");
            linesOfCodePerExtensionHide.stream().filter(e -> e.getCommitters30Days().size() > 0).forEach(extension -> {
                addLangInfo(extension, valueFunction, extension.getCommitsCount30Days(), getSvgIcon());
            });
            landscapeReport.endShowMoreBlockDisappear();
        }
        landscapeReport.endDiv();

        addContributorDependencies(contributorsPerExtension);
        landscapeReport.endSection();
    }

    private String getSvgIcon() {
        return isContributorReport() ? DEVELOPER_SVG_ICON : TEAM_SVG_ICON;
    }

    private void addContributorDependencies(List<CommitsPerExtension> contributorsPerExtension) {
        Set<String> extensionsNames = new HashSet<>();
        Map<String, List<String>> contrExtMap = extensionsPerContributor(contributorsPerExtension, extensionsNames);
        List<ComponentDependency> dependencies = extensionDependencies(contrExtMap);

        GraphvizDependencyRenderer renderer = new GraphvizDependencyRenderer();
        renderer.setMaxNumberOfDependencies(100);
        renderer.setDefaultNodeFillColor("deepskyblue2");
        renderer.setTypeGraph();
        String graphvizContent = renderer.getMermaidContent(new ArrayList<>(extensionsNames), dependencies);

        if (isContributorReport()) {
            new Force3DGraphExporter().export2D3DForceGraph(dependencies, reportsFolder, "extension_dependencies_30d");

            landscapeReport.startDetailsBlock("extension dependencies...");

            landscapeReport.addGraphvizFigure("extension_dependencies_30d", "Extension dependencies", graphvizContent);
            addDownloadLinks("extension_dependencies_30d");
            landscapeReport.addLineBreak();
            landscapeReport.addNewTabLink(" - show extension dependencies as 2D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/extension_dependencies_30d_force_2d.html");
            landscapeReport.addNewTabLink(" - show extension dependencies as 3D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/extension_dependencies_30d_force_3d.html");

            landscapeReport.endDetailsBlock();
        }
    }

    /** Contributor -> the "<ext> (n)" labels of the extensions they committed to in 30 days; also collects the labels. */
    private static Map<String, List<String>> extensionsPerContributor(List<CommitsPerExtension> contributorsPerExtension, Set<String> extensionsNames) {
        Map<String, List<String>> contrExtMap = new HashMap<>();
        contributorsPerExtension.stream().filter(e -> e.getCommitters30Days().size() > 0).forEach(commitsPerExtension -> {
            String extensionDisplayLabel = commitsPerExtension.getExtension() + " (" + commitsPerExtension.getCommitters30Days().size() + ")";
            extensionsNames.add(extensionDisplayLabel);
            commitsPerExtension.getCommitters30Days().forEach(contributor -> {
                if (contrExtMap.containsKey(contributor)) {
                    contrExtMap.get(contributor).add(extensionDisplayLabel);
                } else {
                    contrExtMap.put(contributor, new ArrayList<>(Arrays.asList(extensionDisplayLabel)));
                }
            });
        });
        return contrExtMap;
    }

    /** Edges between main extensions counting the contributors active in both (each unordered pair counted once). */
    private List<ComponentDependency> extensionDependencies(Map<String, List<String>> contrExtMap) {
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependencyMap = new HashMap<>();

        List<String> mainExtensions = getMainExtensions();
        contrExtMap.values().stream().filter(v -> v.size() > 1).forEach(extensions -> {
            extensions.stream().filter(extension1 -> mainExtensions.contains(extension1.replaceAll("\\(.*\\)", "").trim())).forEach(extension1 -> {
                extensions.stream().filter(extension2 -> mainExtensions.contains(extension2.replaceAll("\\(.*\\)", "").trim())).filter(extension2 -> !extension1.equalsIgnoreCase(extension2)).forEach(extension2 -> {
                    String key1 = extension1 + "::" + extension2;
                    String key2 = extension2 + "::" + extension1;

                    if (dependencyMap.containsKey(key1)) {
                        dependencyMap.get(key1).increment(1);
                    } else if (dependencyMap.containsKey(key2)) {
                        dependencyMap.get(key2).increment(1);
                    } else {
                        ComponentDependency dependency = new ComponentDependency(extension1, extension2);
                        dependencyMap.put(key1, dependency);
                        dependencies.add(dependency);
                    }
                });
            });
        });

        dependencies.forEach(dependency -> dependency.setCount(dependency.getCount() / 2));
        return dependencies;
    }

    private List<String> getMainExtensions() {
        return landscapeAnalysisResults.getMainLinesOfCodePerExtension().stream()
                .map(l -> l.getName().replace("*.", "").trim()).collect(Collectors.toList());
    }

    private void addLangInfo(CommitsPerExtension extension, ExtractStringListValue<CommitsPerExtension> extractor, int commitsCount, String suffix) {
        int size = extractor.getValue(extension).size();
        String smallTextForNumber = FormattingUtils.getSmallTextForNumber(size) + suffix;
        addLangInfoBlockExtra(smallTextForNumber, extension.getExtension().replace("*.", "").trim(),
                size + " " + (size == 1 ? "contributor" : "contributors (" + commitsCount + " commits)") + ":\n" +
                        HtmlEscapeUtils.escape(extractor.getValue(extension).stream().limit(100)
                                .collect(Collectors.joining(", "))), FormattingUtils.getSmallTextForNumber(commitsCount) + " commits");
    }

    void addContributorsPerExtension() {
        int commitsCount = landscapeAnalysisResults.getCommitsCount();
        if (commitsCount > 0) {
            List<CommitsPerExtension> perExtension = landscapeAnalysisResults.getContributorsPerExtension();

            if (perExtension.size() > 0) {
                int count = perExtension.size();
                int limit = 100;
                if (perExtension.size() > limit) {
                    perExtension = perExtension.subList(0, limit);
                }
                landscapeReport.startSubSection("Commits & File Extensions (" + count + ")", "");

                landscapeReport.startDetailsBlock("extension stats...");

                landscapeReport.startTable("");
                landscapeReport.addTableHeader("", "Extension",
                        "# contributors<br>30 days", "# commits<br>30 days", "# files<br>30 days",
                        "# contributors<br>90 days", "# commits<br>90 days", "# files<br>90 days",
                        "# contributors", "# commits", "# files");

                perExtension.forEach(commitsPerExtension -> {
                    addCommitExtension(commitsPerExtension);
                });
                landscapeReport.endTable();
                if (perExtension.size() < count) {
                    landscapeReport.addParagraph("Showing top " + limit + " items (out of " + count + ").");
                }

                landscapeReport.endDetailsBlock();

                landscapeReport.endSection();
            }
        }
    }

    private void addCommitExtension(CommitsPerExtension commitsPerExtension) {
        landscapeReport.startTableRow(commitsPerExtension.getCommitters30Days().size() > 0 ? "font-weight: bold;"
                : "color: " + (commitsPerExtension.getCommitters90Days().size() > 0 ? "grey" : "lightgrey"));
        String extension = commitsPerExtension.getExtension();
        landscapeReport.addTableCell("" + DataImageUtils.getLangDataImageDiv42(extension), "text-align: center;");
        landscapeReport.addTableCell("" + extension, "text-align: center; max-width: 100px; width: 100px");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters30Days().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount30Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount30Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters90Days().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount90Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount90Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount(), "text-align: center;");
        landscapeReport.endTableCell();
        landscapeReport.endTableRow();
    }

    private void addLangInfoBlockExtra(String value, String lang, String description, String extra) {
        // Open this report (contributors/teams) pre-filtered to people who have committed to the
        // clicked language; the query is in the URL fragment so the embedded-data page stays cached.
        String link = StringUtils.isNotBlank(lang)
                ? type.plural() + "-report.html?tab=recent#includesLang:" + lang.trim().toLowerCase()
                : null;
        InfoBlocks.addLangInfoBlockExtra(landscapeReport, value, lang, description, extra, link);
    }

    private void addDownloadLinks(String graphId) {
        landscapeReport.startDiv("");
        landscapeReport.addHtmlContent("Download: ");
        landscapeReport.addHtmlContent("<a href=\"#\" onclick=\"return downloadMermaid('" + graphId + "');\">Mermaid (.mmd)</a>");
        landscapeReport.addHtmlContent(" ");
        landscapeReport.addNewTabLink("(open online Mermaid editor)", "https://obren.io/tools/mermaid/");
        landscapeReport.endDiv();
    }
}

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.generators.statichtml.CommitsReportGenerator;
import nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.reports.utils.PromptsUtils;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CustomTab;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.function.Predicate;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;

/**
 * The Visuals tab of the repository report index: the visual code explorers and the file, contributor, component,
 * file-dependency and unit visualization tables linking into the visuals/ folder. Moved out of
 * {@link ReportFileExporter}, which assembles the index page.
 */
class ReportVisualsTab {
    private ReportVisualsTab() {
    }


    static void addVisuals(RichTextReport report, CodeAnalysisResults analysisResults, File htmlExportFolder) {
        addVisualCodeExplorers(report, analysisResults);
        addFileVisualizations(report, analysisResults);
        addContributorVisualizations(report, analysisResults);
        addComponentVisualizations(report, analysisResults);
        addFileDependencyVisualizations(report, analysisResults);
        addUnitVisualizations(report, analysisResults);
    }


    private static void addVisualCodeExplorers(RichTextReport report, CodeAnalysisResults analysisResults) {
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysisResults.getTestAspectAnalysisResults();
        AspectAnalysisResults build = analysisResults.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults generated = analysisResults.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults other = analysisResults.getOtherAspectAnalysisResults();

        report.addLevel2Header("Visual Code Explorers");

        report.startTable();
        addScopeVisuals(report, "main", main.getFilesCount());
        addScopeVisuals(report, "test", test.getFilesCount());
        addScopeVisuals(report, "build and deployment", build.getFilesCount());
        addScopeVisuals(report, "generated", generated.getFilesCount());
        addScopeVisuals(report, "other", other.getFilesCount());
        report.endTable();

        report.addLineBreak();
        report.addLineBreak();
    }


    private static void addFileVisualizations(RichTextReport report, CodeAnalysisResults analysisResults) {
        report.addLevel2Header("File Visualizations");

        report.addParagraph("<a target='_blank' href='FileSize.html'>File size</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(ReportFileExporter.getIconSvg("file_size", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("3D view of file size", "visuals/files_3d.html");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by size category", "visuals/zoomable_circles.html#main_loc_coloring_categories");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles.html#main_loc_coloring");
        report.endListItem();
        report.endUnorderedList();
        report.endTable();

        boolean showDuplication = !analysisResults.skipDuplicationAnalysis() && analysisResults.getDuplicationAnalysisResults().getAllDuplicates().size() > 0;
        if (showDuplication) {
            report.addParagraph("<a target='_blank' href='Duplication.html'>Duplication</a> views:", "margin-bottom: 0;");
            report.startTable("");
            report.startTableRow();
            report.startTableCell("border: none");
            report.addHtmlContent(ReportFileExporter.getIconSvg("duplication", 50));
            report.endTableCell();
            report.startTableCell("border: none");
            report.startUnorderedList();
            report.startListItem();
            report.addNewTabLink("2D force graph of duplication among files", "visuals/duplication_among_files_force_2d.html");
            report.endListItem();
            report.startListItem();
            report.addNewTabLink("3D force graph of duplication among files", "visuals/duplication_among_files_force_3d.html");
            report.endListItem();
            report.startListItem();
            report.addNewTabLink("2D view of duplication among files (with duplicates)", "visuals/duplication_among_files_with_duplicates_force_2d.html");
            report.endListItem();
            report.startListItem();
            report.addNewTabLink("3D view of duplication among files (with duplicates)", "visuals/duplication_among_files_with_duplicates_force_3d.html");
            report.endListItem();
            report.endUnorderedList();
            report.endTable();
        }

        report.addParagraph("<a target='_blank' href='FileAge.html'>File age</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(ReportFileExporter.getIconSvg("file_history", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("files grouped by age category", "visuals/zoomable_circles.html#main_age_coloring_categories");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles.html#main_age_coloring");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(ReportFileExporter.getIconSvg("file_history", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("files grouped by freshness category", "visuals/zoomable_circles.html#main_freshness_coloring_categories");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles.html#main_freshness_coloring");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addParagraph("<a target='_blank' href='FileChurn.html'>File change frequency</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(ReportFileExporter.getIconSvg("change", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("files grouped by change frequency category", "visuals/zoomable_circles.html#main_update_frequency_coloring_categories");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles.html#main_update_frequency_coloring");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addParagraph("<a target='_blank' href='FileChurn.html'>Contributors per file</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(ReportFileExporter.getIconSvg("change", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("files grouped by number of contributors category", "visuals/zoomable_circles.html#main_contributors_count_coloring_categories");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles.html#main_contributors_count_coloring");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addLineBreak();
    }


    private static void addContributorVisualizations(RichTextReport report, CodeAnalysisResults analysisResults) {
        report.addLevel2Header("Contributor Visualizations");
        report.addParagraph("<a target='_blank' href='Contributors.html'>Contributor dependency</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(ReportFileExporter.getIconSvg("contributors", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addHtmlContent("past 30 days: ");
        report.addNewTabLink("2D graph", "visuals/people_dependencies_30_1_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with files)", "visuals/people_dependencies_via_files_30_2_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with shared files only)", "visuals/people_dependencies_via_files_30_2_force_2d_only_shared_file.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph", "visuals/people_dependencies_30_1_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with files)", "visuals/people_dependencies_via_files_30_2_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with shared files only)", "visuals/people_dependencies_via_files_30_2_force_3d_only_shared_file.html");
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past 3 months: ");
        report.addNewTabLink("2D graph", "visuals/people_dependencies_90_3_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with files)", "visuals/people_dependencies_via_files_90_4_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with shared files only)", "visuals/people_dependencies_via_files_90_4_force_2d_only_shared_file.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph", "visuals/people_dependencies_90_3_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with files)", "visuals/people_dependencies_via_files_90_4_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with shared files only)", "visuals/people_dependencies_via_files_90_4_force_3d_only_shared_file.html");
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past 6 months: ");
        report.addNewTabLink("2D graph", "visuals/people_dependencies_180_5_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with files)", "visuals/people_dependencies_via_files_180_6_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with shared files only)", "visuals/people_dependencies_via_files_180_6_force_2d_only_shared_file.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph", "visuals/people_dependencies_180_5_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with files)", "visuals/people_dependencies_via_files_180_6_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with shared files only)", "visuals/people_dependencies_via_files_180_6_force_3d_only_shared_file.html");
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past year: ");
        report.addNewTabLink("2D graph", "visuals/people_dependencies_365_7_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with files)", "visuals/people_dependencies_via_files_365_8_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with shared files only)", "visuals/people_dependencies_via_files_365_8_force_2d_only_shared_file.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph", "visuals/people_dependencies_365_7_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with files)", "visuals/people_dependencies_via_files_365_8_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with shared files only)", "visuals/people_dependencies_via_files_365_8_force_3d_only_shared_file.html");
        report.endListItem();
        report.endTableRow();
        report.endTable();

        report.addLineBreak();
    }


    private static void addComponentVisualizations(RichTextReport report, CodeAnalysisResults analysisResults) {
        report.addLevel2Header("Components and Dependencies Visualizations");

        report.startTable("text-align: center");
        addComponentVisualizationsHeader(report);
        int index[] = {0};
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            index[0] += 1;
            addComponentVisualizationsRow(report, analysisResults, logicalDecomposition, index[0]);
        });
        report.endTable();


        report.addLineBreak();
        report.addLineBreak();
    }

    /** The three header rows: the icons, the report links, the temporal-dependency windows. */
    private static void addComponentVisualizationsHeader(RichTextReport report) {
        report.addHtmlContent("<tr>");
        report.addHtmlContent("<td rowspan='3' style='border: none'></td>");
        report.addHtmlContent("<td colspan='2' style='text-align: center; border: none'>" + ReportFileExporter.getIconSvg("code_organization", 50) + "</td>");
        report.addHtmlContent("<td colspan='6' style='text-align: center; border: none'>" + ReportFileExporter.getIconSvg("temporal_dependency", 50) + "</td>");
        report.addHtmlContent("<td colspan='1' style='text-align: center; border: none'>" + ReportFileExporter.getIconSvg("duplication", 50) + "</td>");
        report.addHtmlContent("<td colspan='2' style='text-align: center; border: none'>" + ReportFileExporter.getIconSvg("commits", 50) + "</td>");
        report.addHtmlContent("</tr>");
        report.addHtmlContent("<tr>");
        report.addHtmlContent("<td colspan='2' rowspan='2' style='text-align: center'><a target='_blank' href='Components.html'>Components</a></td>");
        report.addHtmlContent("<td colspan='9' style='text-align: center'><a target='_blank' href='FileTemporalDependencies.html'>Temporal Dependencies</a></td>");
        report.addHtmlContent("<td colspan='1' rowspan='2' style='text-align: center'><a target='_blank' href='Duplication.html'>Duplication</a></td>");
        report.addHtmlContent("<td colspan='2' rowspan='2' style='text-align: center'><a target='_blank' href='Commits.html'>Commits Racing Charts</a></td>");
        report.addHtmlContent("</tr>");
        report.addHtmlContent("<tr>");
        report.addHtmlContent("<td colspan='3' style='text-align: center'>30 days</td>");
        report.addHtmlContent("<td colspan='3' style='text-align: center'>3 months</td>");
        report.addHtmlContent("<td colspan='3' style='text-align: center'>6 months</td>");
        report.addHtmlContent("</tr>");
    }

    /** One decomposition's row: bubble chart and tree map, the 2D/3D temporal dependency graphs per window, the racing charts. */
    private static void addComponentVisualizationsRow(RichTextReport report, CodeAnalysisResults analysisResults, LogicalDecompositionAnalysisResults logicalDecomposition, int index) {
        FilesHistoryAnalysisResults history = analysisResults.getFilesHistoryAnalysisResults();
        report.startTableRow();
        report.addTableCell(logicalDecomposition.getKey().toUpperCase() + " (" + logicalDecomposition.getComponents().size() + ")");
        report.startTableCell("text-align: center");
        report.addNewTabLink("Bubble Chart", "visuals/bubble_chart_components_" + index + ".html");
        report.endTableCell();
        report.startTableCell("text-align: center");
        report.addNewTabLink("Tree Map", "visuals/tree_map_components_" + index + ".html");
        report.endTableCell();
        addForceGraphCells(report, history.getFilePairsChangedTogether30Days().size() > 0, index, 30);
        addForceGraphCells(report, history.getFilePairsChangedTogether90Days().size() > 0, index, 90);
        addForceGraphCells(report, history.getFilePairsChangedTogether180Days().size() > 0, index, 180);
        report.startTableCell("text-align: center");
        report.addNewTabLink("All Time", "visuals/racing_charts_component_commits_" + index + ".html?tickDuration=600");
        report.endTableCell();
        report.startTableCell("text-align: center");
        report.addNewTabLink("12 Months", "visuals/racing_charts_component_commits_12_months_window_" + index + ".html?tickDuration=600");
        report.endTableCell();
        report.endTableRow();
    }

    /** The 2D and 3D force-graph links of one window, greyed out when no file pairs changed together in it. */
    private static void addForceGraphCells(RichTextReport report, boolean hasPairs, int index, int days) {
        for (String dimension : new String[]{"2d", "3d"}) {
            report.startTableCell("text-align: center");
            if (hasPairs) {
                report.addNewTabLink(dimension.toUpperCase(), "visuals/file_changed_together_dependencies_logical_decomposition_" + index + "_" + days + "_days_force_" + dimension + ".html");
            } else {
                report.addContentInDiv(dimension.toUpperCase(), "color: #c0c0c0");
            }
            report.endTableCell();
        }
    }





    private static void addFileDependencyVisualizations(RichTextReport report, CodeAnalysisResults analysisResults) {
        report.addLevel2Header("File Dependencies Visualizations");

        report.addParagraph("<a target='_blank' href='FileTemporalDependencies.html'>Temporal dependencies</a> among files:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(ReportFileExporter.getIconSvg("temporal_dependency", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addHtmlContent("past 30 days: ");
        if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether30Days().size() > 0) {
            report.addNewTabLink("2D graph", "visuals/file_changed_together_dependencies_files_30_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_30_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph", "visuals/file_changed_together_dependencies_files_30_days_force_3d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_30_days_force_3d.html");
        } else {
            report.addHtmlContent("no dependencies");
        }
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past 3 months: ");
        if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether90Days().size() > 0) {
            report.addNewTabLink("2D graph", "visuals/file_changed_together_dependencies_files_90_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_90_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph", "visuals/file_changed_together_dependencies_files_90_days_force_3d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_90_days_force_3d.html");
        } else {
            report.addHtmlContent("no dependencies");
        }
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past 6 months: ");
        if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether180Days().size() > 0) {
            report.addNewTabLink("2D graph", "visuals/file_changed_together_dependencies_files_180_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_180_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph", "visuals/file_changed_together_dependencies_files_180_days_force_3d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_180_days_force_3d.html");
        } else {
            report.addHtmlContent("no dependencies");
        }
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addLineBreak();
        report.addLineBreak();
    }


    private static void addUnitVisualizations(RichTextReport report, CodeAnalysisResults analysisResults) {
        report.addLevel2Header("Units Visualizations");

        report.addParagraph("Unit <a target='_blank' href='UnitSize.html'>size</a> and <a target='_blank' href='ConditionalComplexity.html'>conditional complexity</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(ReportFileExporter.getIconSvg("unit_size", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("3D view of unit size", "visuals/units_3d_size.html");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("3D view of unit complexity", "visuals/units_3d_complexity.html");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addLineBreak();
    }


    private static void addScopeVisuals(RichTextReport report, String scopeName, int filesCount) {
        String technicalName = scopeName.toLowerCase().replace(" ", "_");
        boolean exists = filesCount > 0;
        report.startTableRow(exists ? "" : "color: #c0c0c0");
        report.startTableCell();
        report.addHtmlContent(ReportFileExporter.getIconSvg(technicalName, 42));
        report.endTableCell();
        report.addTableCell(scopeName.toUpperCase() + " (" + filesCount + ")", "");
        report.startTableCell();
        if (exists) {
            report.addNewTabLink("Circles", "visuals/zoomable_circles.html#" + technicalName.replace("_and_deployment", ""));
        } else {
            report.addContentInDiv("Circles", "color: #c0c0c0");
        }
        report.endTableCell();
        report.startTableCell();
        if (exists) {
            report.addNewTabLink("Sunburst", "visuals/zoomable_sunburst.html#" + technicalName.replace("_and_deployment", ""));
        } else {
            report.addContentInDiv("Sunburst", "color: #c0c0c0");
        }
        report.endTableCell();
        report.endTableCell();
        report.endTableRow();
    }
}

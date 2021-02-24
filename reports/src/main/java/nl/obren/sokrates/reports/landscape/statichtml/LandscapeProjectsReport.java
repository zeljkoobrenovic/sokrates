package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LandscapeProjectsReport {
    private LandscapeAnalysisResults landscapeAnalysisResults;

    public LandscapeProjectsReport(LandscapeAnalysisResults landscapeAnalysisResults) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
    }

    public void saveProjectsReport(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startTable("width: 100%");
        int thresholdCommits = landscapeAnalysisResults.getConfiguration().getContributorThresholdCommits();
        int thresholdContributors = landscapeAnalysisResults.getConfiguration().getProjectThresholdContributors();
        report.addTableHeader("",
                "Project" + (thresholdContributors > 1 ? "<br/>(" + thresholdContributors + "+&nbsp;contributors)" : ""),
                "Main<br/>Language", "LOC<br/>(main)",
                "LOC<br/>(test)", "LOC<br/>(other)",
                "Age", "Contributors" + (thresholdCommits > 1 ? "<br/>(" + thresholdCommits + "+&nbsp;commits)" : ""),
                "Recent<br>Contributors<br>(30d)", "Rookies", "Commits<br>this year", "Report");
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsThisYear()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsThisYear());
        projectsAnalysisResults.forEach(projectAnalysis -> {
            addProjectRow(report, projectAnalysis);
        });
        report.endTable();
    }


    private void addProjectRow(RichTextReport report, ProjectAnalysisResults projectAnalysis) {
        CodeAnalysisResults analysisResults = projectAnalysis.getAnalysisResults();
        Metadata metadata = analysisResults.getMetadata();
        String logoLink = metadata.getLogoLink();

        report.startTableRow();
        report.addTableCell(StringUtils.isNotBlank(logoLink) ? "<img src='" + logoLink + "' style='width: 20px'>" : "", "text-align: center");
        report.addTableCell(metadata.getName());
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysisResults.getTestAspectAnalysisResults();
        AspectAnalysisResults generated = analysisResults.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults build = analysisResults.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults other = analysisResults.getOtherAspectAnalysisResults();

        int thresholdCommits = landscapeAnalysisResults.getConfiguration().getContributorThresholdCommits();
        List<Contributor> contributors = analysisResults.getContributorsAnalysisResults().getContributors()
                .stream().filter(c -> c.getCommitsCount() >= thresholdCommits).collect(Collectors.toCollection(ArrayList::new));

        int contributorsCount = contributors.size();
        int recentContributorsCount = (int) contributors.stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
        int rookiesCount = (int) contributors.stream().filter(c -> c.isRookie(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();

        List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
        StringBuilder locSummary = new StringBuilder();
        if (linesOfCodePerExtension.size() > 0) {
            locSummary.append(linesOfCodePerExtension.get(0).getName().replace("*.", "").trim().toUpperCase());
        } else {
            locSummary.append("-");
        }
        report.addTableCell(locSummary.toString().replace("> = ", ">"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(main.getLinesOfCode(), "-"), "text-align: center");

        report.addTableCell(FormattingUtils.formatCount(test.getLinesOfCode(), "-"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(generated.getLinesOfCode() + build.getLinesOfCode() + other.getLinesOfCode(), "-"), "text-align: center");
        int projectAgeYears = (int) Math.round(analysisResults.getFilesHistoryAnalysisResults().getAgeInDays() / 365.0);
        String age = projectAgeYears == 0 ? "<1y" : projectAgeYears + "y";
        report.addTableCell(age, "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(contributorsCount, "-"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(recentContributorsCount, "-"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(rookiesCount, "-"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(analysisResults.getContributorsAnalysisResults().getCommitsThisYear(), "-"), "text-align: center");
        String projectReportUrl = landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/index.html";
        report.addTableCell("<a href='" + projectReportUrl + "' target='_blank'>"
                + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 40) + "</div></a>", "text-align: center");
        report.endTableRow();
    }

}

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjects;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LandscapeContributorsReport {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private RichTextReport report;

    public LandscapeContributorsReport(LandscapeAnalysisResults landscapeAnalysisResults, RichTextReport report) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.report = report;
    }

    public void saveContributorsTable(List<ContributorProjects> contributors, int totalCommits) {
        report.startTable("width: 100%");
        report.addTableHeader("", "Contributor", "# commits", "# commits<br>30 days", "# commits<br>90 days", "first", "latest", "projects");

        int counter[] = {0};

        contributors.forEach(contributor -> {
            addContributor(totalCommits, counter, contributor);
        });
        report.endTable();
    }


    private void addContributor(int totalCommits, int[] counter, ContributorProjects contributor) {
        report.startTableRow(contributor.getContributor().isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS) ? "font-weight: bold;"
                : "color: " + (contributor.getContributor().isActive(90) ? "grey" : "lightgrey"));
        counter[0] += 1;
        report.addTableCell("" + counter[0], "text-align: center; vertical-align: top; padding-top: 13px;");
        report.addTableCell(StringEscapeUtils.escapeHtml4(contributor.getContributor().getEmail()), "vertical-align: top; padding-top: 13px;");
        int contributerCommits = contributor.getContributor().getCommitsCount();
        double percentage = 100.0 * contributerCommits / totalCommits;
        report.addTableCell(contributerCommits + " (" + FormattingUtils.getFormattedPercentage(percentage) + "%)", "vertical-align: top; padding-top: 13px;");
        report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount30Days()), "vertical-align: top; padding-top: 13px;");
        report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount90Days()), "vertical-align: top; padding-top: 13px;");
        report.addTableCell(contributor.getContributor().getFirstCommitDate(), "vertical-align: top; padding-top: 13px;");
        report.addTableCell(contributor.getContributor().getLatestCommitDate(), "vertical-align: top; padding-top: 13px;");
        StringBuilder projectInfo = new StringBuilder();
        report.startTableCell();
        int projectsCount = contributor.getProjects().size();
        report.startShowMoreBlock(projectsCount + (projectsCount == 1 ? " project" : " projects"));
        contributor.getProjects().forEach(contributorProjectInfo -> {
            String projectName = contributorProjectInfo.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName();
            int commits = contributorProjectInfo.getCommitsCount();
            if (projectInfo.length() > 0) {
                projectInfo.append("<br/>");
            }
            projectInfo.append(projectName + " <span style='color: grey'>(" + commits + (commits == 1 ? " commit" : " commit") + ")</span>");
        });
        report.addHtmlContent(projectInfo.toString());
        report.endTableCell();
        report.endTableRow();
    }


}

/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;

import java.util.List;
import java.util.stream.Collectors;

public class ContributorsReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private int graphCounter = 1;

    public ContributorsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addContributorsAnalysisToReport(RichTextReport report) {
        ContributorsReportUtils.addContributorsPerYear(report, codeAnalysisResults.getContributorsAnalysisResults().getContributorsPerYear());;
        report.addLineBreak();

        report.startTabGroup();
        report.addTab("all_time", "all time", true);
        report.addTab("30_days", "past 30 days", false);
        report.addTab("90_days", "past 3 months", false);
        report.addTab("180_days", "past 6 months", false);
        report.addTab("365_days", "past year", false);
        report.endTabGroup();

        List<Contributor> contributors = codeAnalysisResults.getContributorsAnalysisResults().getContributors();

        report.startTabContentSection("all_time", true);
        addContributorsPanel(report, contributors, c -> c.getCommitsCount());
        report.endTabContentSection();

        report.startTabContentSection("30_days", false);
        List<Contributor> commits30Days = contributors.stream().filter(c -> c.getCommitsCount30Days() > 0).collect(Collectors.toList());
        commits30Days.sort((a, b) -> b.getCommitsCount30Days() - a.getCommitsCount30Days());
        addContributorsPanel(report, commits30Days, c -> c.getCommitsCount30Days());
        report.endTabContentSection();

        report.startTabContentSection("90_days", false);
        List<Contributor> commits90Days = contributors.stream().filter(c -> c.getCommitsCount90Days() > 0).collect(Collectors.toList());
        commits90Days.sort((a, b) -> b.getCommitsCount90Days() - a.getCommitsCount90Days());
        addContributorsPanel(report, commits90Days, c -> c.getCommitsCount90Days());
        report.endTabContentSection();

        report.startTabContentSection("180_days", false);
        List<Contributor> commits180Days = contributors.stream().filter(c -> c.getCommitsCount180Days() > 0).collect(Collectors.toList());
        commits180Days.sort((a, b) -> b.getCommitsCount180Days() - a.getCommitsCount180Days());
        addContributorsPanel(report, commits180Days, c -> c.getCommitsCount180Days());
        report.endTabContentSection();

        report.startTabContentSection("365_days", false);
        List<Contributor> commits365Days = contributors.stream().filter(c -> c.getCommitsCount365Days() > 0).collect(Collectors.toList());
        commits365Days.sort((a, b) -> b.getCommitsCount365Days() - a.getCommitsCount365Days());
        addContributorsPanel(report, commits365Days, c -> c.getCommitsCount365Days());
        report.endTabContentSection();
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
            contributors.forEach(contributor -> {
                int contributorCommitsCount = contributionCounter.count(contributor);
                int w = (int) Math.round(600 * (double) contributorCommitsCount / total[0]);
                int x = 620 - w;
                map.append("<div style='background-color: " + palette.nextColor() + "; display: inline-block; height: 20px; width: " + w + "px' title='" + contributor.getEmail() + "\n" + contributorCommitsCount + " commits (" + (Math.round(100.0 * contributorCommitsCount / total[0])) + "%)'>&nbsp;</div>");
            });
            report.addHtmlContent(map.toString());
            report.addLineBreak();
            report.addLineBreak();
        }
        report.startTable();
        report.addTableHeader("#", "Contributor", "First Commit", "Latest Commit", "# commit");
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

    interface ContributionCounter {
        int count(Contributor contributor);
    }
}

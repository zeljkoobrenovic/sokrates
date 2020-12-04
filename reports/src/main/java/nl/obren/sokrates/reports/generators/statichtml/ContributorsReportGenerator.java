/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;

public class ContributorsReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private int graphCounter = 1;

    public ContributorsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addContributorsAnalysisToReport(RichTextReport report) {
        report.addParagraph("Analysis of commits and contributors' activity.");

        report.startTable();
report.addTableHeader("Contributor", "First Commit", "Latest Commit", "# commit", "# commit 30d", "# commit 90d");
        codeAnalysisResults.getContributorsAnalysisResults().getContributors().forEach(contributor -> {
            report.startTableRow();
            report.addTableCell(contributor.getEmail());
            report.addTableCell(contributor.getFirstCommitDate());
            report.addTableCell(contributor.getLatestCommitDate());
            report.addTableCell(contributor.getCommitsCount() + "");
            report.addTableCell(contributor.getCommitsCount30Days() + "");
            report.addTableCell(contributor.getCommitsCount90Days() + "");
            report.endTableRow();
        });

        report.endTable();
    }
}

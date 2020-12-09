/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionYear;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ContributorsReportUtils {
    public static void addContributorsSection(CodeAnalysisResults analysisResults, RichTextReport report) {
        ContributorsAnalysisResults contributorsAnalysisResults = analysisResults.getContributorsAnalysisResults();
        List<Contributor> contributors = contributorsAnalysisResults.getContributors();
        List<ContributionYear> contributorsPerYear = contributorsAnalysisResults.getContributorsPerYear();
        if (contributors.size() > 0) {
            addContributors(report, contributors);
        }
    }

    public static void addContributorsPerYear(RichTextReport report, List<ContributionYear> contributorsPerYear) {
        if (contributorsPerYear.size() > 0) {
            int limit = 20;
            if (contributorsPerYear.size() > limit) {
                contributorsPerYear = contributorsPerYear.subList(contributorsPerYear.size() - limit, contributorsPerYear.size());
            }

            int maxContributors = contributorsPerYear.stream().mapToInt(c -> c.getContributorsCount()).max().orElse(1);
            int maxCommits = contributorsPerYear.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);

            report.startTable();

            report.startTableRow();
            report.addTableCell("Commits", "border: none;");
            String style = "border: none; text-align: center; vertical-align: bottom; font-size: 80%";
            contributorsPerYear.forEach(year -> {
                report.startTableCell(style);
                int count = year.getCommitsCount();
                report.addParagraph(count + "", "margin: 2px");
                int height = 1 + (int) (64.0 * count / maxCommits);
                report.addHtmlContent("<div style='width: 100%; background-color: darkgrey; height:" + height + "px'></div>");
                report.endTableCell();
            });
            report.endTableRow();

            report.startTableRow();
            report.addTableCell("Contributors", "border: none;");
            contributorsPerYear.forEach(year -> {
                report.startTableCell(style);
                int count = year.getContributorsCount();
                report.addParagraph(count + "", "margin: 2px");
                int height = 1 + (int) (64.0 * count / maxContributors);
                report.addHtmlContent("<div style='width: 100%; background-color: skyblue; height:" + height + "px'></div>");
                report.endTableCell();
            });
            report.endTableRow();

            report.startTableRow();
            report.addTableCell("", "border: none; ");
            contributorsPerYear.forEach(year -> {
                report.addTableCell(year.getYear(), "border: none; text-align: center; font-size: 90%");
            });
            report.endTableRow();

            report.endTable();
        }
    }

    public static void addContributors(RichTextReport indexReport, List<Contributor> contributors) {
        Collections.sort(contributors, (a, b) -> b.getCommitsCount() - a.getCommitsCount());
        int max = contributors.get(0).getCommitsCount();
        int total = contributors.stream().mapToInt(c -> c.getCommitsCount()).sum();
        long activeCount = contributors.stream().filter(c -> c.isActive()).count();
        long rookiesCount = contributors.stream().filter(c -> c.isRookie()).count();
        long veteransCount = activeCount - rookiesCount;
        long historicalCount = contributors.size() - activeCount;
        indexReport.addLevel2Header("Recent Contributors (" + activeCount
                + " = " + veteransCount + " " + (veteransCount == 1 ? "veteran" : "veterans")
                + " + " + rookiesCount + " " + (rookiesCount == 1 ? "rookie" : "rookies") + ")");
        indexReport.addParagraph("Contributed in past 6 months (a rookie = the first contribution in past year)", "color: grey");
        List<Contributor> contributor30Days = contributors.stream().filter(c -> c.isActive(30)).collect(Collectors.toList());
        List<Contributor> contributor90Days = contributors.stream().filter(c -> c.isActive(90) && !c.isActive(30)).collect(Collectors.toList());
        List<Contributor> contributor180Days = contributors.stream().filter(c -> c.isActive(180) && !c.isActive(90)).collect(Collectors.toList());
        if (contributor30Days.size() > 0) {
            indexReport.addParagraph("Past 30 days (" + contributor30Days.size() + "):", "font-size: 80%");
            contributor30Days.forEach(contributor -> {
                addContributor(indexReport, max, total, contributor);
            });
        } else {
            indexReport.addParagraph("No contributors in past 30 days.", "font-size: 80%");
        }
        indexReport.addHorizontalLine();
        if (contributor90Days.size() > 0) {
            indexReport.addParagraph("Past 31 to 90 days (" + contributor90Days.size() + "):", "font-size: 80%");
            contributor90Days.forEach(contributor -> {
                addContributor(indexReport, max, total, contributor);
            });
        } else {
            indexReport.addParagraph("No contributors in past 31 to 90 days.", "font-size: 80%");
        }
        indexReport.addHorizontalLine();
        if (contributor180Days.size() > 0) {
            indexReport.addParagraph("Past 91 to 180 days (" + contributor180Days.size() + "):", "font-size: 80%");
            contributor180Days.forEach(contributor -> {
                addContributor(indexReport, max, total, contributor);
            });
        } else {
            indexReport.addParagraph("No contributors in past 91 to 180 days.", "font-size: 80%");
        }
        indexReport.addLevel2Header("Historical Contributors (" + historicalCount + ")", "margin-top: 40px");
        indexReport.addParagraph("Last contributed more than 6 months ago", "color: grey");
        contributors.stream().filter(c -> !c.isActive()).forEach(contributor -> {
            addContributor(indexReport, max, total, contributor);
        });
    }

    public static void addContributor(RichTextReport indexReport, int max, int total, Contributor contributor) {
        int commitsCount = contributor.getCommitsCount();
        double opacity = 0.2 + 0.8 * commitsCount / max;
        double percentage = 100.0 * commitsCount / total;
        String info = StringEscapeUtils.escapeHtml4(contributor.getEmail()
                + " " + commitsCount
                + " commits (" + FormattingUtils.getFormattedPercentage(percentage) + "%),"
                + " between " + contributor.getFirstCommitDate() + " and " + contributor.getLatestCommitDate());

        if (contributor.isRookie()) {
            indexReport.addHtmlContent("<div style='border:2px solid green; border-radius: 5px; display: inline-block;opacity:" + opacity + "' title='" + info + "'>");
        } else {
            indexReport.addHtmlContent("<div style='display: inline-block;opacity:" + opacity + "' title='" + info + "'>");
        }
        indexReport.addHtmlContent(contributor.isActive() ? getIconSvg("contributor") : getIconSvg("contributor_historical"));
        indexReport.addHtmlContent("</div>");
    }

    public static String getIconSvg(String icon) {
        return getIconSvg(icon, 80);
    }

    public static String getIconSvg(String icon, int size) {
        String svg = HtmlTemplateUtils.getResource("/icons/" + icon + ".svg");
        svg = svg.replaceAll("height='.*?'", "height='" + size + "px'");
        svg = svg.replaceAll("width='.*?'", "width='" + size + "px'");
        return svg;
    }

}

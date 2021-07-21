package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjectInfo;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjects;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LandscapeIndividualContributorsReports {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private List<RichTextReport> reports = new ArrayList<>();
    private boolean recent = false;

    public LandscapeIndividualContributorsReports(LandscapeAnalysisResults landscapeAnalysisResults) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
    }

    public List<RichTextReport> getIndividualReports() {
        List<ContributorProjects> contributors = landscapeAnalysisResults.getAllContributors();

        contributors.forEach(contributor -> {
            reports.add(getIndividualReport(contributor));
        });

        return reports;
    }

    private RichTextReport getIndividualReport(ContributorProjects contributorProjects) {
        Contributor contributor = contributorProjects.getContributor();
        RichTextReport report = new RichTextReport(contributor.getEmail(), contributor.getEmail().replaceAll("[\\-\\.\\@\\+]", "_") + ".html");
        report.setDisplayName(contributor.getEmail());
        report.setLogoLink(DataImageUtils.DEVELOPER);

        report.startDiv("margin-top: 10px; margin-bottom: 22px;");
        String template = this.landscapeAnalysisResults.getConfiguration().getContributorLinkTemplate();
        if (StringUtils.isNotBlank(template)) {
            String link = LandscapeContributorsReport.getContributorUrlFromTemplate(contributor.getEmail(), template);
            report.addNewTabLink("More details...", link);
            report.setParentUrl(link);
            report.addLineBreak();
            report.addLineBreak();
        }
        report.addContentInDiv("First commit date: <b>" + contributor.getFirstCommitDate() + "</b>");
        report.addContentInDiv("Latest commit date: <b>" + contributor.getLatestCommitDate() + "</b>");
        report.addContentInDiv("Projects count: <b>" + contributorProjects.getProjects().size() + "</b>");
        report.addContentInDiv("Commits count: <b>" + contributor.getCommitsCount30Days() + "</b> " +
                "<span style='color: lightgrey; font-size: 90%'>(30d)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount90Days() + "</b><span style='color: lightgrey; font-size: 90%'> (3m)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount180Days() + "</b><span style='color: lightgrey; font-size: 90%'> (6m)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount365Days() + "</b><span style='color: lightgrey; font-size: 90%'> (1y)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount() + "</b><span style='color: lightgrey; font-size: 90%'> (all time)</span>"
        );
        report.endDiv();

        report.startTabGroup();
        report.addTab("month", "Project Activity Per Month", true);
        report.addTab("week", "Per Week", false);
        report.endTabGroup();

        report.startTabContentSection("week", false);
        addPerWeek(contributorProjects, report);
        report.endTabContentSection();

        report.startTabContentSection("month", true);
        addPerMonth(contributorProjects, report);
        report.endTabContentSection();

        return report;
    }

    private void addPerWeek(ContributorProjects contributorProjects, RichTextReport report) {
        report.startDiv("width: 100%; overflow-x: scroll;");
        report.startTable();

        final List<String> pastWeeks = DateUtils.getPastWeeks(104, landscapeAnalysisResults.getLatestCommitDate());
        report.startTableRow();
        report.addTableCell("", "min-width: 300px; border: none");
        report.addTableCell("Commits (30d)", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("Active Days&nbsp;(2y)", "max-width: 100px; text-align: center; border: none");
        List<ContributorProjectInfo> projects = contributorProjects.getProjects();
        pastWeeks.forEach(pastWeek -> {
            int projectCount[] = {0};
            projects.forEach(project -> {
                boolean found[] = {false};
                project.getCommitDates().forEach(date -> {
                    String weekMonday = DateUtils.getWeekMonday(date);
                    if (weekMonday.equals(pastWeek)) {
                        found[0] = true;
                        return;
                    }
                });
                if (found[0]) {
                    projectCount[0] += 1;
                    return;
                }
            });
            String tooltip = "Week of " + pastWeek + ": " + projectCount[0] + (projectCount[0] == 1 ? " project" : " projects");
            report.startTableCell("font-size: 70%; border: none; color: lightgrey; text-align: center");
            report.addContentInDivWithTooltip(projectCount[0] + "", tooltip, "text-align: center");
            report.endTableCell();
        });
        report.endTableRow();

        List<ContributorProjectInfo> activeProjects = new ArrayList<>();

        projects.forEach(project -> {
            int daysCount[] = {0};
            pastWeeks.forEach(pastWeek -> {
                project.getCommitDates().forEach(date -> {
                    String weekMonday = DateUtils.getWeekMonday(date);
                    if (weekMonday.equals(pastWeek)) {
                        daysCount[0] += 1;
                    }
                });

            });
            if (daysCount[0] > 0) {
                activeProjects.add(project);
            }
        });

        activeProjects.forEach(project -> {
            report.startTableRow();
            report.addTableCell(project.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName(), "border: none");
            report.addTableCell(project.getCommits30Days() + "", "text-align: center; border: none");
            report.addTableCell(project.getCommitDates().size() + "", "text-align: center; border: none");
            pastWeeks.forEach(pastWeek -> {
                int daysCount[] = {0};
                project.getCommitDates().forEach(date -> {
                    String weekMonday = DateUtils.getWeekMonday(date);
                    if (weekMonday.equals(pastWeek)) {
                        daysCount[0] += 1;
                    }
                });
                report.startTableCell("text-align: center; padding: 0; border: none; vertical-align: middle");
                if (daysCount[0] > 0) {
                    int size = 10 + daysCount[0] * 4;
                    String tooltip = "Week of " + pastWeek + ": " + daysCount[0] + (daysCount[0] == 1 ? " commit day" : " commit days");
                    report.addContentInDivWithTooltip("", tooltip,
                            "display: inline-block; padding: 0; margin: 0; " +
                                    "background-color: skyblue; border-radius: 50%; width: " + size + "px; height: " + size + "px;");
                } else {
                    report.addContentInDiv("-", "color: lightgrey; font-size: 80%");
                }
                report.endTableCell();
            });
            report.endTableRow();
        });
        report.endTable();
        report.endDiv();
    }

    private void addPerMonth(ContributorProjects contributorProjects, RichTextReport report) {
        report.startDiv("width: 100%; overflow-x: scroll;");
        report.startTable();

        final List<String> pastMonths = DateUtils.getPastMonths(24, landscapeAnalysisResults.getLatestCommitDate());
        report.startTableRow();
        report.addTableCell("", "min-width: 200px; border: none; border: none");
        report.addTableCell("Commits (30d)", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("Active Days&nbsp;(2y)", "max-width: 100px; text-align: center; border: none");
        pastMonths.forEach(pastMonth -> {
            int projectCount[] = {0};
            contributorProjects.getProjects().forEach(project -> {
                boolean found[] = {false};
                project.getCommitDates().forEach(date -> {
                    String weekMonday = DateUtils.getMonth(date);
                    if (weekMonday.equals(pastMonth)) {
                        found[0] = true;
                        return;
                    }
                });
                if (found[0]) {
                    projectCount[0] += 1;
                    return;
                }
            });
            String tooltip = "Month " + pastMonth + ": " + projectCount[0] + (projectCount[0] == 1 ? " project" : " projects");
            report.startTableCell("font-size: 70%; border: none; color: lightgrey; text-align: center");
            report.addContentInDivWithTooltip(projectCount[0] + "", tooltip, "text-align: center");
            report.endTableCell();
        });
        report.endTableRow();
        List<ContributorProjectInfo> projects = new ArrayList<>(contributorProjects.getProjects());
        Collections.sort(projects, (a, b) -> {
            int diff = b.getCommits30Days() - a.getCommits30Days();
            return diff == 0 ? b.getCommitDates().size() - a.getCommitDates().size() : diff;
        });

        projects.forEach(project -> {
            report.startTableRow();
            report.addTableCell(project.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName(), "border: none");
            report.addTableCell(project.getCommits30Days() + "", "text-align: center; border: none");
            report.addTableCell(project.getCommitDates().size() + "", "text-align: center; border: none");
            pastMonths.forEach(pastMonth -> {
                int count[] = {0};
                project.getCommitDates().forEach(date -> {
                    String month = DateUtils.getMonth(date);
                    if (month.equals(pastMonth)) {
                        count[0] += 1;
                    }
                });
                report.startTableCell("text-align: center; padding: 0; border: none; vertical-align: middle");
                if (count[0] > 0) {
                    int size = 10 + (count[0] / 4) * 4;
                    String tooltip = "Month " + pastMonth + ": " + count[0] + (count[0] == 1 ? " commit day" : " commit days");
                    report.addContentInDivWithTooltip("", tooltip,
                            "padding: 0; margin: 0; display: inline-block; background-color: skyblue; border-radius: 50%; width: " + size + "px; height: " + size + "px;");
                } else {
                    report.addContentInDiv("-", "color: lightgrey; font-size: 80%");
                }
                report.endTableCell();
            });
            report.endTableRow();
        });
        report.endTable();
        report.endDiv();
    }

}

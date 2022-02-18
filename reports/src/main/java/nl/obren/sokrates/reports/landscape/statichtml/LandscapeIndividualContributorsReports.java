package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.ContributorPerExtensionHelper;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.ContributorPerExtensionStats;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjectInfo;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjects;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LandscapeIndividualContributorsReports {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private List<RichTextReport> reports = new ArrayList<>();

    public LandscapeIndividualContributorsReports(LandscapeAnalysisResults landscapeAnalysisResults) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
    }

    public static String getSafeFileName(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        for (char c : string.toCharArray()) {
            if (c == '.' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else {
                sb.append("_");
            }
        }
        return sb.toString();
    }

    public static String getContributorIndividualReportFileName(String email) {
        return getSafeFileName(email).toLowerCase() + ".html";
    }

    public List<RichTextReport> getIndividualReports(List<ContributorProjects> contributors) {
        contributors.forEach(contributor -> {
            reports.add(getIndividualReport(contributor));
        });

        return reports;
    }

    private RichTextReport getIndividualReport(ContributorProjects contributorProjects) {
        Contributor contributor = contributorProjects.getContributor();
        RichTextReport report = new RichTextReport(contributor.getEmail(), getContributorIndividualReportFileName(contributor.getEmail()));
        report.setRenderLogo(false);
        String breadcrumbsLabel = landscapeAnalysisResults.getConfiguration().getMetadata().getName() + " / Contributors";
        String breadcrumbsHtml = "<div style='opacity: 0.7; font-size: 13px; margin-bottom: 12px;'><a href='../index.html'>" + breadcrumbsLabel + "</a></div>";

        String avatarHtml = "";
        String avatarUrl = LandscapeContributorsReport.getAvatarUrl(contributor.getEmail(), landscapeAnalysisResults.getConfiguration().getContributorAvatarLinkTemplate());
        if (avatarUrl != null) {
            avatarHtml = "<div style='vertical-align: middle; display: inline-block; width: 88px; margin-top: 2px;'>" +
                    "<img style='border-radius: 50%; height: 80px; width: 80px; margin-right: 10px;' src='" + avatarUrl + "' " +
                    "onerror=\"this.onerror=null;this.src='" + DataImageUtils.DEVELOPER + "';\">" +
                    "</div>";
        } else {
            avatarHtml = "<div style='vertical-align: middle; display: inline-block; width: 48px; margin-top: 2px;'>" +
                    "<img style='border-radius: 50%; height: 40px; width: 40px; margin-right: 10px;' src='" + DataImageUtils.DEVELOPER + "'>" +
                    "</div>";
        }

        report.setDisplayName(breadcrumbsHtml + avatarHtml + contributor.getEmail());

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
        report.addContentInDiv("Projects count: " +
                "<b>" + contributorProjects.getProjects().stream().filter(p -> p.getCommits30Days() > 0).count()
                + "</b><span style='color: lightgrey; font-size: 90%'> (30d)</span>&nbsp;&nbsp;&nbsp;" + "<b>" + contributorProjects.getProjects().stream().filter(p -> p.getCommits90Days() > 0).count()
                + "</b><span style='color: lightgrey; font-size: 90%'> (3m)</span>&nbsp;&nbsp;&nbsp;" +
                "<b>" + contributorProjects.getProjects().size() + "</b> <span style='color: lightgrey; font-size: 90%'> (all time)</span>");
        report.addContentInDiv("Commits count: <b>" + contributor.getCommitsCount30Days() + "</b> " +
                "<span style='color: lightgrey; font-size: 90%'>(30d)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount90Days() + "</b><span style='color: lightgrey; font-size: 90%'> (3m)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount180Days() + "</b><span style='color: lightgrey; font-size: 90%'> (6m)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount365Days() + "</b><span style='color: lightgrey; font-size: 90%'> (1y)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount() + "</b><span style='color: lightgrey; font-size: 90%'> (all time)</span>"
        );
        report.endDiv();

        ContributorPerExtensionHelper helper = new ContributorPerExtensionHelper();

        List<Pair<String, ContributorPerExtensionStats>> extensionUpdates = helper.getContributorStatsPerExtension(landscapeAnalysisResults.getConfiguration(), contributorProjects);

        report.addContentInDiv("File updates per extension (90 days):");
        report.startTable();
        report.startTableRow();
        int max = helper.getContributorsPerExtensionStream(extensionUpdates).mapToInt(e -> e.getRight().getFileUpdates90Days()).max().orElse(1);

        helper.getContributorsPerExtensionStream(extensionUpdates).forEach(extensionUpdate -> {
            report.startTableCell("border: 0; text-align: center; vertical-align: bottom");
            int fileUpdates90Days = extensionUpdate.getRight().getFileUpdates90Days();
            report.addContentInDiv(FormattingUtils.getSmallTextForNumber(fileUpdates90Days), "text-align: center; font-size: 70%; color: lightgrey;");
            int height = (int) (64.0 * fileUpdates90Days / max) + 1;
            report.addContentInDiv("", "background-color: skyblue; width: 35px; height: " + height + "px;");
            report.endTableCell();
        });
        report.endTableRow();
        report.startTableRow();
        helper.getContributorsPerExtensionStream(extensionUpdates).forEach(extensionUpdate -> {
            report.addTableCell(DataImageUtils.getLangDataImageDiv30(extensionUpdate.getLeft()), "border: 0");
        });
        report.endTableRow();
        report.endTable();

        report.addLineBreak();

        report.startTabGroup();
        report.addTab("year", "Project Activity Per Year", true);
        report.addTab("month", "Per Month", false);
        report.addTab("week", "Per Week", false);
        report.endTabGroup();

        Collections.sort(contributorProjects.getProjects(), (a, b) -> 10000 * (b.getCommits30Days() - a.getCommits30Days()) +
                100 * (b.getCommits90Days() - a.getCommits90Days()) +
                (b.getCommitsCount() - a.getCommitsCount()));

        report.startTabContentSection("week", false);
        addPerWeek(contributorProjects, report);
        report.endTabContentSection();

        report.startTabContentSection("month", false);
        addPerMonth(contributorProjects, report);
        report.endTabContentSection();

        report.startTabContentSection("year", true);
        addPerYear(contributorProjects, report);
        report.endTabContentSection();

        return report;
    }


    private void addPerWeek(ContributorProjects contributorProjects, RichTextReport report) {

        report.startDiv("width: 100%; overflow-x: scroll;");
        report.startTable();

        final List<String> pastWeeks = DateUtils.getPastWeeks(104, landscapeAnalysisResults.getLatestCommitDate());
        report.startTableRow();
        report.addTableCell("", "min-width: 300px; border: none");
        report.addTableCell("Commits<br>(3m)", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("Commit<br>Days", "max-width: 100px; text-align: center; border: none");
        List<ContributorProjectInfo> projects = new ArrayList<>(contributorProjects.getProjects());
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

        Collections.sort(activeProjects, (a, b) -> b.getLatestCommitDate().compareTo(a.getLatestCommitDate()));

        activeProjects.forEach(project -> {
            String textOpacity = project.getCommits90Days() > 0 ? "font-weight: bold;" : "opacity: 0.4";
            report.startTableRow();
            report.startTableCell("border: none;" + textOpacity);
            report.addNewTabLink(project.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName(),
                    "../../" + project.getProjectAnalysisResults().getSokratesProjectLink().getHtmlReportsRoot() + "/index.html");
            report.endTableCell();
            report.addTableCell(project.getCommits90Days() > 0 ? project.getCommits90Days() + "" : "-", "text-align: center; border: none; " + textOpacity);
            report.addTableCell(project.getCommitDates().size() + "", "text-align: center; border: none; " + textOpacity);
            int index[] = {0};
            pastWeeks.forEach(pastWeek -> {
                int daysCount[] = {0};
                index[0] += 1;
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
                    String opacity = "" + Math.max(0.9 - (index[0] - 1) * 0.05, 0.2);
                    report.addContentInDivWithTooltip("", tooltip,
                            "display: inline-block; padding: 0; margin: 0; " +
                                    "background-color: #483D8B; border-radius: 50%; width: " + size + "px; height: " + size + "px; opacity: " + opacity + ";");
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
        report.addTableCell("Commits<br>(3m)", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("Commit<br>Days", "max-width: 100px; text-align: center; border: none");
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
        Collections.sort(projects, (a, b) -> b.getLatestCommitDate().compareTo(a.getLatestCommitDate()));

        projects.forEach(project -> {
            report.startTableRow();
            String textOpacity = project.getCommits90Days() > 0 ? "font-weight: bold;" : "opacity: 0.4";
            report.startTableCell("border: none; " + textOpacity);
            report.addNewTabLink(project.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName(),
                    "../../" + project.getProjectAnalysisResults().getSokratesProjectLink().getHtmlReportsRoot() + "/index.html");
            report.endTableCell();
            report.addTableCell(project.getCommits90Days() > 0 ? project.getCommits90Days() + "" : "-", "text-align: center; border: none; " + textOpacity);
            report.addTableCell(project.getCommitDates().size() + "", "text-align: center; border: none; " + textOpacity);
            int index[] = {0};
            pastMonths.forEach(pastMonth -> {
                int count[] = {0};
                project.getCommitDates().forEach(date -> {
                    String month = DateUtils.getMonth(date);
                    if (month.equals(pastMonth)) {
                        count[0] += 1;
                    }
                });
                index[0] += 1;
                report.startTableCell("text-align: center; padding: 0; border: none; vertical-align: middle;");
                if (count[0] > 0) {
                    int size = 10 + (count[0] / 4) * 4;
                    String tooltip = "Month " + pastMonth + ": " + count[0] + (count[0] == 1 ? " commit day" : " commit days");
                    String opacity = "" + Math.max(0.9 - (index[0] - 1) * 0.2, 0.2);
                    report.addContentInDivWithTooltip("", tooltip,
                            "padding: 0; margin: 0; display: inline-block; background-color: #483D8B; opacity: " + opacity + "; border-radius: 50%; width: " + size + "px; height: " + size + "px;");
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

    private void addPerYear(ContributorProjects contributorProjects, RichTextReport report) {
        report.startDiv("width: 100%; overflow-x: scroll;");
        report.startTable();

        final List<String> pastYears = DateUtils.getPastYears(landscapeAnalysisResults.getConfiguration().getCommitsMaxYears(), landscapeAnalysisResults.getLatestCommitDate());
        report.startTableRow();
        report.addTableCell("", "min-width: 200px; border: none; border: none");
        report.addTableCell("Commits<br>(3m)", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("Commit<br>Days", "max-width: 100px; text-align: center; border: none");
        int maxProjectDays[] = {1};
        pastYears.forEach(pastYear -> {
            int projectCount[] = {0};
            int projectDays[] = {0};
            contributorProjects.getProjects().forEach(project -> {
                boolean found[] = {false};
                project.getCommitDates().forEach(date -> {
                    String year = DateUtils.getYear(date);
                    if (year.equals(pastYear)) {
                        found[0] = true;
                        return;
                    }
                });
                if (found[0]) {
                    projectCount[0] += 1;
                    projectDays[0] += project.getCommitDates().stream().filter(date -> date.startsWith(pastYear + "-")).count();
                    return;
                }
            });

            maxProjectDays[0] = Math.max(projectDays[0], maxProjectDays[0]);
        });
        pastYears.forEach(pastYear -> {
            int projectCount[] = {0};
            int projectDays[] = {0};
            contributorProjects.getProjects().forEach(project -> {
                boolean found[] = {false};
                project.getCommitDates().forEach(date -> {
                    String year = DateUtils.getYear(date);
                    if (year.equals(pastYear)) {
                        found[0] = true;
                        return;
                    }
                });
                if (found[0]) {
                    projectCount[0] += 1;
                    projectDays[0] += project.getCommitDates().stream().filter(date -> date.startsWith(pastYear + "-")).count();
                    return;
                }
            });
            String tooltip = "Month " + pastYear + ": " + projectCount[0] + (projectCount[0] == 1 ? " project" : " projects"
                    + ", " + projectDays[0] + " commit " + (projectDays[0] == 1 ? "day" : "days"));
            report.startTableCell("vertical-align: bottom; font-size: 70%; border: none; color: lightgrey; text-align: center");
            String content = projectCount[0] + "&nbsp;p<br>" + projectDays[0] + "&nbsp;cd";
            content += "<div style='vertical-align: bottom; text-align: center; margin: auto; background-color: skyblue; width: 32px; height: "
                    + ((int) (1 + 32 * ((double) projectDays[0] / maxProjectDays[0]))) +
                    "px;'> </div>" + pastYear;
            report.addContentInDivWithTooltip(content, tooltip, "text-align: center");
            report.endTableCell();

        });
        report.endTableRow();
        List<ContributorProjectInfo> projects = new ArrayList<>(contributorProjects.getProjects());
        Collections.sort(projects, (a, b) -> b.getLatestCommitDate().compareTo(a.getLatestCommitDate()));

        projects.forEach(project -> {
            report.startTableRow();
            String textOpacity = project.getCommits90Days() > 0 ? "font-weight: bold;" : "opacity: 0.4";
            report.startTableCell("border: none; " + textOpacity);
            report.addNewTabLink(project.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName(),
                    "../../" + project.getProjectAnalysisResults().getSokratesProjectLink().getHtmlReportsRoot() + "/index.html");
            report.endTableCell();
            report.addTableCell(project.getCommits90Days() > 0 ? project.getCommits90Days() + "" : "-", "text-align: center; border: none; " + textOpacity);
            report.addTableCell(project.getCommitDates().size() + "", "text-align: center; border: none; " + textOpacity);
            int index[] = {0};
            pastYears.forEach(pastYear -> {
                int count[] = {0};
                project.getCommitDates().forEach(date -> {
                    String year = DateUtils.getYear(date);
                    if (year.equals(pastYear)) {
                        count[0] += 1;
                    }
                });
                index[0] += 1;
                report.startTableCell("text-align: center; padding: 0; border: none; vertical-align: middle;");
                if (count[0] > 0) {
                    int size = (int) (10 + Math.min(1, (count[0] / 366.0)) * 40);
                    String tooltip = "Year " + pastYear + ": " + count[0] + (count[0] == 1 ? " commit day" : " commit days");
                    String opacity = "" + Math.max(0.9 - (index[0] - 1) * 0.2, 0.2);
                    report.addContentInDivWithTooltip("", tooltip,
                            "padding: 0; margin: 0; display: inline-block; background-color: #483D8B; opacity: " + opacity + "; border-radius: 50%; width: " + size + "px; height: " + size + "px;");
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

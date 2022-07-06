package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.ContributorPerExtensionHelper;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.ContributorPerExtensionStats;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositoryInfo;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LandscapeIndividualContributorsReports {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private List<RichTextReport> reports = new ArrayList<>();

    public LandscapeIndividualContributorsReports(LandscapeAnalysisResults landscapeAnalysisResults) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
    }

    public static String getContributorIndividualReportFileName(String email) {
        return SystemUtils.getSafeFileName(email).toLowerCase() + ".html";
    }

    public List<RichTextReport> getIndividualReports(List<ContributorRepositories> contributors) {
        contributors.forEach(contributor -> {
            reports.add(getIndividualReport(contributor));
        });

        return reports;
    }

    private RichTextReport getIndividualReport(ContributorRepositories contributorRepositories) {
        Contributor contributor = contributorRepositories.getContributor();
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
        report.addContentInDiv("Repositories count: " +
                "<b>" + contributorRepositories.getRepositories().stream().filter(p -> p.getCommits30Days() > 0).count()
                + "</b><span style='color: lightgrey; font-size: 90%'> (30d)</span>&nbsp;&nbsp;&nbsp;" + "<b>" + contributorRepositories.getRepositories().stream().filter(p -> p.getCommits90Days() > 0).count()
                + "</b><span style='color: lightgrey; font-size: 90%'> (3m)</span>&nbsp;&nbsp;&nbsp;" +
                "<b>" + contributorRepositories.getRepositories().size() + "</b> <span style='color: lightgrey; font-size: 90%'> (all time)</span>");
        report.addContentInDiv("Commits count: <b>" + contributor.getCommitsCount30Days() + "</b> " +
                "<span style='color: lightgrey; font-size: 90%'>(30d)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount90Days() + "</b><span style='color: lightgrey; font-size: 90%'> (3m)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount180Days() + "</b><span style='color: lightgrey; font-size: 90%'> (6m)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount365Days() + "</b><span style='color: lightgrey; font-size: 90%'> (1y)&nbsp;&nbsp;&nbsp;</span>" +
                "<b>" + contributor.getCommitsCount() + "</b><span style='color: lightgrey; font-size: 90%'> (all time)</span>"
        );
        report.endDiv();

        ContributorPerExtensionHelper helper = new ContributorPerExtensionHelper();

        List<Pair<String, ContributorPerExtensionStats>> extensionUpdates = helper.getContributorStatsPerExtension(landscapeAnalysisResults.getConfiguration(), contributorRepositories);

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
        report.addTab("year", "Repository Activity Per Year", true);
        report.addTab("month", "Per Month", false);
        report.addTab("week", "Per Week", false);
        report.endTabGroup();

        Collections.sort(contributorRepositories.getRepositories(), (a, b) -> 10000 * (b.getCommits30Days() - a.getCommits30Days()) +
                100 * (b.getCommits90Days() - a.getCommits90Days()) +
                (b.getCommitsCount() - a.getCommitsCount()));

        report.startTabContentSection("week", false);
        addPerWeek(contributorRepositories, report);
        report.endTabContentSection();

        report.startTabContentSection("month", false);
        addPerMonth(contributorRepositories, report);
        report.endTabContentSection();

        report.startTabContentSection("year", true);
        addPerYear(contributorRepositories, report);
        report.endTabContentSection();

        return report;
    }


    private void addPerWeek(ContributorRepositories contributorRepositories, RichTextReport report) {

        report.startDiv("width: 100%; overflow-x: scroll;");
        report.startTable();

        final List<String> pastWeeks = DateUtils.getPastWeeks(104, landscapeAnalysisResults.getLatestCommitDate());
        report.startTableRow();
        report.addTableCell("", "min-width: 300px; border: none");
        report.addTableCell("Commits<br>(3m)", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("Commit<br>Days", "max-width: 100px; text-align: center; border: none");
        List<ContributorRepositoryInfo> repositories = new ArrayList<>(contributorRepositories.getRepositories());
        pastWeeks.forEach(pastWeek -> {
            int repositoryCount[] = {0};
            repositories.forEach(repository -> {
                boolean found[] = {false};
                repository.getCommitDates().forEach(date -> {
                    String weekMonday = DateUtils.getWeekMonday(date);
                    if (weekMonday.equals(pastWeek)) {
                        found[0] = true;
                        return;
                    }
                });
                if (found[0]) {
                    repositoryCount[0] += 1;
                    return;
                }
            });
            String tooltip = "Week of " + pastWeek + ": " + repositoryCount[0] + (repositoryCount[0] == 1 ? " repository" : " repositories");
            report.startTableCell("font-size: 70%; border: none; color: lightgrey; text-align: center");
            report.addContentInDivWithTooltip(repositoryCount[0] + "", tooltip, "text-align: center");
            report.endTableCell();
        });
        report.endTableRow();

        List<ContributorRepositoryInfo> activeRepositories = new ArrayList<>();

        repositories.forEach(repository -> {
            int daysCount[] = {0};
            pastWeeks.forEach(pastWeek -> {
                repository.getCommitDates().forEach(date -> {
                    String weekMonday = DateUtils.getWeekMonday(date);
                    if (weekMonday.equals(pastWeek)) {
                        daysCount[0] += 1;
                    }
                });

            });
            if (daysCount[0] > 0) {
                activeRepositories.add(repository);
            }
        });

        Collections.sort(activeRepositories, (a, b) -> b.getLatestCommitDate().compareTo(a.getLatestCommitDate()));

        activeRepositories.forEach(repository -> {
            String textOpacity = repository.getCommits90Days() > 0 ? "font-weight: bold;" : "opacity: 0.4";
            report.startTableRow();
            report.startTableCell("border: none;" + textOpacity);
            report.addNewTabLink(repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName(),
                    "../../" + repository.getRepositoryAnalysisResults().getSokratesRepositoryLink().getHtmlReportsRoot() + "/index.html");
            report.endTableCell();
            report.addTableCell(repository.getCommits90Days() > 0 ? repository.getCommits90Days() + "" : "-", "text-align: center; border: none; " + textOpacity);
            report.addTableCell(repository.getCommitDates().size() + "", "text-align: center; border: none; " + textOpacity);
            int index[] = {0};
            pastWeeks.forEach(pastWeek -> {
                int daysCount[] = {0};
                index[0] += 1;
                repository.getCommitDates().forEach(date -> {
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

    private void addPerMonth(ContributorRepositories contributorRepositories, RichTextReport report) {
        report.startDiv("width: 100%; overflow-x: scroll;");
        report.startTable();

        final List<String> pastMonths = DateUtils.getPastMonths(24, landscapeAnalysisResults.getLatestCommitDate());
        report.startTableRow();
        report.addTableCell("", "min-width: 200px; border: none; border: none");
        report.addTableCell("Commits<br>(3m)", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("Commit<br>Days", "max-width: 100px; text-align: center; border: none");
        pastMonths.forEach(pastMonth -> {
            int repositoryCount[] = {0};
            contributorRepositories.getRepositories().forEach(repository -> {
                boolean found[] = {false};
                repository.getCommitDates().forEach(date -> {
                    String weekMonday = DateUtils.getMonth(date);
                    if (weekMonday.equals(pastMonth)) {
                        found[0] = true;
                        return;
                    }
                });
                if (found[0]) {
                    repositoryCount[0] += 1;
                    return;
                }
            });
            String tooltip = "Month " + pastMonth + ": " + repositoryCount[0] + (repositoryCount[0] == 1 ? " repository" : " repositories");
            report.startTableCell("font-size: 70%; border: none; color: lightgrey; text-align: center");
            report.addContentInDivWithTooltip(repositoryCount[0] + "", tooltip, "text-align: center");
            report.endTableCell();
        });
        report.endTableRow();
        List<ContributorRepositoryInfo> repositories = new ArrayList<>(contributorRepositories.getRepositories());
        Collections.sort(repositories, (a, b) -> b.getLatestCommitDate().compareTo(a.getLatestCommitDate()));

        repositories.forEach(repository -> {
            report.startTableRow();
            String textOpacity = repository.getCommits90Days() > 0 ? "font-weight: bold;" : "opacity: 0.4";
            report.startTableCell("border: none; " + textOpacity);
            report.addNewTabLink(repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName(),
                    "../../" + repository.getRepositoryAnalysisResults().getSokratesRepositoryLink().getHtmlReportsRoot() + "/index.html");
            report.endTableCell();
            report.addTableCell(repository.getCommits90Days() > 0 ? repository.getCommits90Days() + "" : "-", "text-align: center; border: none; " + textOpacity);
            report.addTableCell(repository.getCommitDates().size() + "", "text-align: center; border: none; " + textOpacity);
            int index[] = {0};
            pastMonths.forEach(pastMonth -> {
                int count[] = {0};
                repository.getCommitDates().forEach(date -> {
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

    private void addPerYear(ContributorRepositories contributorRepositories, RichTextReport report) {
        report.startDiv("width: 100%; overflow-x: scroll;");
        report.startTable();

        final List<String> pastYears = DateUtils.getPastYears(landscapeAnalysisResults.getConfiguration().getCommitsMaxYears(), landscapeAnalysisResults.getLatestCommitDate());
        report.startTableRow();
        report.addTableCell("", "min-width: 200px; border: none; border: none");
        report.addTableCell("Commits<br>(3m)", "max-width: 100px; text-align: center; border: none");
        report.addTableCell("Commit<br>Days", "max-width: 100px; text-align: center; border: none");
        int maxRepositoryDays[] = {1};
        pastYears.forEach(pastYear -> {
            int repositoryCount[] = {0};
            int repositoryDays[] = {0};
            contributorRepositories.getRepositories().forEach(repository -> {
                boolean found[] = {false};
                repository.getCommitDates().forEach(date -> {
                    String year = DateUtils.getYear(date);
                    if (year.equals(pastYear)) {
                        found[0] = true;
                        return;
                    }
                });
                if (found[0]) {
                    repositoryCount[0] += 1;
                    repositoryDays[0] += repository.getCommitDates().stream().filter(date -> date.startsWith(pastYear + "-")).count();
                    return;
                }
            });

            maxRepositoryDays[0] = Math.max(repositoryDays[0], maxRepositoryDays[0]);
        });
        pastYears.forEach(pastYear -> {
            int repositoryCount[] = {0};
            int repositoryDays[] = {0};
            contributorRepositories.getRepositories().forEach(repository -> {
                boolean found[] = {false};
                repository.getCommitDates().forEach(date -> {
                    String year = DateUtils.getYear(date);
                    if (year.equals(pastYear)) {
                        found[0] = true;
                        return;
                    }
                });
                if (found[0]) {
                    repositoryCount[0] += 1;
                    repositoryDays[0] += repository.getCommitDates().stream().filter(date -> date.startsWith(pastYear + "-")).count();
                    return;
                }
            });
            String tooltip = "Month " + pastYear + ": " + repositoryCount[0] + (repositoryCount[0] == 1 ? " repository" : " repositories"
                    + ", " + repositoryDays[0] + " commit " + (repositoryDays[0] == 1 ? "day" : "days"));
            report.startTableCell("vertical-align: bottom; font-size: 70%; border: none; color: lightgrey; text-align: center");
            String content = repositoryCount[0] + "&nbsp;p<br>" + repositoryDays[0] + "&nbsp;cd";
            content += "<div style='vertical-align: bottom; text-align: center; margin: auto; background-color: skyblue; width: 32px; height: "
                    + ((int) (1 + 32 * ((double) repositoryDays[0] / maxRepositoryDays[0]))) +
                    "px;'> </div>" + pastYear;
            report.addContentInDivWithTooltip(content, tooltip, "text-align: center");
            report.endTableCell();

        });
        report.endTableRow();
        List<ContributorRepositoryInfo> repositories = new ArrayList<>(contributorRepositories.getRepositories());
        Collections.sort(repositories, (a, b) -> b.getLatestCommitDate().compareTo(a.getLatestCommitDate()));

        repositories.forEach(repository -> {
            report.startTableRow();
            String textOpacity = repository.getCommits90Days() > 0 ? "font-weight: bold;" : "opacity: 0.4";
            report.startTableCell("border: none; " + textOpacity);
            report.addNewTabLink(repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName(),
                    "../../" + repository.getRepositoryAnalysisResults().getSokratesRepositoryLink().getHtmlReportsRoot() + "/index.html");
            report.endTableCell();
            report.addTableCell(repository.getCommits90Days() > 0 ? repository.getCommits90Days() + "" : "-", "text-align: center; border: none; " + textOpacity);
            report.addTableCell(repository.getCommitDates().size() + "", "text-align: center; border: none; " + textOpacity);
            int index[] = {0};
            pastYears.forEach(pastYear -> {
                int count[] = {0};
                repository.getCommitDates().forEach(date -> {
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

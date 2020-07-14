package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProject;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LandscapeDataExport {
    private LandscapeAnalysisResults analysisResults;
    private File folder;
    private File dataFolder;

    public LandscapeDataExport(LandscapeAnalysisResults analysisResults, File folder) {
        this.analysisResults = analysisResults;
        this.folder = folder;
        folder.mkdirs();
        dataFolder = new File(folder, "data");
        dataFolder.mkdirs();
    }

    public void exportProjects() {
        StringBuilder builder = new StringBuilder();

        builder.append("Project\tMain Lanuguage\tLOC (main)\tLOC (test)\tLOC (other)\tAge (years)\tContributors\tRecent Contributors\tRookies\tCommits this year\n");

        int thresholdCommits = analysisResults.getConfiguration().getContributorThresholdCommits();

        analysisResults.getProjectAnalysisResults().forEach(project -> {
            CodeAnalysisResults projectAnalysis = project.getAnalysisResults();
            builder.append(projectAnalysis.getMetadata().getName()).append("\t");
            AspectAnalysisResults main = projectAnalysis.getMainAspectAnalysisResults();

            List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
            StringBuilder locSummary = new StringBuilder();
            if (linesOfCodePerExtension.size() > 0) {
                locSummary.append(linesOfCodePerExtension.get(0).getName().replace("*.", "").trim().toUpperCase());
            } else {
                locSummary.append("-");
            }
            builder.append(locSummary.toString().replace("> = ", ">")).append("\t");
            builder.append(main.getLinesOfCode()).append("\t");
            builder.append(projectAnalysis.getTestAspectAnalysisResults().getLinesOfCode()).append("\t");
            builder.append(projectAnalysis.getGeneratedAspectAnalysisResults().getLinesOfCode()
                    + projectAnalysis.getBuildAndDeployAspectAnalysisResults().getLinesOfCode()
                    + projectAnalysis.getOtherAspectAnalysisResults().getLinesOfCode()).append("\t");

            int projectAgeYears = (int) Math.round(projectAnalysis.getFilesHistoryAnalysisResults().getAgeInDays() / 365.0);
            builder.append(projectAgeYears).append("\t");

            List<Contributor> contributors = projectAnalysis.getContributorsAnalysisResults().getContributors()
                    .stream().filter(c -> c.getCommitsCount() >= thresholdCommits).collect(Collectors.toCollection(ArrayList::new));

            int contributorsCount = contributors.size();
            int recentContributorsCount = (int) contributors.stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
            int rookiesCount = (int) contributors.stream().filter(c -> c.isRookie(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
            builder.append(contributorsCount).append("\t");
            builder.append(recentContributorsCount).append("\t");
            builder.append(rookiesCount).append("\t");
            builder.append(projectAnalysis.getContributorsAnalysisResults().getCommitsThisYear());
            builder.append("\n");
        });

        try {
            FileUtils.write(new File(dataFolder, "projects.txt"), builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void exportContributors() {
        StringBuilder builder = new StringBuilder();

        builder.append("Contributor\t# commits\tFirst commit\tLatest commit\tProjects\n");

        List<ContributorProject> contributors = analysisResults.getContributors();

        contributors.forEach(contributor -> {
            builder.append(contributor.getContributor().getDisplayName()).append("\t");
            int contributerCommits = contributor.getContributor().getCommitsCount();
            builder.append(contributerCommits).append("\t");
            builder.append(contributor.getContributor().getFirstCommitDate()).append("\t");
            builder.append(contributor.getContributor().getLatestCommitDate()).append("\t");
            builder.append(contributor.getProjects().stream().map(p -> p.getAnalysisResults().getMetadata().getName()).collect(Collectors.joining(", ")));
            builder.append("\n");
        });
        try {
            FileUtils.write(new File(dataFolder, "contributors.txt"), builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

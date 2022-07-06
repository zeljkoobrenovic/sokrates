package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator;
import nl.obren.sokrates.reports.landscape.statichtml.repositories.TagMap;
import nl.obren.sokrates.reports.landscape.utils.TagStats;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class LandscapeDataExport {
    public static final String REPOSITORIES_DATA_FILE_NAME = "repositories.txt";
    private LandscapeAnalysisResults analysisResults;
    private File dataFolder;

    public LandscapeDataExport(LandscapeAnalysisResults analysisResults, File folder) {
        this.analysisResults = analysisResults;
        folder.mkdirs();
        dataFolder = new File(folder, "data");
        dataFolder.mkdirs();
    }

    public void exportRepositories(TagMap tagMap) {
        exportRepositories(analysisResults.getFilteredRepositoryAnalysisResults(), REPOSITORIES_DATA_FILE_NAME);
        tagMap.keySet().forEach(key -> {
            TagStats tagStats = tagMap.getTagStats(key);
            exportRepositories(tagStats.getRepositoryAnalysisResults(), getTagRepositoriesFileName(key));
        });
        exportRepositories(analysisResults.getIgnoredRepositoryAnalysisResults(), "ignoredRepositories.txt");
    }

    public static String getTagRepositoriesFileName(String key) {
        return "tag_" + SystemUtils.getSafeFileName(key) + "_repositories.txt";
    }

    private void exportRepositories(List<RepositoryAnalysisResults> repositories, String fileName) {
        StringBuilder builder = new StringBuilder();

        builder.append("Repository\tMain Lanuguage\tLOC (main)\tLOC (test)\tLOC (other)\tAge (years)\tContributors\tRecent Contributors\tRookies\tCommits this year\tLatest commit\n");

        repositories.forEach(repository -> {
            appendRepository(builder, repository);
        });

        try {
            FileUtils.write(new File(dataFolder, fileName), builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendRepository(StringBuilder builder, RepositoryAnalysisResults repository) {
        CodeAnalysisResults repositoryAnalysis = repository.getAnalysisResults();
        builder.append(repositoryAnalysis.getMetadata().getName()).append("\t");
        AspectAnalysisResults main = repositoryAnalysis.getMainAspectAnalysisResults();

        List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
        StringBuilder locSummary = new StringBuilder();
        if (linesOfCodePerExtension.size() > 0) {
            locSummary.append(linesOfCodePerExtension.get(0).getName().replace("*.", "").trim().toUpperCase());
        } else {
            locSummary.append("-");
        }
        builder.append(locSummary.toString().replace("> = ", ">")).append("\t");
        builder.append(main.getLinesOfCode()).append("\t");
        builder.append(repositoryAnalysis.getTestAspectAnalysisResults().getLinesOfCode()).append("\t");
        builder.append(repositoryAnalysis.getGeneratedAspectAnalysisResults().getLinesOfCode()
                + repositoryAnalysis.getBuildAndDeployAspectAnalysisResults().getLinesOfCode()
                + repositoryAnalysis.getOtherAspectAnalysisResults().getLinesOfCode()).append("\t");

        int repositoryAgeYears = (int) Math.round(repositoryAnalysis.getFilesHistoryAnalysisResults().getAgeInDays() / 365.0);
        builder.append(repositoryAgeYears).append("\t");

        List<Contributor> contributors = repositoryAnalysis.getContributorsAnalysisResults().getContributors();

        int contributorsCount = contributors.size();
        int recentContributorsCount = (int) contributors.stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
        int rookiesCount = (int) contributors.stream().filter(c -> c.isRookie(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
        builder.append(contributorsCount).append("\t");
        builder.append(recentContributorsCount).append("\t");
        builder.append(rookiesCount).append("\t");
        builder.append(repositoryAnalysis.getContributorsAnalysisResults().getCommitsThisYear()).append("\t");
        builder.append(repositoryAnalysis.getContributorsAnalysisResults().getLatestCommitDate());
        builder.append("\n");
    }


    public void exportContributors() {
        StringBuilder builder = new StringBuilder();

        builder.append("Contributor\t# commits (all time)\t# commits (30 days)\t# commits (90 days)\t# commits (180 days)\t# commits (365 days)\tFirst commit\tLatest commit\tRepositories\n");

        List<ContributorRepositories> contributors = analysisResults.getContributors();

        contributors.forEach(contributor -> {
            builder.append(contributor.getContributor().getEmail()).append("\t");
            int contributorCommits = contributor.getContributor().getCommitsCount();
            int contributorCommits30Days = contributor.getContributor().getCommitsCount30Days();
            int contributorCommits90Days = contributor.getContributor().getCommitsCount90Days();
            int contributorCommits180Days = contributor.getContributor().getCommitsCount180Days();
            int contributorCommits365Days = contributor.getContributor().getCommitsCount365Days();
            builder.append(contributorCommits).append("\t");
            builder.append(contributorCommits30Days).append("\t");
            builder.append(contributorCommits90Days).append("\t");
            builder.append(contributorCommits180Days).append("\t");
            builder.append(contributorCommits365Days).append("\t");
            builder.append(contributor.getContributor().getFirstCommitDate()).append("\t");
            builder.append(contributor.getContributor().getLatestCommitDate()).append("\t");
            builder.append(contributor.getRepositories().stream().map(p -> p.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName()).collect(Collectors.joining(", ")));
            builder.append("\n");
        });

        try {
            FileUtils.write(new File(dataFolder, "contributors.txt"), builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportAnalysisResults() {
        try {
            String analysisResultsJson = new JsonGenerator().generate(analysisResults);
            FileUtils.write(new File(dataFolder, "landscapeAnalysisResults.json"), analysisResultsJson, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

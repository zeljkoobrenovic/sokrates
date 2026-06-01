package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator;
import nl.obren.sokrates.reports.generators.explorers.FilesExportUtils;
import nl.obren.sokrates.reports.landscape.statichtml.repositories.TagMap;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import nl.obren.sokrates.reports.landscape.utils.ContributorPerExtensionHelper;
import nl.obren.sokrates.reports.landscape.utils.FeaturesOfInterestAggregator;
import nl.obren.sokrates.reports.landscape.utils.RepositoryConcernData;
import nl.obren.sokrates.reports.landscape.utils.TagStats;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.TeamsConfig;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.FileExport;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LandscapeDataExport {
    public static final String REPOSITORIES_DATA_FILE_NAME = "repositories.txt";
    public static final String REPOSITORIES_DATA_JSON_FILE_NAME = "repositories.json";
    private LandscapeAnalysisResults analysisResults;
    private File dataFolder;
    private File reportsFolder;

    public LandscapeDataExport(LandscapeAnalysisResults analysisResults, File folder) {
        this.analysisResults = analysisResults;
        folder.mkdirs();
        dataFolder = new File(folder, "data");
        dataFolder.mkdirs();

        this.reportsFolder = folder;
    }

    public void exportRepositories(TagMap tagMap) {
        exportJson(tagMap);

        exportRepositories(analysisResults.getFilteredRepositoryAnalysisResults(), REPOSITORIES_DATA_FILE_NAME);
        tagMap.keySet().forEach(key -> {
            TagStats tagStats = tagMap.getTagStats(key);
            exportRepositories(tagStats.getRepositoryAnalysisResults(), getTagRepositoriesFileName(key));
        });
        exportRepositories(analysisResults.getIgnoredRepositoryAnalysisResults(), "ignoredRepositories.txt");
    }

    private void exportJson(TagMap tagMap) {
        try {
            LandscapeConfiguration configuration = analysisResults.getConfiguration();
            String latestCommitDate = analysisResults.getLatestCommitDate();
            List<RepositoryExport> repositoryExports = new ArrayList<>();
            List<FileExport> files = new ArrayList<>();

            analysisResults.getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
                RepositoryExport repositoryExport = new RepositoryExport(repositoryAnalysisResults, tagMap, configuration, latestCommitDate);
                repositoryExports.add(repositoryExport);
                repositoryAnalysisResults.getFiles().forEach(file -> {
                    if (!files.contains(file) && !file.getPath().startsWith("- -")) {
                        files.add(file);
                    }
                });
            });


            FileUtils.write(new File(dataFolder, REPOSITORIES_DATA_JSON_FILE_NAME), new JsonGenerator().generate(repositoryExports), UTF_8);
            ExplorerTemplate explorerTemplate = new ExplorerTemplate();

            FileUtils.write(new File(dataFolder, "files.json"), new JsonGenerator().generate(files), UTF_8);

            // The repositories explorer has been folded into the Size & Details tab of
            // repositories.html; we only keep the shared language-icon map here.
            List<String> repositoryLangs = repositoryExports.stream().map(RepositoryExport::getMainLang).collect(Collectors.toList());
            String repositoryLangIcons = DataImageUtils.getLangDataImageMapJson(repositoryLangs);

            List<String> fileLangs = files.stream().map(FileExport::getMainLang).collect(Collectors.toList());
            String fileLangIcons = DataImageUtils.getLangDataImageMapJson(fileLangs);
            Map<String, String> filesPlaceholders = new HashMap<>();
            filesPlaceholders.put("langIcons", fileLangIcons);
            // The landscape aggregates many repositories with potentially different thresholds, so use
            // the standard thresholds for the risk colouring.
            filesPlaceholders.put("fileSizeThresholds",
                    FilesExportUtils.thresholdsJson(Thresholds.defaultFileSizeThresholds()));
            filesPlaceholders.put("fileAgeThresholds",
                    FilesExportUtils.thresholdsJson(Thresholds.defaultFileAgeThresholds()));
            filesPlaceholders.put("fileUpdateFrequencyThresholds",
                    FilesExportUtils.thresholdsJson(Thresholds.defaultFileUpdateFrequencyThresholds()));
            filesPlaceholders.put("fileContributorsCountThresholds",
                    FilesExportUtils.thresholdsJson(Thresholds.defaultFileContributorsCountThresholds()));
            // No per-repository reports or cached source pages at the landscape level, so suppress
            // both the report links and the file-name source links.
            filesPlaceholders.put("reportLinkBase", "");
            filesPlaceholders.put("saveSourceFiles", "false");
            String filesExplorer = explorerTemplate.render("files-explorer.html", files, filesPlaceholders);
            FileUtils.write(new File(reportsFolder, "files-explorer.html"), filesExplorer, UTF_8);

            // The full, client-rendered repositories report (replaces the old static repositories.html).
            JsonGenerator jsonGenerator = new JsonGenerator();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("langIcons", repositoryLangIcons);
            placeholders.put("features", jsonGenerator.generateCompressed(buildFeaturesData(configuration)));
            placeholders.put("options", jsonGenerator.generateCompressed(buildOptions(configuration, repositoryExports)));
            String repositoriesReport = explorerTemplate.render("repositories-report.html", repositoryExports, placeholders);
            FileUtils.write(new File(reportsFolder, "repositories.html"), repositoriesReport, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> buildOptions(LandscapeConfiguration configuration, List<RepositoryExport> repositoryExports) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("showCommits", analysisResults.getCommitsCount() > 0);
        options.put("showTags", repositoryExports.stream().anyMatch(r -> r.getTags() != null && !r.getTags().isEmpty()));
        options.put("showControls", configuration.isShowRepositoryControls());
        options.put("historyYears", configuration.getRepositoriesHistoryLimit());
        return options;
    }

    /**
     * Aggregates features of interest across repositories into a compact matrix
     * (concerns + per-repository match/file counts) for the report's Features tab.
     */
    private Map<String, Object> buildFeaturesData(LandscapeConfiguration configuration) {
        FeaturesOfInterestAggregator aggregator = new FeaturesOfInterestAggregator(analysisResults.getRepositoryAnalysisResults());
        aggregator.aggregateFeaturesOfInterest(configuration.getRepositoriesListLimit());

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> concernsOut = new ArrayList<>();
        List<List<RepositoryConcernData>> concerns = aggregator.getConcerns().stream()
                .filter(c -> c.size() > 0).collect(Collectors.toList());
        concerns.forEach(concern -> {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("name", concern.get(0).getConcern().getName());
            c.put("count", concern.size());
            concernsOut.add(c);
        });
        result.put("concerns", concernsOut);

        List<Map<String, Object>> reposOut = new ArrayList<>();
        aggregator.getRepositories().forEach(repository -> {
            RepositoryConcernData first = repository.get(0);
            RepositoryAnalysisResults repo = first.getRepository();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("name", first.getRepositoryName());
            r.put("mainLang", mainLangOf(repo));
            r.put("reportFolderUrl", configuration.getRepositoryReportsUrlPrefix()
                    + repo.getSokratesRepositoryLink().getHtmlReportsRoot() + "/");
            Map<String, Object> perConcern = new LinkedHashMap<>();
            concerns.forEach(concern -> {
                String concernName = concern.get(0).getConcern().getName();
                String key = first.getRepositoryName() + "::" + concernName;
                RepositoryConcernData data = aggregator.getRepositoriesConcernMap().get(key);
                if (data != null) {
                    Map<String, Object> cell = new LinkedHashMap<>();
                    cell.put("matches", data.getConcern().getNumberOfRegexLineMatches());
                    cell.put("files", data.getConcern().getFilesCount());
                    perConcern.put(concernName, cell);
                }
            });
            r.put("perConcern", perConcern);
            reposOut.add(r);
        });
        result.put("repositories", reposOut);
        return result;
    }

    private static String mainLangOf(RepositoryAnalysisResults repo) {
        List<NumericMetric> perExtension = repo.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension();
        if (perExtension != null && !perExtension.isEmpty()) {
            return perExtension.get(0).getName().replace("*.", "").trim().toLowerCase();
        }
        return "";
    }

    public static String getTagRepositoriesFileName(String key) {
        return "tag_" + SystemUtils.getSafeFileName(key) + "_repositories.txt";
    }

    private void exportRepositories(List<RepositoryAnalysisResults> repositories, String fileName) {
        StringBuilder builder = new StringBuilder();

        builder.append("Repository\tMain language\tLOC (main)\tLOC (test)\tLOC (other)\tAge (years)\tContributors\tRecent contributors\tRookies\tCommits this year\tLatest commit\n");

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
        List<ContributorExport> contributorsExport = new ArrayList<>();



        contributors.forEach(contributor -> {
            ContributorExport contributorExport = new ContributorExport(contributor);
            String mostCommittedLang = new ContributorPerExtensionHelper().getBiggestExtension(analysisResults.getConfiguration(), contributor, analysisResults.getPeopleConfig());
            contributorExport.setMainLang(mostCommittedLang);
            contributorsExport.add(contributorExport);
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
            FileUtils.write(new File(dataFolder, "contributors.json"), new JsonGenerator().generate(contributorsExport), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportTeams(TeamsConfig teamsConfig) {
        StringBuilder builder = new StringBuilder();

        builder.append("Team\t# commits (all time)\t# commits (30 days)\t# commits (90 days)\t# commits (180 days)\t# commits (365 days)\tFirst commit\tLatest commit\tRepositories\n");

        List<ContributorRepositories> teams = analysisResults.getTeams();
        List<ContributorExport> contributorsExport = new ArrayList<>();

        teams.forEach(contributor -> {
            ContributorExport contributorExport = new ContributorExport(contributor);
            String mostCommittedLang = new ContributorPerExtensionHelper().getBiggestExtension(analysisResults.getConfiguration(), contributor, analysisResults.getPeopleConfig());
            contributorExport.setMainLang(mostCommittedLang);
            contributorsExport.add(contributorExport);
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
            FileUtils.write(new File(dataFolder, "teams.txt"), builder.toString(), StandardCharsets.UTF_8);
            FileUtils.write(new File(dataFolder, "teams.json"), new JsonGenerator().generate(contributorsExport), StandardCharsets.UTF_8);
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
